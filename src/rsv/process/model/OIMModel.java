package rsv.process.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Comparator;
import org.apache.log4j.Logger;

import rsv.process.model.record.*;

@SuppressWarnings("serial")

public class OIMModel extends OIMDatabase {
	private static final Logger logger = Logger.getLogger(OIMModel.class);	
	
	private static HashMap<String, Integer> cache_resource_fqdn2id = null;
	public Integer lookupResourceID(String fqdn) throws SQLException
	{
		if(cache_resource_fqdn2id == null) {
			cache_resource_fqdn2id = new HashMap<String, Integer>();
	        Statement stmt = OIMDatabase.db.createStatement();
	        ResultSet rs = stmt.executeQuery("select id,fqdn from resource where active = 1 and disable = 0");			
	        while(rs.next()) {
	        	Integer resource_id = rs.getInt("id");
	        	String resource_fqdn = rs.getString("fqdn");
	        	cache_resource_fqdn2id.put(resource_fqdn, resource_id);
	        }
		}
		return cache_resource_fqdn2id.get(fqdn);
	}
	
	private static HashMap<String, Integer> cache_resource_alias2id = null;
	public Integer lookupResourceAlias(String alias) throws SQLException
	{
		if(cache_resource_alias2id == null) {
			cache_resource_alias2id = new HashMap<String, Integer>();
	        Statement stmt = OIMDatabase.db.createStatement();
	        ResultSet rs = stmt.executeQuery("select * from resource_alias");			
	        while(rs.next()) {
	        	Integer resource_id = rs.getInt("resource_id");
	        	String resource_alias = rs.getString("resource_alias");
	        	cache_resource_alias2id.put(resource_alias, resource_id);
	        }
		}
		return cache_resource_alias2id.get(alias);
	}
	
	private static HashMap<String, Integer> cache_resource_service2id = null;
	public Integer lookupServiceHostEndpointOverride(String override) throws SQLException
	{
		if(cache_resource_service2id == null) {
			cache_resource_service2id = new HashMap<String, Integer>();
	        Statement stmt = OIMDatabase.db.createStatement();
	        //TODO - simply looking at url_override could match non grid service group service detail..
	        //TODO - Also, I need to not use uri_override from resource that are inactive, or disabled
	        ResultSet rs = stmt.executeQuery("SELECT * FROM resource_service_detail WHERE `key` = 'uri_override'");			
	        while(rs.next()) {
	        	Integer resource_id = rs.getInt("resource_id");
	        	String endpoint_override = rs.getString("value");
	        	cache_resource_service2id.put(OIMModel.pullHostname(endpoint_override), resource_id);
	        }
		}
		return cache_resource_service2id.get(override);
	}
	
	//convert "se1.accre.vanderbilt.edu:6288/foo/xxx" into "se1.accre.vanderbilt.edu"
	public static String pullHostname(String uri)
	{
		if(uri == null) return null;
		
		int pos = uri.lastIndexOf(':');		
		if(pos == -1) {
			//if I can't find :, then look for / just in case
			int slash_pos = uri.lastIndexOf('/');
			if(slash_pos == -1) {
				//all good..
				return uri;
			}
			pos = slash_pos;
		}
		return uri.substring(0, pos);
	}
	
	public static class ResourcesType extends TreeMap<Integer/*resource_id*/, Resource> {}
	private static ResourcesType cache_resource_id2rec = null;
	public ResourcesType getResources() throws SQLException
	{
		if(cache_resource_id2rec == null) {
			cache_resource_id2rec = new ResourcesType();
			Statement stmt = OIMDatabase.db.createStatement();
	        ResultSet rs = stmt.executeQuery("select id,name from resource where active = 1 and disable = 0");			
	        while(rs.next()) {
	        	Resource rec = new Resource();
	        	int id = rs.getInt("id");
	        	rec.setID(id);
	        	rec.setName(rs.getString("name"));
	        	
	        	//TODO - gather other information that I need
	        	
	        	cache_resource_id2rec.put(id, rec);	        	
	        }
		}
		return cache_resource_id2rec;
	}
	
	private static HashMap<String, Integer> cache_metric_name2id = null;
	public Integer lookupMetricID(String name) throws SQLException
	{
		if(cache_metric_name2id == null) {
			cache_metric_name2id = new HashMap<String, Integer>();
	        Statement stmt = OIMDatabase.db.createStatement();
	        ResultSet rs = stmt.executeQuery("select id,name from metric");			
	        while(rs.next()) {
	        	Integer metric_id = rs.getInt("id");
	        	String metric_name = rs.getString("name");
	        	cache_metric_name2id.put(metric_name, metric_id);
	        }
		}
		return cache_metric_name2id.get(name);
	}
	
	private static HashMap<String, Integer> cache_status_name2id = null;
	public Integer lookupStatusID(String name) throws SQLException
	{
		if(cache_status_name2id == null) {
			cache_status_name2id = new HashMap<String, Integer>();
	        Statement stmt = OIMDatabase.db.createStatement();
	        ResultSet rs = stmt.executeQuery("select id,description from metric_status");			
	        while(rs.next()) {
	        	Integer status_id = rs.getInt("id");
	        	String status_name = rs.getString("description");
	        	cache_status_name2id.put(status_name, status_id);
	        }
		}
		return cache_status_name2id.get(name);
	}
	
	static class CaseInsensitiveComparator implements Comparator<String>
	{
		public int compare(String element1, String element2)
		{
			String a = element1.toString().toLowerCase();
			String b = element2.toString().toLowerCase();
			return a.compareTo( b );
    	}
	}
	
	private static TreeMap<String, Integer> cache_vo_name2id = null;
	public Integer lookupVOID(String name) throws SQLException
	{
		if(cache_vo_name2id == null) {
			//Remove CaseInsensitiveComparator once we sort out issues with VO names
			cache_vo_name2id = new TreeMap<String, Integer>(new CaseInsensitiveComparator());
	        Statement stmt = OIMDatabase.db.createStatement();
	        ResultSet rs = stmt.executeQuery("select id,name from vo where active = 1 and disable = 0");			
	        while(rs.next()) {
	        	Integer vo_id = rs.getInt("id");
	        	String short_name = rs.getString("name");
	        	cache_vo_name2id.put(short_name, vo_id);
	        	//logger.debug("Adding " + resource_fqdn);
	        }
		}
		return cache_vo_name2id.get(name);
	}
	
	private static TreeMap<Integer, VirtualOrganization> cache_vo_id2vo = null;
	public VirtualOrganization lookupVO(Integer id) throws SQLException
	{
		if(cache_vo_id2vo == null) {
			//Remove CaseInsensitiveComparator once we sort out issues with VO names
			cache_vo_id2vo = new TreeMap<Integer, VirtualOrganization>();
	        Statement stmt = OIMDatabase.db.createStatement();
	        ResultSet rs = stmt.executeQuery("select * from vo where active = 1 and disable = 0");			
	        while(rs.next()) {
	        	VirtualOrganization vo = new VirtualOrganization(rs);
	        	cache_vo_id2vo.put(vo.getID(), vo);
	        }
		}
		return cache_vo_id2vo.get(id);
	}
	
	//hashmap<metric_id, fresh_for seconds>
	private static HashMap<Integer, Integer> cache_metric_id2freshfor = null;
	public Integer lookupFreshFor(int metric_id) throws SQLException
	{
		if(cache_metric_id2freshfor == null) {
			cache_metric_id2freshfor = new HashMap<Integer, Integer>();
	        Statement stmt = OIMDatabase.db.createStatement();
	        ResultSet rs = stmt.executeQuery("select id, fresh_for from metric");			
	        while(rs.next()) {
	        	Integer id = rs.getInt("id");
	        	Integer value = rs.getInt("fresh_for");
	        	cache_metric_id2freshfor.put(id, value);
	        }
		}
		return cache_metric_id2freshfor.get(metric_id);
	}
	
	//check if md is fresh at timestamp
	public boolean isFresh(MetricData md, int timestamp) throws SQLException {
		int freshfor = md.getFreshFor();
		if(timestamp < md.getTimestamp() + freshfor) {
			return true;
		}
		return false;
	}
	

	public static class GetResourceGroupsType extends HashMap<Integer/*resource_id*/, Integer/*resource_group_id*/> {}
	/*
	private static GetResourceGroupsType cache_rrg_id2gid = null;
	public Integer getResourceGroup(int resource_id) throws SQLException {
		if(cache_rrg_id2gid == null) {
			cache_rrg_id2gid = new GetResourceGroupsType();
	        Statement stmt = RSVDatabase.db.createStatement();
	        String sql = "select * from resource";
	        ResultSet rs = stmt.executeQuery(sql);
	        while(rs.next()) {
	        	Integer rid = rs.getInt("id");
	        	Integer gid = rs.getInt("resource_group_id");
	        	cache_rrg_id2gid.put(rid, gid);
	        }
		}
		return cache_rrg_id2gid.get(resource_id);
	}	
	*/
	
	//public class ResourceServiceType extends TreeMap<Integer, ArrayList<Integer>> {}//<service_id, status_is>
	private static TreeMap<Integer, ArrayList<Integer>> cache_resourceservice_rid2sid = null;
	public ArrayList<Integer> getResourceService(Integer resource_id) throws SQLException {
		if(cache_resourceservice_rid2sid == null) {
			cache_resourceservice_rid2sid = new TreeMap<Integer, ArrayList<Integer>>();
	        Statement stmt = OIMDatabase.db.createStatement();
	        String sql = "SELECT rs.resource_id, rs.service_id FROM resource_service rs join service s on rs.service_id = s.id";
	        ResultSet rs = stmt.executeQuery(sql);
	        while(rs.next()) {
	        	Integer id = rs.getInt("resource_id");
	        	Integer service_id = rs.getInt("service_id");
	        	ArrayList<Integer> list = cache_resourceservice_rid2sid.get(id);
	        	if(list == null) {
	        		list = new ArrayList<Integer>();
	        		cache_resourceservice_rid2sid.put(id, list);
	        	}
	        	list.add(service_id);
	        }
		}
		ArrayList<Integer> list = cache_resourceservice_rid2sid.get(resource_id);
		if(list == null) return new ArrayList<Integer>();
		return list;
	}
	
	//return list of metric_ids that are critical for the service
	private static TreeMap<Integer, ArrayList<Integer>> cache_criticalmetric_id2ids = null;
	public ArrayList<Integer> getCriticalMetrics(Integer service_id) throws SQLException{
		if(cache_criticalmetric_id2ids == null) {
			cache_criticalmetric_id2ids = new TreeMap<Integer, ArrayList<Integer>>();
	        Statement stmt = OIMDatabase.db.createStatement();
	        String sql = "select metric_id, service_id from metric_service where critical = 1";
	        ResultSet rs = stmt.executeQuery(sql);		
	        while(rs.next()) {
	        	Integer mid = rs.getInt("metric_id");
	        	Integer sid = rs.getInt("service_id");
	        	ArrayList<Integer> list = cache_criticalmetric_id2ids.get(sid);
	        	if(list == null) {
	        		list = new ArrayList<Integer>();
	        		cache_criticalmetric_id2ids.put(sid, list);
	        	}
	        	list.add(mid);
	        }
		}
		ArrayList<Integer> list = cache_criticalmetric_id2ids.get(service_id);
		if(list == null) return new ArrayList<Integer>();
		return list;
	}
	
	//return list of metric_ids that are non-critical for the service
	private static TreeMap<Integer, ArrayList<Integer>> cache_non_criticalmetric_id2ids = null;
	public ArrayList<Integer> getNonCriticalMetrics(Integer service_id) throws SQLException{
		if(cache_non_criticalmetric_id2ids == null) {
			cache_non_criticalmetric_id2ids = new TreeMap<Integer, ArrayList<Integer>>();
	        Statement stmt = OIMDatabase.db.createStatement();
	        String sql = "select metric_id, service_id from metric_service where critical = 0";
	        ResultSet rs = stmt.executeQuery(sql);		
	        while(rs.next()) {
	        	Integer mid = rs.getInt("metric_id");
	        	Integer sid = rs.getInt("service_id");
	        	ArrayList<Integer> list = cache_non_criticalmetric_id2ids.get(sid);
	        	if(list == null) {
	        		list = new ArrayList<Integer>();
	        		cache_non_criticalmetric_id2ids.put(sid, list);
	        	}
	        	list.add(mid);
	        }
		}
		ArrayList<Integer> list = cache_non_criticalmetric_id2ids.get(service_id);
		if(list == null) return new ArrayList<Integer>();
		return list;
	}
	
	//return list of service_ids that a given metric_id is critical for (inverse of getCriticalMetrics)
	private static TreeMap<Integer/*metric_id*/, ArrayList<Integer/*service_id*/>> 
		cache_servicecritical_metricids2serviceids = null;
	public ArrayList<Integer> getServicesCriticalFor(Integer metric_id) throws SQLException{
		if(cache_servicecritical_metricids2serviceids == null) {
			cache_servicecritical_metricids2serviceids = new TreeMap<Integer, ArrayList<Integer>>();
	        Statement stmt = OIMDatabase.db.createStatement();
	        String sql = "select metric_id, service_id from metric_service where critical = 1";
	        ResultSet rs = stmt.executeQuery(sql);		
	        while(rs.next()) {
	        	Integer mid = rs.getInt("metric_id");
	        	Integer sid = rs.getInt("service_id");
	        	ArrayList<Integer> list = cache_servicecritical_metricids2serviceids.get(mid);
	        	if(list == null) {
	        		list = new ArrayList<Integer>();
	        		cache_servicecritical_metricids2serviceids.put(mid, list);
	        	}
	        	list.add(sid);
	        }
		}
		ArrayList<Integer> list = cache_servicecritical_metricids2serviceids.get(metric_id);
		if(list == null) return new ArrayList<Integer>();
		return list;
	}
	
	public static class MetricType extends TreeMap<Integer/*metric_id*/, Metric> {}
	private static MetricType cache_status_id2metric = null;
	public Metric getMetric(int metric_id) throws SQLException {
		if(cache_status_id2metric == null) {
			cache_status_id2metric = new MetricType();
	        Statement stmt = OIMDatabase.db.createStatement();
	        String sql = "select * from metric";
	        ResultSet rs = stmt.executeQuery(sql);
	        while(rs.next()) {
	        	Integer id = rs.getInt("id");
	        	Metric m = new Metric(rs);
	        	cache_status_id2metric.put(id, m);
	        }			
		}
		return cache_status_id2metric.get(metric_id);
	}
	
	public static class ServiceType extends TreeMap<Integer/*service_id*/, Service> {}
	private static ServiceType cache_service_id2service = null;
	public Service getService(Integer service_id) throws SQLException {
		if(cache_service_id2service == null) {
			cache_service_id2service = new ServiceType();
	        Statement stmt = OIMDatabase.db.createStatement();
	        String sql = "select * from service";
	        ResultSet rs = stmt.executeQuery(sql);
	        while(rs.next()) {
	        	Integer id = rs.getInt("id");
	        	Service m = new Service(rs);
	        	cache_service_id2service.put(id, m);
	        }			
		}
		return cache_service_id2service.get(service_id);
	}

	public static class DowntimeType extends TreeMap<Integer/*resource_id*/, ArrayList<Downtime>> {}
	private static DowntimeType cache_downtime_id2downtime = null;
	public ArrayList<Downtime> getDowntimes(Integer resource_id) throws SQLException {
		if(cache_downtime_id2downtime == null) {
			cache_downtime_id2downtime = new DowntimeType();
	        Statement stmt = OIMDatabase.db.createStatement();
	        String sql = "select *,  UNIX_Timestamp(start_time) as unix_start_time, UNIX_Timestamp(end_time) as unix_end_time " + 
	        		"from resource_downtime where disable = 0";
	        ResultSet rs = stmt.executeQuery(sql);
	        while(rs.next()) {
	        	Integer id = rs.getInt("resource_id");
	        	Downtime m = new Downtime(rs);
	        	ArrayList<Downtime> list = cache_downtime_id2downtime.get(id);
	        	if(list == null) {
	        		list = new ArrayList<Downtime>();
	        		cache_downtime_id2downtime.put(id, list);
	        	}
	        	list.add(m);
	        }			
		}
		return cache_downtime_id2downtime.get(resource_id);
	}

	public static class DowntimeServiceType extends TreeMap<Integer/*downtime_id*/, ArrayList<Integer/*service_id*/>> {}
	private static DowntimeServiceType cache_downtimeservice_id2downtime = null;
	public ArrayList<Integer> lookupResourceDowntimeService(Integer downtime_id) throws SQLException {
		if(cache_downtimeservice_id2downtime == null) {
			cache_downtimeservice_id2downtime = new DowntimeServiceType();
	        Statement stmt = OIMDatabase.db.createStatement();
	        String sql = "select * from resource_downtime_service";
	        ResultSet rs = stmt.executeQuery(sql);
	        while(rs.next()) {
	        	Integer id = rs.getInt("resource_downtime_id");
	        	Integer service_id = rs.getInt("service_id");
	        	ArrayList<Integer> list = cache_downtimeservice_id2downtime.get(id);
	        	if(list == null) {
	        		list = new ArrayList<Integer>();
	        		cache_downtimeservice_id2downtime.put(id, list);
	        	}
	        	list.add(service_id);
	        }			
		}
		return cache_downtimeservice_id2downtime.get(downtime_id);
	}
}
