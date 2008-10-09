package rsv.process;

import java.util.Properties;

public class Configuration extends Properties {
	
	public static final String db_url = "db_url";
	public static final String db_user = "db_user";	
	public static final String db_pass = "db_pass";
	
	
	//register names here so that there will be no typos
	public static final String common_filelock_prefix = "common_filelock_prefix";
	public static final String preprocess_gratia_record_count = "preprocess_gratia_record_count";
	public static final String vomatrix_xml_cache = "vomatrix_xml_cache";
	public static final String current_resource_status_xml_cache = "current_resource_status_xml_cache";
	public static final String overall_status_max_record_count = "overall_status_max_record_count";
}
