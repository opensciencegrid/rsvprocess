package rsv.process.model.record;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import rsv.process.model.OIMModel;

public class Downtime {
	
	private static final Logger logger = Logger.getLogger(Downtime.class);	
	
	private int id;
	private int start_time;
	private int end_time;
	private String summary;
	private int class_id;
	private ArrayList<Integer> service_ids;
	
	public Downtime(ResultSet rs) {
		try {
			id = rs.getInt("downtime_id");
			start_time = rs.getInt("unix_start_time");
			end_time = rs.getInt("unix_end_time");
			summary = rs.getString("downtime_summary");
			class_id = rs.getInt("downtime_class_id");
			service_ids = null;
		} catch (SQLException e) {
			logger.error("Failed to inialize Metric record from given resultset", e);
		}
	}
	
	public int getID() { return id; }
	public int getStartTime() { return start_time; }
	public int getEndTime() { return end_time; }
	public int getClassID() { return class_id; }
	public String getSummary() { return summary; }
	public ArrayList<Integer> getServiceIDs() throws SQLException {
		if(service_ids == null) {
			OIMModel oim = new OIMModel();
			service_ids = oim.lookupResourceDowntimeService(id);	
		}
		return service_ids;
	}
}
