#! /bin/sh

if [ ${DEBUG_GOC}0 != "0" ]
    then set -x
fi

function help() {
    echo "-------------------------------------------------------------------------"
    echo "There are 3 big steps that involves RSV process computation."
    echo ""
    echo " 1. Pre-processing: This is the step in which RSVProcess pulls raw "
    echo "  de-normalized data from Gratia DB, and filters/re-indexes the data; "
    echo "  it stores data to metricdata table. Metric Detail - the textual part "
    echo "  of the probes) remains within Gratia DB (as of Oct 7th, 2009)."
    echo " 2. Overall Status Calculation: This step takes data from metricdata, "
    echo "  computes status changes, and stores it to statuschange_XXX tables. "
    echo " 3. Availability / Reliability calculation: Once a day, A/R calculation "
    echo "  step is executed to populate the ar tables to store the A/R number "
    echo "  for each resource for last 24 hours."
    echo ""
    echo "More information: "
    echo " https://internal.grid.iu.edu/twiki/bin/view/Documentation/MyOSG"
    echo "-------------------------------------------------------------------------"
    set - 
    exit 0
}

export GOC=/usr/local/
export JAVA_HOME=/usr/lib/jvm/java-sun
export PATH=$JAVA_HOME/bin:$PATH
export RSVPROCESS_HOME=/usr/local/rsvprocess
export CLASSPATH=lib/log4j-1.2.15.jar:lib/mysql-connector-java-5.1.6-bin.jar:rsvprocess.jar
export LOG=${RSVPROCESS_HOME}/log/manual-process.log
export STDERRLOG=${RSVPROCESS_HOME}/log/manual-process.debug

export RSVPROCESSDB="rsvprocess"
export SLEEPSECONDS=30

function set_current_timestamp() {
    export DATEF=`date -u +%F\ %H:%M:%S`
}

function warn_allow_exit() {
    echo -n "Are you sure?! (y/n):"
    read -e ANS_WARN
    case ${ANS_WARN} in 
	y|Y|yes|YES)
	    echo ""
	    ;;
	*)
	    echo "Ok, will quit without proceeding"
	    set - 
	    exit 1
    esac
}

function check_for_rsvprocess_running() {
    echo ""
    echo "Checking for any existing rsvprocess processes in process table ... "
    RUNNINGRP=`ps -ef | egrep -v "$$|tail " | grep [r]svprocess`
    export RPISRUNNING=$?
    if [ ${DEBUG_GOC}0 != "0" ] 
	then 
	echo "##################################################"
	echo ${RUNNINGRP}
	echo "##################################################"
    fi
}

function wait_if_rsvprocess_running() {
    check_for_rsvprocess_running
    while [ ${RPISRUNNING} -eq 0 ] 
      do
      echo "Found RSVProcess process(es) - will sleep for ${SLEEPSECONDS} seconds!"  2>&1 | tee -a $LOG 
          ## agopu to hayashis: BAD IDEA?!?! to ask peoople to kill the process(es)?
      sleep ${SLEEPSECONDS}
      check_for_rsvprocess_running
    done
    echo "" 2>&1 | tee -a $LOG
    echo "No more existing rsvprocess processes! Will proceed ... " 2>&1 | tee -a $LOG
}

function resource_selector() {
    echo ""
    echo "Please enter a resource selection:"
    echo " all: A and R will be recalculated for all resources in OIM"
    echo " list: Print a list of name and id of all resources in OIM"
    echo " single: A and R will be recalculated for one resource "
    echo "         If you choose this option, we will ask for resource id"
    echo ""
    echo -n " Your selection: "
    read -e ANS_RESOURCE_SELECTION
    echo "Resource selector \"${ANS_RESOURCE_SELECTION}\" entered" 1>> $LOG 
    case ${ANS_RESOURCE_SELECTION} in
	all)
	    export resource_id_start=`echo "SELECT MIN(id) FROM oim.resource;" | mysql -u root | tail -1`
	    export resource_id_end=`echo "SELECT MAX(id) FROM oim.resource;" | mysql -u root | tail -1`
	    ;;
	list)
	    echo "---------------------------------------------------"
	    echo "RESOURCE LIST FROM OIM"
	    echo "---------------------------------------------------"
	    echo "SELECT name, id, fqdn FROM oim.resource ORDER BY name;" | mysql -u root 
	    echo "---------------------------------------------------"
	    resource_selector
	    ;;
	*)
	    echo " Enter a resource id for which A and R will be recalculated "
	    echo "  where [val] is an INTEGER you retrieved from \"list\" option"
	    echo -n " Enter resource id: "
	    read -e ANS_RESOURCE_ID
	    echo "Resource id \"${ANS_RESOURCE_ID}\" entered" 1>> $LOG 
	    export resource_id_start=${ANS_RESOURCE_ID}
	    export resource_id_end=${ANS_RESOURCE_ID}
	    ;;
    esac
}

function get_number_of_days() {
    echo ""
    echo "Enter the number of days you would like to go back and recalculate."
    echo "For example, today is Jan 7 2010, if you want to recalculate A and R"
    echo " starting from Dec 1st 2009, then enter 31+7 = 38."
    echo -n " Enter number of days: "
    read -e ANS_NUMBER_DAYS
    echo "Number of days \"${ANS_NUMBER_DAYS}\" entered"  1>> $LOG 
    export ANS_NUMBER_DAYS
}



################## MAIN SCRIPT BEGINS ####################

if [ ${1}0 = "-h0" ]
    then
    help
fi

set_current_timestamp
echo "${DATEF}: Begin manual-process" 2>&1 | tee -a $LOG

echo "-------------------------------------------------------------------------"
echo "What would you like to do? (Type $0 -h for more information)"
echo " showtimestamp: Show timestamp for latest record"
echo " preprocess: Redo preprocessing from Gratia DB"
echo " status: Just recalculate resource/service status using normalized data"
echo " ar: Just recalculate availability/reliability numbers using normalized data"
echo "-------------------------------------------------------------------------"
echo -n "Please Select: "
read -e ANS_ACTION
echo "Main Option \"${ANS_ACTION}\" entered"  1>> $LOG 
## agopu: REMOVE comment
#ANS_ACTION="preprocess"
case ${ANS_ACTION} in
    showtimestamp)
	RPTIMESTAMP=`echo "SELECT FROM_UNIXTIME(timestamp) FROM ${RSVPROCESSDB}.metricdata WHERE id=(SELECT value FROM ${RSVPROCESSDB}.processlog WHERE ${RSVPROCESSDB}.processlog.key LIKE 'last_metricdata_id_processed');" | mysql -u root|tail -1`
	echo "" 2>&1 | tee -a $LOG
	echo " Current date/time (UTC): ${DATEF}"  2>&1 | tee -a $LOG
	echo " Last Processed Metric Timestamp: ${RPTIMESTAMP}" 2>&1 | tee -a $LOG
	echo "" 2>&1 | tee -a $LOG
	;;    
    preprocess)
	echo -n "Enter timestamp to redo pre-process from Gratia DB in format (2010-01-07 00:00:00): "
	read -e ANS_TIMESTAMP
	echo "Timestamp \"${ANS_TIMESTAMP}\" entered"  1>> $LOG 
	
	## agopu: REMOVE coment
	#ANS_TIMESTAMP="2010-01-07 00:00:00"
	GDBID=`echo "SELECT MIN(id) FROM ${RSVPROCESSDB}.metricdata WHERE timestamp > UNIX_TIMESTAMP('${ANS_TIMESTAMP}');" | mysql -u root | tail -1`
	echo "We will delete all normalized records with DBID greater than ${GDBID} " 2>&1 | tee -a $LOG
	echo " - This action is IRREVERSIBLE (except for possible restoration from tape-backup!)" 2>&1 | tee -a $LOG

	warn_allow_exit

	mv /etc/cron.d/rsvprocess  /tmp/rsvprocess.crontab.$$
	wait_if_rsvprocess_running

	## agopu/hayashi: TODO: Need to check if timestamp is too old and/or if data is available in Gratia DB
	STR_DELETE_NORM_RECS="DELETE FROM ${RSVPROCESSDB}.metricdata WHERE id>${GDBID}; "
	STR_UPDATE_PROCESSLOG="UPDATE ${RSVPROCESSDB}.processlog  SET value=${GDBID}; "
	echo "Will execute following MySQL queries: " 2>&1 | tee -a $LOG
	echo " ${STR_DELETE_NORM_RECS}" 2>&1 | tee -a $LOG
	echo " ${STR_UPDATE_PROCESSLOG}" 2>&1 | tee -a $LOG
	echo "This might take a while!" 2>&1 | tee -a $LOG

	echo " ${STR_DELETE_NORM_RECS}" | mysql -u root 
	echo " ${STR_UPDATE_PROCESSLOG}" | mysql -u root 
	
	mv /tmp/rsvprocess.crontab.$$ /etc/cron.d/rsvprocess
	echo "Restored rsvprocess cronjob; it should catch up with Gratia DB over several minutes" 2>&1 | tee -a $LOG
	echo "We urge you to check ${RSVPROCESS_HOME}/log/run.log" 2>&1 | tee -a $LOG
	echo ""
	echo "IMPORTANT NOTE:" 2>&1 | tee -a $LOG
	echo " Once the ${RSVPROCESSDB} database catches up with the Gratia DB records, status" 2>&1 | tee -a $LOG
	echo " will be automatically calculated but you WILL have to manually" 2>&1 | tee -a $LOG
	echo " reprocess A and R by executing this script with the ar option" 2>&1 | tee -a $LOG
	echo " AFTER the catch up is complete! You can use the \"showtimestamp\"" 2>&1 | tee -a $LOG
	echo " option to see where RSVProcess stands." 2>&1 | tee -a $LOG
	echo ""
	;;
    status)
	resource_selector
	get_number_of_days
	
	current=`date +%s`;
	today=`expr $current / 86400`
	today=`expr $today '*' 86400`

	echo "Recalculating availability/reliability history for last ${ANS_NUMBER_DAYS} days..." 2>&1 | tee -a $LOG
	echo " This might take a while! " 2>&1 | tee -a $LOG
	echo " In another shell, feel free to do:" 2>&1 | tee -a $LOG
	echo "   \"tail -f ${LOG}\" and/or" 2>&1 | tee -a $LOG
	echo "   \"tail -f ${STDERRLOG}\"" 2>&1 | tee -a $LOG

	m=`expr 86400 '*' ${ANS_NUMBER_DAYS}`
	start=`expr $today - $m`
	echo "Start time: $start" 2>&1 | tee -a $LOG
	echo "Current time: $current" 2>&1 | tee -a $LOG

	warn_allow_exit

	mv /etc/cron.d/rsvprocess  /tmp/rsvprocess.crontab.$$
	wait_if_rsvprocess_running

	cd ${RSVPROCESS_HOME}
	for i in $(seq $resource_id_start 1 $resource_id_end)
	  do
	  java rsv.process.control.RSVMain overallstatus $i $start $current 1>>${LOG} 2>>${STDERRLOG}
	done
	cd -
	mv /tmp/rsvprocess.crontab.$$ /etc/cron.d/rsvprocess
	;;
    ar)
	# echo -n "Enter timestamp to redo pre-process from Gratia DB in format (2010-01-07 00:00:00): "
	get_number_of_days

	echo "Recalculating availability/reliability history for all " 2>&1 | tee -a $LOG
	echo " resources for last ${ANS_NUMBER_DAYS} days..." 2>&1 | tee -a $LOG
	echo " This might take a while! "
	echo " Feel free to do tail -f 1>>${LOG} and ${STDERRLOG} in another shell" 2>&1 | tee -a $LOG

	current=`date -u +%s`;
	today=`expr $current / 86400`
	today=`expr $today '*' 86400`

	warn_allow_exit

	mv /etc/cron.d/rsvprocess  /tmp/rsvprocess.foo
	wait_if_rsvprocess_running

	cd ${RSVPROCESS_HOME}
	for((i=0;i<=${ANS_NUMBER_DAYS};i+=1)); do
	    m=`expr 86400 '*' $i`
	    time=`expr $today - $m`
	    echo "AR recalc time: $time" 2>&1 | tee -a $LOG
	    java rsv.process.control.RSVMain availability $time  1>>${LOG} 2>>${STDERRLOG}
	done
	cd - 
	mv /tmp/rsvprocess.foo /etc/cron.d/rsvprocess
	;;
    *) 
	echo "Invalid entry, quitting"
	set - 
	exit 1
	;;
esac

set_current_timestamp
echo "${DATEF}: End manual-process" 2>&1 | tee -a $LOG
echo "-----------------------------------------" 2>&1 | tee -a $LOG
echo "" 2>&1 | tee -a $LOG
set - 
exit 0
