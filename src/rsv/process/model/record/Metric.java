package rsv.process.model.record;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

public class Metric {
	private static final Logger logger = Logger.getLogger(Metric.class);	
	
	//core parameters
	protected int metric_id;
	protected String name;
	protected String description;
	
	public Metric(ResultSet rs) {
		try {
			metric_id = rs.getInt("metric_id");
			name = rs.getString("name");
			description = rs.getString("description");
		} catch (SQLException e) {
			logger.error("Failed to inialize Metric record from given resultset", e);
		}
	}
	
	public int getID() { return metric_id; }
	public String getName() { return name; }
	public String getDescription() { return description; }
	
}
