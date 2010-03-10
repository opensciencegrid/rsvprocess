package rsv.process.model;

import java.sql.BatchUpdateException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.HashMap;

import org.apache.log4j.Logger;

import rsv.process.Configuration;
import rsv.process.control.RSVMain;
import rsv.process.control.RSVPreprocess;

public class MetricInserter extends RSVDatabase {
	private static final Logger logger = Logger.getLogger(MetricInserter.class);
	
	//private PreparedStatement prepStmt = null;
	private PreparedStatement stmt_data = null;
	private PreparedStatement stmt_detail = null;
	
	public MetricInserter()
	{
		String sql;
		HashMap<String, Integer> detail_in_batch = new HashMap<String, Integer>();
		try {
			sql = "insert into metricdata (id, timestamp, resource_id, metric_id, metric_status_id) values (?,?,?,?,?)";
		    stmt_data = RSVDatabase.db.prepareStatement(sql);
		    
			sql = "insert into metricdetail (id, detail) values (?,?)";
			stmt_detail = RSVDatabase.db.prepareStatement(sql);
		    
		} catch (SQLException e) {
			logger.error("failed to prepare for batch insert", e);
		}
	}
	/*
	//remove all records from metricdetail and metricdata with dbid larger than last_dbid
	public int clearRecords(int last_dbid) throws SQLException
	{
		int recs = 0;
		String sql = "delete from metricdetail where metricdata_id > ?";
		PreparedStatement stmt = RSVDatabase.db.prepareStatement(sql);		
	    stmt.setInt(1, last_dbid);
	    stmt.execute();
	    recs += stmt.getUpdateCount();
	    
		sql = "delete from metricdata where id > ?";
		stmt = RSVDatabase.db.prepareStatement(sql);		
	    stmt.setInt(1, last_dbid);
	    stmt.execute();
	    recs += stmt.getUpdateCount();	   
	    
	    return recs;
	}
	*/
	public void add(int id, int timestamp, int resource_id, int metric_id, int status_id, String detail) throws SQLException 
	{	
		stmt_data.setInt(1, id); //use the same key as in the gratia DB
		stmt_data.setInt(2, timestamp);
		stmt_data.setInt(3, resource_id);
		stmt_data.setInt(4, metric_id);
		stmt_data.setInt(5, status_id);
		stmt_data.addBatch();
		
		stmt_detail.setInt(1, id);
		stmt_detail.setString(2, detail);
		stmt_detail.addBatch();
	}
	
	//returns number of records inserted
	public void commit() throws SQLException
	{
		commit_data();
		String store_metricdetail = RSVMain.conf.getProperty("store_metricdetail");
		if(store_metricdetail != null && store_metricdetail.equals("true")) {
			commit_detail();
		}
	}
	public void commit_data() throws SQLException {
		
		logger.info("Executing Insert Batch for metricdata");
		int recs = 0;
		try {
			int[] numUpdates = stmt_data.executeBatch();    
			for(int i = 0;i < numUpdates.length; ++i) {
				recs += numUpdates[i];
			}
			logger.info(recs + " records were inserted to metricdata");
			
			//has any warning?
			SQLWarning w = stmt_data.getWarnings();
			if(w != null) {
				logger.warn(w.getMessage());
			}
		} catch (BatchUpdateException e) {
			logger.error("BatchUpdateException on metricdata insertion", e);
		}
	}
	
	public void commit_detail() throws SQLException {
		logger.info("Executing Insert Batch for metricdetail");
		int recs = 0;
		try {
			int[] numUpdates = stmt_detail.executeBatch();    
			for(int i = 0;i < numUpdates.length; ++i) {
				recs += numUpdates[i];
			}
			logger.info(recs + " records were inserted to metricdetail");
			
			//has any warning?
			SQLWarning w = stmt_detail.getWarnings();
			if(w != null) {
				logger.warn(w.getMessage());
			}
		} catch (BatchUpdateException e) {
			logger.error("BatchUpdateException on metricdetail insertion", e);
		}
	}

}
