echo "create vomatrix cache"
export JAVA_HOME=/usr/local/jdk160
export PATH=$JAVA_HOME/bin:$PATH
export CLASSPATH=lib/log4j-1.2.15.jar:lib/mysql-connector-java-5.1.6-bin.jar:rsvprocess.jar
cd /usr/local/rsvprocess
ant
java rsv.process.control.RSVMain vomatrix
cd -
