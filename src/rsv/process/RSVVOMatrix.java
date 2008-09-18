package rsv.process;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import rsv.process.model.GratiaModel;
import rsv.process.model.MetricDataInserter;
import rsv.process.model.OIMModel;
import rsv.process.model.RSVExtraModel;

public class RSVVOMatrix implements RSVProcess{
	
	private static final Logger logger = Logger.getLogger(RSVVOMatrix.class);
	
	public int run() 
	{
		int ret = RSVMain.exitcode_ok;
		/*
		try {
			
	        
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.error("SQL Error", e);
			ret = RSVMain.exitcode_error;
		}
		 */
		return ret;
	}

}
