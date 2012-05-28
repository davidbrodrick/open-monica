// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.archiver;

import org.apache.log4j.Logger;
import java.io.*;
import java.util.*;
import java.math.*;
import java.util.zip.*;
import java.util.concurrent.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.util.*;
import atnf.atoms.util.*;
import atnf.atoms.time.*;

/**
 * Archiver which stores data into ASCII text files, which are then compressed.
 * 
 * <P>
 * Each record is appened to a text file which lives in a directory hierarchy which corresponds to the heirarchical name of the
 * monitor point. When files either get too large or too old they are compressed and any subsequent data will be stored in a new
 * file.
 * 
 * <P>
 * The compression is transparent to the user as the archiver will decompress files when a archive request is made.
 * 
 * @author David Brodrick
 * @author Le Cuong Ngyuen
 */
public class PointArchiverASCII extends PointArchiver {
  /** OS-dependant file separation character. */
  private static final String FSEP = System.getProperty("file.separator");

  /** Base directory for the data archive. */
  private static String theirArchiveDir;

  /** Directory for writing temporary files. */
  private static File theirTempDir;

  /** Maximum size for an archive file. */
  private static int theirMaxFileSize;

  /** Max time-span for an archive data file. */
  private static int theirMaxFileAge;

  /** Number of threads in the archive thread pool. */
  private static int theirNumThreads;

  /** Logger. */
  private static Logger theirLogger = Logger.getLogger(PointArchiverASCII.class.getName());

  /** Thread pool for archiving. */
  private ThreadPoolExecutor itsThreadPool;

  /** Cache of current file names to write to for each point. */
  private HashMap<String, String> itsFileNameCache = new HashMap<String, String>(1000, 1000);

  static {
    theirArchiveDir = System.getProperty("MoniCA.ArchiveDir");
    if (theirArchiveDir == null) {
      theirArchiveDir = MonitorConfig.getProperty("ArchiveDir");
      if (theirArchiveDir == null) {
        // Support legacy option
        theirArchiveDir = MonitorConfig.getProperty("LogDir");
        if (theirArchiveDir == null) {
          theirLogger.error("Configuration option \"ArchiveDir\" was not defined");
        }
      }
    } else {
      theirLogger.info("ArchiveDir overridden by system property");
    }

    String temp = MonitorConfig.getProperty("ArchiveTempDir");
    if (temp == null) {
      temp = MonitorConfig.getProperty("TempDir");
    }
    if (temp != null) {
      theirTempDir = new File(temp);
    } else {
      theirLogger.error("Configuration option \"ArchiveTempDir\" was not defined");
    }

    temp = MonitorConfig.getProperty("ArchiveMaxSize");
    if (temp == null) {
      temp = MonitorConfig.getProperty("ArchiveSize");
    }
    if (temp == null) {
      theirLogger.error("Configuration option \"ArchiveMaxSize\" was not defined");
    } else {
      try {
        theirMaxFileSize = Integer.parseInt(temp);
      } catch (Exception e) {
        theirLogger.error("Error parsing configuration option \"ArchiveMaxSize\"");
      }
    }

    temp = MonitorConfig.getProperty("ArchiveMaxAge");
    if (temp == null) {
      theirLogger.error("Configuration option \"ArchiveMaxAge\" was not defined");
    } else {
      try {
        theirMaxFileAge = 1000 * Integer.parseInt(temp);
      } catch (Exception e) {
        theirLogger.error("Error parsing configuration option \"ArchiveMaxAge\"");
      }
    }

    temp = MonitorConfig.getProperty("ArchiveNumThreads");
    if (temp == null) {
      theirLogger.error("Configuration option \"ArchiveNumThreads\" was not defined");
      theirNumThreads = 1;
    } else {
      try {
        theirNumThreads = Integer.parseInt(temp);
      } catch (Exception e) {
        theirLogger.error("Error parsing configuration option \"ArchiveNumThreads\"");
        theirNumThreads = 1;
      }
    }
  }

  class ASCIIArchiverWorker implements Runnable {
    /** The point we will archive. */
    private PointDescription itsPoint;

    /** Vector of data to be archived. */
    private Vector<PointData> itsData;

    ASCIIArchiverWorker(PointDescription point, Vector<PointData> data) {
      itsPoint = point;
      itsData = data;
    }

    public void run() {
      try {
        String fileName;
        File file;
        Date filedate = null;
        // Find the file which needs to be written to
        String path = getDir(itsPoint);
        if (itsFileNameCache.containsKey(itsPoint.getFullName())) {
          // We have already archived this point
          fileName = itsFileNameCache.get(itsPoint.getFullName());
          file = new File(fileName);
          if (!file.exists()) {
            itsLogger.debug("Active archive file disappeared: " + fileName);
            // Check if whole directory vanished
            File myDir = new File(path);
            if (!myDir.isDirectory()) {
              myDir.mkdirs();
            }
            filedate = new Date();
            fileName = path + FSEP + getDateTime(filedate);
            file = new File(fileName);
            file.createNewFile();
          }
        } else {
          // Need to find it. Find the right directory and get a listing of the files
          fileName = path + FSEP + getDateTimeNow();
          File myDir = new File(path);
          if (!myDir.isDirectory()) {
            myDir.mkdirs();
          }
          String[] dirFiles = myDir.list();

          // If there are no existing files, create a new empty one
          if (dirFiles == null || dirFiles.length < 1) {
            (new File(fileName)).createNewFile();
            dirFiles = myDir.list();
          }

          // Find the most recent extant, non-archived file
          int latest = -1;
          Date lastdate = null;
          for (int i = 0; i < dirFiles.length; i++) {
            Date thisdate = null;
            String thisfile = dirFiles[i];
            if (thisfile.startsWith(".")) {
              // It's a hidden file so ignore it
              continue;
            }
            if (isCompressed(thisfile)) {
              // Cut the ".zip" off the end of the string
              thisfile = thisfile.substring(0, thisfile.length() - 4);
            }
            thisdate = getDateTime(thisfile);
            if (thisdate == null) {
              itsLogger.debug("PointArchiverASCII:saveNow: Bad file name " + dirFiles[i] + " in directory " + path);
              continue;
            }
            if (latest == -1 || thisdate.after(lastdate)) {
              latest = i;
              lastdate = thisdate;
            }
          }

          if (latest != -1 && isCompressed(dirFiles[latest])) {
            // Latest file is compressed so need a new file
            filedate = new Date();
            fileName = path + FSEP + getDateTime(filedate);
            file = new File(fileName);
            file.createNewFile();
            itsLogger.debug("Created file: " + fileName);
          } else {
            // Found what we were looking for
            fileName = path + FSEP + dirFiles[latest];
            file = new File(fileName);
            filedate = lastdate;
          }
        }

        // Get the timestamp corresponding to the file name
        if (filedate == null) {
          String[] pathelems = fileName.split(FSEP);
          filedate = getDateTime(pathelems[pathelems.length - 1]);
        }

        // Enforce the age and size limits that apply to active files.
        if (filedate.before(new Date(System.currentTimeMillis() - theirMaxFileAge)) || file.length() > theirMaxFileSize) {
          // Compress old file since it's now an archival file
          compress(fileName);
          // Delete uncompressed version of the file - if we can
          try {
            file.delete();
          } catch (Exception e) {
            // Unable to delete it. Remove .zip so we don't duplicate this data
            itsLogger.warn("In saveNow: Can't delete uncompressed file " + fileName + ": " + e);
            (new File(fileName + ".zip")).delete();
          }

          // Create a new file, now, and archive to it instead.
          fileName = path + FSEP + getDateTimeNow();
          file = new File(fileName);
          file.createNewFile();
        }

        itsFileNameCache.put(itsPoint.getFullName(), fileName);

        // Finally we've identified the right file. Write out each data record.
        FileWriter f = new FileWriter(fileName, true);
        PrintWriter outfile = new PrintWriter(new BufferedWriter(f));
        synchronized (itsData) {
          for (int i = 0; i < itsData.size(); i++) {
            try {
              PointData pd = (PointData) itsData.elementAt(i);
              outfile.println(getStringForPD(pd));
            } catch (Exception e) {
              itsLogger.warn("In saveNow: " + e.getMessage() + " (for " + ((PointData) itsData.elementAt(i)).getName() + ")");
            }
          }
          // We've now archived the data that was in the buffer
          itsData.clear();
        }
        // Flush buffers and close files
        outfile.flush();
        f.flush();
        outfile.close();
        f.close();
        // Files flushed, can now flag that archive is no longer in progress
        itsBeingArchived.remove(itsPoint.getFullName());
      } catch (Exception e) {
        itsLogger.error("While archiving: " + itsPoint.getFullName() + ": " + e);
      }

    }
  }

  /** Constructor. */
  public PointArchiverASCII() {
    super();

    itsThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(theirNumThreads);
  }

  /**
   * Purge all data for the given point that is older than the specified age in days.
   * 
   * @param point
   *          The point whos data we wish to purge.
   */
  protected void purgeOldData(PointDescription point) {
    if (point.getArchiveLongevity() < 0)
      return;

    // Get the directory where this point is archived
    String dir = getDir(point);
    // Calculate timestamps for period to purge over
    AbsTime start = AbsTime.factory(0);
    AbsTime end = AbsTime.factory((new AbsTime()).getValue() - 86400000000l * point.getArchiveLongevity());
    // Get list of all files to be purged
    Vector<String> files = getFiles(dir, start, end);
    if (files.size() > 1) {
      // Delete all files except most current (as it may contain still-valid data)
      for (int i = 0; i < files.size() - 1; i++) {
        try {
          (new File(files.get(i))).delete();
        } catch (Exception e) {
          // There was a problem deleting the file. Should we log a message about it?
        }
      }
    }
  }

  /**
   * Method to do the actual archiving.
   * 
   * @param pm
   *          The point whos data we wish to archive.
   * @param data
   *          Vector of data to be archived.
   */
  protected void saveNow(PointDescription pm, Vector<PointData> data) {
    final RelTime sleeptime = RelTime.factory(1000);
    while (itsThreadPool.getQueue().size() > (itsThreadPool.getCorePoolSize() + 1)) {
      // itsLogger.debug(itsThreadPool.getQueue().size() + "\t" + (itsThreadPool.getCorePoolSize()+1));
      try {
        sleeptime.sleep();
      } catch (Exception e) {
      }
    }
    itsThreadPool.execute(new ASCIIArchiverWorker(pm, data));
  }

  /**
   * Method to extract data from the archive.
   * 
   * @param pm
   *          Point to extract data for.
   * @param start
   *          Earliest time in the range of interest.
   * @param end
   *          Most recent time in the range of interest.
   * @return Vector containing all data for the point over the time range.
   */
  public Vector<PointData> extract(PointDescription pm, AbsTime start, AbsTime end) {
    // AbsTime starttime = new AbsTime();
    Vector<PointData> res = new Vector<PointData>(1000, 1000);
    // Get the archive directory for the given point
    String dir = getDir(pm);

    // Get all the archive files relevant to the period of interest
    Vector files = getFiles(dir, start, end);

    // Try to load data from each of the files
    for (int j = 0; j < files.size(); j++) {
      loadFile(res, pm, dir + FSEP + files.get(j), start, end, true);
      if (res.size() >= MAXNUMRECORDS) {
        // Max size limit to prevent server bogging down
        // MonitorMap.logger.warning("MoniCA Server: Truncating archive request
        // to " +
        // res.size() + " records");
        break;
      }
    }
    // AbsTime endtime = new AbsTime();
    // RelTime diff = Time.diff(endtime, starttime);
    // System.err.println("Request for " + pm.getName() + " took "
    // + (diff.getValue()/1000) + " ms for " + res.size() + " records");
    return res;
  }

  /**
   * Return the last update which precedes the specified time. We interpret 'precedes' to mean data_time<=req_time.
   * 
   * @param pm
   *          Point to extract data for.
   * @param ts
   *          Find data preceding this timestamp.
   * @return PointData for preceding update or null if none found.
   */
  public PointData getPreceding(PointDescription pm, AbsTime ts) {
    // Get the archive directory for the given point
    String dir = getDir(pm);
    // Get any the archive files relevant to the period of interest
    Vector<String> files = getFiles(dir, ts, ts);
    if (files == null || files.size() == 0) {
      return null;
    }
    // Ensure the preceding file is also included
    String preceding = getPrecedingFile(dir, (String) files.get(0));
    if (preceding != null) {
      files.insertElementAt(preceding, 0);
    }
    // Try to load data from each of the files
    Vector<PointData> tempbuf = new Vector<PointData>(1000, 1000);
    for (int i = 0; i < files.size(); i++) {
      loadFile(tempbuf, pm, dir + FSEP + files.get(i), null, null, false);
    }

    // Try to find the update which precedes the argument timestamp
    PointData res = null;
    for (int i = 1; i < tempbuf.size(); i++) {
      PointData p1 = (PointData) (tempbuf.get(i - 1));
      PointData p2 = (PointData) (tempbuf.get(i));
      if (p1.getTimestamp().isBeforeOrEquals(ts) && p2.getTimestamp().isAfter(ts)) {
        // This is the datum we were searching for
        res = p1;
        break;
      }
    }
    if (res == null && tempbuf.size() > 0) {
      // Request must be for most recent data in the archive
      res = tempbuf.get(tempbuf.size() - 1);
    }
    return res;
  }

  /**
   * Return the first update which follows the specified time. We interpret 'follows' to mean data_time>=req_time.
   * 
   * @param pm
   *          Point to extract data for.
   * @param ts
   *          Find data following this timestamp.
   * @return PointData for following update or null if none found.
   */
  public PointData getFollowing(PointDescription pm, AbsTime ts) {
    // Get the archive directory for the given point
    String dir = getDir(pm);
    // Get any the archive files relevant to the period of interest
    Vector<String> files = getFiles(dir, ts, ts);
    if (files == null || files.size() == 0) {
      return null;
    }
    // Ensure the following file is also included
    String following = getFollowingFile(dir, (String) files.get(files.size() - 1));
    if (following != null) {
      files.add(following);
    }

    // Try to load data from each of the files
    Vector<PointData> tempbuf = new Vector<PointData>(1000, 1000);
    for (int i = 0; i < files.size(); i++) {
      loadFile(tempbuf, pm, dir + FSEP + files.get(i), null, null, false);
    }

    // Try to find the update which follows the argument timestamp
    PointData res = null;
    for (int i = 1; i < tempbuf.size(); i++) {
      PointData p1 = (PointData) (tempbuf.get(i - 1));
      PointData p2 = (PointData) (tempbuf.get(i));
      if (p1.getTimestamp().isBefore(ts) && p2.getTimestamp().isAfterOrEquals(ts)) {
        // This is the datum we were searching for
        res = p2;
        break;
      }
    }
    return res;
  }

  /** Convert the PointData to a line of ASCII text. */
  protected String getStringForPD(PointData pd) {
    Object data = pd.getData();
    String res = pd.getTimestamp().toString(AbsTime.Format.HEX_BAT) + "\t";
    res += getStringForObject(data);
    if (pd.getAlarm()) {
      res += "A";
    }
    return res;
  }

  /** Recover the PointData from a line of ASCII text. */
  protected PointData getPDForString(PointDescription pm, String data) {
    PointData res = null;
    StringTokenizer st = new StringTokenizer(data, "\t");
    if (st.countTokens() < 3) {
      return null;
    }

    // Try to parse the timestamp
    AbsTime ts = null;
    try {
      ts = AbsTime.factory(st.nextToken());
    } catch (Exception e) {
      System.err.println(e.getMessage());
      return null;
    }

    // Parse the main data field
    String type = st.nextToken();
    String dstr = st.nextToken();
    Object d1 = null;
    try {
      d1 = getObjectForString(type, dstr);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      return null;
    }

    // Check if the record has an active alarm condition saved
    boolean alarm = false;
    if (st.countTokens() == 1) {
      if (st.nextToken().equals("A")) {
        alarm = true;
      }
    }
    res = new PointData(pm.getFullName(), ts, d1, alarm);
    return res;
  }

  /**
   * Get a string representation of the Object. The string includes a type specifier as well as an ASCII representation of the data.
   * These fields are separated by tabs. The <i>getObjectForString</i> method is able to decode this representation and recover the
   * original Object.
   * <P>
   * <i>null</i> objects are properly handled.
   * 
   * @param data
   *          The Object to encode into ASCII text.
   * @return An ASCII String representation of the data.
   */
  protected String getStringForObject(Object data) throws IllegalArgumentException {
    String res = null;
    if (data == null) {
      res = "null\tnull\t";
    } else if (data instanceof Double) {
      res = "dbl\t" + ((Double) data).doubleValue() + "\t";
    } else if (data instanceof Float) {
      res = "flt\t" + ((Float) data).floatValue() + "\t";
    } else if (data instanceof HourAngle) {
      res = "hr\t" + ((Angle) data).getValue() + "\t";
    } else if (data instanceof Angle) {
      res = "ang\t" + ((Angle) data).getValue() + "\t";
    } else if (data instanceof Integer) {
      res = "int\t" + ((Integer) data).intValue() + "\t";
    } else if (data instanceof String) {
      String d = (String) data;
      if (d.indexOf("\t") != -1) {
        // Probably best to escape the tab
        System.err.println("PointArchiverASCII: WARNING: String " + "contained a tab!");
        d = d.replace('\t', ' '); // Substitude for a space
      }
      res = "str\t" + d + "\t";
    } else if (data instanceof Boolean) {
      res = "bool\t" + ((Boolean) data).booleanValue() + "\t";
    } else if (data instanceof Short) {
      res = "short\t" + ((Short) data).shortValue() + "\t";
    } else if (data instanceof Long) {
      res = "long\t" + ((Long) data).longValue() + "\t";
    } else if (data instanceof AbsTime) {
      res = "abst\t" + ((AbsTime) data).toString(AbsTime.Format.HEX_BAT);
    } else if (data instanceof RelTime) {
      res = "relt\t" + ((RelTime) data).toString(RelTime.Format.DECIMAL_BAT);
    } else if (data instanceof BigInteger) {
      res = "big\t" + ((BigInteger) data).toString() + "\t";
    } else {
      // Unhandled data type
      throw new IllegalArgumentException("PointarchiverASCII: Unknown Type \"" + data.getClass() + "\"");
    }
    return res;
  }

  /**
   * Use the ASCII <i>type</i> and <i>data</i> to reconstruct the data Object. This method essentially performs the opposite
   * procedure to that implemented by <i>getStringForObject</i>.
   * 
   * @param type
   *          Short string representing the class of the data.
   * @param data
   *          The actual data in ASCII text form.
   * @return The reconstructed object.
   */
  protected Object getObjectForString(String type, String data) {
    Object res = null;
    if (type.equals("dbl")) {
      res = new Double(data);
    } else if (type.equals("flt")) {
      res = new Float(data);
    } else if (type.equals("ang")) {
      res = Angle.factory(data);
    } else if (type.equals("hr")) {
      res = new HourAngle(Double.parseDouble(data));
    } else if (type.equals("int")) {
      res = new Integer(data);
    } else if (type.equals("str")) {
      res = data;
    } else if (type.equals("bool")) {
      res = new Boolean(data);
    } else if (type.equals("short")) {
      res = new Short(data);
    } else if (type.equals("long")) {
      res = new Long(data);
    } else if (type.equals("abst")) {
      long foo = Long.parseLong(data, 16); // Hex
      res = AbsTime.factory(foo);
    } else if (type.equals("relt")) {
      long foo = Long.parseLong(data); // Decimal
      res = RelTime.factory(foo);
    } else if (type.equals("big")) {
      res = new BigInteger(data);
    } else if (type.equals("null")) {
      res = null;
    } else {
      System.err.println("PointArchiverASCII: Parse error at \"" + type + "\"");
      res = null;
    }
    return res;
  }

  /**
   * Get the names of archive files relevant to the given time range for the point.
   * 
   * @param dir
   *          Archive directory to search.
   * @param start
   *          Earliest time in the range of interest.
   * @param end
   *          Most recent time in the range of interest.
   * @return Vector containing all filenames of relevance.
   */
  private Vector<String> getFiles(String dir, AbsTime start, AbsTime end) {
    Vector<String> res = new Vector<String>();
    TreeMap<Long, String> map = new TreeMap<Long, String>();

    // Get listing of all files in the archive dir for the given point
    // /Should use filename filter with this .list() call
    String[] files = (new File(dir)).list();
    // Can't do anything if there were no files!
    if (files == null || files.length == 0) {
      return res;
    }

    // Map the filenames to dates, and sort
    for (int i = 0; i < files.length; i++) {
      Date date = null;
      if (isCompressed(files[i])) {
        // Remove the ".zip" from the file name so we can parse it
        date = getDateTime(files[i].substring(0, files[i].length() - 4));
      } else {
        date = getDateTime(files[i]);
      }
      if (date == null) {
        System.err.println("PointArchiverASCII:getFiles: Bad File Name " + files[i] + " in directory " + dir);
        continue;
      }
      AbsTime atime = AbsTime.factory(date);
      map.put(new Long(atime.getValue()), files[i]);
    }

    // Get an Iterator so we can look through the files
    Iterator it = map.keySet().iterator();

    boolean hit = false; // Have we found a file within the range yet
    Object prevkey = null; // Keep reference to previous file's epoch

    while (it.hasNext()) {
      Object key = it.next();
      AbsTime ftime = AbsTime.factory(((Long) key).longValue());

      if (ftime.isBefore(start)) {
        // Haven't caught up to the start time yet
        prevkey = key;
      } else if (ftime.isAfter(end)) {
        // This file not of interest, but prev might be
        if (!hit) {
          // No files completely within the range
          // but the previous file may contain useful data
          if (prevkey != null) {
            hit = true;
            res.add(map.get(prevkey)); // (if there was one)
          }
        }
        // No point looking further into the future
        break;

      } else {
        // We need this file it is within the range
        if (!hit) {
          // This file's start time is in the range,
          // so the previous file may also contain useful data
          hit = true;
          if (prevkey != null) {
            res.add(map.get(prevkey)); // (if there was one)
          }
        }
        res.add(map.get(key));
      }
    }
    if (!hit) {
      // No files completely within the range
      // but most recent file may contain useful data
      if (prevkey != null) {
        res.add(map.get(prevkey)); // (if there was one)
      }
    }

    return res;
  }

  /**
   * Return the archive file which chronologically follows the argument file.
   * 
   * @param dir
   *          Directory which relates to this monitor point.
   * @param fname
   *          The argument file.
   * @return Next chronological file name, or null if none exist.
   */
  private String getFollowingFile(String dir, String fname) {
    // Get timestamp corresponding to argument file
    AbsTime argdate = null;
    if (isCompressed(fname)) {
      // Remove the ".zip" from the file name so we can parse it
      argdate = AbsTime.factory(getDateTime(fname.substring(0, fname.length() - 4)));
    } else {
      argdate = AbsTime.factory(getDateTime(fname));
    }

    // Get listing of all files in the archive dir for the given point
    String[] files = (new File(dir)).list();
    if (files == null || files.length == 0) {
      return null;
    }

    // Map the filenames to dates, and find target file
    RelTime afterdiff = null;
    String aftername = null;
    for (int i = 0; i < files.length; i++) {
      Date date = null;
      if (isCompressed(files[i])) {
        // Remove the ".zip" from the file name so we can parse it
        date = getDateTime(files[i].substring(0, files[i].length() - 4));
      } else {
        date = getDateTime(files[i]);
      }
      if (date == null) {
        System.err.println("PointArchiverASCII:getFollowingFile: Bad File Name " + files[i] + " in directory " + dir);
        continue;
      }
      AbsTime thisdate = AbsTime.factory(date);
      if (thisdate.isAfter(argdate)) {
        RelTime thisdiff = Time.diff(thisdate, argdate);
        if (afterdiff == null || thisdiff.getValue() < afterdiff.getValue()) {
          afterdiff = thisdiff;
          aftername = files[i];
        }
      }
    }
    return aftername;
  }

  /**
   * Return the archive file which chronologically preceeds the argument file.
   * 
   * @param dir
   *          Directory which relates to this monitor point.
   * @param fname
   *          The argument file.
   * @return Previous chronological file name, or null if none exist.
   */
  private String getPrecedingFile(String dir, String fname) {
    // Get timestamp corresponding to argument file
    AbsTime argdate = null;
    if (isCompressed(fname)) {
      // Remove the ".zip" from the file name so we can parse it
      argdate = AbsTime.factory(getDateTime(fname.substring(0, fname.length() - 4)));
    } else {
      argdate = AbsTime.factory(getDateTime(fname));
    }

    // Get listing of all files in the archive dir for the given point
    String[] files = (new File(dir)).list();
    if (files == null || files.length == 0) {
      return null;
    }

    // Map the filenames to dates, and find target file
    RelTime beforediff = null;
    String beforename = null;
    for (int i = 0; i < files.length; i++) {
      Date date = null;
      if (isCompressed(files[i])) {
        // Remove the ".zip" from the file name so we can parse it
        date = getDateTime(files[i].substring(0, files[i].length() - 4));
      } else {
        date = getDateTime(files[i]);
      }
      if (date == null) {
        System.err.println("PointArchiverASCII:getPrecedingFile: Bad File Name " + files[i] + " in directory " + dir);
        continue;
      }
      AbsTime thisdate = AbsTime.factory(date);
      if (thisdate.isBefore(argdate)) {
        RelTime thisdiff = Time.diff(argdate, thisdate);
        if (beforediff == null || thisdiff.getValue() < beforediff.getValue()) {
          beforediff = thisdiff;
          beforename = files[i];
        }
      }
    }
    return beforename;
  }

  /**
   * Load data within the given time range from the file.
   * 
   * @param res
   *          Vector which holds the loaded data.
   * @param fname
   *          Full path to the file to load data from.
   * @param pm
   *          PointDescription we are reconstructing data for.
   * @param pname
   *          Name of the monitor point we are loading.
   * @param start
   *          The earliest time of interest, null to ignore.
   * @param end
   *          The most recent time of interest, null to ignore.
   * @param truncate
   *          Whether to truncate at the archive query limit.
   */
  private void loadFile(Vector<PointData> res, PointDescription pm, String fname, AbsTime start, AbsTime end, boolean truncate) {
    try {
      // If the file's compressed we need to decompress it first
      boolean hadtodecompress = false;
      if (isCompressed(fname)) {
        fname = decompress(fname);
        hadtodecompress = true;
      }
      int num = 0;
      BufferedReader reader = new BufferedReader(new FileReader(fname));

      while (reader.ready()) {
        String line = reader.readLine();
        // Read the next data record from the archive file
        PointData pd = getPDForString(pm, line);
        if (pd == null) {
          continue;
        }
        // Check if it's in the right time range
        AbsTime ts = pd.getTimestamp();
        if (start != null && ts.isBefore(start)) {
          continue; // Data's too early
        }
        if (res.size() >= MAXNUMRECORDS || (end != null && ts.isAfter(end))) {
          break; // No more useful data in this file
        }
        res.add(pd);

        num++;
        if (truncate && res.size() >= MAXNUMRECORDS) {
          break;
        }
      }
      reader.close();

      // System.err.println("PointArchiverASCII:loadFile: "
      // + "LOADED " + num + " FROM " + fname);

      if (hadtodecompress) {
        // We need to delete the temporary file now
        (new File(fname)).delete();
      }
    } catch (Exception e) {
      System.err.println("PointArchiverASCII:loadFile: " + fname + " " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Test if the specified filename corresponds to a compressed file. In practice this just means we check for a <i>.zip</i>
   * extension.
   * 
   * @param filename
   *          The file name to check.
   * @return <code>True</code> if the file is compressed, <code>False</code> otherwise.
   */
  private boolean isCompressed(String filename) {
    return filename.endsWith(".zip");
  }

  /**
   * Decompress the specified file and return the path to a temporary file. The temporary file should generally be deleted by the
   * caller once it is no longer required.
   * 
   * @param filename
   *          Full path to the file to decompress.
   * @return Name of temporary file containing the decompressed data.
   */
  private String decompress(String filename) {
    File f = null;
    InputStream compressed = null;
    PrintWriter uncompressed = null;
    String res = null;

    try {
      ZipFile zip = new ZipFile(filename);
      String foo = filename.substring(filename.lastIndexOf(FSEP) + 1);
      foo = foo.substring(0, foo.length() - 4);
      ZipEntry ze = zip.getEntry(foo);
      compressed = zip.getInputStream(ze);
      if (!theirTempDir.isDirectory()) {
        // Ensure directory exists
        theirTempDir.mkdirs();
      }
      f = File.createTempFile("foo", ".tmp", theirTempDir);
      uncompressed = new PrintWriter(new BufferedWriter(new FileWriter(f)));
      res = f.getPath();

      // Extract data and copy to plain text file
      final int blen = 4096;
      byte[] bbuf = new byte[blen];
      char[] cbuf = new char[blen];
      int len;
      while ((len = compressed.read(bbuf)) > 0) {
        for (int i = 0; i < len; i++) {
          cbuf[i] = (char) bbuf[i];
        }
        uncompressed.write(cbuf, 0, len);
      }

      compressed.close();
      uncompressed.close();
    } catch (Exception e) {
      // if (compressed!=null) compressed.close();
      // if (uncompressed!=null) uncompressed.close();
      e.printStackTrace();
    }

    // System.err.println("PointArchiverASCII:decompress: Extracted "
    // + filename + " to " + res);
    return res;
  }

  /**
   * Compress the specified file. The file location is not not changed but the file will be renamed with a <i>.zip</i> extension.
   * 
   * @param filename
   *          The name of the file to be compressed.
   */
  public void compress(String filename) {
    try {
      File fin = new File(filename);
      FileInputStream uncompressed = new FileInputStream(fin);
      File fout = new File(filename + ".zip");
      ZipOutputStream compressed = new ZipOutputStream(new FileOutputStream(fout));
      // Add the ZIP header info
      String shortname;
      int lastslash = filename.lastIndexOf(FSEP);
      if (lastslash != -1) {
        shortname = filename.substring(lastslash + 1);
      } else {
        shortname = filename;
      }
      compressed.putNextEntry(new ZipEntry(shortname));

      // Copy data to compressed file
      byte[] buf = new byte[4096];
      int len;
      while ((len = uncompressed.read(buf)) > 0) {
        compressed.write(buf, 0, len);
      }

      compressed.close();
      // System.err.println("PointArchiverASCII:compress: Compressed " +
      // filename);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Get the appropriate filename representation of the current time.
   * 
   * @return Filename representation of the current time.
   */
  public static String getDateTimeNow() {
    return getDateTime(new Date());
  }

  /**
   * Get a filename representation of the given epoch.
   * 
   * @param date
   *          Date to translate into a filename.
   * @return Filename corresponding to the given Date.
   */
  public static String getDateTime(Date date) {
    GregorianCalendar calendar = new GregorianCalendar();
    calendar.setTime(date);
    calendar.setTimeZone(SimpleTimeZone.getTimeZone("GMT"));
    StringBuffer buf = new StringBuffer("");
    // YYYYMMDD-HHMM format
    buf.append(calendar.get(Calendar.YEAR));
    if (calendar.get(Calendar.MONTH) < 9) {
      buf.append("0");
    }
    buf.append(calendar.get(Calendar.MONTH) + 1);
    if (calendar.get(Calendar.DAY_OF_MONTH) < 10) {
      buf.append("0");
    }
    buf.append(calendar.get(Calendar.DAY_OF_MONTH));
    buf.append("-");
    if (calendar.get(Calendar.HOUR_OF_DAY) < 10) {
      buf.append("0");
    }
    buf.append(calendar.get(Calendar.HOUR_OF_DAY));
    if (calendar.get(Calendar.MINUTE) < 10) {
      buf.append("0");
    }
    buf.append(calendar.get(Calendar.MINUTE));
    return buf.toString();
  }

  /**
   * Get the epoch represented by the given file name.
   * 
   * @param parseDate
   *          The filename to parse.
   * @return Date represented by the given filename.
   */
  public static Date getDateTime(String parseDate) {
    try {
      int i = 0;
      // YYYYMMDD-HHMM format
      int year = Integer.parseInt(parseDate.substring(i, i += 4));
      int month = Integer.parseInt(parseDate.substring(i, i += 2)) - 1;
      int day = Integer.parseInt(parseDate.substring(i, i += 2));
      i++;
      int hour = Integer.parseInt(parseDate.substring(i, i += 2));
      int minute = Integer.parseInt(parseDate.substring(i, i += 2));
      GregorianCalendar pope = new GregorianCalendar(year, month, day, hour, minute);
      pope.setTimeZone(SimpleTimeZone.getTimeZone("GMT"));
      return pope.getTime();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Get the save directory for the given point.
   * 
   * @param pm
   *          Point to get the archive directory for.
   * @return Name of appropriate archive directory.
   */
  public static String getDir(PointDescription pm) {
    String tempname = pm.getName();
    tempname = tempname.replace(".", FSEP);
    return theirArchiveDir + FSEP + tempname + FSEP + pm.getSource();
  }

  public static final void main(String args[]) {
    PointArchiverASCII paa = new PointArchiverASCII();
    paa.getPrecedingFile("/home/ozforeca/open-monica/archive/weather/in_temp/home/", "20080709-0954.zip");
    /*
     * if (args.length<1) { System.err.println("USAGE: Specify a file to be compressed"); System.exit(1); }
     * 
     * System.out.println("Will compress " + args[0]); paa.compress(args[0]); paa.decompress(args[0]+".zip");
     */
  }

}
