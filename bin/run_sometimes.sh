#!/bin/bash

export JAVA_HOME=/home/hayashis/app/jdk1.6.0_07
export PATH=$JAVA_HOME/bin:$PATH

export RSVPROCESS_HOME=/usr/local/rsvprocess
ant -f $RSVPROCESS_HOME/build.xml vomatrix
if [ ! $? -eq 0 ]; then
    echo "vomatrix update has failed"
fi;
