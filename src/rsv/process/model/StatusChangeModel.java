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
public class StatusChangeModel extends ModelBase {
	private static final Logger logger = Logger.getLogger(StatusChangeModel.class);	
	
	@SuppressWarnings("serial")
	public static class LSCType extends TreeMap<Integer, ServiceStatus> {}//<service_id, status_is>
	public LSCType getLastStatusChange_Service(int resource_id, Integer timestamp) throws SQLException {
		LSCType ret = new LSCType();
        Statement stmt = ModelBase.db.createStatement();
        String where_timestamp = "";
        if(timestamp != null) {
        	where_timestamp = " and timestamp < " + timestamp + " ";
        }
        String sql = "select * from statuschange_service s, "+
        	"(select max(timestamp) last_timestamp ,service_id from statuschange_service "+
        	"where resource_id = "+ resource_id + " " + where_timestamp +
        	"group by service_id) last "+
        	"where s.timestamp = last.last_timestamp and s.service_id = last.service_id ";
        logger.debug(sql);
        ResultSet rs = stmt.executeQuery(sql);
        while(rs.next()) {
        	Integer service_id = rs.getInt("service_id");
        	
        	ServiceStatus ss = new ServiceStatus();
        	ss.status_id = rs.getInt("status_id");
        	ret.put(service_id, ss);
        }
        return ret;
	}
	
	public ResourceStatus getLastStatusChange_Resource(int resource_id, Integer timestamp) throws SQLException {
        Statement stmt = ModelBase.db.createStatement();
        String where_timestamp = "";
        if(timestamp != null) {
        	where_timestamp = " and timestamp < " + timestamp + " ";
        }
        String sql = "select status_id from statuschange_resource s, "+
        	"(select max(timestamp) last_timestamp from statuschange_resource "+
        	"where resource_id = "+ resource_id + " " + where_timestamp +
        	") last "+
        	"where s.timestamp = last.last_timestamp";
        logger.debug(sql);
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
		PreparedStatement stmt = ModelBase.db.prepareStatement(sql);		
	    stmt.setInt(1, resource_id);
		stmt.setInt(2, start);
	    stmt.setInt(3, end);
	    stmt.execute();
	    recs += stmt.getUpdateCount();
	    
		//statuschagne_resource
		sql = "delete from rsvextra.statuschange_resource where resource_id = ? and timestamp >= ? and timestamp <= ?";
		stmt = ModelBase.db.prepareStatement(sql);		
		stmt.setInt(1, resource_id);
	    stmt.setInt(2, start);
	    stmt.setInt(3, end);
	    stmt.execute();
	    recs += stmt.getUpdateCount();
	    
	    /*
		//statuschagne_resource_group
	    OIMModel oim = new OIMModel();
	    ArrayList<Integer> group_ids = oim.getResourceGroups(resource_id);
	    for(Integer group_id : group_ids) {
			sql = "delete from rsvextra.statuschange_resource_group where resource_group_id = ? and timestamp >= ? and timestamp <= ?";
			stmt = ModelBase.db.prepareStatement(sql);		
			stmt.setInt(1, group_id);
		    stmt.setInt(2, start);
		    stmt.setInt(3, end);
		    stmt.execute();
		    recs += stmt.getUpdateCount();
	    }
	    */
	    return recs;
	}
	
	//returns number of record inserted
	public int outputServiceStatusChanges(ArrayList<ServiceStatus> service_statuschanges) throws SQLException
	{
		String sql = "insert into rsvextra.statuschange_service (resource_id, service_id, status_id, timestamp, detail) values (?,?,?,?,?)";
	    PreparedStatement stmt_data = ModelBase.db.prepareStatement(sql);
	    
	    for(ServiceStatus s : service_statuschanges) {
			stmt_data.setInt(1, s.resource_id);
			stmt_data.setInt(2, s.service_id);
			stmt_data.setInt(3, s.status_id);
			stmt_data.setInt(4, s.timestamp);
			stmt_data.setString(5, s.note.substring(0, 256));
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
		String sql = "insert into rsvextra.statuschange_resource (resource_id, status_id, timestamp, detail) values (?,?,?,?)";
	    PreparedStatement stmt_data = ModelBase.db.prepareStatement(sql);
	    
	    for(ResourceStatus s : resource_statuschanges) {
			stmt_data.setInt(1, s.resource_id);
			stmt_data.setInt(2, s.status_id);
			//stmt_data.setInt(3, s.responsible_service_id);
			stmt_data.setInt(3, s.timestamp);
			stmt_data.setString(4, s.note.substring(0, 256));
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
}
