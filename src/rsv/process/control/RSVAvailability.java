package rsv.process.control;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;

import rsv.process.Configuration;
import rsv.process.model.ServiceAR;
import rsv.process.model.OIMModel;
import rsv.process.model.StatusChangeModel;
import rsv.process.model.OIMModel.ResourcesType;
import rsv.process.model.record.Downtime;
import rsv.process.model.record.ServiceStatus;
import rsv.process.model.record.Status;

public class RSVAvailability implements RSVProcess {
	private static final Logger logger = Logger.getLogger(RSVAvailability.class);
	OIMModel oim = new OIMModel();
	ServiceAR sar = new ServiceAR();
	StatusChangeModel scm = new StatusChangeModel();
	
	public int run(String args[]) {
		int ret = RSVMain.exitcode_ok;
		
		int start_time;
		int end_time;
		
		if(args.length == 2) {
			if(args[1].compareTo("yesterday") == 0) {
				Calendar cal = Calendar.getInstance();
				Date current_date = cal.getTime();
				int currenttime = (int) (current_date.getTime()/1000);
				end_time = currenttime / 86400 * 86400;
				start_time = end_time - 86400;
			} else {
				start_time = Integer.parseInt(args[1]);
				end_time = start_time + 86400;
			}
		} else {
			System.out.println("Please provide start & end time");
			return RSVMain.exitcode_invalid_arg;
		}
		
		int r = outputARCache(start_time, end_time);
		if(r != RSVMain.exitcode_ok) {
			logger.error("Error while executing outputARCache()");
			ret = RSVMain.exitcode_error;
		}
		
		return ret;
	}
	
	private int outputARCache(int start_time, int end_time)
	{
		int ret = RSVMain.exitcode_ok;
		
		try {
			
			ResourcesType resources = oim.getResources();
		
			Calendar cal = Calendar.getInstance();
			Date current_date = cal.getTime();
			//int currenttime = (int) (current_date.getTime()/1000);
			int total_time = end_time - start_time;
			
			logger.info("Calculating Availability and reliability for all resources/services");
			
			Date start_date = new Date();
			start_date.setTime(start_time*1000L);
			
			Date end_date = new Date();
			end_date.setTime(end_time*1000L);
			
			logger.debug("Resport Start Time: " + start_date + " ~ End Time: " + end_date);
			logger.debug("Resport Start Time: " + start_time + " ~ End Time: " + end_time);
			
			//allxml += "<Resources>";
			for(Integer resource_id : resources.keySet()) {
				/*
				//debug
				if(resource_id != 123) continue;
				start_time = 1348272000;
				end_time = 1348358400;
				*/
				
				//allxml += "<Resource>";
				ArrayList<Downtime> downtimes = oim.getDowntimes(resource_id);
				ArrayList<Integer> services = oim.getResourceService(resource_id);
				//allxml += "<ResourceID>" + resource_id + "</ResourceID>";
				
				//allxml += "<Services>";
				for(Integer service_id : services) {
					//allxml += "<Service>";
					//allxml += "<ServiceID>"+service_id+"</ServiceID>";				
					logger.debug(resource_id + " " + service_id);
					ServiceStatus init_status = scm.getInitServiceStatus(resource_id, service_id, start_time);
					if(init_status == null) {
						init_status = new ServiceStatus();
						init_status.status_id = Status.UNKNOWN;
						logger.debug("\tNo initial status");
					} else {
						logger.debug("\tInitial Status: " + init_status.status_id);
					}
					ArrayList<ServiceStatus> changes = scm.getServiceStatusChanges(resource_id, service_id, start_time, end_time);
					int available_time = calculateUptime(init_status, changes, start_time, end_time);
					logger.debug("\t Available Time: " + available_time);
					
					double availability = ((double)available_time/total_time);
					
					int reliable_time;
					if(downtimes != null) {
						//dump changes list
						logger.debug("\tBefore imposing");
						dump(changes);
						changes = superImposeDowntime(init_status, changes, start_time, end_time, downtimes, service_id);
						logger.debug("\tAfter imposing");
						dump(changes);
						reliable_time = calculateUptime(init_status, changes, start_time, end_time);
 					} else {
 						//if there are no downtime info..
 						reliable_time = available_time;
 					}
					
					double reliability = ((double)reliable_time/total_time);
					logger.debug("\t Reliable Time: " + reliable_time);
					
					if(sar != null) {
						sar.insert(resource_id, service_id, availability, reliability, start_time);
					}
				}
			}
		} catch (SQLException e) {
			logger.error("SQL Error", e);
			ret = RSVMain.exitcode_error;
		}
		return ret;	
	}
	
	void dump(ArrayList<ServiceStatus> changes)
	{
		for(ServiceStatus status : changes) {
			logger.debug("At: " + status.timestamp + " status: " + status.status_id + " note:" + status.note);
		}
	}
	
	int calculateUptime(ServiceStatus current_status, ArrayList<ServiceStatus> changes, 
		int start_time, int end_time)
	{
		int up_time = 0;
		int current_time = start_time;
		
		for(ServiceStatus new_status : changes) {
			switch(current_status.status_id) {
			case Status.OK:
			case Status.WARNING:
			case Status.DOWNTIME:
				up_time += new_status.timestamp - current_time;
				break;
			}
			current_time = new_status.timestamp;
			current_status = new_status;
		}
		
		//process last period
		switch(current_status.status_id) {
		case Status.OK:
		case Status.WARNING:
		case Status.DOWNTIME:
			up_time += end_time - current_time;
		}

		return up_time;
	}
	
	ArrayList<ServiceStatus> superImposeDowntime(ServiceStatus init_status, ArrayList<ServiceStatus> changes, 
			int start_time, int end_time,
			ArrayList<Downtime> downtimes, int service_id) throws SQLException
	{
		for(Downtime downtime : downtimes) {
			ArrayList<Integer> service_ids = downtime.getServiceIDs();
			if(service_ids == null) continue;
			if(service_ids.contains(service_id)) {
				int down_start = downtime.getStartTime();
				int down_end = downtime.getEndTime();
				
				//if this downtime ends before start time, ignore
				if(down_end < start_time) continue;
				
				//if this downtime starts after end_time, ignore
				if(down_start > end_time) continue;
				
				logger.debug("\t\tImposing downtime start:" + down_start + " end:" + down_end);				
				
				//find the beginning status that I am superimposing
				ServiceStatus first = null;
				for(ServiceStatus status : changes) {
					if(status.timestamp < down_start) continue;
					if(status.timestamp > down_end) break;;
					first = status;
					break;
				}

				//clear status changes occured between start_time, and end_time
				//while remembering the last change
				ArrayList<ServiceStatus> newlist = new ArrayList<ServiceStatus>();
				ServiceStatus last = null;
				for(ServiceStatus status : changes) {
					if(status.timestamp < down_start || status.timestamp > down_end) {
						newlist.add(status);
					} else {
						last = status;
					}
				}
				changes = newlist;
				
				//use init_status if there is no status changes
				if(first == null) {
					first = init_status;
				}
				//use first if there is no last
				if(last == null) {
					last = first;
				}
				
				//now we have everything there is know
				if(down_start < start_time) {
					down_start = start_time;
				}
				
				newlist = new ArrayList<ServiceStatus>();
		
				//copy all status changes before down_start
				for(ServiceStatus status : changes) {
					if(status.timestamp > down_start) break;
					newlist.add(status);
				}
				
				//insert new downtime begin if not already downtime
				if(first.status_id != Status.DOWNTIME) {
					ServiceStatus newdown = new ServiceStatus();
					newdown.status_id = Status.DOWNTIME;
					newdown.timestamp = down_start;
					newlist.add(newdown);
				}
				
				//if downtime ends before end_time, add the end of end_time and rest of status changes
				if(down_end <= end_time) {
					//insert end downtime
					last.timestamp = down_end;
					newlist.add(last);
					
					//copy all the rest
					for(ServiceStatus status : changes) {
						if(status.timestamp <= down_end) continue;
						newlist.add(status);
					}			
				}
				changes = newlist;
			}
		}
		return changes;
	}

}
