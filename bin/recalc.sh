#!/bin/bash

cd ..

export JAVA_HOME=/usr/lib/jvm/java-openjdk
export PATH=$JAVA_HOME/bin:$PATH
export RSVPROCESS_HOME=/usr/local/rsvprocess
export CLASSPATH=lib/log4j-1.2.15.jar:lib/mysql-connector-java-5.1.6-bin.jar:rsvprocess.jar

current=`date +%s`;
today=`expr $current / 86400`
today=`expr $today '*' 86400`
m=`expr 86400 '*' 660`
start=`expr $today - $m`
echo $start
echo $current

resource_id_start=1
resource_id_end=1
for i in $(seq $resource_id_start 1 $resource_id_end)
do
        java rsv.process.control.RSVMain overallstatus $i $start $current
done

cd -

