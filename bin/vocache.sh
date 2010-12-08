#!/bin/bash
export JAVA_HOME=/usr/lib/jvm/java-sun
export PATH=$JAVA_HOME/bin:$PATH
export CLASSPATH=lib/log4j-1.2.15.jar:lib/mysql-connector-java-5.1.6-bin.jar:rsvprocess.jar
ant -f /usr/local/rsvprocess/build.xml vomatrix
#java rsv.process.control.RSVMain vomatrix
