package rsv.process.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class OIMModel extends ModelBase {
	private static final Logger logger = Logger.getLogger(OIMModel.class);	
	
	private static HashMap<String, Integer> cache_resource_fqdn2id = null;
	public Integer lookupResourceID(String fqdn) throws SQLException
	{
		if(cache_resource_fqdn2id == null) {
			cache_resource_fqdn2id = new HashMap<String, Integer>();
	        Statement stmt = ModelBase.db.createStatement();
	        ResultSet rs = stmt.executeQuery("select * from oim.resource");			
	        while(rs.next()) {
	        	Integer resource_id = rs.getInt("resource_id");
	        	String resource_fqdn = rs.getString("fqdn");
	        	cache_resource_fqdn2id.put(resource_fqdn, resource_id);
	        	//logger.debug("Adding " + resource_fqdn);
	        }
		}
		return cache_resource_fqdn2id.get(fqdn);
	}
	
	private static HashMap<String, Integer> cache_metric_name2id = null;
	public Integer lookupMetricID(String name) throws SQLException
	{
		if(cache_metric_name2id == null) {
			cache_metric_name2id = new HashMap<String, Integer>();
	        Statement stmt = ModelBase.db.createStatement();
	        ResultSet rs = stmt.executeQuery("select * from oim.metric");			
	        while(rs.next()) {
	        	Integer metric_id = rs.getInt("metric_id");
	        	String metric_name = rs.getString("name");
	        	cache_metric_name2id.put(metric_name, metric_id);
	        	//logger.debug("Adding " + resource_fqdn);
	        }
		}
		return cache_metric_name2id.get(name);
	}
	
	private static HashMap<String, Integer> cache_status_name2id = null;
	public Integer lookupStatusID(String name) throws SQLException
	{
		if(cache_status_name2id == null) {
			cache_status_name2id = new HashMap<String, Integer>();
	        Statement stmt = ModelBase.db.createStatement();
	        ResultSet rs = stmt.executeQuery("select * from oim.metric_status");			
	        while(rs.next()) {
	        	Integer status_id = rs.getInt("metric_status_id");
	        	String status_name = rs.getString("metric_status_description");
	        	cache_status_name2id.put(status_name, status_id);
	        	//logger.debug("Adding " + resource_fqdn);
	        }
		}
		return cache_status_name2id.get(name);
	}
}
