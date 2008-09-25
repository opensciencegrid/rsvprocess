package rsv.process.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

public class GratiaModel extends ModelBase {
	
	private static final Logger logger = Logger.getLogger(GratiaModel.class);	
	
	public ResultSet getMetricRecords(int start_dbid, int limit) throws SQLException
	{
        Statement stmt = ModelBase.db.createStatement();
        ResultSet rs = stmt.executeQuery("select *, UNIX_Timestamp(Timestamp) as utimestamp from gratia.MetricRecord" + 
        		" where dbid > " + start_dbid + 
        		" order by dbid" +  //TODO - is this necessary?
        		" limit " + limit);
        return rs;
	}
}
