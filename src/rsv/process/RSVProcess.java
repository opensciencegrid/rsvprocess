package rsv.process;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class RSVProcess {
	
	private static final Logger logger = Logger.getLogger(RSVProcess.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//PropertyConfigurator.configure("log4j.properties");
		
		logger.info("Initializing RSV Process");
		RSVProcess app = new RSVProcess();
		
	}

}
