package rsv.process.control;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.TreeSet;
//import java.util.TreeSet;

import org.apache.log4j.Logger;

import rsv.process.model.OIMModel;
import rsv.process.model.ProcessLogModel;
import rsv.process.model.MetricDataModel;
import rsv.process.model.StatusChangeModel;
import rsv.process.TimeRange.TimePeriod;
import rsv.process.*;
import rsv.process.model.record.Metric;
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
	private Integer last_mdid = null;
	
	public int run(String args[]) {
		int ret = RSVMain.exitcode_ok;
		ArrayList<ServiceStatus> all_service_statuschanges = new ArrayList<ServiceStatus>();
		ArrayList<ResourceStatus> all_resource_statuschanges = new ArrayList<ResourceStatus>();
		
		try {

			//Step 1: Find ITP
			TreeMap<Integer, TimeRange> itps = new TreeMap<Integer, TimeRange>();

			if(args.length == 4) {
				//recalculate on specified resource, start, end.. via command line
				TimeRange tr = new TimeRange();
				tr.add(Integer.parseInt(args[2]), Integer.parseInt(args[3]));
				itps.put(Integer.parseInt(args[1]), tr);
			} else {			
				//recalculate based on the latest metric records
				TreeMap<Integer, TimeRange> itps_original = calculateITPs(); //resets last_mdid.. so that I can update log later.
				for(Integer resource_id : itps_original.keySet()) {	
					ArrayList<Integer/*service_id*/> services = oim.getResourceService(resource_id); 
					if(services.size() == 0) {
						logger.info("Resource " + resource_id + " has no services - skipping this one.");
					} else {
						itps.put(resource_id, itps_original.get(resource_id));
					}
				}
			}
			
			//Step 2: For each resource we found...
			for(Integer resource_id : itps.keySet()) {
				
				ArrayList<ServiceStatus> service_statuschanges = new ArrayList<ServiceStatus>();
				ArrayList<ResourceStatus> resource_statuschanges = new ArrayList<ResourceStatus>();	
				TimeRange itp = itps.get(resource_id);
				ArrayList<TimePeriod> ranges = itp.getRanges();
				
				//for each ranges (currently there is only 1 range per resource, but we can improve this later)
				for(TimePeriod tp : ranges) {
					Date start_date = new Date(tp.start*1000L);
					Date end_date = new Date(tp.end*1000L);
					logger.info("Processing resource ID " + resource_id + " between " + tp.start + "(" + start_date.toString() + ") and " + tp.end + "(" + end_date.toString() + ")");
					
					//B. Retrieve Initial Status History (all statuschange_xxx tables)
					LSCType initial_service_statuses = scm.getLastStatusChange_Service(resource_id, tp.start);
					ResourceStatus initial_resource_status = scm.getLastStatusChange_Resource(resource_id, tp.start);
					
					//C. Retrieve Initial Relevant Record Set (RRS)
					RelevantRecordSet initial_rrs = new RelevantRecordSet(resource_id, tp.start);
					
					//D1. Pull metricdata inside ITP
					MetricDataModel mdm = new MetricDataModel();
					ArrayList<MetricData> mds = mdm.getMeticData(resource_id, tp.start, tp.end);//sorted by timestamp
					//add dummy metric data at expiration time for each metric data to recalculate status when metric expires.
					ArrayList<MetricData> mds_with_dummy = addExpirationTriggers(initial_rrs, mds, tp.end);

					//D2. Calculate Service Status Changes inside this ITP.
					service_statuschanges = calculateServiceStatusChanges(resource_id, initial_service_statuses, initial_rrs, mds_with_dummy);
					//now, the statuschange could contain status changes due to initial_rrs which could occur before tp.start
					//We need to remove those or we will have duplicates.
					for(ServiceStatus status : service_statuschanges) {
						if(status.timestamp > tp.start) {
							all_service_statuschanges.add(status);
						}
					}
					//all_service_statuschanges.addAll(service_statuschanges);
					
					//D3. Calculate Resource Status Changes.
					resource_statuschanges = calculateResourceStatusChanges(resource_id, initial_resource_status, initial_service_statuses, service_statuschanges);
					//filter same reason as service status
					for(ResourceStatus status : resource_statuschanges) {
						if(status.timestamp > tp.start) {
							all_resource_statuschanges.add(status);
						}
					}
					//all_resource_statuschanges.addAll(resource_statuschanges);
				}
			}
			
			//pre-E. Clear ITP window 
			for(Integer resource_id : itps.keySet()) {
				
				TimeRange itp = itps.get(resource_id);
				ArrayList<TimePeriod> ranges = itp.getRanges();
				for(TimePeriod tp : ranges) {
					int removed = scm.clearStatusChanges(resource_id, tp.start, tp.end);
					java.util.Date start_date = new java.util.Date((long)tp.start * 1000);

					logger.info("For resource " + resource_id + " - cleared total of " + removed + " records inside ITP of start: " + tp.start + "(" +
							start_date + ") end: " + tp.end + " (duration: " + (tp.end - tp.start)/60 + " minutes)");
				}
			}
			
			//E. Write out any status changes recorded		
			scm.outputServiceStatusChanges(all_service_statuschanges);
			scm.outputResourceStatusChanges(all_resource_statuschanges);
			if(last_mdid != null) {
				lm.updateLastMetricDataIDProcessed(last_mdid);	   
			}

		} catch (SQLException e) {
			logger.error("SQL Error", e);
			ret = RSVMain.exitcode_error;
		}
		
		return ret;
	}
	
	//from a list of metricdata, add dummymetricdata where the metricdata expires.
	private ArrayList<MetricData> addExpirationTriggers(RelevantRecordSet initial_rrs, ArrayList<MetricData> mds, Integer endtime) throws SQLException 
	{
		//logger.debug("endtime " + endtime);
		
		//create list of expiration_points that we will need to insert as DummyMetricData
		TreeSet<Integer/*timestamp*/> expiration_points = new TreeSet<Integer>();
		
		//add from initial_rrs
		for(MetricData md : initial_rrs.getAllMetricData()) {
			int e = md.getTimestamp() + md.getFreshFor();
			if(e <= endtime) {
				expiration_points.add(e);
			}
		}	
		
		//add from mds
		for(MetricData md : mds) {
			int e = md.getTimestamp() + md.getFreshFor();
			if(e <= endtime) {
				expiration_points.add(e);
				//logger.debug("candidate at " + md.getTimestamp() + " metric_id: " + md.getID() + " expired at : " + e);
			}
		}
		
		//Now, optimize the expiration list by removing unneccessary "possible" expiration points.
		ArrayList<MetricData> mds_with_dummy = new ArrayList<MetricData>();
		Iterator<Integer> ep_it = expiration_points.iterator();
		Iterator<MetricData> md_it = mds.iterator();
		Integer ep_next = null;
		MetricData md_next = null;
		while(ep_it.hasNext() || md_it.hasNext()) {
			
			//fill the queue
			if(ep_next == null && ep_it.hasNext()) {
				ep_next = ep_it.next();
			}
			if(md_next == null && md_it.hasNext()) {
				md_next = md_it.next();
			}
			
			//pick which one should go next
			if(ep_next == null && md_next != null) {
				//only md is available
				mds_with_dummy.add(md_next);
				md_next = null;
			} else if(ep_next != null && md_next == null) {
				//only ep is available
				DummyMetricData dummy = new DummyMetricData(ep_next);
				mds_with_dummy.add(dummy);
				ep_next = null;
			} else {
				//now the interesting case..
				if(ep_next < md_next.getTimestamp()) {
					//ep is before next md.. add dummy
					DummyMetricData dummy = new DummyMetricData(ep_next);
					mds_with_dummy.add(dummy);
					ep_next = null;				
				} else if(ep_next < md_next.getTimestamp()) {
					//ep is *on* next md.. ad md and clear both
					mds_with_dummy.add(md_next);
					md_next = null;
					ep_next = null;
				} else {
					//md is before ep.. ad md
					mds_with_dummy.add(md_next);
					md_next = null;
				}
			}
		}
		
		//process last ones
		if(ep_next != null) {
			DummyMetricData dummy = new DummyMetricData(ep_next);
			mds_with_dummy.add(dummy);
		}
		if(md_next != null) {
			mds_with_dummy.add(md_next);
		}

		return mds_with_dummy;
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
		for(Integer service_id : services) {
			ArrayList<Integer> critical = oim.getCriticalMetrics(service_id);
			critical_metrics.put(service_id, critical);
		}
		
		//iterate each metric data inside ITP
		for(MetricData md : mds) {
						
			//first of all, update rrs with the new metric data unless it's dummy (for expiration test)
			if(!(md instanceof DummyMetricData)) {
				rrs.update(md);
			}
			
			//for each service,
			for(Integer service_id : services) {
				//calculate the service status (since a probe can influence multiple services - by OIM design)
				ServiceStatus new_status = calculateServiceStatus(critical_metrics.get(service_id), rrs, md.getTimestamp());
				
				//logger.debug(new_status.note);
				
				//4. record status changes (if any)
				ServiceStatus current_servicestatus = current_status.get(service_id);
				if(current_servicestatus == null || 
						current_servicestatus.status_id != new_status.status_id) {
					
					//set some additional info
					new_status.resource_id = resource_id;
					new_status.service_id = service_id;
					new_status.timestamp = md.getTimestamp();
					
					//service status has changed
					current_status.put(service_id, new_status);
					statuschanges.add(new_status);
					Date change_date = new Date(new_status.timestamp*1000L);
					logger.debug("Resource " + resource_id + 
							" Service Status for " + service_id + 
							" has changed to " + new_status.status_id + 
							" at " + change_date.toString() + "(" + new_status.timestamp + ")" +
							" reason: " + new_status.note);
				}
			}
		}
			
		return statuschanges;
	}
	
	//this function is used by RSVCache as well..
	public ServiceStatus calculateServiceStatus(ArrayList<Integer/*metric_id*/> critical, RelevantRecordSet rrs, int timestamp) throws SQLException
	{
		ServiceStatus new_status = new ServiceStatus();
		
		//reset counters
		int expired = 0;
		int non_expired_critical = 0;
		int nullmetric = 0;
		int unknown = 0;
		int warning = 0;
		int ok = 0;
		
		//hold some status details that are used to calculate the overall status.
		String status_detail = "";

		//let's count the critical metrics status
		for(Integer metric_id : critical) {
			MetricData critical_metricdata = rrs.getCurrent(metric_id);
			Metric m = oim.getMetric(metric_id);
			
			if(critical_metricdata == null) {
				nullmetric++;
				status_detail += m.getName() + "=NA ";
				continue;
			}
			if(!oim.isFresh(critical_metricdata, timestamp)) {
				expired++;
				status_detail += critical_metricdata.getID()+"=EXPIRED ";
				continue;
			}
			
			int status_id = critical_metricdata.getStatusID();
			switch(status_id) {
			case Status.CRITICAL:
				non_expired_critical++;
				status_detail += critical_metricdata.getID()+"=CRITICAL ";
				continue;
			case Status.WARNING:
				warning++;
				status_detail += critical_metricdata.getID()+"=WARNING ";
				continue;
			case Status.UNKNOWN:
				unknown++;
				status_detail += critical_metricdata.getID()+"=UNKNOWN ";
				continue;
			case Status.OK:
				ok++;
				status_detail += critical_metricdata.getID() + "=OK ";
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
			//new_status.timestamp = first_expired_time;
		} else if(nullmetric > 0) {
			new_status.status_id = Status.UNKNOWN;
			new_status.note = nullmetric + " of " + critical.size() + " critical metrics have not been reported.";
		} else if(unknown > 0) {
			new_status.status_id = Status.UNKNOWN;
			new_status.note = unknown + " of " + critical.size() + " critical metrics are in UNKNOWN status.";
		} else if(warning > 0) {
			new_status.status_id = Status.WARNING;
			new_status.note = warning + " of " + critical.size() + " critical metrics are in WARNING status.";				
		} else if(ok > 0) {
			new_status.status_id = Status.OK;
			new_status.note = "No issues found for this service.";						
		} else {
			new_status.status_id = Status.UNKNOWN;
			new_status.note = "No metric status has been reported for this service.";
		}
		
		//this is most likely for temporary, and once everything starts to working correctly, we wouldn't need this
		new_status.note += "\n" + status_detail;
		
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
		int downtime = 0;
		int ok = 0;
		
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
			case Status.DOWNTIME:
				downtime++;
				continue;
			case Status.OK:
				ok++;
				continue;
			}
		}
		
		ResourceStatus rs = new ResourceStatus();
		
		//analyze..
		if(critical > 0) {
			rs.status_id = Status.CRITICAL;
			rs.note = critical + " of " + current_service_statuses.size() + " services are in CRITICAL status.";
		} else if(downtime > 0) {
			rs.status_id = Status.DOWNTIME;
			rs.note = downtime + " of " + current_service_statuses.size() + " services are in maintenance.";
		} else if(unknown > 0) {
			rs.status_id = Status.UNKNOWN;
			rs.note = unknown + " of " + current_service_statuses.size() + " services are in UNKNOWN status.";
		} else if(warning > 0) {
			rs.status_id = Status.WARNING;
			rs.note = warning + " of " + current_service_statuses.size() + " services are in WARNING status.";
		} else if(ok > 0) {
			rs.status_id = Status.OK;
			rs.note = "No issues found for this resource.";		
		} else {
			rs.status_id = Status.UNKNOWN;
			rs.note = "No service status has been reported.";
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
			
			//keep the last id retrived
			last_mdid = rs.getInt("id");
			
			TimeRange itp = itps.get(resource_id);
			if(itp == null) {
				itp = new TimeRange();
				itps.put(resource_id, itp);
			} 
			itp.add(start, end);
		}
		
		return itps;
	}
}

