// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.archiver;

import java.io.*;
import java.util.*;
import java.math.*;
import java.util.zip.*;
import java.text.*;

import atnf.atoms.mon.*;
import atnf.atoms.mon.util.*;
import atnf.atoms.util.*;
import atnf.atoms.time.*;

/**
 * BAT_time\t  data_type\t  data\t  raw_type\t  raw_data
 * WITH compression!
 *
 * @author David Brodrick
 * @author Le Cuong Ngyuen
 * @version $Id: PointArchiverASCII.java,v 1.7 2007/06/05 01:41:36 bro764 Exp bro764 $
 **/
public class PointArchiverASCII
extends PointArchiver
{
  static private File theirTempDir = new File("/tmp/mon-temp/");

  /** Constructor. */
  public
  PointArchiverASCII(String foo)
  {
    super(foo);
    if (!theirTempDir.isDirectory()) theirTempDir.mkdirs();
  }


  /** Method to do the actual archiving.
   * @param pm The point whos data we wish to archive.
   * @param data Vector of data to be archived. */
  protected
  void
  saveNow(PointMonitor pm, Vector data)
  {
    try {
      //Find the right directory and get a listing of the files
      String compressFile = null;
      String path = getDir(pm);
      String fileName = path+FSEP+getDateTimeNow();
      File myDir = new File(path);
      if (!myDir.isDirectory()) myDir.mkdirs();
      ///Should probably use a filter with this .list() call
      String[] dirFiles = myDir.list();

      //If there are no exisiting files, create a new empty one
      if (dirFiles==null || dirFiles.length<1) {
        (new File(fileName)).createNewFile();
        dirFiles = myDir.list();
      }

      //Find the most recent file
      ///I wonder if this will become a significant processing burden
      ///when the number of files becomes large? If so, we could check
      ///if the directory listing is pre-sorted in some useful way.
      int latest = -1;
      Date lastdate = null;
      for (int i=0; i<dirFiles.length; i++) {
        Date thisdate = null;
        String thisfile = dirFiles[i];
        if (isCompressed(thisfile)) {
          //cut the ".zip" off the end of the string
          thisfile = thisfile.substring(0, thisfile.length()-4);
        }
        thisdate = getDateTime(thisfile);
        if (thisdate==null) {
          System.err.println("PointArchiverASCII:saveNow: Bad file name " +
                              dirFiles[i] + " in directory " + path);
          continue;
        }
        if (latest==-1 || thisdate.after(lastdate)) {
          latest = i;
          lastdate = thisdate;
        }
      }

      fileName = path+FSEP+dirFiles[latest];
      //Enforce the age and size limits that apply to active files.
      //Also check to ensure the file hasn't been manually compressed
      if (lastdate.before(new Date(System.currentTimeMillis() - MAXAGE)) ||
          (new File(fileName)).length()>MAXLENGTH ||
        isCompressed(fileName)) {
        //Compress old file since it's now an archival file
        compress(fileName);
        //Delete uncompressed version of the file - if we can
        try {
          (new File(fileName)).delete();
        } catch (Exception e) {
          //Unable to delete it. Remove .zip so we don't duplicate this data
          System.err.println("PointArchiverASCII:saveNow: Can't delete " +
                             fileName + ": " + e.getMessage());
          (new File(fileName+".zip")).delete();
        }
        
        ///Could also use this opportunity to compress any other old files
        ///which arent already compressed for whatever reason..

        //Create a new file, now, and archive to it instead.
        fileName = path+FSEP+getDateTimeNow();
        (new File(fileName)).createNewFile();
      }

      //Finally we've identified the right file. Write out each data record.
      FileWriter f = new FileWriter(fileName, true);
      PrintWriter outfile = new PrintWriter(new BufferedWriter(f));
      for (int i=0; i<data.size(); i++) {
        try {
          PointData pd = (PointData)data.elementAt(i);
          outfile.println(getStringForPD(pd));
        } catch (Exception e) {
          System.err.println("PointArchiverASCII:" + e.getMessage() + " (for " +
          ((PointData)data.elementAt(i)).getName() + ")");
        }
      }
      outfile.flush();
      f.flush();
      outfile.close();
      f.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  /** Method to extract data from the archive.
   * @param pm Point to extract data for.
   * @param start Earliest time in the range of interest.
   * @param end Most recent time in the range of interest.
   * @param undersample Undersampling factor.
   * @return Vector containing all data for the point over the time range. */
  public
  Vector
  extract(PointMonitor pm, AbsTime start, AbsTime end, int undersample)
  {
    AbsTime starttime = new AbsTime();
    Vector res = new Vector(1000,8000);
    //Get the archive directory for the given point
    String dir = getDir(pm);

    //Get all the archive files relevant to the period of interest
    Vector files = getFiles(dir, start, end);

    //Try to load data from each of the files
    for (int j=0; j<files.size(); j++) {
      loadFile(res, pm, dir+FSEP+files.get(j), start, end);
      if (res.size()>MAXNUMRECORDS) {
        //Max size limit to prevent server bogging down
        MonitorMap.logger.warning("MoniCA Server: Truncating archive request to " + res.size() + " records");
        break;
      }
    }
    AbsTime endtime = new AbsTime();
    RelTime diff = Time.diff(endtime, starttime);
    //System.err.println("Request for " + pm.getName() + " took "
    //                   + (diff.getValue()/1000) + " ms for "
    //                   + res.size() + " records");
    return res;
  }


  /** Convert the PointData to a line of ASCII text. */
  protected
  String
  getStringForPD(PointData pd)
  {
    Object data = pd.getData();
    Object raw = pd.getRaw();
    String res = pd.getTimestamp().toString(AbsTime.Format.HEX_BAT) + "\t";
    res += getStringForObject(data);
    //If the 'raw' data field is populated then write it out also
    if (data!=raw && raw!=null) res += getStringForObject(raw);
    return res;
  }


  /** Recover the PointData from a line of ASCII text. */
  protected
  PointData
  getPDForString(PointMonitor pm, String data)
  {
    PointData res = null;
    StringTokenizer st = new StringTokenizer(data, "\t");
    if (st.countTokens()<3) return null;

    //Try to parse the timestamp
    AbsTime ts = null;
    try {
      ts = AbsTime.factory(st.nextToken());
    } catch (Exception e) {
      System.err.println(e.getMessage());
      return null;
    }

    //Parse the main data field
    String type = st.nextToken();
    String dstr = st.nextToken();
    Object d1 = null;
    try {
      d1 = getObjectForString(type, dstr);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      return null;
    }
    //Parse the "raw" data field if this information was recorded
    Object d2 = null;
    if (st.countTokens()>1) {
      type = st.nextToken();
      dstr = st.nextToken();
      try {
        d2 = getObjectForString(type, dstr);
      } catch (Exception e) {
        System.err.println(e.getMessage());
        return null;
      }
      res = new PointData(pm.getName(), pm.getSource(), ts, d2, d1);
    } else {
      //No raw data field
      res = new PointData(pm.getName(), pm.getSource(), ts, d1);
    }
    return res;
  }


  /** Get a string representation of the Object. The string includes a type
   * specifier as well as an ASCII representation of the data. These fields
   * are separated by tabs. The <i>getObjectForString</i> method is able
   * to decode this representation and recover the original Object.
   * <P><i>null</i> objects are properly handled.
   * @param data The Object to encode into ASCII text. 
   * @return An ASCII String representation of the data. */
  protected
  String
  getStringForObject(Object data)
  throws IllegalArgumentException
  {
    String res = null;
    if (data==null) {
      res = "null\tnull\t";
    } else if (data instanceof Double) {
      res = "dbl\t" + ((Double)data).doubleValue() + "\t";
    } else if (data instanceof Float) {
      res = "flt\t" + ((Float)data).floatValue() + "\t";
    } else if (data instanceof HourAngle) {
      res = "hr\t" + ((Angle)data).getValue() + "\t";
    } else if (data instanceof Angle) {
      res = "ang\t" + ((Angle)data).getValue() + "\t";
    } else if (data instanceof Integer) {
      res = "int\t" + ((Integer)data).intValue() + "\t";
    } else if (data instanceof String) {
      String d = (String)data;
      if (d.indexOf("\t")!=-1) {
        //Probably best to escape the tab
        System.err.println("PointArchiverASCII: WARNING: String "
			    + "contained a tab!");
        d = d.replace('\t', ' '); //Substitude for a space
      }
      res = "str\t" + d + "\t";
    } else if (data instanceof Boolean) {
      res = "bool\t" + ((Boolean)data).booleanValue() + "\t";
    } else if (data instanceof Short) {
      res = "short\t" + ((Short)data).shortValue() + "\t";
    } else if (data instanceof Long) {
      res = "long\t" + ((Long)data).longValue() + "\t";
    } else if (data instanceof AbsTime) {
      res = "abst\t" + ((AbsTime)data).toString(AbsTime.Format.HEX_BAT);
    } else if (data instanceof RelTime) {
      res = "relt\t" + ((RelTime)data).toString(RelTime.Format.DECIMAL_BAT);
    } else if (data instanceof BigInteger) {
      res = "big\t" + ((BigInteger)data).toString() + "\t";
    } else {
      //Unhandled data type
      throw new IllegalArgumentException("PointarchiverASCII: Unknown Type \""
					 + data.getClass() + "\"");
    }
    return res;
  }


  /** Use the ASCII <i>type</i> and <i>data</i> to reconstruct the data
   * Object. This method essentially performs the opposite procedure to that
   * implemented by <i>getStringForObject</i>.
   * @param type Short string representing the class of the data.
   * @param data The actual data in ASCII text form.
   * @return The reconstructed object. */
  protected
  Object
  getObjectForString(String type, String data)
  {
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
      long foo = Long.parseLong(data,16); //Hex
      res = AbsTime.factory(foo);
    } else if (type.equals("relt")) {
      long foo = Long.parseLong(data); //Decimal
      res = RelTime.factory(foo);
    } else if (type.equals("big")) {
      res = new BigInteger(data);
    } else if (type.equals("null")) {
      res = null;
    } else {
      System.err.println("PointArchiverASCII: Parse error at \""
			 + type + "\"");
      res = null;
    }
    return res;
  }


  /** Get the names of archive files relevant to the given time range for
   * the point.
   * @param dir Archive directory to search.
   * @param start Earliest time in the range of interest.
   * @param end Most recent time in the range of interest.
   * @return Vector containing all filenames of relevance. */
  private
  Vector
  getFiles(String dir, AbsTime start, AbsTime end)
  {
    Vector res = new Vector();
    TreeMap map = new TreeMap();

    //Get listing of all files in the archive dir for the given point
    ///Should use filename filter with this .list() call
    String[] files = (new File(dir)).list();
    //Can't do anything if there were no files!
    if (files==null || files.length==0) return res;

    //Map the filenames to dates, and sort
    for (int i=0; i<files.length; i++) {
      Date date = null;
      if (isCompressed(files[i])) {
        //Remove the ".zip" from the file name so we can parse it
        date = getDateTime(files[i].substring(0,files[i].length()-4));
      } else {
        date = getDateTime(files[i]);
      }
      if (date==null) {
        System.err.println("PointArchiverASCII:getFiles: Bad File Name " +
                           files[i] + " in directory " + dir);
        continue;
      }
      AbsTime atime = AbsTime.factory(date);
      map.put(new Long(atime.getValue()), files[i]);
    }

    //Get an Iterator so we can look through the files
    Iterator it = map.keySet().iterator();

    boolean hit = false; //Have we found a file within the range yet
    Object prevkey = null; //Keep reference to previous file's epoch

    while (it.hasNext()) {
      Object key = it.next();
      AbsTime ftime = AbsTime.factory(((Long)key).longValue());

      if (ftime.isBefore(start)) {
        //Haven't caught up to the start time yet
        prevkey = key;
      } else if (ftime.isAfter(end)) {
        //This file not of interest, but prev might be
        if (!hit) {
          //No files completely within the range
          //but the previous file may contain useful data
          if (prevkey!=null) {
            hit = true;
            res.add(map.get(prevkey)); //(if there was one)
          }
        }
        //No point looking further into the future
        break;

      } else {
        //We need this file it is within the range
        if (!hit) {
          //This file's start time is in the range,
          //so the previous file may also contain useful data
          hit = true;
          if (prevkey!=null) {
            res.add(map.get(prevkey)); //(if there was one)
          }
        }
        res.add(map.get(key));
      }
    }
    if (!hit) {
      //No files completely within the range
      //but most recent file may contain useful data
      if (prevkey!=null) res.add(map.get(prevkey)); //(if there was one)
    }

    return res;
  }


  /** Load data within the given time range from the file.
   * @param res Vector which holds the loaded data.
   * @param fname Full path to the file to load data from.
   * @param pm PointMonitor we are reconstructing data for.
   * @param pname Name of the monitor point we are loading.
   * @param start The earliest time of interest.
   * @param end The most recent time of interest. */
  private
  void
  loadFile(Vector res, PointMonitor pm, String fname,
           AbsTime start, AbsTime end)
  {
    try {
      //If the file's compressed we need to decompress it first
      boolean hadtodecompress = false;
      if (isCompressed(fname)) {
        fname = decompress(fname);
        hadtodecompress = true;
      }
      int num = 0;
      BufferedReader reader = new BufferedReader(new FileReader(fname));

      while (reader.ready()) {
        String line=reader.readLine();
        //Read the next data record from the archive file
        PointData pd = getPDForString(pm, line);
        if (pd==null) continue;
        //Check if it's in the right time range
        AbsTime ts = pd.getTimestamp();
        if (ts.isBefore(start)) continue; //Data's too early
        if (ts.isAfter(end)) break; //No more useful data in this file
        res.add(pd);
        num++;
      }
      reader.close();

      //System.err.println("PointArchiverASCII:loadFile: "
      //  		 + "LOADED " + num + " FROM " + fname);

      if (hadtodecompress) {
        //We need to delete the temporary file now
        (new File(fname)).delete();
      }
    } catch (Exception e) {
      System.err.println("PointArchiverASCII:loadFile: " + fname + " "
			 + e.getMessage());
    }
  }


  /** Test if the specified filename corresponds to a compressed file.
   * In practice this just means we check for a <i>.zip</i> extension.
   * @param filename The file name to check.
   * @return <code>True</code> if the file is compressed, <code>False</code>
   *         otherwise. */
  private
  boolean
  isCompressed(String filename)
  {
    return filename.endsWith(".zip");
  }


  /** Decompress the specified file and return the path to a temporary file.
   * The temporary file should generally be deleted by the caller once it 
   * is no longer required.
   * @param filename Full path to the file to decompress.
   * @return Name of temporary file containing the decompressed data. */
  private
  String
  decompress(String filename)
  {
    File f = null;
    InputStream compressed = null;
    PrintWriter uncompressed = null;
    String res = null;

    try {
      ZipFile zip = new ZipFile(filename);
      String foo = filename.substring(filename.lastIndexOf(FSEP)+1);
      foo = foo.substring(0, foo.length()-4);
      ZipEntry ze = zip.getEntry(foo);
      compressed = zip.getInputStream(ze);;
      f = File.createTempFile("foo", ".tmp", theirTempDir);
      uncompressed = new PrintWriter(new BufferedWriter(new FileWriter(f)));
      res = f.getPath();

      //Extract data and copy to plain text file
      final int blen=4096;
      byte[] bbuf = new byte[blen];
      char[] cbuf = new char[blen];
      int len;
      while ((len = compressed.read(bbuf)) > 0) {
        for (int i=0; i<len; i++) {
          cbuf[i]=(char)bbuf[i];
        }
        uncompressed.write(cbuf, 0, len);
      }

      compressed.close();
      uncompressed.close();
    } catch (Exception e) {
      //if (compressed!=null) compressed.close();
      //if (uncompressed!=null) uncompressed.close();
      e.printStackTrace();
    }

    //System.err.println("PointArchiverASCII:decompress: Extracted "
    //    	       + filename + " to " + res);
    return res;
  }


  /** Compress the specified file. The file location is not not changed but
   * the file will be renamed with a <i>.zip</i> extension.
   * @param filename The name of the file to be compressed. */
  public
  void
  compress(String filename)
  {
    try {
      File fin = new File(filename);
      FileInputStream uncompressed = new FileInputStream(fin);
      File fout = new File(filename+".zip");
      ZipOutputStream compressed = new ZipOutputStream(new FileOutputStream(fout));
      //Add the ZIP header info
      String shortname;
      int lastslash = filename.lastIndexOf(FSEP);
      if (lastslash!=-1) {
        shortname = filename.substring(lastslash+1);
      } else {
        shortname = filename;
      }
      compressed.putNextEntry(new ZipEntry(shortname));

      //Copy data to compressed file
      byte[] buf = new byte[4096];
      int len;
      while ((len = uncompressed.read(buf)) > 0) {
        compressed.write(buf, 0, len);
      }

      compressed.close();
      //System.err.println("PointArchiverASCII:compress: Compressed " + filename);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  public static final void main(String args[])
  {
    if (args.length<1) {
      System.err.println("USAGE: Specify a file to be compressed");
      System.exit(1);
    }

    System.out.println("Will compress " + args[0]);
    PointArchiverASCII paa = new PointArchiverASCII("foo");
    paa.compress(args[0]);
    paa.decompress(args[0]+".zip");
  }

}
