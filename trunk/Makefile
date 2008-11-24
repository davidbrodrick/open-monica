#This software used to exist under an in-house build system, this Makefile
#is just a quick hack to get MoniCA running outside of ATNF..

CLIENT_JAR=monica-client.jar
SERVER_JAR=monica-server.jar

CLASSPATH=".:3rdParty/caj-1.1.5.jar:3rdParty/jcommon-0.9.1.jar:3rdParty/jfreechart-0.9.16.jar:3rdParty/jca-2.3.2.jar:3rdParty/jep-2.24.jar:3rdParty/jsch-0.1.37.jar"

CLIENT_FILES =atnf/atoms/mon/*.class \
              atnf/atoms/mon/client/*.class \
              atnf/atoms/mon/util/*.class \
              atnf/atoms/mon/limit/*.class \
              atnf/atoms/mon/gui/*.class \
              atnf/atoms/mon/gui/monpanel/*.class \
              atnf/atoms/mon/gui/monpanel/*.gif \
              atnf/atoms/mon/gui/monpanel/*.wav \
              atnf/atoms/time/*.class \
              atnf/atoms/util/*.class \
              monitor-preloads.txt \
              monitor-servers.txt

SERVER_FILES =atnf/atoms/mon/*.class \
              atnf/atoms/mon/util/*.class \
              atnf/atoms/mon/limit/*.class \
              atnf/atoms/mon/transaction/*.class \
              atnf/atoms/mon/translation/*.class \
              atnf/atoms/mon/datasource/*.class \
              atnf/atoms/mon/archiver/*.class \
              atnf/atoms/mon/archivepolicy/*.class \
              atnf/atoms/time/*.class \
              atnf/atoms/util/*.class \
              monitor-points.txt \
              monitor-sources.txt \
              monitor-epics.txt \
              monitor-config.txt \
              monitor-setups.txt

all: compile

#The build procedure is sad.. we just compile every java file, which involves a great 
#deal of repeated compilation for some classes. Need to migrate to ant or the like.
compile:
	find . -iname "*.java" -exec javac -source 1.4 -target 1.4 -nowarn -classpath ${CLASSPATH} {} \;
        
client:
	jar cmf manifest-client.txt ${CLIENT_JAR} ${CLIENT_FILES}
	echo monica | jarsigner -keystore demo-keys ${CLIENT_JAR} monica
	@echo

server:
	jar cmf manifest-server.txt ${SERVER_JAR} ${SERVER_FILES}

clean:
	rm -f ${CLIENT_JAR} ${SERVER_JAR}
	find atnf/ -iname "*.class" -exec rm -f {} \;
	find atnf/ -iname "*.html" -exec rm -f {} \;
