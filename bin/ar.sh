#!/bin/bash

echo "recalculating last n-day availability history"

export JAVA_HOME=/home/hayashis/app/jdk1.6.0_07
export PATH=$JAVA_HOME/bin:$PATH

export RSVPROCESS_HOME=/usr/local/rsvprocess

export CLASSPATH=lib/log4j-1.2.15.jar:lib/mysql-connector-java-5.1.6-bin.jar:rsvprocess.jar

current=`date +%s`;
today=`expr $current / 86400`
today=`expr $today '*' 86400`

for((i=0;i<=100;i+=1)); do
m=`expr 86400 '*' $i`
time=`expr $today - $m`
echo $time
java rsv.process.control.RSVMain availability $time
done
