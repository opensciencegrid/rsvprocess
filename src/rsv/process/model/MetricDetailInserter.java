package rsv.process.model;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import org.apache.log4j.Logger;

import rsv.process.RSVPreprocess;

public class MetricDetailInserter extends ModelBase {
	private static final Logger logger = Logger.getLogger(MetricDetailInserter.class);
	
	//private PreparedStatement prepStmt = null;
	private PreparedStatement stmt = null;

	
	public MetricDetailInserter()
	{
		try {
		    String sql = "insert into rsvextra.metricdetail (gratia_dbid, detail) values (?, ?)";
		    stmt = ModelBase.db.prepareStatement(sql);
		} catch (SQLException e) {
			logger.error("failed to prepare for butch insert", e);
		}
	}
	
	public void add(int gratia_dbid, String detail) throws SQLException {
		stmt.setInt(1, gratia_dbid);
		stmt.setString(2, detail);
		stmt.addBatch();
	}
	
	//returns number of records inserted
	public int commit() throws SQLException
	{
		int records = 0;
		logger.info("Executing Insert Batch");
		int [] numUpdates=stmt.executeBatch();        
		for (int i=0; i < numUpdates.length; i++) {           
		    if (numUpdates[i] == -2)
		    	logger.warn("executeBatch(): Execution " + i + ": unknown number of rows updated");
		    else
		    	records += numUpdates[i];
		}
		
		//has any warning?
		SQLWarning w = stmt.getWarnings();
		if(w != null) {
			logger.warn(w.getMessage());
		}
		
		return records;
	}

}
