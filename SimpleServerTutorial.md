# Introduction #

This is a very simple example of how to set MoniCA up to start reading data from an ASCII socket. Before following this tutorial you should have gone through the [Installation](Installation.md) process and verified that the basic software build is working okay.

# Dummy Server #

The tutorial assumes that MoniCA will be connecting to a socket, over which it will receive lines of ASCII text with a fixed number of fields in each line. By using the simple program below together with the widely available program 'socket' you can create such a server to use for the excercise.

```
#!/bin/bash
#Put this program somewhere, eg, /tmp/foo.sh and make executable.

while true
do
  echo $RANDOM $RANDOM $RANDOM
  sleep 1
done
```

You can then make this available using socket, eg to create a server listening on port 8000 use:

```
socket -slp /tmp/foo.sh 8000
```

You should quickly test that the dummy server is running okay by connecting to the port with telnet, eg 'telnet localhost 8000'

# Config Files #

## monitor-sources.txt ##

You need to tell MoniCA to connect to the server on the specified host/port using an 'ASCIILine' interface, and to fire updates to a particular point whenever a new line is read. The additional argument is a timeout value in milliseconds.

```
ASCIILine localhost:8000:5000:site.hidden.line
```

## monitor-points.txt ##

This is the config file where you define a top-level point which will receive the raw lines from the socket, as well as the definitions for the individual points which will extract particular fields from the ASCII line.

If your line is delimited by a character other than spaces, eg commas, then you can specify that character as an argument to the StringToArray translation defined for the first point.

```
#The first one is the full ASCII data line, it is only looked at by other points
hidden.line         "The full ASCII line"  ""  ""    site T -                         - StringToArray-                                     -                - 1000000 -
#Definitions for three points which listen to updates to the full line and extract particular fields
test.mydata1        "First data field"     ""  "MHz" site T Listen-"site.hidden.line" - {Array-"0", StringToNumber-"Integer"}              -                - 1000000 -
#Let's set an alarm threshold for the second point
test.mydata2        "Second data field"    ""  "A"   site T Listen-"site.hidden.line" - {Array-"1", StringToNumber-"Integer"}              Range-"0""16384" - 1000000 -
#Let's apply a mathematical formula to this last point
test.mydata3        "Third data field"     ""  "V"   site T Listen-"site.hidden.line" - {Array-"2", StringToNumber-"Integer", EQ-"x/1000"} -                - 1000000 -
```

Be careful of any line-wrapping when copying the text above.

# Building and Testing #

Now that you have modified your config files, rebuild and restart the server as described in the [Installation](Installation.md) wiki page.

You should then be able to start a client program and connect to your server. Go to the 'Setup' menu and add a new 'Point Table' to the display. You should then be able to select the points under 'test' in the tree and click okay to watch the values update in real time.