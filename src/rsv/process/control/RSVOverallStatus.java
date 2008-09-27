package rsv.process.control;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeMap;
import java.util.ArrayList;
//import java.util.TreeSet;

import org.apache.log4j.Logger;

import rsv.process.model.OIMModel;
import rsv.process.model.ProcessLogModel;
import rsv.process.model.MetricDataModel;
import rsv.process.model.StatusChangeModel;
import rsv.process.TimeRange.TimePeriod;
import rsv.process.*;
import rsv.process.model.record.MetricData;
import rsv.process.model.record.Status;
import rsv.process.model.record.ServiceStatus;
import rsv.process.model.record.ResourceStatus;
import rsv.process.model.StatusChangeModel.LSCType;

class DummyMetricData extends MetricData
{
	//construct dummy metricdata with just timestamp set
	public DummyMetricData(int time) {
		timestamp = time;
	}
}


public class RSVOverallStatus implements RSVProcess {
	private static final Logger logger = Logger.getLogger(RSVOverallStatus.class);
	private OIMModel oim = new OIMModel();
	private StatusChangeModel scm = new StatusChangeModel();
	private ProcessLogModel lm = new ProcessLogModel();
	private int last_mdid;
	
	public int run() {
		int ret = RSVMain.exitcode_ok;
		ArrayList<ServiceStatus> all_service_statuschanges = new ArrayList<ServiceStatus>();
		ArrayList<ResourceStatus> all_resource_statuschanges = new ArrayList<ResourceStatus>();	
		
		try {
			
			//Step 1: Find ITP
			TreeMap<Integer, TimeRange> itps = calculateITPs();
			
			//Step 2: For each resource we found...
			for(Integer resource_id : itps.keySet()) {
				
				//debug
				//if(resource_id != 107) continue;
				
				logger.info("Processing resource ID : " + resource_id);
				
				ArrayList<ServiceStatus> service_statuschanges = new ArrayList<ServiceStatus>();
				ArrayList<ResourceStatus> resource_statuschanges = new ArrayList<ResourceStatus>();	
				TimeRange itp = itps.get(resource_id);
				ArrayList<TimePeriod> ranges = itp.getRanges();
				
				//for each ranges (currently there is only 1 range per resource, but we can improve this later)
				for(TimePeriod tp : ranges) {
					
					//A. Clear Status Change History within ITP (on all statuschange_xxx tables)
					int removed = scm.clearStatusChanges(resource_id, tp.start, tp.end);
					logger.info("Cleared " + removed + " records inside ITP of start: " + tp.start + " and end: " + tp.end);
		
					//B. Retrieve Initial Status History (all statuschange_xxx tables)
					LSCType initial_service_statuses = scm.getLastStatusChange_Service(resource_id, tp.start);
					ResourceStatus initial_resource_status = scm.getLastStatusChange_Resource(resource_id, tp.start);
					
					//C. Retrieve Initial Relevant Record Set (RRS)
					RelevantRecordSet initial_rrs = new RelevantRecordSet(resource_id, tp.start);
					
					//D1. Pull metricdata inside ITP
					MetricDataModel mdm = new MetricDataModel();
					ArrayList<MetricData> mds = mdm.getMeticData(resource_id, tp.start, tp.end);
					
					//D1b. Add blank metric to force status recalculation at tp.end
					int size = mds.size();
					MetricData last = null;
					if(size > 0) {
						last = mds.get(size-1);
					}
					if(last == null || last.getTimestamp() != tp.end) {
						DummyMetricData dummy = new DummyMetricData(tp.end);
						mds.add(dummy);
					}

					//D2. Calculate Service Status Changes inside this ITP.
					service_statuschanges = calculateServiceStatusChanges(resource_id, initial_service_statuses, initial_rrs, mds);
					all_service_statuschanges.addAll(service_statuschanges);
					
					//D3. Calculate Resource Status Changes.
					resource_statuschanges = calculateResourceStatusChanges(resource_id, initial_resource_status, initial_service_statuses, service_statuschanges);
					all_resource_statuschanges.addAll(resource_statuschanges);
				}
			}
			
			//E. Write out any status changes recorded		
			scm.outputServiceStatusChanges(all_service_statuschanges);
			scm.outputResourceStatusChanges(all_resource_statuschanges);
			lm.updateLastMetricDataIDProcessed(last_mdid);	   
			
		} catch (SQLException e) {
			logger.error("SQL Error", e);
			ret = RSVMain.exitcode_error;
		}
		
		return ret;
	}

	private ArrayList<ServiceStatus> calculateServiceStatusChanges(int resource_id, 
			LSCType initial_service_statuses, 
			RelevantRecordSet rrs, 
			ArrayList<MetricData> mds) throws SQLException
	{
		//initial_service_statues will be used by calculateResourceStatusChanges(), so let's not change it.
		LSCType current_status = (LSCType) initial_service_statuses.clone();
		
		ArrayList<ServiceStatus> statuschanges = new ArrayList<ServiceStatus>();

		//load all services (and critical metrics for each services) that this resource support
		TreeMap<Integer/*service_id*/, ArrayList<Integer/*metric_id*/>> critical_metrics = 
			new TreeMap<Integer, ArrayList<Integer>>();
		ArrayList<Integer/*service_id*/> services = oim.getResourceService(resource_id); 
		//TreeSet<Integer/*metric_id*/> all_critical_metrics = new TreeSet<Integer>();
		for(Integer service_id : services) {
			ArrayList<Integer> critical = oim.getCriticalMetrics(service_id);
			//all_critical_metrics.addAll(critical);
			critical_metrics.put(service_id, critical);
		}
		
		//iterate each metric data inside ITP
		for(MetricData md : mds) {
			
			//ignore if this is not a critical metrics
			//if(!all_critical_metrics.contains(md.getMetricID())) continue;
			
			//first of all, update rrs with the new metric data unless it's dummy (for expiration test)
			if(!(md instanceof DummyMetricData)) {
				rrs.update(md);
			}
			
			//for each service,
			for(Integer service_id : services) {
				//calculate the service status
				ServiceStatus new_status = calculateServiceStatus(critical_metrics.get(service_id), rrs, md.getTimestamp());
				
				//4. record status changes (if any)
				ServiceStatus current_servicestatus = current_status.get(service_id);
				if(current_servicestatus == null || 
						current_servicestatus.status_id != new_status.status_id) {
					
					//set some additional info
					new_status.resource_id = resource_id;
					new_status.service_id = service_id;
					
					//service status has changed
					current_status.put(service_id, new_status);
					statuschanges.add(new_status);
					logger.debug("Resource " + resource_id + 
							" Service Status for " + service_id + 
							" has changed to " + new_status.status_id + 
							" at " + new_status.timestamp +
							" reason: " + new_status.note);
				}
			}
		}
			
		return statuschanges;
	}
	
	//this function is used by RSVCache as well..
	public ServiceStatus calculateServiceStatus(ArrayList<Integer/*metric_id*/> critical, RelevantRecordSet rrs, int timestamp) throws SQLException
	{

		//2. calculate overall "service" status		
		ServiceStatus new_status = new ServiceStatus();
		new_status.timestamp = timestamp;
		
		//reset counters
		int expired = 0;
		int first_expired_time = 0;
		int non_expired_critical = 0;
		int nullmetric = 0;
		int unknown = 0;
		int warning = 0;

		//let's count the critical metrics status
		for(Integer metric_id : critical) {
			MetricData critical_metricdata = rrs.getCurrent(metric_id);
					
			if(critical_metricdata == null) {
				nullmetric++;
				continue;
			}
			if(!oim.isFresh(critical_metricdata, timestamp)) {
				expired++;
				if(first_expired_time == 0) {
					first_expired_time = critical_metricdata.getTimestamp() + critical_metricdata.getFreshFor();
				}
				continue;
			}	
			
			int status_id = critical_metricdata.getStatusID();
			switch(status_id) {
			case Status.CRITICAL:
				non_expired_critical++;
				continue;
			case Status.WARNING:
				warning++;
				continue;
			case Status.UNKNOWN:
				unknown++;
				continue;
			}					
		}	
		
		//let's analyze
		if(non_expired_critical > 0) {
			new_status.status_id = Status.CRITICAL;
			new_status.note = non_expired_critical + " of " + critical.size() + " critical metrics are in CRITICAL status.";
		} else if(expired > 0) {
			new_status.status_id = Status.UNKNOWN;
			new_status.note = expired + " of " + critical.size() + " critical metrics have expired.";
			//reset effective time to be first_expired_time
			new_status.timestamp = first_expired_time;
		} else if(nullmetric > 0) {
			new_status.status_id = Status.UNKNOWN;
			new_status.note = nullmetric + " of " + critical.size() + " critical metrics have not been reported.";
		} else if(unknown > 0) {
			new_status.status_id = Status.UNKNOWN;
			new_status.note = unknown + " of " + critical.size() + " critical metrics are in UNKNOWN status.";
		} else if(warning > 0) {
			new_status.status_id = Status.WARNING;
			new_status.note = warning + " of " + critical.size() + " critical metrics are in WARNING status.";				
		} else {
			new_status.status_id = Status.OK;
			new_status.note = "No issues found.";						
		}
		
		return new_status;
	}
	
	
	private ArrayList<ResourceStatus> calculateResourceStatusChanges(int resource_id, 
			ResourceStatus current_resource_status, 
			LSCType initial_service_statuses, 
			ArrayList<ServiceStatus> service_statuschanges)
	{
		LSCType current_service_statuses = (LSCType) initial_service_statuses.clone();
		ArrayList<ResourceStatus> resource_statuschanges = new ArrayList<ResourceStatus>();
		
		for(ServiceStatus change : service_statuschanges) {
			//update current list
			current_service_statuses.put(change.service_id, change);
			
			ResourceStatus rs = calculateResourceStatus(current_service_statuses);
			
			//record if any status change has occured
			if(current_resource_status == null ||
					current_resource_status.status_id != rs.status_id) {
				
				//set some additional info
				rs.resource_id = resource_id;
				rs.timestamp = change.timestamp;
				//rs.responsible_service_id = change.service_id;
				
				current_resource_status = rs;
				resource_statuschanges.add(rs);
				
				logger.debug("Resource " + resource_id + 
						" overall status has changed to " + rs.status_id + 
						" at " + rs.timestamp +
						" reason: " + rs.note);
						//" service responsible: " + rs.responsible_service_id);
			}
		}
		
		return resource_statuschanges;
	}
	
	//this function is used by RSVCache as well..
	public ResourceStatus calculateResourceStatus(LSCType current_service_statuses)
	{
		//prepare counter
		int critical = 0;
		int unknown = 0;
		int warning = 0;
		
		//let's count
		for(ServiceStatus s : current_service_statuses.values()) {
			switch(s.status_id) {
			case Status.CRITICAL:
				critical++;
				continue;
			case Status.WARNING:
				warning++;
				continue;
			case Status.UNKNOWN:
				unknown++;
				continue;
			}
		}
		
		ResourceStatus rs = new ResourceStatus();
		
		//analyze..
		if(critical > 0) {
			rs.status_id = Status.CRITICAL;
			rs.note = critical + " of " + current_service_statuses.size() + " services are in CRITICAL status.";
		} else if(unknown > 0) {
			rs.status_id = Status.UNKNOWN;
			rs.note = unknown + " of " + current_service_statuses.size() + " services are in UNKNOWN status.";
		} else if(warning > 0) {
			rs.status_id = Status.WARNING;
			rs.note = warning + " of " + current_service_statuses.size() + " services are in WARNING status.";
		} else {
			rs.status_id = Status.OK;
			rs.note = "No issues found for critical services.";		
		}
		
		return rs;
	}
	
	private TreeMap<Integer/*resource_id*/, TimeRange> calculateITPs() throws SQLException
	{
		int maxrecords = Integer.parseInt(RSVMain.conf.getProperty(Configuration.overall_status_max_record_count));
		MetricDataModel mdm = new MetricDataModel();
		
		//find the last processed dbid
		ProcessLogModel lm = new ProcessLogModel();
		
		int last_id = lm.getLastMetricDataIDProcessed();
		logger.info("Resuming overall status calculation after MetricData ID = " + last_id);
		
		//get new metricdata records
		logger.info("Pulling upto " + maxrecords + " records from rsvextra.metricdata");
		ResultSet rs = mdm.getMetricDataRecords(last_id, maxrecords);
		
		//compute ITP (Invalidated Time Period)
		TreeMap<Integer/*resource_id*/, TimeRange> itps = new TreeMap<Integer, TimeRange>();
		while(rs.next()){
			int metric_id = rs.getInt("metric_id");
			int resource_id = rs.getInt("resource_id");
			int start = rs.getInt("timestamp");
			int fresh_for = oim.lookupFreshFor(metric_id);
			int end = start + fresh_for;
			
			TimeRange itp = itps.get(resource_id);
			if(itp == null) {
				itp = new TimeRange();
				itps.put(resource_id, itp);
			} 
			itp.add(start, end);
			
			//keep the last id retrived
			last_mdid = rs.getInt("id");
		}
		
		/*
		//debug - dump ITP
		for(Integer resource_id : itps.keySet()) {
			TimeRange itp = itps.get(resource_id);
			logger.debug("ITP for resource: " + resource_id);
			ArrayList<TimePeriod> ranges = itp.getRanges();
			for(TimePeriod tp : ranges) {
				logger.debug("\tStart: "+tp.start + " End:"+tp.end);
			}
		}
		*/
		return itps;
	}
}

