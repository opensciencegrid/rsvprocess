package rsv.process;

import rsv.process.model.GratiaModel;
import rsv.process.model.MetricInserter;
import rsv.process.model.OIMModel;
import rsv.process.model.ProcessLogModel;
import rsv.process.model.RSVExtraModel;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

public class RSVPreprocess implements RSVProcess {

	//preprocess property keys
	public static final String property_gratia_record_count = "preprocess.gratia_record_count";
	
	private static final Logger logger = Logger.getLogger(RSVPreprocess.class);
	
	public int run() 
	{
		int ret = RSVMain.exitcode_ok;
		
		try {
			//find the last processed dbid
			ProcessLogModel lm = new ProcessLogModel();
			int last_dbid = lm.getLastGratiaIDProcessed();
			logger.info("Resuming preprocess after Gratia DBID = " + last_dbid);
			
			OIMModel oim = new OIMModel();
			GratiaModel grartia = new GratiaModel();	
			int maxrecords = Integer.parseInt(RSVMain.conf.getProperty(property_gratia_record_count));
			
			//some bookkeeping
			int records_pulled = 0;
			int records_added = 0;

			logger.info("Pulling upto " + maxrecords + " records from Gratia.MetricRecord");
			ResultSet rs = grartia.getMetricRecords(last_dbid, maxrecords);
			
			MetricInserter mdetail = new MetricInserter();
			
			while(rs.next()){
	            records_pulled++;
	        	
	            //lookup resource_id
	            String resourcefqdn = rs.getString("ServiceUri");
	            Integer resource_id = oim.lookupResourceID(resourcefqdn);	   
	            if(resource_id == null) continue;
	           
	            //lookup metric_id
	            String metricname = rs.getString("MetricName");
	            Integer metric_id = oim.lookupMetricID(metricname);	   
	            if(metric_id == null) continue;
	            
	            //lookup status_id
	            String metricstatus = rs.getString("MetricStatus");
	            Integer status_id = oim.lookupStatusID(metricstatus);	   
	            if(status_id == null) continue;
	            	           
	            int dbid = rs.getInt("dbid");
	            last_dbid = dbid;
	            
	            int utimestamp = rs.getInt("utimestamp");
	            if(utimestamp == 0) continue;
	            
	            String metricdetail = rs.getString("DetailsData");
	            if(metricdetail == null) continue;
	            
	            //request for insertions
	            mdetail.add(dbid, utimestamp, resource_id, metric_id, status_id, metricdetail);
	            records_added++;
	        }

	        //do some reporting
	        logger.info("Records pulled from Gratia: " + records_pulled);
	        logger.info("Valid records being sent to MetricData/MetricDetail Tables: " + records_added);
	        
			mdetail.commit();
			
			logger.info("Updated process log with last dbid of " + last_dbid);
			
			lm.updateLastGratiaIDProcessed(last_dbid);	        
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.error("SQL Error", e);
			ret = RSVMain.exitcode_error;
		}

		return ret;
	}

}
