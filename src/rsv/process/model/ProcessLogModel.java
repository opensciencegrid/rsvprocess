package rsv.process.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

public class ProcessLogModel extends ModelBase {
	private static final Logger logger = Logger.getLogger(ProcessLogModel.class);	
	
	public int getLastGratiaIDProcessed() throws SQLException
	{
        Statement stmt = ModelBase.db.createStatement();
        ResultSet rs = stmt.executeQuery("select max(last_gratia_id_processed) as last from rsvextra.processlog");
        rs.next();
        return rs.getInt("last");
	}
	public void updateLastGratiaIDProcessed(int newid) throws SQLException
	{
	    String sql = "insert into processlog (last_gratia_id_processed) values (?)";
	    PreparedStatement stmt = ModelBase.db.prepareStatement(sql);
		stmt.setInt(1, newid);
		stmt.execute();
	}
}
