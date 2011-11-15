package rsv.process.control;

import rsv.process.model.GratiaModel;
import rsv.process.model.MetricInserter;
import rsv.process.model.OIMModel;
import rsv.process.model.ProcessLogModel;
import rsv.process.model.ResourceDetailModel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import rsv.process.Configuration;

public class RSVPreprocess implements RSVProcess {
	
	private static final Logger logger = Logger.getLogger(RSVPreprocess.class);
	
	public int run(String args[]) 
	{
		int ret = RSVMain.exitcode_ok;
		//do some initialization
		OIMModel oim = new OIMModel();
		GratiaModel grartia = new GratiaModel();	
		MetricInserter mdetail = new MetricInserter();
		ResourceDetailModel resourcedetail = new ResourceDetailModel();
		ProcessLogModel lm = new ProcessLogModel();
		int maxrecords = Integer.parseInt(RSVMain.conf.getProperty(Configuration.preprocess_gratia_record_count));
		
		//for bookkeeping
		int records_pulled = 0;
		int records_added = 0;
		
		try {
			
			//find the last processed dbid
			int last_dbid = lm.getLastGratiaIDProcessed();
			logger.info("Resuming preprocess after Gratia DBID = " + last_dbid);
			
			/* -- Per Arvind's request, we are no longer doing this clear up.
			//make sure what log says is true - or we will end of processing same records twice
			int removed = mdetail.clearRecords(last_dbid);
			if(removed != 0) {
				logger.warn("Found "+removed+" records (total) with dbid larger than what we see in process log. Removing them for data integrity.");
			}
			*/
			
			//get gratia records
			logger.info("Pulling upto " + maxrecords + " records from Gratia.MetricRecord");
			ResultSet rs = grartia.getMetricRecords(last_dbid, maxrecords);
			
			//error counters
			int count_invalid_resource_id = 0;
			int count_invalid_metric_id = 0;
			int count_invalid_gatheredat = 0;
			int count_invalid_status_id = 0;
			int count_invalid_timestamp = 0;
			//int count_invalid_detail = 0;
			
			//process each records
			while(rs.next()){
	            records_pulled++;
	            
	            //lookup dbid
	            int dbid = rs.getInt("dbid");
	            last_dbid = dbid;
	        	
	            String serviceuri = rs.getString("ServiceUri");
	            if(serviceuri == null) {
	            	//if uri is null, no point of looking up anything.. bail
            		count_invalid_resource_id++;
            		continue;	
	            }
	            
	            //strip port, and other garbage
	            serviceuri = OIMModel.pullHostname(serviceuri);
	            //lookup resource_id
	            Integer resource_id = oim.lookupResourceID(serviceuri);	   
	            if(resource_id == null) {
	            	//if not found, lookup resource alias
	    	        //logger.info("Looking up resource id from alias table for " + serviceuri);
	            	resource_id = oim.lookupResourceAlias(serviceuri);
	            	if(resource_id == null) {
	            		//if not found, lookup service uri:port
		    	        //logger.info("Looking up resource id from service override table for " + serviceuri);
		            	resource_id = oim.lookupServiceHostEndpointOverride(serviceuri);
		            	if(resource_id == null) {
		            		//well, then we should really reject this
			    	        //logger.info("Failed to find " + serviceuri);
		            		count_invalid_resource_id++;
		            		continue;
		            	} else {
		            		//logger.debug("dbid" + dbid + " -- using endpoint override for " + resource_id);
		            	}
	            	}
	            }
	           
	            //lookup metric_id
	            String metricname = rs.getString("MetricName");
	            Integer metric_id = oim.lookupMetricID(metricname);	   
	            if(metric_id == null) {
	            	count_invalid_metric_id++;
	            	continue;
	            }
	            ArrayList<Integer/*service_id*/> services = oim.getServicesCriticalFor(metric_id);
	            /*
	            if(services.contains(1) || services.contains(5)) {
	            	//this metric is critical for at least one service. check the gathered at
	            	String gatheredat = rs.getString("GatheredAt");
	            	if(!gatheredat.matches("rsv-client\\d.grid.iu.edu")) {
	            		//critical metric must come from our central server reject..
	            		count_invalid_gatheredat++;
	            		continue;
	            	}
	            }
	            */
	            
	            //lookup status_id
	            String metricstatus = rs.getString("MetricStatus");
	            Integer status_id = oim.lookupStatusID(metricstatus);	   
	            if(status_id == null) {
	            	count_invalid_status_id++;
	            	continue;
	            }
	            
	            //lookup unix timestamp
	            int utimestamp = rs.getInt("utimestamp");
	            if(utimestamp == 0) {
	            	count_invalid_timestamp++;
	            	continue;
	            }
	            
	            //detailsdata
	            String detail = rs.getString("DetailsData");
	         
	            mdetail.add(dbid, utimestamp, resource_id, metric_id, status_id, detail);
	            records_added++;
	        }
			
            //mdetail.add(20000003, 1000, 123, 1, 1);
            //mdetail.add(20000004, 1000, 123, 1, 2);

			//ALTER IGNORE TABLE `metricdata` ADD UNIQUE INDEX `unique_records`(`timestamp`, `resource_id`, `metric_id`);
			
	        //do some reporting
	        logger.info("Records pulled from Gratia: " + records_pulled);
	        logger.info("Valid records being sent to MetricData/MetricDetail Tables: " + records_added);
	        logger.info("\tRecords with invalid resource_id: " + count_invalid_resource_id);
	        logger.info("\tRecords with invalid metric_id: " + count_invalid_metric_id);
	        logger.info("\tRecords with count_invalid_gatheredat: " + count_invalid_gatheredat);
	        logger.info("\tRecords with invalid status_id: " + count_invalid_status_id);
	        logger.info("\tRecords with invalid timestamp: " + count_invalid_timestamp);
	        //logger.info("\tRecords with invalid detail: " + count_invalid_detail);
	        
	        //now, let's commit all changes..
			mdetail.commit();
			lm.updateLastGratiaIDProcessed(last_dbid);	 
			
			logger.info("Updated process log with last dbid of " + last_dbid);
			
		} catch (SQLException e) {
			logger.error("SQL Error", e);
			//SendMail.sendErrorEmail(e.getMessage());
			ret = RSVMain.exitcode_error;
		}

		return ret;
	}
}
