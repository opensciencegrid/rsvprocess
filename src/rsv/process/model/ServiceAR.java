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
public class ServiceAR extends ModelBase {
	private static final Logger logger = Logger.getLogger(ServiceAR.class);	

	//returns number of record inserted
	public int insert(int resource_id, int service_id, double a, double r, int timestamp) throws SQLException
	{
		int recs = 0;
		
		//clear previous data (just in case)
		String sql = "delete from rsvextra.service_ar where resource_id = ? and service_id = ? and timestamp = ?";
		PreparedStatement stmt = ModelBase.db.prepareStatement(sql);		
	    stmt.setInt(1, resource_id);
		stmt.setInt(2, service_id);
	    stmt.setInt(3, timestamp);
	    stmt.execute();
	    recs += stmt.getUpdateCount();
	    
		sql = "insert into rsvextra.service_ar (resource_id, service_id, availability, reliability, timestamp) values (?, ?, ?, ?, ?)";
		stmt = ModelBase.db.prepareStatement(sql);		
	    stmt.setInt(1, resource_id);
		stmt.setInt(2, service_id);
		stmt.setDouble(3, a);
		stmt.setDouble(4, r);
	    stmt.setInt(5, timestamp);
	    stmt.execute();
	    recs += stmt.getUpdateCount();
	    
		return recs;
	}
}
