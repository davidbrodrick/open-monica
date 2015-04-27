# Introduction #

MoniCA requires configuration for options such as where to save archived data, how to authenticate users, etc.. Configuration resources also provide the definitions for what systems to monitor and which quantities to monitor on those systems.

This page is intended to give a very brief introduction to these.

# File Locations #

## Bundled Resources ##
Most configuration files live in the `config/` subdirectory of the open-monica project directory. These get built into the jar file when you build MoniCA (therefore you need to rebuild the jar file if you change the files).

An enumeration of these files includes the following:

  * **monitor-config.txt** Contains many of the run-time definitions for the server, such as archive directory location, number of threads to use, [RADIUS server](RADIUSAuth.md) details, etc.. This file is loaded as a resource from the jar file but the `MoniCA.ConfFile` system property can also be defined to specify an alternate location for the configuration data.
  * **monitor-points.txt** The list of point definitions to be created by the server. This file is described in detail [here](MonitorPointsFileFormat.md).
  * **monitor-sources.txt** This defines which devices we should connect to for monitoring/control. Some of the ExternalSystem classes are documented [here](ExternalSystems.md), most of the others are documented with Javadoc tags in the [source code](http://code.google.com/p/open-monica/source/browse/#svn%2Ftrunk%2Fsrc%2Fatnf%2Fatoms%2Fmon%2Fexternalsystem) files.
  * **monitor-setups.txt** This contains the SavedSetup definitions which are read by the server and then provided to Java clients when they connect.
  * **log4j.properties** Log4J logging configuration. More info [here](http://logging.apache.org/log4j/2.x/manual/configuration.html).
  * **monitor-servers.txt** This is used by the client to define the selection of possible servers to present as options to the user when the client first starts up.
  * **monitor-preloads.txt** A largely static file used by the client, which only needs updating if you develop new classes for the Java GUI client.

# Conf.d Configuration Directories #

In addition to being able to use configuration resources bundled into the jar file, MoniCA is also able to read configuration files from a directory as specified in the monitor-config.txt file, or by setting the `MoniCA.ConfDir` system property:

```
##############################
# LOCATION OF EXTRA CONFIGURATION FILES:
# This directory can include the following sub-directories:
# points.d/ - Point definitions, ala monitor-points.txt
# systems.d/ - External systems, ala monitor-sources.txt
# setups.d/ - Saved display setups, ala monitor-setups.txt
ConfDir /home/user/my-monica-conf/
```

As suggested above, this configuration directory has up to three subdirectories. Any configuration files must reside in the appropriate subdirectory (ie. no files will be read from the ConfDir directory itself).

Each subdirectory is treated as a "conf.d" configuration directory where the files are parsed in lexicographic order, so `points.d/01-mypoints.conf` would be loaded before `points.d/99-mypoints.txt`. Files do not need to start with a number, nor end in any particular suffix - all files in the directory will be loaded.

The configuration directory can be used together with the configuration resources bundled in the jar file. In this case the bundled resources are loaded first.

# Template Files #

Templates for the above files can be found in the default-files/ subdirectory of the open-monica project directory. You can copy these files into your config/ directory to get started. An example can be found on the [Installation](Installation.md) page.