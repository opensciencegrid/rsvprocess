package rsv.process;

import rsv.process.model.GratiaModel;
import rsv.process.model.MetricDataInserter;
import rsv.process.model.OIMModel;
import rsv.process.model.RSVExtraModel;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

public class RSVPreprocess implements RSVProcess {
	
	private static final Logger logger = Logger.getLogger(RSVPreprocess.class);
	
	public int run() 
	{
		try {
			//find the last processed dbid
			RSVExtraModel rem = new RSVExtraModel();
			int last_dbid = rem.getLastGratiaDBID();
			logger.info("Resuming preprocess after Gratia DBID = " + last_dbid);
			
			OIMModel oim = new OIMModel();
			GratiaModel grartia = new GratiaModel();	
			int maxrecords = 5000;
			int records_pulled = 0;
			int records_added = 0;
			logger.info("Processing upto " + maxrecords + " records from Gratia.MetricRecord");
			ResultSet rs = grartia.getMetricRecords(last_dbid, maxrecords);
			
			MetricDataInserter md = new MetricDataInserter();
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
	            int utimestamp = rs.getInt("utimestamp");
	            
	            md.add(utimestamp, resource_id, metric_id, status_id, dbid);
	            records_added++;
	        }
	        int records_inserted = md.commit();
	        
	        logger.info("Records pulled from Gratia: " + records_pulled);
	        logger.info("Records sent to Metricdata Table: " + records_added);
	        logger.info("Records inserted to MetricData table: " + records_inserted);  
	        if(records_added != records_inserted) {
	        	logger.warn("Record counts for sent("+records_added+
	        			") and actually inserted("+records_inserted+
	        			") to metricdata table is different. It should be the same");
	        }
	        
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.error("SQL Error", e);
		}

		return RSVMain.exitcode_ok;
	}

}
