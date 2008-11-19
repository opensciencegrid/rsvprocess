package rsv.process.model;

import java.sql.SQLException;

public interface ServiceAR {
	public int insert(int resource_id, int service_id, double a, double r, int timestamp) throws SQLException;
}
