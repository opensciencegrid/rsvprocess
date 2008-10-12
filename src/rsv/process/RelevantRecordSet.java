package rsv.process;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Collection;

import rsv.process.model.MetricDataModel;
import rsv.process.model.OIMModel;
import rsv.process.model.record.MetricData;
import rsv.process.model.record.Status;

public class RelevantRecordSet {
	
	//services that the resource supports
	ArrayList<Integer> services = null;
	//current metric set
	TreeMap<Integer, MetricData> current = new TreeMap<Integer, MetricData>(); //metric_id, MetricData
	
	OIMModel oim = new OIMModel();
	
	public RelevantRecordSet(int resource_id, int start) throws SQLException
	{
		//load initial metricdata
		MetricDataModel mdm = new MetricDataModel();
		MetricDataModel.LMDType rrs = mdm.getLastMetricDataSet(resource_id, start);
		
		//consume the initial metricdata
		for(Integer metric_id : rrs.keySet()) {
			MetricData md = rrs.get(metric_id);
			//if status is unknown, try to find the effective records
			if(md.getStatusID() == Status.UNKNOWN) {
				MetricData effective_md = mdm.getLastNonUnknownMetricData(resource_id, md.getMetricID(), md.getTimestamp());
				if(effective_md != null) {
					if(oim.isFresh(effective_md, md.getTimestamp())) {
						noteEffectiveMetricReplacement(effective_md, md);
						md = effective_md;
					}
				}
			}
			current.put(metric_id, md);
		}
	}
	
	//update the current rrs with a new metricdata
	public void update(MetricData md) throws SQLException {
		//handle UNKNOWN status
		if(md.getStatusID() == Status.UNKNOWN) {
			MetricData curmet = current.get(md.getMetricID());
			if(curmet != null && 
					curmet.getStatusID() != Status.UNKNOWN && 
					oim.isFresh(curmet, md.getTimestamp())) {
				//current is still fresh. let's ignore..
				noteEffectiveMetricReplacement(curmet, md);
				md.addNote("This UNKNOWN status metric data was ignored by an earlier non-UNKNOWN MetricData ID:" + curmet.getID());
				return;
			}
		}
		current.put(md.getMetricID(), md);
	}
	/*
	public TreeMap<Integer, MetricData> getCurrentRRS() 
	{
		return current;
	}
	*/
	public Collection<MetricData> getAllMetricData()
	{
		return current.values();
	}
	
	public MetricData getCurrent(int metric_id) {
		return current.get(metric_id);
	}
	
	public void noteEffectiveMetricReplacement(MetricData effective, MetricData unknown) {
		effective.addNote("Original UNKNOWN status metricdata " + unknown.getID() + " was replaced by this metric.");
	}

}
