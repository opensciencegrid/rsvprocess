package rsv.process.control;

import org.apache.log4j.Logger;

import rsv.process.model.GratiaDatabase;
import rsv.process.model.OIMDatabase;
import rsv.process.model.RSVDatabase;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileOutputStream;
import java.nio.channels.*;

import rsv.process.Configuration;

public class RSVMain {
	public static final int exitcode_invalid_arg = -1;
	public static final int exitcode_ok = 0;
	public static final int exitcode_warning = 1;
	public static final int exitcode_error = 2;
	
	private static final Logger logger = Logger.getLogger(RSVMain.class);
	public static Configuration conf = null;
	
	public static final String version = "1.17";
	
	public static boolean debug = false;

	public static void main(String[] args) {
		int ret = exitcode_ok;		
		try {
			logger.info("Initializing RSV Process " + version);
			conf = new Configuration();
			
			conf.load(new FileInputStream("rsvprocess.conf"));
			RSVMain app = new RSVMain();
			
			if(RSVMain.conf.getProperty(Configuration.debug).equals("true")) {
				debug = true;
			}
			
			if(args.length == 0) {
				showUsage();
			} else {
				String command = args[0];
				
				//get file lock to make sure I am the only one running this process
				String lock_filename = conf.getProperty(Configuration.common_filelock_prefix) + "." + command;
				FileOutputStream fos= new FileOutputStream(lock_filename);
				FileLock fl = fos.getChannel().tryLock();
				if(fl != null) {
					//ok. run specified process
					try {
						ret = app.dispatch(command, args);
					} catch (Exception e) {
						logger.error("Unhandled exception" , e);
						//TODO - send email to GOC?
						ret = exitcode_warning;
					}
					fl.release();
				} else {
					System.out.println("Failed to obtain filelock on " + lock_filename);
				}
				fos.close();
			}
		} catch (FileNotFoundException e) {
			logger.error("rsvprocess.conf not found in currernt directory.", e);
			//SendMail.sendErrorEmail(e.getMessage());
			ret = exitcode_error;
		} catch (IOException e) {
			logger.error("Failed to read rsvprocess.conf", e);
			//SendMail.sendErrorEmail(e.getMessage());
			ret = exitcode_error;
		} catch (Exception e) {
			logger.error(e);
			//SendMail.sendErrorEmail(e.getMessage());
			ret = exitcode_error;			
		}
		
		exit(ret);
	}
	
	public static void exit(int ret)
	{
		printErrorCode(ret);
		RSVDatabase.closeDB();
		GratiaDatabase.closeDB();
		OIMDatabase.closeDB();
		
		System.exit(ret);
	}
	
	public int dispatch(String command, String args[]) {

		//determine which process to run
		RSVProcess process = null;
		if(command.compareToIgnoreCase("preprocess") == 0) {
			process = new RSVPreprocess();
		} else if(command.compareToIgnoreCase("overallstatus") == 0) {
			process = new RSVOverallStatus();
		} else if(command.compareToIgnoreCase("availability") == 0) {
			process = new RSVAvailability();
		} else if(command.compareToIgnoreCase("vomatrix") == 0) {
			process = new RSVVOMatrix();
		} else if(command.compareToIgnoreCase("cache") == 0) {
			process = new RSVCurrentStatusCache();
		} 
		
		//then run it
		if(process == null) {
			logger.error("Unknown command specified: " + command);
			showUsage();
			return RSVMain.exitcode_invalid_arg;
		} else {
			int ret = process.run(args);
			logger.info("Process Ended with return code " + ret);
			return ret;
		}
	}
	
	public static int showUsage()
	{
		System.out.println("RSVProcess Usage");
		System.out.println("> java rsvprocess.jar [command]");
		System.out.println("\t[command]");
		System.out.println("\tpreprocess - Run Preprocess");
		System.out.println("\toverallstatus - Overall Status Calculation Process");
		System.out.println("\t\t optional arguments: [resource_id] [start_time] [end_time] -- " + 
					"Causes status recalculation on specific resource and specific time period. " + 
					"This will not update the processlog.");
		System.out.println("\tavailability - Calculate Availability, Reliability Number for all resources / services and create xml cache");
		System.out.println("\t\t[start_time] [end_time]");
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
