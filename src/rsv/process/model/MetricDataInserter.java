package rsv.process.model;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import rsv.process.RSVPreprocess;

public class MetricDataInserter extends ModelBase {
	private static final Logger logger = Logger.getLogger(MetricDataInserter.class);
	
	//private PreparedStatement prepStmt = null;
	private Statement stmt = null;
	private String batch = "";
	
	public MetricDataInserter()
	{
		try {
			ModelBase.db.setAutoCommit(false);
	        stmt = ModelBase.db.createStatement();
		} catch (SQLException e) {
			logger.error("failed to prepare for butch insert", e);
		}
	}
	
	public void add(int timestamp, int resource_id, int metric_id, int metric_status_id, int gratia_dbid) throws SQLException {
		if(batch.length() < 3000) {
			if(batch.length() != 0) batch += ", ";
			batch += "(" + 
				timestamp +", " + 
				resource_id + ", " + 
				metric_id + ", " + 
				metric_status_id + ", " + 
				gratia_dbid + ")";
		} else {
			String sql = "insert into rsvextra.metricdata" +
				" (timestamp, resource_id, metric_id, metric_status_id, gratia_dbid) values " + batch;
			//logger.debug(sql);
			stmt.addBatch(sql);
			batch = "";
		}
	}
	
	//returns number of records inserted
	public int commit() throws SQLException
	{
		int records = 0;
		logger.info("Inserting new metricdata records in our insert batch");
		int [] numUpdates=stmt.executeBatch();              
		for (int i=0; i < numUpdates.length; i++) {           
		    if (numUpdates[i] == -2)
		    	logger.warn("executeBatch(): Execution " + i + ": unknown number of rows updated");
		    else
		    	records += numUpdates[i];
		}
		ModelBase.db.commit(); 
		ModelBase.db.setAutoCommit(true);
		
		return records;
	}

}
