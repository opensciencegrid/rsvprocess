#!/bin/bash
export JAVA_HOME=/usr/lib/jvm/java-sun
export PATH=$JAVA_HOME/bin:$PATH
export RSVPROCESS_HOME=/usr/local/rsvprocess

ant -f $RSVPROCESS_HOME/build.xml preprocess
if [ ! $? -eq 0 ]; then
    echo "preprocess calculation has failed"
fi;

ant -f $RSVPROCESS_HOME/build.xml overallstatus
if [ ! $? -eq 0 ]; then
    echo "overallstatus calculation has failed"
fi;

