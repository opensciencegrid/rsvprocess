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

GOC=/usr/local/
GOCUSER="agopu"

RPHOME=${GOC}/rsvprocess
RPDB="rsvprocess"

DATEU=`date -u +%u`

#if [ ${SRC}0 = "0" ]
#    then echo "Environment variable SRC is not set - quitting."
#    exit 1
#fi

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
    RUNNINGRP=`ps -ef | grep -v $$ | grep [r]svprocess`
    export RPISRUNNING=$?
    if [ ${DEBUG_GOC}0 != "0" ] 
	then 
	echo "##################################################"
	echo ${RUNNINGRP}
	echo "##################################################"
    fi
}

if [ ${1}0 = "-h0" ]
    then
    help
fi

echo "-------------------------------------------------------------------------"
echo "What would you like to do? (Type $0 -h for more information)"
echo " preprocess: Redo preprocessing from Gratia DB"
echo " status: Just recalculate resource/service status using normalized data"
echo " ar: Just recalculate availability/reliability numbers using normalized data"
echo "-------------------------------------------------------------------------"
echo -n "Please Select: "
#read -e ANS_ACTION
## agopu: REMOVE comment
ANS_ACTION="preprocess"
case ${ANS_ACTION} in
    status)
	echo ""
	export INSTALL_HOSTNAME=gadget.grid.iu.edu
	;;
    ar)
	echo ""
	;;
    preprocess)
#	echo -n "Enter timestamp to redo pre-process from Gratia DB in format (2010-01-07 00:00:00): "
#	read -e ANS_TIMESTAMP
	## agopU: REMOVE coment
	ANS_TIMESTAMP="2010-01-07 00:00:00"
	GDBID=`echo "SELECT MIN(id) FROM ${RPDB}.metricdata WHERE timestamp > UNIX_TIMESTAMP('${ANS_TIMESTAMP}');" | mysql -u root | tail -1`
	echo "We will delete all normalized records with DBID greater than ${GDBID} "
	echo " - This action is IRREVERSIBLE (except for possible restoration from tape-backup!)"
	warn_allow_exit
	mv /etc/cron.d/rsvprocess  /tmp/rsvprocess.foo
	check_for_rsvprocess_running
	while [ ${RPISRUNNING} -eq 0 ] 
	  do
	  echo "Found RSVProcess process(es) - will sleep for 10 seconds!" ## agopu to hayashis: BAD IDEA?!?! to ask peoople to kill the process(es)?
	  sleep 10
	  check_for_rsvprocess_running
	done
	
	echo ""
	echo "No more existing rsvprocess processes! Will proceed ... "
	## agopu/hayashi: TODO: Need to check if timestamp is too old and/or if data is available in Gratia DB
	STR_DELETE_NORM_RECS="DELETE FROM ${RPDB}.metricdata WHERE id>${GDBID}; "
	STR_UPDATE_PROCESSLOG="UPDATE ${RPDB}.processlog  SET value=${GDBID}; "
	echo "Will execute following MySQL queries: "
	echo " ${STR_DELETE_NORM_RECS}"
	echo " ${STR_UPDATE_PROCESSLOG}"
	echo "This might take a while!"

	echo " ${STR_DELETE_NORM_RECS}" | mysql -u root 
	echo " ${STR_UPDATE_PROCESSLOG}" | mysql -u root 
	
	mv /tmp/rsvprocess.foo /etc/cron.d/rsvprocess
	echo "Restored rsvprocess cronjob; it should catch up with Gratia DB over several minutes"
	echo "We urge you to check ${RPHOME}/log/run.log"
	;;
    *) 
	echo "Invalid entry, quitting"
	set - 
	exit 1
	;;
esac

echo "All done!"
set - 
exit 0
