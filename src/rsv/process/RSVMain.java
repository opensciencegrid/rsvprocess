package rsv.process;

import org.apache.log4j.Logger;
import rsv.process.model.ModelBase;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class RSVMain {
	public static final int exitcode_invalid_arg = -1;
	public static final int exitcode_ok = 0;
	public static final int exitcode_warning = 1;
	public static final int exitcode_error = 2;
	
	private static final Logger logger = Logger.getLogger(RSVMain.class);
	public static Properties conf = null;

	public static void main(String[] args) {
		int ret = exitcode_ok;		
		logger.info("Initializing RSV Process");
		conf = new Properties();
		try {
			conf.load(new FileInputStream("rsvprocess.conf"));
			RSVMain app = new RSVMain();
			
			if(args.length != 1) {
				showUsage();
			} else {
				//run specified process
				String command = args[0];
				ret = app.dispatch(command);
			}
		} catch (FileNotFoundException e) {
			logger.error("rsvprocess.conf not found in currernt directory.", e);
			ret = exitcode_error;
		} catch (IOException e) {
			logger.error("Failed to read rsvprocess.conf", e);
			ret = exitcode_error;
		}
		
		exit(ret);
	}
	
	public static void exit(int ret)
	{
		printErrorCode(ret);
		ModelBase.closeDB();
		
		System.exit(ret);
	}
	
	public int dispatch(String command) {
		RSVProcess process = null;
		if(command.compareToIgnoreCase("preprocess") == 0) {
			process = new RSVPreprocess();
		}
		if(command.compareToIgnoreCase("overallstatus") == 0) {
			process = new RSVOverallStatus();
		}
		if(command.compareToIgnoreCase("cache") == 0) {
			process = new RSVCache();
		}
		if(command.compareToIgnoreCase("availability") == 0) {
			process = new RSVAvailability();
		}
		if(command.compareToIgnoreCase("vomatrix") == 0) {
			process = new RSVVOMatrix();
		}
		if(process == null) {
			return RSVMain.exitcode_invalid_arg;
		} else {
			return process.run();
		}
	}
	
	public static int showUsage()
	{
		System.out.println("RSVProcess Usage");
		System.out.println("> java rsvprocess.jar [command]");
		System.out.println("\t[command]");
		System.out.println("\tpreprocess - Run Preprocess");
		System.out.println("\toverallstatus - Overall Status Calculation Process");
		System.out.println("\tcache - Current status cache update process");
		System.out.println("\tavailability - Availability, Reliability Number Calculation");
		System.out.println("\tvomatrix - VO Matrix Processing");
		return exitcode_ok;
	}
	
	public static void printErrorCode(int code) {
		switch(code) {
		case RSVMain.exitcode_invalid_arg:
			System.out.println("Invalid Argument");
			break;
		case RSVMain.exitcode_ok:
			System.out.println("Process ended OK");
			break;
		case RSVMain.exitcode_warning:
			System.out.println("Process ended with Warning");
			break;
		case RSVMain.exitcode_error:
			System.out.println("Process ended with Error");
			break;
		}
	}
}
