package rsv.process.control;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;

import org.apache.log4j.Logger;

import rsv.process.Configuration;
import rsv.process.RelevantRecordSet;
import rsv.process.model.OIMModel;
import rsv.process.model.OIMModel.ResourcesType;
import rsv.process.model.record.MetricData;
import rsv.process.model.record.Resource;
import rsv.process.model.record.ResourceStatus;
import rsv.process.model.record.Service;
import rsv.process.model.record.ServiceStatus;
import rsv.process.model.record.Status;
import rsv.process.model.record.Metric;
import rsv.process.model.StatusChangeModel.LSCType;

public class RSVCache implements RSVProcess {
	private static final Logger logger = Logger.getLogger(RSVCache.class);
	OIMModel oim = new OIMModel();
	
	public int run(String args[]) {
		int ret = RSVMain.exitcode_ok;
		
		int r = outputCurrentStatusCache();
		if(r != RSVMain.exitcode_ok) {
			logger.error("Error while executing outputCurrentStatusCache()");
			ret = RSVMain.exitcode_error;
		}
		
		return ret;
	}

	private int outputCurrentStatusCache()
	{
		int ret = RSVMain.exitcode_ok;
		
		Calendar cal = Calendar.getInstance();
		Date current_date = cal.getTime();
		int currenttime = (int) (current_date.getTime()/1000);
		
		try {
			
			ResourcesType resources = oim.getResources();
			RSVOverallStatus overall = new RSVOverallStatus();
			
			String allxml = "<?xml version=\"1.0\"?>\n";
			allxml += "<CurrentStatus>";
			allxml += "<Timestamp>"+currenttime+"</Timestamp>";
			
			logger.info("Processing resources..");
			
			for(Integer resource_id : resources.keySet()) {
				
				//ignore resources that doesn't have any services
				Resource r = resources.get(resource_id);
				ArrayList<Integer/*service_id*/> services = oim.getResourceService(resource_id); 
				if(services.size() == 0) continue;
				
				//logger.info("Processing resource ID " + resource_id + " with services count of " + services.size());
				
				//load RRS				
				RelevantRecordSet rrs = new RelevantRecordSet(resource_id, currenttime);
					
				//place holder for resource specific xml
				String xml = "<?xml version=\"1.0\"?>\n";
				xml += "<CurrentResourceStatus>";
				xml += "<Timestamp>"+currenttime+"</Timestamp>";
				
				LSCType service_statues = new LSCType();
				
				//service details
				xml += "<Services>";
				for(Integer service_id : services) {
					xml += "<Service>";
					xml += "<ServiceID>"+service_id+"</ServiceID>";
					Service s = oim.getService(service_id);
					xml += "<ServiceName>"+s.getName()+"</ServiceName>";					
					xml += "<ServiceDescription>"+s.getDescription()+"</ServiceDescription>";	
					
					ArrayList<Integer> critical_metrics = oim.getCriticalMetrics(service_id);
					ArrayList<Integer> non_critical_metrics = oim.getNonCriticalMetrics(service_id);
					
					//calculate service status
					ServiceStatus status = overall.calculateServiceStatus(critical_metrics, rrs, currenttime);
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
				ResourceStatus rstatus = overall.calculateResourceStatus(service_statues);
				
				String resource_detail = "";
				resource_detail += "<ResourceID>"+resource_id+"</ResourceID>";
				resource_detail += "<ResourceName>"+r.getName()+"</ResourceName>";
				resource_detail += "<Status>"+Status.getStatus(rstatus.status_id)+"</Status>";	
				resource_detail += "<Note>"+rstatus.note+"</Note>";
				xml += resource_detail;
				
				allxml += "<ResourceStatus>";
				allxml += resource_detail;
				allxml += "</ResourceStatus>";
				
				xml += "</CurrentResourceStatus>";
				
				//output resource specific XML to configured location
		    	String filename_template = RSVMain.conf.getProperty(Configuration.current_resource_status_xml_cache);
		    	String filename = filename_template.replaceFirst("<ResourceID>", resource_id.toString());
				try{
			    	FileWriter fstream = new FileWriter(filename);
			    	BufferedWriter out = new BufferedWriter(fstream);
			    	out.write(xml);
			    	out.close();
			    } catch (IOException e) {
			    		logger.error("Caught exception while outputing xml cache for: " + filename, e);
						ret = RSVMain.exitcode_error;
			    }
			}
						
			allxml += "</CurrentStatus>";
	    	
			//output all resource status summary
			String filename_template = RSVMain.conf.getProperty(Configuration.current_resource_status_xml_cache);
	    	String filename = filename_template.replaceFirst("<ResourceID>", "all");
	    	FileWriter fstream;
			try {
				fstream = new FileWriter(filename);
		    	BufferedWriter out = new BufferedWriter(fstream);
		    	out.write(allxml);
		    	out.close();
		    } catch (IOException e) {
	    		logger.error("Caught exception while outputing xml cache: " + filename, e);
				ret = RSVMain.exitcode_error;
			}

		} catch (SQLException e) {
			logger.error("SQL Error", e);
			ret = RSVMain.exitcode_error;
		}
		return ret;		
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
}
