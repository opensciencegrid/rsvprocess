package rsv.process.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

public class ResourceDetailModel extends RSVDatabase {
	private static final Logger logger = Logger.getLogger(ResourceDetailModel.class);	
	public void update(int resource_id, int metric_id, String xml) throws SQLException
	{
		//find current record
		String sql = "select * from resource_detail where resource_id = ? and metric_id = ?";
	    PreparedStatement stmt = RSVDatabase.db.prepareStatement(sql);
		stmt.setInt(1, resource_id);
	    stmt.setInt(2, metric_id);
		ResultSet rs = stmt.executeQuery();	
		if(rs.next()) {
			//do update
			sql = "update resource_detail set xml = ? where resource_id = ? and metric_id = ?";
		    stmt = RSVDatabase.db.prepareStatement(sql);
			stmt.setString(1, xml);
		    stmt.setInt(2, resource_id);
		    stmt.setInt(3, metric_id);
		} else {
			//do insert
			sql = "insert into resource_detail (resource_id, metric_id, xml) values (?, ?, ?)";
		    stmt = RSVDatabase.db.prepareStatement(sql);
		    stmt.setInt(1, resource_id);
		    stmt.setInt(2, metric_id);
			stmt.setString(3, xml);
		}
        logger.info("Upserted resource_detail for resource " + resource_id + " for metric " + metric_id);
		stmt.execute();		
	}
}
