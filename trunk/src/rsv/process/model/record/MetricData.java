package rsv.process.model.record;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import rsv.process.model.MetricDataModel;
import rsv.process.model.ModelBase;
import rsv.process.model.OIMModel;

public class MetricData {
	private static final Logger logger = Logger.getLogger(MetricData.class);	
	
	//core parameters
	protected int metricdata_id;
	protected int metric_id;
	protected int resource_id;
	protected int timestamp;
	protected int status_id;
	
	private String note = "";
	
	public MetricData(ResultSet rs) {
		try {
			metricdata_id = rs.getInt("id");
			metric_id = rs.getInt("metric_id");
			resource_id = rs.getInt("resource_id");
			status_id = rs.getInt("metric_status_id");
			timestamp = rs.getInt("timestamp");
		} catch (SQLException e) {
			logger.error("Failed to inialize MetricData record from given resultset", e);
		}
	}
	
	protected MetricData() {
		//should only be used by DummyMetricData, etc..
	}
	
	public int getID() { return metricdata_id; }
	public int getStatusID() { return status_id; }
	public int getResourceID() { return resource_id; }	
	public int getMetricID() { return metric_id; }
	public int getTimestamp() { return timestamp; }	
	
	public String fetchDetail() throws SQLException {
		MetricDataModel mdm = new MetricDataModel();
		return mdm.getDetail(getID());
	}

	public int getFreshFor() throws SQLException {
		OIMModel oim = new OIMModel();
		return oim.lookupFreshFor(metric_id);
	}
	
	public void addNote(String _note) {
		note += _note;
	}
}
