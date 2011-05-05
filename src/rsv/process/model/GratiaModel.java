package rsv.process.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import rsv.process.model.record.GratiaMetricRecord;

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
	
	/*
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
	*/
	
	public GratiaMetricRecord getDetail(int id) throws SQLException
	{
        Statement stmt = GratiaDatabase.db.createStatement();
        ResultSet rs = stmt.executeQuery("select DetailsData,ServiceUri,GatheredAt from MetricRecord where dbid = " + id);
        if(rs.next()) {
        	GratiaMetricRecord rec = new GratiaMetricRecord();
        	rec.DetailsData = rs.getString(1);
        	rec.ServiceUri = rs.getString(2);
        	rec.GatheredAt = rs.getString(3);
        
        	if(rec.DetailsData.length() == 255) {
            	//details maybe truncated.. try loading xml version
                rs = stmt.executeQuery("select extraxml from MetricRecord_Xml where dbid = " + id);
                if(rs.next()) {
    	        	//unwrap <DetailsData> tags
                	String xml = rs.getString(1);
    	        	if(xml.length() > 26) {
    	        		rec.DetailsData = xml.substring(13, xml.length() - 13 - 1);
    	        	}
                }
        	}
        	return rec;
        }
        return null;
	}	
}
