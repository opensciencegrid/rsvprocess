package rsv.process.model.record;

public class Status {
	public static final int OK = 1;
	public static final int WARNING = 2;
	public static final int CRITICAL = 3;
	public static final int UNKNOWN = 4;
	
	public static String getStatus(int s) {
		switch(s) {
		case OK: return "OK";
		case WARNING: return "WARNING";
		case CRITICAL: return "CRITICAL";
		case UNKNOWN: return "UNKNOWN";
		}
		return "BAD STATUS ID";
	}
}
