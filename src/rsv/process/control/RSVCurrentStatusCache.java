package rsv.process.control;

import rsv.process.model.OIMModel;
import rsv.process.model.OIMModel.ResourcesType;
import rsv.process.model.StatusChangeModel.LSCType;
import rsv.process.model.record.Downtime;
import rsv.process.model.record.GratiaMetricRecord;
import rsv.process.model.record.Metric;
import rsv.process.model.record.MetricData;
import rsv.process.model.record.Resource;
import rsv.process.model.record.ResourceStatus;
import rsv.process.model.record.Service;
import rsv.process.model.record.ServiceStatus;
import rsv.process.model.record.Status;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringEscapeUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import rsv.process.Configuration;
import rsv.process.EventPublisher;
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
			//SendMail.sendErrorEmail(e.getMessage());
			ret = RSVMain.exitcode_error;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		return ret;
	}
	
	private Document parseXmlFile(String filename){
		//get the factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	
		try {

			//Using factory get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();

			//parse using builder to get DOM representation of the XML file
			Document doc = db.parse(filename);
			doc.getDocumentElement().normalize();
			return doc;
			
		}catch(ParserConfigurationException pce) {
			pce.printStackTrace();
		}catch(SAXException se) {
			se.printStackTrace();
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
		
		return null;
	}
	
	private void updateCurrentStatusCache(Resource r, int currenttime) throws SQLException, IOException 
	{	
		int resource_id = r.getID();
		
		//figure out xml file name for this resource
    	String filename_template = RSVMain.conf.getProperty(Configuration.current_resource_status_xml_cache);
    	String filename = filename_template.replaceFirst("<ResourceID>", String.valueOf(resource_id));
    	
    	File file = new File(filename);
    	Document previous = null;
    	if(file.exists()) {
    		//load previous version of xml
    		previous = parseXmlFile(filename);
    	}
		
		//load RRS				
		RelevantRecordSet rrs = new RelevantRecordSet(resource_id, currenttime);
			
		//place holder for resource specific xml
		StringBuffer xml = new StringBuffer();
		xml.append("<?xml version=\"1.0\"?>\n");
		xml.append("<CurrentResourceStatus>");
		xml.append("<Timestamp>"+currenttime+"</Timestamp>");
		
		LSCType service_statues = new LSCType();
		
		//for each service..
		xml.append("<Services>");
		ArrayList<Integer/*service_id*/> services = oim.getResourceService(resource_id); 
		for(Integer service_id : services) {
			xml.append("<Service>");
			xml.append("<ServiceID>"+service_id+"</ServiceID>");
			Service s = oim.getService(service_id);
			xml.append("<ServiceName>"+StringEscapeUtils.escapeXml(s.getName())+"</ServiceName>");					
			xml.append("<ServiceDescription>"+StringEscapeUtils.escapeXml(s.getDescription())+"</ServiceDescription>");	
			
			ArrayList<Integer> critical_metrics = oim.getCriticalMetrics(service_id);
			ArrayList<Integer> non_critical_metrics = oim.getNonCriticalMetrics(service_id);

			//calculate service status
			ServiceStatus status = RSVOverallStatus.calculateServiceStatus(oim, critical_metrics, rrs, currenttime);	
			status.service_id = service_id;
			
			//is this in downtime?
			Downtime down = getDownTime(resource_id, service_id, currenttime);
			if(down != null) {
				xml.append("<DowntimeNote>");
				xml.append("<InternalStatus>"+Status.getStatus(status.status_id)+"</InternalStatus>");
				xml.append("<Note>This service is currently under maintenance</Note>");
				xml.append("<MaintenanceSummary>" + StringEscapeUtils.escapeXml(down.getSummary())+"</MaintenanceSummary>");
				Date from = new Date(down.getStartTime()*1000L);
				Date to = new Date(down.getEndTime()*1000L);
				xml.append("<From>" + from + "</From><To>" + to + "</To>");
				xml.append("</DowntimeNote>");
				
				//override status with DOWMTIME
				status.status_id = Status.DOWNTIME;
			}
			service_statues.put(service_id, status);	
			xml.append("<Status>"+Status.getStatus(status.status_id)+"</Status>");
			xml.append("<Note>"+StringEscapeUtils.escapeXml(status.note)+"</Note>");	
			
			//output critical metric details
			xml.append("<CriticalMetrics>");
			outputMetricXML(critical_metrics, rrs, xml);
			xml.append("</CriticalMetrics>");
			
			//output non-critical metric details
			xml.append("<NonCriticalMetrics>");
			outputMetricXML(non_critical_metrics, rrs, xml);
			xml.append("</NonCriticalMetrics>");
			
			xml.append("</Service>");
		}
		xml.append("</Services>");
		
		//calculate resource status
		ResourceStatus rstatus = RSVOverallStatus.calculateResourceStatus(oim, service_statues);
		
		xml.append("<ResourceID>"+resource_id+"</ResourceID>");
		xml.append("<ResourceName>"+StringEscapeUtils.escapeXml(r.getName())+"</ResourceName>");
		xml.append("<Status>"+Status.getStatus(rstatus.status_id)+"</Status>");
		xml.append("<Note>"+StringEscapeUtils.escapeXml(rstatus.note)+"</Note>");
		
		xml.append("</CurrentResourceStatus>");
		
		//if we have previous xml, let's compare with previous value and post events if status changes
		if(previous != null) {
			Element status_elem = getStatusElement(previous);
			String previous_status = status_elem.getTextContent();
	    	String new_status = Status.getStatus(rstatus.status_id);
	    	if(!previous_status.equals(new_status)) {
	    		publish_statuschange_event(resource_id, r.getName(), rstatus);
	    	}
		}
		  	
		//write new xml
    	FileWriter fstream = new FileWriter(filename);
    	BufferedWriter out = new BufferedWriter(fstream);
    	out.write(xml.toString());
    	out.close();
	}
	
	private Element getStatusElement(Document doc) {
		NodeList roots = doc.getChildNodes();
		Node root = roots.item(0);
		NodeList nodes = root.getChildNodes();
		for(int i = 0;i < nodes.getLength(); ++i) {
			Node node = nodes.item(i);
			if (root.getNodeType() == Node.ELEMENT_NODE) {
				Element elem = (Element) node;
			    if(elem.getNodeName().equals("Status")) {
			    	return elem;
			    }	     
			}
		}
		return null;
	}
	
	private void publish_statuschange_event(Integer resource_id, String resource_name, ResourceStatus rstatus) {
		EventPublisher publisher = new EventPublisher();
		String routing_key = resource_name;//TODO - should we add facility/site/rg name too?
		String msg = 
			"<ResourceStatusChange>"+
				"<ResourceID>"+resource_id+"</ResourceID>"+
				"<Status>"+Status.getStatus(rstatus.status_id)+"</Status>" +
				"<Note>"+StringEscapeUtils.escapeXml(rstatus.note)+"</Note>"+
			"</ResourceStatusChange>";
		publisher.publish(routing_key, msg);
	}
	
	private void outputMetricXML(ArrayList<Integer> critical_metrics, RelevantRecordSet rrs, StringBuffer xml) throws SQLException
	{
		for(Integer metric_id : critical_metrics) {
			Metric m = oim.getMetric(metric_id);
			
			xml.append("<Metric>");
			xml.append("<MetricID>"+metric_id+"</MetricID>");
			xml.append("<MetricName>"+StringEscapeUtils.escapeXml(m.getName())+"</MetricName>");
			xml.append("<MetricCommonName>"+StringEscapeUtils.escapeXml(m.getCommonName())+"</MetricCommonName>");
			xml.append("<MetricDescription>"+StringEscapeUtils.escapeXml(m.getDescription())+"</MetricDescription>");
			xml.append("<MetricFreshFor>"+m.getFreshFor()+"</MetricFreshFor>");
			
			MetricData md = rrs.getCurrent(metric_id);
			if(md == null) {
				xml.append("<Status/>");
				xml.append("<Timestamp/>");
				xml.append("<Detail/>");			
				xml.append("<MetricDataID/>");
			} else {
				xml.append("<Status>"+Status.getStatus(md.getStatusID())+"</Status>");
				xml.append("<Timestamp>"+md.getTimestamp()+"</Timestamp>");
				GratiaMetricRecord rec = md.fetchDetail();
				if(rec != null) {
					xml.append("<Detail><![CDATA["+rec.DetailsData+"]]></Detail>");
					xml.append("<ServiceURI>"+rec.ServiceUri+"</ServiceURI>");
					xml.append("<GatheredAt>"+rec.GatheredAt+"</GatheredAt>");
				} else {
					//no detail available.. maybe truncated / archived
					xml.append("<Detail>(Gratia Record not available)</Detail>");
					xml.append("<ServiceURI></ServiceURI>");
					xml.append("<GatheredAt></GatheredAt>");
				}
				xml.append("<MetricDataID>"+md.getID()+"</MetricDataID>");
			}
			xml.append("</Metric>");
		}
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
