This page lists external systems and any pertinent information about them. Feel free to document your external system here.

# JSONSocket #

This is derived from ASCIISocket and simply reads data in a JSON format and writes data that is being set back in JSON format. Example: A device sends this JSON string:
```
{ "Control":0,"LED_set":183 }
```

Given you want them stored in your system as follows:
```
sometree.Control
sometree.LED_set
```

Define the source in monitor-sources.txt:
```
JSONSocket 127.0.0.1:9999
```

Define your data source in monitor-points:
```
# define the data source
hidden.myJSON "My JSON Data" "" ""  mysource  T Generic-"127.0.0.1:9999" - - - - 100000 -

sometree.Control "Control" "" "" mysource T Listen-"mysource.hidden.JSON" - NV-"Control"  "" Change-  1000000 -
sometree.LED_set "LED set" "" "" mysource T Listen-"mysource.hidden.JSON" - NV-"LED_set"  "" Change-  1000000 -
```

The control point then needs a separate entry:
```
sometree.ctrl.Control  "Control" "" "" mysource T - Strings-"127.0.0.1:9999""$V" - -  Change-  - -
sometree.ctrl.Set      "Set Pt"  "" "" mysource T - Strings-"127.0.0.1:9999""$V" - -  Change-  - -
```

**Limitations:**
  * This currently assumes all JSON data coming in is of `Float` data type. If anyone cares to fix this so it can cater to any data type that would be nice. I'll get to it in early 2016 otherwise.
  * All data sent out is sent as a string. This probably does not need fixing, but if you're game, go for it.

# ISServer #

ISServer is an external system class in open-monica which allows easy importing of "any format data", i.e. data you've obtained from scripts, greps, pipes, netcats and other quickfixes likely to survive longer than you ever thought when you hacked them. In very simple terms, ISServer connects to an instance of the ISServer.pl script running on any machine and sends the tickle string, or just sends the string "all" if you don't specify a tickle string. By default, this elicits the reaction on the server of blurting out your data formatted in a way conveniently ingestible by open-monica.

## Details ##
Here's how to use it:

in `monitor-sources.txt` enter this line for your system: `ISServer 127.0.0.1:7111:5000:ticklestring` where
  * 127.0.0.1 is the IP address the machine running ISServer.pl,
  * 7111 is the port on which it's configured to run,
  * 5000 is the time interval to poll it at (in milliseconds, so 5000 = 5s) and finally,
  * the optional tickle string is the string sent to the server's TCP port to elicit the data you need. If not specified, the string "all" is sent which causes the default configuration of ISServer.pl to just send all the data in its data directory.

## Installation ##
Simply look at the ISServer example scripts in the clientlibs directory: When Gather\_Sample\_Data.pl runs it collects the data and stores it to files in  the data directory. ISServer.pl is the server script which serves the contents of that data directory to a TCP client (i.e. open-monica) when instructed to do so. In most scenarios, both should be run from a cron job, so make sure you enter them accordingly after having them tested manually. They both have mechanisms built in to ascertain they are not started more than once, so are safe to run from a crontab like this:

```
*/1 * * * * /usr/local/bin/open-monica-server.sh start >/dev/null 2>&1
*/1 * * * * /Users/yourname/MoniCA/Gather_Sample_Data.pl >/dev/null 2>&1
*/1 * * * * /Users/yourname/MoniCA/Get_Temperatures.pl >/dev/null 2>&1
*/1 * * * * /Users/yourname/MoniCA/ISServer.pl >/dev/null 2>&1
```

In the Gather\_Sample\_Data.pl script you need to
  * customise the data directory
  * modify which data you want gathered (which can be the output of any script you can think of running)

In ISServer.pl all you need to customise is
  * the port number (if you want that customised) as well as
  * the data directory so it points to the same location as the data gathering script.

Note that you can also modify the behaviour of the ISServer script so it sends different data based on the aptly named "tickle string" in the open-monica setup.

## Required perl modules ##
Make sure you can install perl modules on the machine you want this to run. You'll need to install the following modules:
  * POE
  * File::NFSLock