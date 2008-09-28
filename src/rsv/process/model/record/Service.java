package rsv.process.model.record;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

public class Service {
	
	private static final Logger logger = Logger.getLogger(Service.class);	
	
	private int id;
	private String name;
	private String description;
	
	public Service(ResultSet rs) {
		try {
			id = rs.getInt("service_id");
			name = rs.getString("name");
			description = rs.getString("description");
		} catch (SQLException e) {
			logger.error("Failed to inialize Metric record from given resultset", e);
		}
	}
	
	public int getID() { return id; }
	public String getName() { return name; }
	public String getDescription() { return description; }
}
