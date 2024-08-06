# Introduction #

The Perl module `MoniCA.pm` makes it possible to quickly and easily query a MoniCA
server through the ASCII interface with a Perl program. This module takes care of:
  * time conversion (BAT to other formats)
  * response handling, giving back a handy Perl hash for each data point returned
  * long time range queries, where the MoniCA server may force it to be made in more than one query

This Perl module was written by Chris Phillips, and extended by Jamie Stevens.

# Details #

## Requirements & Installation ##

Using this Perl module also requires that you install the `Astro::Time` Perl module from CPAN:
http://search.cpan.org/~cphil/Astro

Installing the `MoniCA.pm` module is easy. Simply change into the `clientlibs/perl` directory, and give the following commands:
```
perl Makefile.PL
make
sudo make install
```

Providing that it finds all its prerequisites, you will now be able to use the `MoniCA.pm` module. If you see errors about missing modules, use the CPAN search to find and install them.

## Usage ##

To use this Perl module, include the following code at the top of your Perl program:
```
use Astro::Time;
use ATNF::MoniCA;
```

This will allow you to use the functions provided by the `MoniCA.pm' module.

### Simple Example ###

Let's look at an example Perl program that queries a MoniCA server.
```
#!/usr/bin/perl

use strict;
use Astro::Time;
use ATNF::MoniCA;

my $mon=monconnect("monserver.domain.com");
if (!defined $mon){
  print "Could not connect to monitor server monserver.domain.com\n";
  exit;
}

my @monitor_points=("site.environment.weather.Temperature","site.environment.weather.RelHumidity");
my @point_vals=monpoll($mon,@monitor_points);
for (my $i=0;$i<=$#point_vals;$i++){
  print "point ".$point_vals[$i]->point." has value ".$point_vals[$i]->val." at time ".$point_vals[$i]->bat."\n";
}
```

From this example you can see how to connect to a specified monitoring server:
```
my $mon=monconnect("monserver.domain.com");
```

The `$mon` that is returned from this is a handle to the MoniCA server through its ASCII interface, and will need to be passed to whatever routines you later use to get values for monitoring points. If `$mon` comes back as "undefined", then the MoniCA server could not be contacted by the `monconnect` routine; you should check that the server is running, and that you have access to its ASCII port from the machine you're running your Perl code on.

The example then shows how to query the server for the latest values for a number of points, using the `monpoll` routine:
```
my @point_vals=monpoll($mon,@monitor_points);
```

As you can see, you call `monpoll` with the MoniCA server handle `$mon`, and with a list of monitoring point names `@monitor_points`. For our example, we have set:
```
my @monitor_points=("site.environment.weather.Temperature","site.environment.weather.RelHumidity");
```

You may specify as many point names you want to this command. If you only want the latest value of one point, you can alter your call to:
```
my $monitor_point="site.environment.weather.Temperature";
my $point_val=monpoll($mon,$monitor_point);
```

What is returned is a list of `MonPoint` objects, each of which has three properties:
  * `point`: the name of the monitoring point
  * `bat`: the BAT time of the returned value
  * `val`: the value of this monitoring point

So the last part of the example can be seen to loop over all the returned `MonPoint`s and print out all the information they contain:
```
for (my $i=0;$i<=$#point_vals;$i++){
  print "point ".$point_vals[$i]->point." has value ".$point_vals[$i]->val." at time ".$point_vals[$i]->bat."\n";
}
```

Let's run the program, and see the output:
```
point site.environment.weather.Temperature has value 35.4 at time 0x11109a8ee65d58
point site.environment.weather.RelHumidity has value 49.0 at time 0x11109a8ee65d58
```

### Available routines ###

We have just seen the `monconnect` and `monpoll` routines and how to use them. What follows is the full list of routines available from the `MoniCA.pm` package, and short usage examples.

#### monconnect ####

Set up a connection to a MoniCA server.

Usage:
```
$mon = monconnect($monserver_name);
```

  * `$monserver_name` is the name or address of the MoniCA server to connect to
  * `$mon` is, if a connection is made successfully, the MoniCA connection handle. If no connection is able to be made, `$mon` will be undefined.

#### dUT ####

Return the difference between UTC1 and TAI for the passed time

Usage:
```
$dUTC = dUT($mon, $mjd);
```

  * `$mon` is a successfully created MoniCA connection handle
  * `$mjd` is the MJD to begin the query from
  * `$dUTC` is the difference in seconds between UTC1 and TAI

#### setDUTC ####

Globally set the UTC1-TAI difference. All subsequent BAT to MJD conversions will make this adjustment internally.

Usage:
```
setdUTC($dUTC);
```

  * `$dUTC`  the difference in seconds between UTC1 and TAI

#### monpoll ####

Query the latest values for a list of MoniCA points.

Usage:
```
@returned_values = monpoll($mon, @point_names);
```

  * `$mon` is a successfully created MoniCA connection handle
  * `@point_names` is a list of MoniCA point names
  * `@returned_values` is a list of `MonPoint` objects, one for each point that was requested.

A `MonPoint` object has the properties:
  * `point`: the name of the monitoring point
  * `bat`: the BAT time of the returned value
  * `val`: the value of this monitoring point

#### monpoll2 ####

Query the latest values and error states for a list of MoniCA points.

Usage:
```
@returned_values = monpoll2($mon, @point_names);
```

  * `$mon` is a successfully created MoniCA connection handle
  * `@point_names` is a list of MoniCA point names
  * `@returned_values` is a list of `MonFullPoint` objects, one for each point that was requested.

A `MonFullPoint` object has the properties:
  * `point`: the name of the monitoring point
  * `bat`: the BAT time of the returned value
  * `val`: the value of this monitoring point
  * `units`: a string describing what the units of the value are
  * `errorstate`: a boolean flag indicating whether the point is in an error state; a value `true` indicates an error.

#### monsince ####

Get the values for a single MoniCA point between a specified time and the present.

Usage:
```
@pointvals = monsince($mon, $mjd, $pointname; $maxnper);
```

  * `$mon` is a successfully created MoniCA connection handle
  * `$mjd` is the MJD to begin the query from
  * `$pointname` is the name of the MoniCA point to query
  * `$maxnper` is the maximum number of points to return per query to the server. As documented for the ASCII interface, the MoniCA server may not return values covering the full time range in a single response, if a query would take too long to respond to. The Perl routine `monsince` will handle this situation, and will requery the server for values since the time of the last returned point. For each query that `monsince` makes, it will always include the first returned point, the last returned point, and `$maxnper-2` more points spread evenly across the points that were received from the server. All will points to be returned if `$maxnper` is not passed.
  * `@pointvals` is a list of `MonBetweenPoint` objects.

A `MonBetweenPoint` object has the properties:
  * `bat`: the BAT time of the returned value
  * `val`: the value of this monitoring point at the time `bat`

#### monbetween ####

Get the values for a single MoniCA point between two specified times.

Usage:
```
@pointvals = monbetween($mon, $mjd1, $mjd2, $pointname, $maxnper);
```

  * `$mon` is a successfully created MoniCA connection handle
  * `$mjd1` is the MJD to begin the query from
  * `$mjd2` is the MJD to end the query at
  * `$pointname` is the name of the MoniCA point to query
  * `$maxnper` is the maximum number of points to return per query to the server. As documented for the ASCII interface, the MoniCA server may not return values covering the full time range in a single response, if a query would take too long to respond to. The Perl routine `monbetween` will handle this situation, and will requery the server for values since the time of the last returned point. For each query that `monbetween` makes, it will always include the first returned point, the last returned point, and `$maxnper-2` more points spread evenly across the points that were received from the server. If you want all points to be returned, set `$maxnper` to a number <=0.
  * `@pointvals` is a list of `MonBetweenPoint` objects.

#### mondetails ####

Query the server for the descriptions for one or more MoniCA points.

Usage:
```
@pointdescriptions = mondetails($mon, @pointnames);
```

  * `$mon` is a successfully created MoniCA connection handle
  * `@pointnames` is a list of MoniCA point names
  * `@pointdescriptions` is a list of `MonDetail` objects, one for each point that was requested.

A `MonDetail` object has the properties:
  * `point`: the name of the monitoring point
  * `updatetime`: the time, in seconds, between server updates for this point
  * `units`: a string describing what the units of the value are
  * `description`: a human-readable string describing what the point represents

#### monnames ####

Query the server for the available monitor point names.

Usage:
```
@pointnames = monnames($mon);
```

  * `$mon` is a successfully created MoniCA connection handle
  * `@pointnames` is a list of strings, one for each monitor point available on the server.

#### montill ####

#### monpreceeding ####

#### monfollowing ####

#### monset\_m ####

Set the values for one or more points on the server.

Usage:
```
@setresults = monset_m($mon, $user, $pass, $encrypt, @monsetpoints);
```

  * `$mon` is a successfully created MoniCA connection handle
  * `$user` is a string representing the username required to set the value for the points
  * `$pass` is a string representing the password required to set the value for the points
  * `$encrypt` is a number, instructing this function how to deal with the username and password. It can have one of the following values:
    * `0`: do not encrypt the username and password, either because you wish to transmit them in plain-text (bad idea), or they are already appropriately encrypted
    * `1`: encrypt the username and password with the session-specific RSA key provided by the server
    * `2`: encrypt the username and password with the persistent RSA key provided by the server
  * `@monsetpoints` is list (or scalar) of `MonSetPoint` objects.
  * `@setresults` is a list (or scalar) or `MonSetPoint` objects, identical to the list sent as `@monsetpoints`, but with the `success` property filled.

A `MonSetPoint` object has the properties:
  * `point`: the name of the monitoring point
  * `type`: the type of the point (ie. `'int'` for integer, a full list can be found on the [ClientASCII page](https://code.google.com/p/open-monica/wiki/ClientASCII) under the set command)
  * `val`: the value to set the point to, appropriate for the `type`
  * `success`: upon return from a setting routine, this value will either be `0` if the setting failed, or `1` if it succeeded

#### monset ####

A legacy routine to enable setting the values for one or more points on the server. It calls the `monset_m` routine internally, but does not support encryption of the username and password. It exists so that old codes that use `monset` before encryption support became available will work without modification.

Usage:
```
@setresults = monset($mon, $user, $pass, @monsetpoints);
```

See `monset_m` for a description of these arguments.

#### monalarms ####

Get the state of the currently alarmed points, or those points that have been shelved.

Usage:
```
@alarmstates = monalarms($mon);
```

  * `$mon` is a successfully created MoniCA connection handle
  * `@alarmstates` is a list of `MonAlarm` objects describing the currently alarmed points on the MoniCA server.

A `MonAlarm` object has the properties:
  * `point`: the name of the alarm point
  * `priority`: the severity of the alarm, as an integer (described on the [alarm functionality](https://code.google.com/p/open-monica/wiki/AlarmFunctionality) page)
  * `alarm`: whether the alarm is active, as a string (true or false)
  * `acknowledged`: whether the alarm has been acknowledged, as a string (true or false)
  * `acknowledgedby`: a string, either `null` if the alarm has not been acknowledged, or the name of the user that acknowledged the alarm
  * `acknowledgedat`: the BAT timestamp when the alarm was acknowledged, or `null` otherwise
  * `shelved`: whether the alarm has been shelved, as a string (true or false)
  * `shelvededby`: a string, either `null` if the alarm has not been shelved, or the name of the user that shelved the alarm
  * `shelvededat`: the BAT timestamp when the alarm was shelved, or `null` otherwise
  * `guidance`: the guidance text associated with this alarm condition

#### monallalarms ####

Get the state of all the points that are defined as alarms on the server, regardless of their current state.

Usage:
```
@alarmstates = monallalarms($mon);
```

  * `$mon` is a successfully created MoniCA connection handle
  * `@alarmstates` is a list of `MonAlarm` objects describing the currently alarmed points on the MoniCA server.

The `MonAlarm` object properties are described in the `monalarms` function description.

#### monalarmack\_m ####

Acknowledge or deacknowledge an alarm.

Usage:
```
@ackresults = monalarmack_m($mon, $user, $pass, $encrypt, @alarmnames);
```

  * `$mon` is a successfully created MoniCA connection handle
  * `$user` is a string representing the username required to acknowledge the alarms
  * `$pass` is a string representing the password required to acknowledge the alarms
  * `$encrypt` is a number, instructing this function how to deal with the username and password. It can have one of the following values:
    * `0`: do not encrypt the username and password, either because you wish to transmit them in plain-text (bad idea), or they are already appropriately encrypted
    * `1`: encrypt the username and password with the session-specific RSA key provided by the server
    * `2`: encrypt the username and password with the persistent RSA key provided by the server
  * `@alarmnames` is a list (or scalar) of hashes that describe the alarm to acknowledge or deacknowledge, with the properties:
    * `point`: the name of the alarm point
    * `value`: a string, either `true` to acknowledge the alarm, or `false` to deacknowledge it
  * `@ackresults` is a list (or scalar) of strings, in the same order as the alarms specified in `@alarmnames`; each string will be `OK` if the acknowledgement succeeded, or `ERROR` if not

#### monalarmack ####

A legacy routine to enable alarm acknowledgement for one or more alarm points on the server. It calls the `monalarmack_m` routine internally, but does not support encryption of the username and password. It exists so that old codes that use `monalarmack` before encryption support became available will work without modification.

Usage:
```
@ackresults = monalarmack($mon, $user, $pass, @alarmnames);
```

See `monalarmack_m` for a description of these arguments.

#### monalarmshelve\_m ####

Shelve or unshelve an alarm.

Usage:
```
@ackresults = monalarmshelve_m($mon, $user, $pass, $encrypt, @alarmnames);
```

  * `$mon` is a successfully created MoniCA connection handle
  * `$user` is a string representing the username required to shelve the alarms
  * `$pass` is a string representing the password required to shelve the alarms
  * `$encrypt` is a number, instructing this function how to deal with the username and password. It can have one of the following values:
    * `0`: do not encrypt the username and password, either because you wish to transmit them in plain-text (bad idea), or they are already appropriately encrypted
    * `1`: encrypt the username and password with the session-specific RSA key provided by the server
    * `2`: encrypt the username and password with the persistent RSA key provided by the server
  * `@alarmnames` is a list (or scalar) of hashes that describe the alarm to shelve or unshelve, with the properties:
    * `point`: the name of the alarm point
    * `value`: a string, either `true` to shelve the alarm, or `false` to unshelve it
  * `@ackresults` is a list (or scalar) of strings, in the same order as the alarms specified in `@alarmnames`; each string will be `OK` if the shelving succeeded, or `ERROR` if not

#### monalarmshelve ####

A legacy routine to enable alarm shelving for one or more alarm points on the server. It calls the `monalarmshelve_m` routine internally, but does not support encryption of the username and password. It exists so that old codes that use `monalarmshelve` before encryption support became available will work without modification.

Usage:
```
@ackresults = monalarmshelve($mon, $user, $pass, @alarmnames);
```

See `monalarmshelve_m` for a description of these arguments.

#### getRSA ####

This routine will return either of the two RSA public keys that can be used to encrypt the username and password for all point setting or alarm acknowledgement/shelving actions.

Usage:
```
$rsa_key = getRSA($mon, $option);
```

  * `$mon` is a successfully created MoniCA connection handle
  * `$option` is an optional integer that indicates which RSA public key to return:
    * `0` will return the key that is valid only while the `$mon` handle stays open (this is the session-specific key)
    * `1` will return the key that is valid while the MoniCA server stays up (this is the persistent key)
    * omitting this argument is equivalent to setting it to `0`
  * `$rsa_key` is a reference to a hash with the properties:
    * `exponent`: the RSA key exponent, as a string
    * `modulus`: the RSA key modulus, as a string