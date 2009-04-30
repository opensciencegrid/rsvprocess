package rsv.process;

import java.util.Properties;

public class Configuration extends Properties {
	//RSV DB
	public static final String rsv_db_url = "rsv_db_url";
	public static final String rsv_db_user = "rsv_db_user";	
	public static final String rsv_db_pass = "rsv_db_pass";

	//GRATIA DB
	public static final String gratia_db_url = "gratia_db_url";
	public static final String gratia_db_user = "gratia_db_user";	
	public static final String gratia_db_pass = "gratia_db_pass";
	
	//OIM DB
	public static final String oim_db_url = "oim_db_url";
	public static final String oim_db_user = "oim_db_user";	
	public static final String oim_db_pass = "oim_db_pass";
	
	//register names here so that there will be no typos
	public static final String common_filelock_prefix = "common_filelock_prefix";
	public static final String preprocess_gratia_record_count = "preprocess_gratia_record_count";
	public static final String vomatrix_xml_cache = "vomatrix_xml_cache";
	public static final String current_resource_status_xml_cache = "current_resource_status_xml_cache";
	public static final String overall_status_max_record_count = "overall_status_max_record_count";
	//public static final String aandr_cache = "aandr_cache";
}
