#!/bin/bash
export JAVA_HOME=/usr/lib/jvm/java-sun
export PATH=$JAVA_HOME/bin:$PATH
export RSVPROCESS_HOME=/usr/local/rsvprocess

ant -f $RSVPROCESS_HOME/build.xml vomatrix
if [ ! $? -eq 0 ]; then
    echo "vomatrix update has failed"
fi;
