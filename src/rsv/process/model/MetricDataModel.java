package rsv.process.model;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;
import java.util.TreeMap;

import rsv.process.model.record.MetricData;
import rsv.process.model.record.Resource;

public class MetricDataModel extends ModelBase {
	private static final Logger logger = Logger.getLogger(MetricDataModel.class);	
	
	//pull the last metric data set before specified timestamp on specified resource
	//returns map of <metric_id, metricdata record>
	//note - on rsv viewer, similar sql exists but it pull metric including at the specified timestamp.
	//here, we don't include it because metric *at* the timestamp given will be pulled as part of ITP 
	//records
	public class LMDType extends TreeMap<Integer, MetricData> {}
	public LMDType getLastMetricDataSet(int resource_id, Integer timestamp) throws SQLException {
		LMDType ret = new LMDType();
        Statement stmt = ModelBase.db.createStatement();
        String where_timestamp = "";
        if(timestamp != null) {
        	where_timestamp = " and timestamp < " + timestamp + " ";
        }
        String sql = "select * from metricdata m, "+
        	"(select max(timestamp) last_timestamp ,metric_id from metricdata "+
        	"where resource_id = "+ resource_id + " " + where_timestamp +
        	"group by metric_id) last "+
        	"where m.timestamp = last.last_timestamp and m.metric_id = last.metric_id and m.resource_id = " + resource_id +
        	" order by timestamp";
        //logger.debug(sql);
        ResultSet rs = stmt.executeQuery(sql);
        while(rs.next()) {
        	Integer metric_id = rs.getInt("metric_id");
        	MetricData m = new MetricData(rs);
        	ret.put(metric_id, m);
        }
        return ret;
	}
	
	public String getDetail(int id) throws SQLException {
        Statement stmt = ModelBase.db.createStatement();
        ResultSet rs = stmt.executeQuery("select detail from metricdetail where metricdata_id = " + id);
        rs.next();
        return rs.getString(1);
	}
	
	public ResultSet getMetricDataRecords(int start_dbid, int limit) throws SQLException
	{
        Statement stmt = ModelBase.db.createStatement();
        
        ResultSet rs = stmt.executeQuery("select * from rsvextra.metricdata" + 
        		" where id > " + start_dbid + 
        		" order by id " +
        		" limit " + limit); 
        
        return rs;
	}
	
	public MetricData getLastNonUnknownMetricData(int resource_id, int metric_id, int timestamp) throws SQLException
	{
		String sql = "select * from metricdata where resource_id = ? and metric_id = ? and timestamp < ? order by timestamp desc limit 1";
		PreparedStatement stmt = ModelBase.db.prepareStatement(sql);		
	    stmt.setInt(1, resource_id);
	    stmt.setInt(2, metric_id);
	    stmt.setInt(3, timestamp);
	    ResultSet rs = stmt.executeQuery();
	    if(rs.next()) {
        	MetricData m = new MetricData(rs);
        	return m; 	
	    }
	    return null;
	}
	
	//get metric data between start and end
	public ArrayList<MetricData> getMeticData(int resource_id, int start, int end) throws SQLException
	{
		ArrayList<MetricData> mds = new ArrayList<MetricData>();
		String sql = "select * from metricdata where resource_id = ? and timestamp >= ? and timestamp <= ? order by timestamp";
		PreparedStatement stmt = ModelBase.db.prepareStatement(sql);		
	    stmt.setInt(1, resource_id);
	    stmt.setInt(2, start);
	    stmt.setInt(3, end);
	    ResultSet rs = stmt.executeQuery();		
	    while(rs.next()) {
        	MetricData m = new MetricData(rs);
        	mds.add(m);
	    }		
		return mds;
	}
}
