package rsv.process.control;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.TreeMap;
import org.apache.log4j.Logger;

import rsv.process.Configuration;
import rsv.process.model.MetricDataModel;
import rsv.process.model.OIMModel;
import rsv.process.model.record.MetricData;

public class RSVVOMatrix implements RSVProcess{
	
	private static final Logger logger = Logger.getLogger(RSVVOMatrix.class);
	
	//some psudo-static configs
	private static final Integer vosupported_metric_id = 19;
	private static final String vodetail_token = "# List of VOs this site claims to support ";
	
	public int run() 
	{
		int ret = RSVMain.exitcode_ok;
				
		try {			
			String xml = "<?xml version=\"1.0\"?>\n";
			xml += "<VOMembership>\n";
			MetricDataModel mdm = new MetricDataModel();
			OIMModel oim = new OIMModel();
			
			OIMModel.ResourcesType resources = oim.getResources();
			for(Integer resource_id : resources.keySet()) {
				MetricDataModel.LMDType mset = mdm.getLastMetricDataSet(resource_id, null);
				MetricData m = mset.get(vosupported_metric_id);
				if(m == null) {
					logger.warn("\tNo VO Detail for resource ID: " + resource_id);
				} else {
					String voinfo = m.fetchDetail();
					TreeMap<Integer, String> volist = null;
					try {
						if(voinfo.substring(0, vodetail_token.length()).compareTo(vodetail_token) == 0) {
							String vos = voinfo.substring(vodetail_token.length());
							Scanner s = new Scanner(vos);
							volist = new TreeMap<Integer, String>();
							while (s.hasNext()) {
								//lookup the VOID
								String voname = s.next();
								Integer vo_id = oim.lookupVOID(voname);
								if(vo_id == null) {
									logger.warn("Unknown VO name: "+ voname + " found for resource " + resource_id);
								} else {
									volist.put(vo_id, voname);
								}
						    }
						}
						
					} catch(StringIndexOutOfBoundsException e) {
						logger.warn("\tInvalid VO Detail for resource ID: " + resource_id);
					}
					
					//output XML
					xml += "<Resource id=\""+resource_id+"\">";
					if(volist != null) {
						for(Integer vo : volist.keySet()) {
							xml += "<VO id=\""+vo+"\">"+volist.get(vo)+"</VO>";
						}
					}
					xml += "<Raw><![CDATA["+voinfo+"]]></Raw>";
					xml += "</Resource>\n";
				}
			}        

			xml += "</VOMembership>\n";
			//output XML to specified location
		    try{
		    	FileWriter fstream = new FileWriter(RSVMain.conf.getProperty(Configuration.vomatrix_xml_cache));
		    	BufferedWriter out = new BufferedWriter(fstream);
		    	out.write(xml);
		    	out.close();
		    } catch (Exception e) {
		    		logger.error("Caught exception while outputing xml cache", e);
					ret = RSVMain.exitcode_error;
		    }
		} catch (SQLException e) {
			logger.error("SQL Error", e);
			ret = RSVMain.exitcode_error;
		}

		return ret;
	}

}
