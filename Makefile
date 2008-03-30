#This software used to exist under an in-house build system, this Makefile
#is just a quick hack to get MoniCA running outside of ATNF..

CLIENT_JAR=monica-client.jar
SERVER_JAR=monica-server.jar

CLIENT_FILES =atnf/atoms/mon/*.class \
              atnf/atoms/mon/client/*.class \
              atnf/atoms/mon/util/*.class \
              atnf/atoms/mon/limit/*.class \
              atnf/atoms/mon/gui/*.class \
              atnf/atoms/mon/gui/monpanel/*.class \
              atnf/atoms/mon/gui/monpanel/*.gif \
              atnf/atoms/time/*.class \
              atnf/atoms/util/StaticOnly.class \
              atnf/atoms/util/*Angle*.class \
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
	find . -iname "*.java" -exec javac {} \; 2>&1 | grep -v Xlint | grep -v unchecke
        
client:
	jar cmf manifest.txt ${CLIENT_JAR} ${CLIENT_FILES}
	jarsigner -keystore demo-keys ${CLIENT_JAR} monica

clean:
	rm -f ${CLIENT_JAR} ${SERVER_JAR}
	find atnf/ -iname "*.class" -exec rm -f {} \;
	find atnf/ -iname "*.html" -exec rm -f {} \;
