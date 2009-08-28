package rsv.process.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

public class ProcessLogModel extends RSVDatabase {
	private static final Logger logger = Logger.getLogger(ProcessLogModel.class);	
	
	private String getValue(String key) throws SQLException
	{
		PreparedStatement stmt = RSVDatabase.db.prepareStatement("select value from processlog where `key` = ?");
		stmt.setString(1, key);
		ResultSet rs = stmt.executeQuery();
		if(rs.next()) {
			return rs.getString(1);
		}
		return null;
	}
	
	private void set(String key, String value) throws SQLException
	{
		String sql;
		if(getValue(key) == null) {
			sql = "insert into processlog (value, `key`, timestamp) values (?, ?, NOW())";
		} else {
			sql = "update processlog set value = ?, timestamp = NOW() where `key` = ?";
		}
	    PreparedStatement stmt = RSVDatabase.db.prepareStatement(sql);
		stmt.setString(1, value);
	    stmt.setString(2, key);
		stmt.execute();	
	}
	
	public int getLastGratiaIDProcessed() throws SQLException
	{
		String value = getValue("last_gratia_id_processed");
		if(value == null) return 0;
		return Integer.parseInt(value);
	}
	public void updateLastGratiaIDProcessed(int newid) throws SQLException
	{
		set("last_gratia_id_processed", new Integer(newid).toString());
	}
	
	public int getLastMetricDataIDProcessed() throws SQLException
	{
		String value = getValue("last_metricdata_id_processed");
		if(value == null) return 0;
		return Integer.parseInt(value);
	}
	public void updateLastMetricDataIDProcessed(int newid) throws SQLException
	{
		set("last_metricdata_id_processed", new Integer(newid).toString());
	}
}
