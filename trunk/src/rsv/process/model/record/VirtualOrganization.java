package rsv.process.model.record;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import rsv.process.model.MetricDataModel;
import rsv.process.model.ModelBase;
import rsv.process.model.OIMModel;

public class VirtualOrganization {
	private static final Logger logger = Logger.getLogger(VirtualOrganization.class);	
	
	//core parameters
	protected int vo_id;
	protected String short_name;
	protected String long_name;
	protected String description;
	
	public VirtualOrganization(ResultSet rs) {
		try {
			vo_id = rs.getInt("vo_id");
			short_name = rs.getString("short_name");
			long_name = rs.getString("long_name");
		} catch (SQLException e) {
			logger.error("Failed to inialize VirtualOrganization record from given resultset", e);
		}
	}
	
	protected VirtualOrganization() {
		//should only be used by DummyMetricData, etc..
	}
	
	public int getID() { return vo_id; }
	public String getShortName() { return short_name; }
	public String getLongName() { return long_name; }	
	public String getDescription() { return description; }
}
