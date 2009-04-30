package rsv.process.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import rsv.process.Configuration;
import rsv.process.control.RSVMain;

public class OIMDatabase {
	
	private static final Logger logger = Logger.getLogger(OIMDatabase.class);	
	protected static Connection db = null;
	
	OIMDatabase()
	{
		try {
			if(db == null) {
				logger.info("Initializing OIMDatabase");
				db = connectDB();
				db.setAutoCommit(false);
			}
		} catch (SQLException e) {
			logger.error("Fafiled to initialize OIMDatabase", e);
		}
	}
	
	protected static Connection connectDB()
	{
    	//connect to mysql
		String url = RSVMain.conf.getProperty(Configuration.oim_db_url);
        Connection con = null;
		try {
			con = DriverManager.getConnection(url,
					RSVMain.conf.getProperty(Configuration.oim_db_user), 
					RSVMain.conf.getProperty(Configuration.oim_db_pass));
			logger.info("Connected to " + url);
		} catch (SQLException e) {
			logger.error("Caught exception while connecting to db", e);
		}
        return con;        
	}
	
	//call this before application ends
	public static void closeDB() 
	{
        try {
        	if(db != null) {
        		db.commit(); 
        		db.close();
        		logger.info("Closed OIMDatabase connection");
        	}
		} catch (SQLException e) {
			logger.error("Caught exception while closing db", e);
		}
	}
}
