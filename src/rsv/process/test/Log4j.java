package rsv.process.test;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Log4j implements Base{

	private static final Logger logger = Logger.getLogger(Log4j.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//configure log4j
		// PropertyConfigurator.configure("log4j.properties");

		 Log4j test = new Log4j();
		 test.run();

	}
	public void run() {
		System.out.println("Testing Log4J");	
    	//BasicConfigurator.configure();
		
    	logger.debug("Hello world.");
    	logger.info("What a beatiful day.");
	}
}
