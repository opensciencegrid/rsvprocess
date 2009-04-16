package rsv.process.model;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import org.apache.log4j.Logger;

import rsv.process.control.RSVPreprocess;

public class RecentMetricInserter extends RSVDatabase {
	private static final Logger logger = Logger.getLogger(RecentMetricInserter.class);
	
	//private PreparedStatement prepStmt = null;
	private PreparedStatement stmt_data = null;
	
	public RecentMetricInserter()
	{
		try {
			String sql = "insert into rsvextra.recent_metricdata (id, fqan, metric_name, metric_status, timestamp, detail) values (?,?,?,?,?,?)";
		    stmt_data = RSVDatabase.db.prepareStatement(sql);

		} catch (SQLException e) {
			logger.error("failed to prepare for butch insert", e);
		}
	}
	
	//clear old records
	public int clearOldRecords() throws SQLException
	{
		int recs = 0;
		String sql = "delete from rsvextra.recent_metricdata where timestamp < (curtime() - 86400*3)"; //keep 3 days
		PreparedStatement stmt = RSVDatabase.db.prepareStatement(sql);		
	    stmt.execute();
	    recs += stmt.getUpdateCount();
	    
	    return recs;
	}
	
	public void add(int id, String fqan, String metric_name, String metric_status, int timestamp, String detail) throws SQLException {
		stmt_data.setInt(1, id);
		stmt_data.setString(2, fqan);
		stmt_data.setString(3, metric_name);
		stmt_data.setString(4, metric_status);
		stmt_data.setInt(5, timestamp);
		stmt_data.setString(6, detail);
		stmt_data.addBatch();
	}
	
	//returns number of records inserted
	public void commit() throws SQLException
	{
		logger.info("Executing Insert Batch");
		
		int recs = 0;
		int[] numUpdates = stmt_data.executeBatch();    
		for(int i = 0;i < numUpdates.length; ++i) {
			recs += numUpdates[i];
		}
		logger.info(recs + " records were inserted to Recent MetricData");

		//has any warning?
		SQLWarning w = stmt_data.getWarnings();
		if(w != null) {
			logger.warn(w.getMessage());
		}
	}

}
