package rsv.process.test;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;


public class Log4j implements Test{

	private static final Logger logger = Logger.getLogger(Log4j.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		 Log4j test = new Log4j();
		 test.run();

	}
	public void run() {
		System.out.println("Test Log4J");	
    	BasicConfigurator.configure();
    	logger.debug("Hello world.");
    	logger.info("What a beatiful day.");
	}
}
