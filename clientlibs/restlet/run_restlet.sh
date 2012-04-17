#!/bin/sh

wdir=`dirname $0`
cd $wdir
server="$1"

java -classpath  ../lib/log4j-1.2.15.jar:../lib/Ice.jar:../lib/org.restlet.jar:../lib/gson-2.1.jar:../lib/open-monica.jar:../lib/monica-restlet.jar cass.monica.rest.MoniCAApplication $server


