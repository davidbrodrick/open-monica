# Introduction #

This document describes the file format for the point definitions files, which specifies the statically defined points you want created on your server. The meaning of some of the fields may make more sense after reading the IntroServerCode page.

Point definitions can be defined in the config/monitor-points.txt configuration resource. This gets built into the jar file. They may also be defined in discrete files in a points/ subdirectory of the ConfDir configuration directory defined in the monitor-config.txt configuration resource.

# Comments #

The file may contain blank lines as well as comments. Comments may be single line style or block comments.

```
#This is a single line comment

/*
 This is a block comment.
 It may span more than one line.
*/
```

# Point Definitions #

## Introduction ##

The file contains one definition per line, which specify the required fields for creating one or more points. One of the fields in the definition is the 'sources' field which is used to simplify the definition of essentially identical points for more than one source/end-point.

Each point definition line must contain thirteen fields. Some fields may have multiple parts, in which case the parts are contained by curly brackets and the parts are delimited by commas. Below is an example of a sources field which defines multiple sources:

```
#Curly brackets and commas can be used for compound fields
{server1, server2, testserver}
```

Several of the fields are used to tell MoniCA which classes should be created to populate the relevant fields of the PointDescription object and may also specify a number of arguments for the created object. For these fields the format is to specify the class name followed by a hyphen (`-`) and then the arguments which each argument being surrounded by double quotes (`"`). Below is an example:

```
Mean-"60""true"
```

Normally MoniCA will search within the appropriate bundled package for the specified class but you may also implement and use your own subclasses for many of these fields by ensuring the class files are in your CLASSPATH and then specifying the fully qualified class name:

```
my.test.package.MySpecialMeanFilter-"60""true"
```

## Name ##
The name field specifies where in the hierarchy of points the point being defined belongs. Is uses a `.` notation to delimit the different hierarchical components. Eg `environment.weather.Temperature`

## Description ##
The description contains a brief free-form description of the point, delimited by `"` double quotes. Eg `"Outside Ambient Temperature"`

## Short Description ##
The short description is intended to be used by display-limited clients and contains a very short description of the point which must be no more than ten characters long. This field is delimited by `"` double quotes. Eg `"Temperature"`

## Units ##
The units field contains the engineering units/dimensions of the quantity being measured. It is only used for display purposes. The field is delimited by `"` double quotes, eg `"kHz"`. The quotes may contain an empty string for dimensionless quantities (`""`).

## Sources ##
This specifies which 'sources' the point definition should be used to create points for. For instance just say you had six radio telescope antennas which had identical control/monitor hardware, you do not need to define each point six times on six separate lines, instead you can define the point once and specify the six antenna names as the sources for that point. MoniCA would then create six identical points, one for each antenna. Almost all transaction classes will substitute the source name whereever the macro `$1` is specified in the channel string, which is the mechanism you can use to ensure the multiple points actually interact with different end-points.

If the point is defined for just a single source then you only need to specify the name of the source for this field, or for a point definition with multiple sources you use the compound semantics already described.

## Enabled State ##
This may be set to either `T` or `F` to specify whether a point should be actively collected/processed or not respectively. Normally this would be `T` however you may have a point that is no longer supported in your system but for which you would still like to make archival data available, in which case you could maintain the point definition but set it to disabled.  Some experience indicates that the associated `System` entry needs to be removed for this to take effect.

## Input Transactions ##
This field specifies which input transactions should be created for the point. Input transactions contain, for instance, hardware addressing information which can be used by an ExternalSystem in order to specify which specific input on the external device should be polled to collect input data for the point. In principle this field may contain a compound expression specifying multiple transactions but in practice this rarely makes sense as the input is normally collected from a single place. This field may be set to `-` if the point doesn't define any input transactions.

## Output Transactions ##
All semantics for this is identical to the input transactions specification, the only difference is that output transaction fields are used by the ExternalSystem to write control data rather than to collect monitor data. Compound expressions where the output is written to multiple outputs are more commonly used than for input transactions. This field may be set to `-` if the point doesn't define any output transactions.

## Translations ##
The translations tell MoniCA how to convert the raw input data into real world units, string mappings, etc.. Translations are commonly chained together using a compound statement however if you only need to define a single translation then the compound syntax is not required.

## Alarm Criteria ##
Alarm criteria check the value of the point and determine whether the current value is indicative of an alarm condition or not. Multiple alarm criteria may optionally be defined using the compound syntax.

## Archive Policies ##
Archive policies tell MoniCA when it is appropriate to archive the value of a point. There are various kinds of policy subclasses such as archive every Nth sample or archive when the point is in an alarm state. Compound syntax can be used to specify more than one alarm policy for the point if required. In this case the value will be archived when any of the policies flag that archiving is required.

## Update Interval ##
This field tells MoniCA how often the point should be updated. It tells an ExternalSystem subclass how often to poll for raw input data for the point, it is also used for instance by the TranslationCalculationTimed to determine how freqently to read the input values and peform the calculation. On the client side this field is used to schedule when to poll the server for updated values for the point. The value is specified in integer microseconds and may be set to `-` for points which update aperiodically (ie points where the data is somehow pushed into MoniCA by an external agent rather than being polled).

## Archive Longevity ##
This defines, in integer days, how long data should be stored in the archive. A process will periodically purge old data from the archive if this field is defined. It may be set to `-` to maintain archival data forever.

## Notifications ##
[Notification](AlarmFunctionality.md) classes can be used for things like sending emails or SMS alert messages when appropriate criteria are met. Multiple notification objects may optionally be defined using the compound syntax.

## Priority ##
All points can have alarm criteria but in some systems you do not actually want to be notified when every point has an off-nominal value. By default points have a priority of -1 (no priority) but you can set this priority to 0 (information), 1 (minor), 2 (major) or 3 (severe) to assign a priority to a point. This will make the alarm status and management of the point active via the alarm manager framework. More information is available [here](AlarmFunctionality.md).

## Guidance ##
Point which have a priority associated with them may define guidance text which can be presented to the user to help them in an alarm situation (eg "Call the on-call staff."). This guidance text can contain macros which will be [substituted](Substitutions.md) for the current value, alarm status, etc..

# Examples #
Below is a point which uses the pingcheck ExternalSystem to test the connectivity to a host called ozforecast.com.au every five seconds. The name of the source is arbitrarily set as OzF. The StringMatch alarm criteria tells the point to enter an alarm state whenever the value is not equal to true. The value will be archived whenever it changes state or when it is in an alarm state. The point has no translations or output transactions defined and data never expires from the archive.
```
network.connectivity  "Network Connectivity"  "Connectivity"  ""  OzF  T Strings-"pingcheck""ozforecast.com.au"  -  -  StringMatch-"true""true"  {Change-, Alarm-}  5000000  -
```
