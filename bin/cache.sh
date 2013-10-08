echo "create cache"
export JAVA_HOME=/usr/lib/jvm/java-openjdk
export PATH=$JAVA_HOME/bin:$PATH
#export CLASSPATH=lib/log4j-1.2.15.jar:lib/mysql-connector-java-5.1.6-bin.jar:rsvprocess.jar
#cd /usr/local/rsvprocess
#ant
#java rsv.process.control.RSVMain cache
#cd -


export RSVPROCESS_HOME=/usr/local/rsvprocess
ant -f $RSVPROCESS_HOME/build.xml cache
if [ ! $? -eq 0 ]; then
    echo "cache has failed"
fi;


