#This software used to exist under an in-house build system, this Makefile
#is just a quick hack to get MoniCA running outside of ATNF..

CLIENT_JAR=monica-client.jar
SERVER_JAR=monica-server.jar

CLASSPATH=".:caj-1.1.3.jar:jcommon-0.9.1.jar:jfreechart-0.9.16.jar:jca-2.3.1.jar:jep-2.24.jar:jsch-0.1.37.jar"

CLIENT_FILES =atnf/atoms/mon/*.class \
              atnf/atoms/mon/client/*.class \
              atnf/atoms/mon/util/*.class \
              atnf/atoms/mon/limit/*.class \
              atnf/atoms/mon/gui/*.class \
              atnf/atoms/mon/gui/monpanel/*.class \
              atnf/atoms/mon/gui/monpanel/*.gif \
              atnf/atoms/time/*.class \
              atnf/atoms/util/StaticOnly.class \
              atnf/atoms/util/Angle.class \
              atnf/atoms/util/Log*.class \
              atnf/atoms/util/Named*.class \
              atnf/atoms/util/Enum*.class \
              atnf/atoms/util/Ex*.class \
              atnf/atoms/util/Immutable.class \
              monitor-preloads.txt \
              monitor-servers.txt

all: compile

#The compile is really sad.. first we just compile every java file, which
#involves a great deal of repeated compilation and second we supress some
#of the warning messages.
compile:
	find . -iname "*.java" -exec javac -nowarn -classpath ${CLASSPATH} {} \;
        
client:
	jar cmf manifest.txt ${CLIENT_JAR} ${CLIENT_FILES}
	jarsigner -keystore demo-keys ${CLIENT_JAR} monica

clean:
	rm -f ${CLIENT_JAR} ${SERVER_JAR}
	find atnf/ -iname "*.class" -exec rm -f {} \;
	find atnf/ -iname "*.html" -exec rm -f {} \;
