echo "create cache"
export JAVA_HOME=/home/hayashis/app/jdk1.6.0_07
export PATH=$JAVA_HOME/bin:$PATH
export CLASSPATH=lib/log4j-1.2.15.jar:lib/mysql-connector-java-5.1.6-bin.jar:rsvprocess.jar
cd /usr/local/rsvprocess
java rsv.process.control.RSVMain cache
cd -

