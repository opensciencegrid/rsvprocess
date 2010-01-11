package rsv.process.control;

import rsv.process.lib.SendMail;
import rsv.process.model.OIMModel;
import rsv.process.model.OIMModel.ResourcesType;
import rsv.process.model.StatusChangeModel.LSCType;
import rsv.process.model.record.Downtime;
import rsv.process.model.record.Metric;
import rsv.process.model.record.MetricData;
import rsv.process.model.record.Resource;
import rsv.process.model.record.ResourceStatus;
import rsv.process.model.record.Service;
import rsv.process.model.record.ServiceStatus;
import rsv.process.model.record.Status;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;
import rsv.process.Configuration;
import rsv.process.RelevantRecordSet;

public class RSVCurrentStatusCache implements RSVProcess {
	
	private static final Logger logger = Logger.getLogger(RSVCurrentStatusCache.class);
	OIMModel oim;
	
	public int run(String args[]) 
	{
		int ret = RSVMain.exitcode_ok;
		oim = new OIMModel();	
		try  {
			//Step 5. Recalculate current status cache for all resources
			Calendar cal = Calendar.getInstance();
			Date current_date = cal.getTime();
			int currenttime = (int) (current_date.getTime()/1000);
			ResourcesType resources = oim.getResources();
			logger.debug("Updating current status cache files for " + resources.size() + " resources.");
			for(Integer resource_id : resources.keySet()) {
				updateCurrentStatusCache(resources.get(resource_id), currenttime);
			}
		} catch (IOException e) {
			logger.error("IO Exception", e);
			SendMail.sendErrorEmail(e.getMessage());
			ret = RSVMain.exitcode_error;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		return ret;
	}
	
	private void updateCurrentStatusCache(Resource r, int currenttime) throws SQLException, IOException 
	{	
		int resource_id = r.getID();
		
		//load RRS				
		RelevantRecordSet rrs = new RelevantRecordSet(resource_id, currenttime);
			
		//place holder for resource specific xml
		String xml = "<?xml version=\"1.0\"?>\n";
		xml += "<CurrentResourceStatus>";
		xml += "<Timestamp>"+currenttime+"</Timestamp>";
		
		LSCType service_statues = new LSCType();
		
		//for each service..
		xml += "<Services>";
		ArrayList<Integer/*service_id*/> services = oim.getResourceService(resource_id); 
		for(Integer service_id : services) {
			xml += "<Service>";
			xml += "<ServiceID>"+service_id+"</ServiceID>";
			Service s = oim.getService(service_id);
			xml += "<ServiceName>"+s.getName()+"</ServiceName>";					
			xml += "<ServiceDescription>"+s.getDescription()+"</ServiceDescription>";	
			
			ArrayList<Integer> critical_metrics = oim.getCriticalMetrics(service_id);
			ArrayList<Integer> non_critical_metrics = oim.getNonCriticalMetrics(service_id);

			//calculate service status
			ServiceStatus status = RSVOverallStatus.calculateServiceStatus(oim, critical_metrics, rrs, currenttime);	
			status.service_id = service_id;
			
			//is this in downtime?
			Downtime down = getDownTime(resource_id, service_id, currenttime);
			if(down != null) {
				xml += "<DowntimeNote>";
				xml += "<InternalStatus>"+Status.getStatus(status.status_id)+"</InternalStatus>";
				xml += "<Note>This service is currently under maintenance</Note>";
				xml += "<MaintenanceSummary>" + down.getSummary()+"</MaintenanceSummary>";
				Date from = new Date(down.getStartTime()*1000L);
				Date to = new Date(down.getEndTime()*1000L);
				xml += "<From>" + from + "</From><To>" + to + "</To>";
				xml += "</DowntimeNote>";
				
				//override status with DOWMTIME
				status.status_id = Status.DOWNTIME;
			}
			service_statues.put(service_id, status);	
			xml += "<Status>"+Status.getStatus(status.status_id)+"</Status>";
			xml += "<Note>"+status.note+"</Note>";	
			
			//output critical metric details
			xml += "<CriticalMetrics>";
			xml += outputMetricXML(critical_metrics, rrs);
			xml += "</CriticalMetrics>";
			
			//output non-critical metric details
			xml += "<NonCriticalMetrics>";
			xml += outputMetricXML(non_critical_metrics, rrs);
			xml += "</NonCriticalMetrics>";
			
			xml += "</Service>";
		}
		xml += "</Services>";
		
		//calculate resource status
		ResourceStatus rstatus = RSVOverallStatus.calculateResourceStatus(oim, service_statues);
		
		String resource_detail = "";
		resource_detail += "<ResourceID>"+resource_id+"</ResourceID>";
		resource_detail += "<ResourceName>"+r.getName()+"</ResourceName>";
		resource_detail += "<Status>"+Status.getStatus(rstatus.status_id)+"</Status>";	
		resource_detail += "<Note>"+rstatus.note+"</Note>";
		xml += resource_detail;
		
		xml += "</CurrentResourceStatus>";
		
		//output resource specific XML to configured location
    	String filename_template = RSVMain.conf.getProperty(Configuration.current_resource_status_xml_cache);
    	String filename = filename_template.replaceFirst("<ResourceID>", String.valueOf(resource_id));
    	
    	//update A&R
    	//TODO---

    	FileWriter fstream = new FileWriter(filename);
    	BufferedWriter out = new BufferedWriter(fstream);
    	out.write(xml);
    	out.close();
	}
	
	private String outputMetricXML(ArrayList<Integer> critical_metrics, RelevantRecordSet rrs) throws SQLException
	{
		String xml = "";
		for(Integer metric_id : critical_metrics) {
			Metric m = oim.getMetric(metric_id);
			
			xml += "<Metric>";
			xml += "<MetricID>"+metric_id+"</MetricID>";
			xml += "<MetricName>"+m.getName()+"</MetricName>";
			xml += "<MetricCommonName>"+m.getCommonName()+"</MetricCommonName>";
			xml += "<MetricDescription>"+m.getDescription()+"</MetricDescription>";
			xml += "<MetricFreshFor>"+m.getFreshFor()+"</MetricFreshFor>";
			
			MetricData md = rrs.getCurrent(metric_id);
			if(md == null) {
				xml += "<Status/>";
				xml += "<Timestamp/>";
				xml += "<Detail/>";			
				xml += "<MetricDataID/>";
			} else {
				xml += "<Status>"+Status.getStatus(md.getStatusID())+"</Status>";
				xml += "<Timestamp>"+md.getTimestamp()+"</Timestamp>";
				xml += "<Detail><![CDATA["+md.fetchDetail()+"]]></Detail>";
				xml += "<MetricDataID>"+md.getID()+"</MetricDataID>";
			}
			xml += "</Metric>";
		}
		return xml;
	}
	
	private Downtime getDownTime(int resource_id, int service_id, int timestamp) throws SQLException 
	{
		ArrayList<Downtime> downtimes = oim.getDowntimes(resource_id);
		if(downtimes != null) {
			for(Downtime downtime : downtimes) {
				//TODO - check boundary case policies.
				if(downtime.getStartTime() < timestamp && downtime.getEndTime() > timestamp) {
					ArrayList<Integer/*service_id*/> services = downtime.getServiceIDs();
					if(services.contains(service_id)) {
						return downtime;
					}
				}
			}
		}	
		return null;
	}
	
}
