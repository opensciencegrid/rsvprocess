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
        ResultSet rs = stmt.executeQuery("select dbid,ServiceUri,MetricName,MetricStatus, UNIX_Timestamp(Timestamp) as utimestamp, DetailsData from MetricRecord" + 
        		" where dbid > " + start_dbid + 
        		" order by dbid" +  //TODO - is this necessary?
        		" limit " + limit);
        return rs;
	}
	
	/* -- original version that pulls detail from MetricRecord table
	public String getDetail(int id) throws SQLException
	{
        Statement stmt = GratiaDatabase.db.createStatement();
        ResultSet rs = stmt.executeQuery("select DetailsData from MetricRecord where dbid = " + id);
        if(rs.next()) {
        	return rs.getString(1);
        }
        return null;
	}
	*/
	
	public String getDetail(int id) throws SQLException
	{
        Statement stmt = GratiaDatabase.db.createStatement();
        ResultSet rs = stmt.executeQuery("select extraxml from MetricRecord_Xml where dbid = " + id);
        if(rs.next()) {
        	String xml = rs.getString(1);
        	//unwrap <DetailsData> tags
        	if(xml.length() > 26) {
        		return xml.substring(13, xml.length() - 13 - 1);
        	} else {
        		//try MetricRecord
                stmt = GratiaDatabase.db.createStatement();
                rs = stmt.executeQuery("select DetailsData from MetricRecord where dbid = " + id);
                if(rs.next()) {
                	return rs.getString(1);
                }
        	}
        }
        return null;
	}
}
