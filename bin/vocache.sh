#!/bin/bash
#export JAVA_HOME=/usr/lib/jvm/java-sun
export JAVA_HOME=/usr/lib/jvm/java-openjdk
export PATH=$JAVA_HOME/bin:$PATH
#export CLASSPATH=lib/log4j-1.2.15.jar:lib/mysql-connector-java-5.1.6-bin.jar:rsvprocess.jar
#ant -f /usr/local/rsvprocess/build.xml vomatrix


export RSVPROCESS_HOME=/usr/local/rsvprocess
ant -f $RSVPROCESS_HOME/build.xml vomatrix
if [ ! $? -eq 0 ]; then
    echo "vomatrix has failed"
fi;


