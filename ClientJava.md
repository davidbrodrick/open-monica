# Introduction #

Rather than using Ice to directly interface a client application to a MoniCA server, Java programmers can take advantage of classes provided with the MoniCA distribution. These are the same classes that the MoniCA GUI client uses. This makes the application immune from transport implementation changes and provides a much higher level interface to facilitate faster development.


# Select Server #

In order to tell the MoniCA library which server you require it to connect to, you would normally specify a value for the system property **MoniCA.server**. The value may take either _hostname_ or _hostname:port_ syntax, or if you are using an Ice locator service you can assign the property a value like _locator://hostname:port_.

```
-DMoniCA.server=my.server.com
```

If no value is specified for this property then MoniCA will present a graphical display element allowing the user to choose between the servers specified in the _monitor-servers.txt_ resource. If the _java.awt.headless_ property is set to _true_ then this will prevent the MoniCA library from issuing the graphical prompt to the user, and the application will not be able to run.

# Subscribing to Realtime Updates #

If your application needs to subscribe to updates for specific points in realtime, you can use the _atnf.atoms.mon.client.DataMaintainer_ class which provides a very high level interface. This involves implementing a class to receive updates as they happen and then subscribing to the set of points you are interested in.

## Callback Interface ##

You need to implement a class which implements the _atnf.atoms.mon.PointListener_ interface.

```
import atnf.atoms.mon.*;

public class Example implements PointListener
{
   public void onPointEvent(Object source, PointEvent evt)
   {
      PointData data = evt.getPointData();
      System.out.println("New data value for " + data.getName() + " is " + data.getData());
   }
}
```

Usually the only field of the _atnf.atoms.mon.PointEvent_ argument you will need to access is the _getPointData()_ method. This returns the latest _atnf.atoms.mon.PointData_ object to be collected for one of the points you have subscribed to.

## Subscribing ##

Once you have defined your _PointListener_ subclass it is trivial to use the _DataMaintainer_ to subscribe or unsubscribe from realtime updates. The relevant methods are:

```
  /** Subscribe the specified listener to updates from the specified point. */
  DataMaintainer.subscribe(String pointname, PointListener pl);

  /** Subscribe the specified listener to updates from all of the given points. */
  DataMaintainer.subscribe(Vector<String> pointnames, PointListener pl);

  /** Unsubscribe the listener from the specified point. */
  DataMaintainer.unsubscribe(String pointname, PointListener pl);

  /** Unsubscribe the listener from all points contained in the vector. */
  DataMaintainer.unsubscribe(Vector<String> pointnames, PointListener pl);
```

# Obtaining a Reference #

To access the complete functionality of the interface, client applications can obtain a reference to a _MoniCAClient_ instance by invoking the _atnf.atoms.mon.client.MonClientUtil.getServer()_ method. _MoniCAClient_ is an abstract class which is transport independent and the intention will be to maintain the same interface if and when alternative transports to Ice become available.

```
   import atnf.atoms.mon.comms.MoniCAClient;
   import atnf.atoms.mon.client.MonClientUtil;
   ...
   MoniCAClient myref = MonClientUtil.getServer();
   ...
```

# Interface Methods #

The javadoc provides detailed information for the _MoniCAClient_ class. The javadoc's can be produced by invoking ant and the resulting documentation will be generated into a directory called **javadoc**.

```
ant javadoc
```

# Example #

A class which demonstrates both an archival data query and also subscribes to realtime updates is bundled with MoniCA in the _atnf.atoms.mon.apps.TestApp_ class. An example of building and running this class, to connect to a publicly available server, is:

```
ant
java -DMoniCA.server=narrabri.ozforecast.com.au -cp open-monica.jar atnf.atoms.mon.apps.TestApp home.weather.wind_speed
```

This will produce output along the lines of:

```
MonClientUtil: Connecting to host "narrabri.ozforecast.com.au" on port 8052
ARCHIVE QUERY RESULTS FOR home.weather.wind_speed
        (2010-08-05 03:22:01.639)       2.52
        (2010-08-05 03:22:50.320)       3.6
        (2010-08-05 03:23:38.065)       7.2
        (2010-08-05 03:24:25.762)       8.64
        (2010-08-05 03:25:13.523)       8.64
        (2010-08-05 03:26:02.173)       5.04
SUBSCRIBING TO REAL-TIME UPDATES:
home.weather.wind_avg_speed     (2010-08-05 03:26:02.173)       5.04
home.weather.wind_avg_speed     (2010-08-05 03:26:49.966)       6.12
... etc ...
```