#!/bin/bash

#test
export JAVA_HOME=/usr/lib/jvm/java-sun
export PATH=$JAVA_HOME/bin:$PATH
export RSVPROCESS_HOME=/usr/local/rsvprocess

ant -f $RSVPROCESS_HOME/build.xml availability_yesterday
if [ ! $? -eq 0 ]; then
    echo "availability(yesterday) calculation has failed"
fi;
