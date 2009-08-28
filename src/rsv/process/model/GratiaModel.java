package rsv.process.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

public class GratiaModel extends GratiaDatabase {
	
	private static final Logger logger = Logger.getLogger(GratiaModel.class);	
	
	public ResultSet getMetricRecords(int start_dbid, int limit) throws SQLException
	{
        Statement stmt = GratiaDatabase.db.createStatement();
        ResultSet rs = stmt.executeQuery("select dbid,ServiceUri,MetricName,MetricStatus, UNIX_Timestamp(Timestamp) as utimestamp from gratia.MetricRecord" + 
        		" where dbid > " + start_dbid + 
        		" order by dbid" +  //TODO - is this necessary?
        		" limit " + limit);
        return rs;
	}
	
	public String getDetail(int id) throws SQLException
	{
        Statement stmt = GratiaDatabase.db.createStatement();
        ResultSet rs = stmt.executeQuery("select DetailsData from gratia.MetricRecord where dbid = " + id);
        if(rs.next()) {
        	return rs.getString(1);
        }
        return null;
	}
}
