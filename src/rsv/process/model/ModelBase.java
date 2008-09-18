package rsv.process.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.apache.log4j.Logger;

public class ModelBase {
	
	private static final Logger logger = Logger.getLogger(ModelBase.class);	
	protected static Connection db = null;
	
	ModelBase()
	{
		try {
			if(db == null) {
				logger.info("Initializing ModelBase");
				db = connectDB();
				db.setAutoCommit(false);
			}
		} catch (SQLException e) {
			logger.error("Fafiled to initialize ModelBase", e);
		}
	}
	
	private static Connection connectDB()
	{
    	//connect to mysql
        String url = "jdbc:mysql://rsv-itb.grid.iu.edu:49152/rsvextra";
        Connection con = null;
		try {
			con = DriverManager.getConnection(url,"rsv-view-write", "soichisviews");
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
        		logger.info("Closed db connection");
        	}
		} catch (SQLException e) {
			logger.error("Caught exception while closing db", e);
		}
	}
}
