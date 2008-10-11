package rsv.process.control;

import rsv.process.model.GratiaModel;
import rsv.process.model.MetricInserter;
import rsv.process.model.OIMModel;
import rsv.process.model.ProcessLogModel;

import java.sql.ResultSet;
import java.sql.SQLException;

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
		ProcessLogModel lm = new ProcessLogModel();
		int maxrecords = Integer.parseInt(RSVMain.conf.getProperty(Configuration.preprocess_gratia_record_count));
		
		//for bookkeeping
		int records_pulled = 0;
		int records_added = 0;
		
		try {
			
			//find the last processed dbid
			int last_dbid = lm.getLastGratiaIDProcessed();
			logger.info("Resuming preprocess after Gratia DBID = " + last_dbid);
			
			//make sure what log says is true - or we will end of processing same records twice
			int removed = mdetail.clearRecords(last_dbid);
			if(removed != 0) {
				logger.warn("Found "+removed+" records (total) with dbid larger than what we see in process log. Removing them for data integrity.");
			}
			
			//get gratia records
			logger.info("Pulling upto " + maxrecords + " records from Gratia.MetricRecord");
			ResultSet rs = grartia.getMetricRecords(last_dbid, maxrecords);
			
			//error counters
			int count_invalid_resource_id = 0;
			int count_invalid_metric_id = 0;
			int count_invalid_status_id = 0;
			int count_invalid_timestamp = 0;
			int count_invalid_detail = 0;
			
			//process each records
			while(rs.next()){
	            records_pulled++;
	            
	            //lookup dbid
	            int dbid = rs.getInt("dbid");
	            last_dbid = dbid;
	        	
	            //lookup resource_id
	            String resourcefqdn = rs.getString("ServiceUri");
	            Integer resource_id = oim.lookupResourceID(resourcefqdn);	   
	            if(resource_id == null) {
	            	count_invalid_resource_id++;
	            	continue;
	            }
	           
	            //lookup metric_id
	            String metricname = rs.getString("MetricName");
	            Integer metric_id = oim.lookupMetricID(metricname);	   
	            if(metric_id == null) {
	            	count_invalid_metric_id++;
	            	continue;
	            }
	            
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
	            
	            //lookup metricdetail
	            String metricdetail = rs.getString("DetailsData");
	            if(metricdetail == null) {
	            	count_invalid_detail++;
	            	continue;
	            }
	            
	            //all good. request for insertions
	            mdetail.add(dbid, utimestamp, resource_id, metric_id, status_id, metricdetail);
	            records_added++;
	        }

	        //do some reporting
	        logger.info("Records pulled from Gratia: " + records_pulled);
	        logger.info("Valid records being sent to MetricData/MetricDetail Tables: " + records_added);
	        logger.info("\tRecords with invalid resource_id: " + count_invalid_resource_id);
	        logger.info("\tRecords with invalid metric_id: " + count_invalid_metric_id);
	        logger.info("\tRecords with invalid status_id: " + count_invalid_status_id);
	        logger.info("\tRecords with invalid timestamp: " + count_invalid_timestamp);
	        logger.info("\tRecords with invalid detail: " + count_invalid_detail);
	        
	        //now, let's commit all changes..
			mdetail.commit();
			lm.updateLastGratiaIDProcessed(last_dbid);	 
			
			logger.info("Updated process log with last dbid of " + last_dbid);
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.error("SQL Error", e);
			ret = RSVMain.exitcode_error;
		}

		return ret;
	}

}
