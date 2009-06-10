package rsv.process.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import rsv.process.model.record.ServiceStatus;
import rsv.process.model.record.ResourceStatus;

//interfaces with StatusChange_XXX tables
public class StatusChangeModel extends RSVDatabase {
	private static final Logger logger = Logger.getLogger(StatusChangeModel.class);	
	
	@SuppressWarnings("serial")
	public static class LSCType extends TreeMap<Integer, ServiceStatus> {}//LSC - Last Status Change <service_id, status_is>
	public LSCType getLastStatusChange_Service(int resource_id, Integer timestamp, ArrayList<Integer> services) throws SQLException {
		LSCType ret = new LSCType();
        Statement stmt = RSVDatabase.db.createStatement();
        String where_timestamp = "";
        if(timestamp != null) {
        	where_timestamp = " and timestamp < " + timestamp + " ";
        }
        String sql = "select * from statuschange_service s, "+
        	"(select max(timestamp) last_timestamp ,service_id from statuschange_service "+
        	"where resource_id = "+ resource_id + " " + where_timestamp +
        	"group by service_id) last "+
        	"where s.timestamp = last.last_timestamp and s.service_id = last.service_id ";
        //logger.debug(sql);
        ResultSet rs = stmt.executeQuery(sql);
        while(rs.next()) {
        	Integer service_id = rs.getInt("service_id");
        	//only store service_id that this resource has (if someone removes service, then we need to filter removed ones)
        	if(services.contains(service_id)) {
	        	ServiceStatus ss = new ServiceStatus();
	        	ss.status_id = rs.getInt("status_id");
	        	ret.put(service_id, ss);
        	}
        }
        return ret;
	}
	
	public ResourceStatus getLastStatusChange_Resource(int resource_id, Integer timestamp) throws SQLException {
        Statement stmt = RSVDatabase.db.createStatement();
        String where_timestamp = "";
        if(timestamp != null) {
        	where_timestamp = " and timestamp < " + timestamp + " ";
        }
        String sql = "select status_id from statuschange_resource s, "+
        	"(select max(timestamp) last_timestamp from statuschange_resource "+
        	"where resource_id = "+ resource_id + " " + where_timestamp +
        	") last "+
        	"where s.timestamp = last.last_timestamp";
        //logger.debug(sql);
        ResultSet rs = stmt.executeQuery(sql);
        if(rs.next()) {
        	ResourceStatus status = new ResourceStatus();
        	status.status_id = rs.getInt(1);
        	return status;
        }
        return null;
	}
	
	public int clearStatusChanges(int resource_id, int start, int end) throws SQLException
	{
		int recs = 0;
		
		//statuschagne_service
		String sql = "delete from rsvextra.statuschange_service where resource_id = ? and timestamp >= ? and timestamp <= ?";
		PreparedStatement stmt = RSVDatabase.db.prepareStatement(sql);		
	    stmt.setInt(1, resource_id);
		stmt.setInt(2, start);
	    stmt.setInt(3, end);
	    stmt.execute();
	    recs += stmt.getUpdateCount();
	    
		//statuschagne_resource
		sql = "delete from rsvextra.statuschange_resource where resource_id = ? and timestamp >= ? and timestamp <= ?";
		stmt = RSVDatabase.db.prepareStatement(sql);		
		stmt.setInt(1, resource_id);
	    stmt.setInt(2, start);
	    stmt.setInt(3, end);
	    stmt.execute();
	    recs += stmt.getUpdateCount();
	    
	    return recs;
	}
	
	//returns number of record inserted
	public int outputServiceStatusChanges(ArrayList<ServiceStatus> service_statuschanges) throws SQLException
	{
		int note_max_length = 256;
		String sql = "insert into rsvextra.statuschange_service (resource_id, service_id, status_id, timestamp, detail) values (?,?,?,?,?)";
	    PreparedStatement stmt_data = RSVDatabase.db.prepareStatement(sql);
	    
	    for(ServiceStatus s : service_statuschanges) {
			stmt_data.setInt(1, s.resource_id);
			stmt_data.setInt(2, s.service_id);
			stmt_data.setInt(3, s.status_id);
			stmt_data.setInt(4, s.timestamp);
			if(s.note.length() > note_max_length) s.note = s.note.substring(0, note_max_length);
			stmt_data.setString(5, s.note);
			stmt_data.addBatch();
	    }
	    
		int recs = 0;
		int[] numUpdates = stmt_data.executeBatch();    
		for(int i = 0;i < numUpdates.length; ++i) {
			recs += numUpdates[i];
		}
		logger.debug(recs + " records were inserted to statuschange_service table");
		
		return recs;
	}
	
	public int outputResourceStatusChanges(ArrayList<ResourceStatus> resource_statuschanges) throws SQLException
	{
		int note_max_length = 256;
		String sql = "insert into rsvextra.statuschange_resource (resource_id, status_id, timestamp, detail) values (?,?,?,?)";
	    PreparedStatement stmt_data = RSVDatabase.db.prepareStatement(sql);
	    
	    for(ResourceStatus s : resource_statuschanges) {
			stmt_data.setInt(1, s.resource_id);
			stmt_data.setInt(2, s.status_id);
			//stmt_data.setInt(3, s.responsible_service_id);
			stmt_data.setInt(3, s.timestamp);
			if(s.note.length() > note_max_length) s.note = s.note.substring(0, note_max_length);
			stmt_data.setString(4, s.note);
			stmt_data.addBatch();
	    }
	    
		int recs = 0;
		int[] numUpdates = stmt_data.executeBatch();    
		for(int i = 0;i < numUpdates.length; ++i) {
			recs += numUpdates[i];
		}
		logger.debug(recs + " records were inserted to statuschange_resource table");
		
		return recs;		
	}
	
	public ServiceStatus getInitServiceStatus(int resource_id, int service_id, int start) throws SQLException {
		Statement stmt = RSVDatabase.db.createStatement();
		String sql = "select * from statuschange_service where resource_id = " + resource_id + " and service_id = " + service_id + " and timestamp = coalesce(("+
			" select max(timestamp) from statuschange_service where resource_id = " + resource_id + " and service_id = " + service_id + " and timestamp <= " + start +
			"), 0)";
		
		ResultSet rs = stmt.executeQuery(sql);
		if(rs.next()) {
			ServiceStatus ss = new ServiceStatus();
			ss.status_id = rs.getInt("status_id");
			ss.timestamp = rs.getInt("timestamp");
			return ss;
		} else {
			return null;
		}
	}
	
	public ArrayList<ServiceStatus> getServiceStatusChanges(int resource_id, int service_id, int begin, int end) throws SQLException {
		ArrayList<ServiceStatus> ret = new ArrayList<ServiceStatus>();
        Statement stmt = RSVDatabase.db.createStatement();
        String sql = "select * from statuschange_service s "+
        	"where resource_id = "+ resource_id + " and service_id = " + service_id +
        	" and timestamp >= " + begin + " and timestamp < " + end +
        	" order by timestamp";
        ResultSet rs = stmt.executeQuery(sql);
        while(rs.next()) {       	
        	ServiceStatus ss = new ServiceStatus();
        	ss.status_id = rs.getInt("status_id");
			ss.timestamp = rs.getInt("timestamp");
        	ret.add(ss);
        }
        return ret;
	}
	
	public ArrayList<ResourceStatus> getStatusChanges_Resource(int resource_id, int begin, int end) throws SQLException {
		ArrayList<ResourceStatus> ret = new ArrayList<ResourceStatus>();
        Statement stmt = RSVDatabase.db.createStatement();
        String sql = "select * from statuschange_resource s "+
        	"where resource_id = "+ resource_id + 
        	" and timestamp >= " + begin + " and timestamp < " + end +
        	" order by timestamp";
        ResultSet rs = stmt.executeQuery(sql);
        while(rs.next()) {       	
        	ResourceStatus r = new ResourceStatus();
			r.timestamp = rs.getInt("timestamp");
        	r.status_id = rs.getInt("status_id");
        	ret.add(r);
        }
        return ret;
	}
}
