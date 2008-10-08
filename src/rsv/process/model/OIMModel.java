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

public class OIMModel extends ModelBase {
	private static final Logger logger = Logger.getLogger(OIMModel.class);	
	
	private static HashMap<String, Integer> cache_resource_fqdn2id = null;
	public Integer lookupResourceID(String fqdn) throws SQLException
	{
		if(cache_resource_fqdn2id == null) {
			cache_resource_fqdn2id = new HashMap<String, Integer>();
	        Statement stmt = ModelBase.db.createStatement();
	        ResultSet rs = stmt.executeQuery("select resource_id,fqdn from oim.resource where active = 1 and disable = 0");			
	        while(rs.next()) {
	        	Integer resource_id = rs.getInt("resource_id");
	        	String resource_fqdn = rs.getString("fqdn");
	        	cache_resource_fqdn2id.put(resource_fqdn, resource_id);
	        	//logger.debug("Adding " + resource_fqdn);
	        }
		}
		return cache_resource_fqdn2id.get(fqdn);
	}
	
	public static class ResourcesType extends TreeMap<Integer/*resource_id*/, Resource> {}
	private static ResourcesType cache_resource_id2rec = null;
	public ResourcesType getResources() throws SQLException
	{
		if(cache_resource_id2rec == null) {
			cache_resource_id2rec = new ResourcesType();
			Statement stmt = ModelBase.db.createStatement();
	        ResultSet rs = stmt.executeQuery("select resource_id,name from oim.resource where active = 1 and disable = 0");			
	        while(rs.next()) {
	        	Resource rec = new Resource();
	        	int id = rs.getInt("resource_id");
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
	        Statement stmt = ModelBase.db.createStatement();
	        ResultSet rs = stmt.executeQuery("select metric_id,name from oim.metric where active = 1 and disable = 0");			
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
	        ResultSet rs = stmt.executeQuery("select metric_status_id,metric_status_description from oim.metric_status");			
	        while(rs.next()) {
	        	Integer status_id = rs.getInt("metric_status_id");
	        	String status_name = rs.getString("metric_status_description");
	        	cache_status_name2id.put(status_name, status_id);
	        	//logger.debug("Adding " + resource_fqdn);
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
	        Statement stmt = ModelBase.db.createStatement();
	        ResultSet rs = stmt.executeQuery("select vo_id,short_name from oim.virtualorganization where active = 1 and disable = 0");			
	        while(rs.next()) {
	        	Integer vo_id = rs.getInt("vo_id");
	        	String short_name = rs.getString("short_name");
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
	        Statement stmt = ModelBase.db.createStatement();
	        ResultSet rs = stmt.executeQuery("select * from oim.virtualorganization where active = 1 and disable = 0");			
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
	        Statement stmt = ModelBase.db.createStatement();
	        ResultSet rs = stmt.executeQuery("select metric_id, fresh_for from oim.metric where active = 1 and disable = 0");			
	        while(rs.next()) {
	        	Integer id = rs.getInt("metric_id");
	        	Integer value = rs.getInt("fresh_for");
	        	cache_metric_id2freshfor.put(id, value);
	        }
		}
		return cache_metric_id2freshfor.get(metric_id);
	}
	public boolean isFresh(MetricData md, int timestamp) throws SQLException {
		int freshfor = md.getFreshFor();
		if(timestamp < md.getTimestamp() + freshfor) {
			return true;
		}
		return false;
	}
	
	public static class GetResourceGroupsType extends HashMap<Integer, ArrayList<Integer>> {}//<service_id, status_is>
	private static GetResourceGroupsType cache_rrg_id2gid = null;
	public ArrayList<Integer> getResourceGroups(int resource_id) throws SQLException {
		if(cache_rrg_id2gid == null) {
			cache_rrg_id2gid = new GetResourceGroupsType();
	        Statement stmt = ModelBase.db.createStatement();
	        String sql = "select * from oim.resource_resource_group";
	        ResultSet rs = stmt.executeQuery(sql);
	        while(rs.next()) {
	        	Integer rid = rs.getInt("resource_id");
	        	Integer gid = rs.getInt("resource_group_id");
	        	
	        	ArrayList<Integer> list = cache_rrg_id2gid.get(rid);
	        	if(list == null) {
	        		list = new ArrayList<Integer>();
	        		cache_rrg_id2gid.put(rid, list);
	        	}
	        	list.add(gid);
	        }
		}
		return cache_rrg_id2gid.get(resource_id);

	}	
	
	//public class ResourceServiceType extends TreeMap<Integer, ArrayList<Integer>> {}//<service_id, status_is>
	private static TreeMap<Integer, ArrayList<Integer>> cache_resourceservice_rid2sid = null;
	public ArrayList<Integer> getResourceService(Integer resource_id) throws SQLException {
		if(cache_resourceservice_rid2sid == null) {
			cache_resourceservice_rid2sid = new TreeMap<Integer, ArrayList<Integer>>();
	        Statement stmt = ModelBase.db.createStatement();
	        /*
	    	String sql = "SELECT r.resource_id, s.service_id, s.name, s.description " +
	    	"FROM oim.resource_service r " +
	    	"LEFT JOIN oim.service s ON r.service_id = s.service_id " +
	    	"WHERE r.active = 1 and r.disable = 0 and s.active = 1 and s.disable = 0 and s.service_id " +
	    	"IN ( " +
	    	"SELECT service_id " +
	    	"FROM oim.service_service_group " +
	    	"WHERE active = 1 and disable = 0 and service_group_id = 1 " +
	    	") " +
	    	"AND s.service_id NOT " +
	    	"IN ( " +
	    	"SELECT DISTINCT PS.parent_service_id psid " +
	    	"FROM oim.service PS " +
	    	"WHERE PS.parent_service_id IS NOT NULL and active = 1 and disable = 0" +
	    	") ";
	    	*/
	        String sql = "SELECT rs.resource_id, rs.service_id FROM oim.resource_service rs join oim.service s on rs.service_id = s.service_id";
	    	//logger.debug(sql);
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
	        Statement stmt = ModelBase.db.createStatement();
	        String sql = "select metric_id, service_id from oim.metric_service where critical = 1";
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
	        Statement stmt = ModelBase.db.createStatement();
	        String sql = "select metric_id, service_id from oim.metric_service where critical = 0";
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

	public static class StatusType extends TreeMap<Integer, String> {}//<service_id, status_is>
	private static StatusType cache_status_id2status = null;
	public StatusType getStatus() throws SQLException {
		if(cache_status_id2status == null) {
			cache_status_id2status = new StatusType();
	        Statement stmt = ModelBase.db.createStatement();
	        String sql = "select * from oim.resource_resource_group";
	        ResultSet rs = stmt.executeQuery(sql);
	        while(rs.next()) {
	        	Integer id = rs.getInt("metric_status_id");
	        	String desc = rs.getString("metric_status_description");
	        	cache_status_id2status.put(id, desc);
	        }
		}
        return cache_status_id2status;
	}
	
	public static class MetricType extends TreeMap<Integer/*metric_id*/, Metric> {}
	private static MetricType cache_status_id2metric = null;
	public Metric getMetric(int metric_id) throws SQLException {
		if(cache_status_id2metric == null) {
			cache_status_id2metric = new MetricType();
	        Statement stmt = ModelBase.db.createStatement();
	        String sql = "select * from oim.metric where active = 1 and disable = 0";
	        ResultSet rs = stmt.executeQuery(sql);
	        while(rs.next()) {
	        	Integer id = rs.getInt("metric_id");
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
	        Statement stmt = ModelBase.db.createStatement();
	        String sql = "select * from oim.service where active = 1 and disable = 0";
	        ResultSet rs = stmt.executeQuery(sql);
	        while(rs.next()) {
	        	Integer id = rs.getInt("service_id");
	        	Service m = new Service(rs);
	        	cache_service_id2service.put(id, m);
	        }			
		}
		return cache_service_id2service.get(service_id);
	}

}
