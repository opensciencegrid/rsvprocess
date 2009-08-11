package rsv.process.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.HashMap;

import org.apache.log4j.Logger;

import rsv.process.control.RSVPreprocess;

public class MetricInserter extends RSVDatabase {
	private static final Logger logger = Logger.getLogger(MetricInserter.class);
	
	//private PreparedStatement prepStmt = null;
	private PreparedStatement stmt_data = null;
	private PreparedStatement stmt_detail = null;	
	private PreparedStatement stmt_detail_check = null;
	private HashMap<String, Integer> detail_in_batch = new HashMap<String, Integer>();
	
	public MetricInserter()
	{
		HashMap<String, Integer> detail_in_batch = new HashMap<String, Integer>();
		try {
			String sql = "insert into metricdata (id, timestamp, resource_id, metric_id, metric_status_id, detail_id) values (?,?,?,?,?,?)";
		    stmt_data = RSVDatabase.db.prepareStatement(sql);
		    
			sql = "insert into metricdetail (id, detail) values (?, ?)";
		    stmt_detail = RSVDatabase.db.prepareStatement(sql);
		    
		    sql = "select id from metricdetail where detail = ? limit 1";
		    stmt_detail_check = RSVDatabase.db.prepareStatement(sql);

		} catch (SQLException e) {
			logger.error("failed to prepare for butch insert", e);
		}
	}
	
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
	
	public void add(int id, int timestamp, int resource_id, int metric_id, int status_id, String detail) throws SQLException 
	{	
		Integer detail_id = id;
		
		//did we just inserted this already?
		Integer existing_id = detail_in_batch.get(detail);
		if(existing_id != null) {
			detail_id = existing_id;
		} else {
			//now look up on DB to see if the same detail already exists	
			stmt_detail_check.setString(1, detail);
			ResultSet res = stmt_detail_check.executeQuery();
			if(res.next()) {
				detail_id = res.getInt(1);
			} else {
				//doesn't exist. insert new detail
				stmt_detail.setInt(1, id);//use the same key as in the gratia DB
				stmt_detail.setString(2, detail);
				stmt_detail.addBatch();
				
				detail_in_batch.put(detail, id);
			}
		}

		//insert metricdata
		stmt_data.setInt(1, id); //use the same key as in the gratia DB
		stmt_data.setInt(2, timestamp);
		stmt_data.setInt(3, resource_id);
		stmt_data.setInt(4, metric_id);
		stmt_data.setInt(5, status_id);
		stmt_data.setInt(6, detail_id); //it could be the gratia DB (for now detail) or existing detail_id
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
		logger.info(recs + " records were inserted to MetricData");

		recs = 0;
		numUpdates = stmt_detail.executeBatch();  
		for(int i = 0;i < numUpdates.length; ++i) {
			recs += numUpdates[i];
		}
		logger.info(recs + " records were inserted to MetricDetail");

		//has any warning?
		SQLWarning w = stmt_data.getWarnings();
		if(w != null) {
			logger.warn(w.getMessage());
		}
		w = stmt_detail.getWarnings();
		if(w != null) {
			logger.warn(w.getMessage());
		}
	}

}
