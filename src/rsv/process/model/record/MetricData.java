package rsv.process.model.record;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.log4j.Logger;

import rsv.process.model.GratiaModel;
import rsv.process.model.MetricDataModel;
import rsv.process.model.OIMModel;

public class MetricData {
	private static final Logger logger = Logger.getLogger(MetricData.class);	
	
	//core parameters
	protected int metricdata_id;
	protected int metric_id;
	protected int resource_id;
	protected int timestamp;
	protected int status_id;
	//protected int detail_id;
	
	private String note = "";
	
	public MetricData(ResultSet rs) {
		try {
			metricdata_id = rs.getInt("id");
			metric_id = rs.getInt("metric_id");
			resource_id = rs.getInt("resource_id");
			status_id = rs.getInt("metric_status_id");
			timestamp = rs.getInt("timestamp");
			//detail_id = rs.getInt("detail_id");
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
	//public int getDetailID() { return detail_id; }	

	public String fetchDetail() throws SQLException {
		GratiaModel mdm = new GratiaModel();
		return mdm.getDetail(getID());
	}
	
	public int getFreshFor() throws SQLException {
		OIMModel oim = new OIMModel();
		Integer i = oim.lookupFreshFor(metric_id);
		if(i == null) {
			logger.error("Metric ID: " +metric_id + " has no fresh for value (letting it null-pointer-ed)");
			return 0; //good default value?
		}
		return i;
	}
	
	public void addNote(String _note) {
		note += _note;
	}
}
