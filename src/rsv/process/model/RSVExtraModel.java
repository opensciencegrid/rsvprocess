package rsv.process.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

public class RSVExtraModel extends ModelBase {
	
	private static final Logger logger = Logger.getLogger(RSVExtraModel.class);	
	
	public int getLastGratiaDBID() throws SQLException
	{
        Statement stmt = ModelBase.db.createStatement();
        ResultSet rs = stmt.executeQuery("select max(gratia_dbid) from rsvextra.metricdata");
        rs.next();
        return rs.getInt("max(gratia_dbid)");
	}
	
	public int loadMetricData(int last_dbid)
	{
		/*
        Statement stmt = ModelBase.db.createStatement();
        ResultSet rs = stmt.executeQuery("insert into rsvextra.metricdata " +
        		"select m.metric_id from gratia.MetricRecord g join oim.metric m on g.MetricName = m.name");	
        */
		return 999;
	}
}
