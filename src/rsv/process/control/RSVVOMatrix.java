package rsv.process.control;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import rsv.process.Configuration;
import rsv.process.model.MetricDataModel;
import rsv.process.model.OIMModel;
import rsv.process.model.record.MetricData;
import rsv.process.model.record.Resource;
import rsv.process.model.record.VirtualOrganization;

public class RSVVOMatrix implements RSVProcess{
	
	private static final Logger logger = Logger.getLogger(RSVVOMatrix.class);
	
	//some psudo-static configs
	private static final Integer vosupported_metric_id = 19;
	private static final String vodetail_token = "# List of VOs this site claims to support ";
	
	public int run(String args[]) 
	{
		int ret = RSVMain.exitcode_ok;
				
		try {			
			StringBuffer xml = new StringBuffer();
			xml.append("<?xml version=\"1.0\"?>\n");
			xml.append("<VOMembership>\n");
			MetricDataModel mdm = new MetricDataModel();
			OIMModel oim = new OIMModel();
			TreeMap<Integer/*vo_id*/, TreeSet<Integer/*resource_id*/>> void2resources = new TreeMap<Integer, TreeSet<Integer>>();
			
			//grouped by Resource ID
			xml.append("<ResourceGrouped>");
			OIMModel.ResourcesType resources = oim.getResources();
			for(Integer resource_id : resources.keySet()) {
				
				StringBuffer errors = new StringBuffer();
				String voinfo = null;
				TreeMap<Integer, String> volist = null;
				
				//ignore resource with no service
				ArrayList<Integer/*service_id*/> services = oim.getResourceService(resource_id); 
				if(services.size() == 0) continue;
				
				MetricDataModel.LMDType mset = mdm.getLastMetricDataSet(resource_id, null);
				MetricData m = mset.get(vosupported_metric_id);
				if(m == null) {
					errors.append("No VO Detail reported for this resource through RSV\n");
				} else {
					voinfo = m.fetchDetail();
					logger.debug(voinfo.length());
					logger.debug(voinfo);
					try {
						if(voinfo != null && voinfo.substring(0, vodetail_token.length()).equals(vodetail_token)) {
							String vos = voinfo.substring(vodetail_token.length());
							Scanner s = new Scanner(vos);
							volist = new TreeMap<Integer, String>();
							while (s.hasNext()) {
								//lookup the VOID
								String voname = s.next();
								Integer vo_id = oim.lookupVOID(voname);
								if(vo_id == null) {
									errors.append("Unknown VO name: "+ voname + " found\n");
								} else {
									volist.put(vo_id, voname);
									
									//store the entry to void2resources (for later output of VO grouped list)
									TreeSet<Integer> rs = void2resources.get(vo_id);
									if(rs == null) {
										rs = new TreeSet<Integer>();
										void2resources.put(vo_id, rs);
									}
									if(!rs.contains(resource_id)) {
										rs.add(resource_id);
									}
								}
						    }
						} else {
							errors.append("Failed to find VO detail information in RSV metric\n");
						}
						
					} catch(StringIndexOutOfBoundsException e) {
						errors.append("Invalid VO Detail: " + e.getMessage() + "\n");
					}
				}
				
				//output XML
				Resource r = resources.get(resource_id);
				xml.append("<Resource id=\""+resource_id+"\">");
				xml.append("<Name>"+r.getName()+"</Name>");
				xml.append("<MembersRaw><![CDATA["+voinfo+"]]></MembersRaw>");
				xml.append("<ErrorMessage><![CDATA["+errors.toString()+"]]></ErrorMessage>");
				xml.append("<Members>");
				if(volist != null) {
					for(Integer vo : volist.keySet()) {
						xml.append("<VO id=\""+vo+"\">"+volist.get(vo)+"</VO>");
					}
				}
				xml.append("</Members>");
				xml.append("</Resource>\n");
			}      
			xml.append("</ResourceGrouped>");
			
			//grouped by VO
			xml.append("<VOGrouped>");
			for(Integer vo_id : void2resources.keySet()) {
				TreeSet<Integer> rs = void2resources.get(vo_id);
				xml.append("<VO id=\""+vo_id+"\">");
				VirtualOrganization vo = oim.lookupVO(vo_id);
				xml.append("<Name>"+vo.getShortName()+"</Name>");
				xml.append("<Members>");
				for(Integer resource_id : rs) {
					xml.append("<Resource>");
					Resource r = resources.get(resource_id);
					xml.append("<ResourceID>" + r.getID() + "</ResourceID>");
					xml.append("<ResourceName>" + r.getName() + "</ResourceName>");
					xml.append("</Resource>");
				}
				xml.append("</Members>");
				xml.append("</VO>");
			}
			xml.append("</VOGrouped>");

			xml.append("</VOMembership>\n");
			//output XML to specified location
		    try{
		    	logger.debug("Wriging generated XML to : " + Configuration.vomatrix_xml_cache);
		    	FileWriter fstream = new FileWriter(RSVMain.conf.getProperty(Configuration.vomatrix_xml_cache));
		    	BufferedWriter out = new BufferedWriter(fstream);
		    	out.write(xml.toString());
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
