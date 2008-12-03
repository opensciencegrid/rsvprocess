package rsv.process.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

public class ProcessLogModel extends RSVDatabase {
	private static final Logger logger = Logger.getLogger(ProcessLogModel.class);	
	
	private String getLastValueFor(String key) throws SQLException
	{
		PreparedStatement stmt = RSVDatabase.db.prepareStatement("select value from processlog p, "+
				"(select `key`, max(timestamp) as timestamp from processlog " +
				"where `key` = ? group by `key`) l " +
				"where p.`key` = l.`key` and p.timestamp = l.timestamp ");
		stmt.setString(1, key);
		ResultSet rs = stmt.executeQuery();
		if(rs.next()) {
			return rs.getString(1);
		}
		return null;
	}
	
	private void update(String key, String value) throws SQLException
	{
	    String sql = "insert into processlog (`key`, value, timestamp) values (?, ?, NOW())";
	    PreparedStatement stmt = RSVDatabase.db.prepareStatement(sql);
		stmt.setString(1, key);
	    stmt.setString(2, value);
		stmt.execute();		
	}
	
	public int getLastGratiaIDProcessed() throws SQLException
	{
		String value = getLastValueFor("last_gratia_id_processed");
		if(value == null) return 0;
		return Integer.parseInt(value);
	}
	public void updateLastGratiaIDProcessed(int newid) throws SQLException
	{
		update("last_gratia_id_processed", new Integer(newid).toString());
	}
	
	public int getLastMetricDataIDProcessed() throws SQLException
	{
		String value = getLastValueFor("last_metricdata_id_processed");
		if(value == null) return 0;
		return Integer.parseInt(value);
	}
	public void updateLastMetricDataIDProcessed(int newid) throws SQLException
	{
		update("last_metricdata_id_processed", new Integer(newid).toString());
	}
}
