//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.archiver;

import java.io.*;
import java.util.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.util.*;
import atnf.atoms.time.AbsTime;

/**
 * Saves all objects as Serialized java objects, then compresses the files
 * when they get too big.
 * <P>Why 13 in the code? Well thats the length the filename should be
 * without an extension.
 * @version $Id: PointArchiverCompress.java,v 1.5 2004/09/07 00:58:21 bro764 Exp $
 * @author Le Cuong Nguyen
 * @author David Brodrick
 */
public class
PointArchiverCompress
extends PointArchiver
{
  private final String itsExt = MonitorConfig.getProperty("CompressExt");

  /** Holds the undersampling factor for the current extraction. */
  private int itsUndersampleFactor;

  /** Holds the undersampling incrementor for the current extraction. */
  private int itsUndersampleCount;


  public PointArchiverCompress(String arg)
  {
    super(arg);
  }


  /** Save the given data to the archive.
   * @param pm The point whos data we wish to archive.
   * @param data Vector of data to be archived. */
  public synchronized
  void
  saveNow(PointMonitor pm, Vector data)
  {
    try {
      String compressFile = null;
      String path = getDir(pm);
      String fileName = path+FSEP+getDateTimeNow();
      File myDir = new File(path);
      if (!myDir.isDirectory()) myDir.mkdirs();
      String[] dirFiles = myDir.list();
      if (dirFiles==null || dirFiles.length < 1) {
	(new File(fileName)).createNewFile();
	dirFiles = myDir.list();
      }
      int lastTSIndex = 0;
      String fname = "";

      // Find the last uncompressed file
      for (int i = 0; i < dirFiles.length; i++) {
	if (isCompressed(dirFiles[i])) continue;
	if (getDateTime(dirFiles[i]).after(getDateTime(dirFiles[lastTSIndex]))) lastTSIndex = i;
      }

      if (isCompressed(dirFiles[lastTSIndex])) fname = fileName;
      else {
	// May need to compress file - File should not be compressed yet!
	String lastFile = path+FSEP+dirFiles[lastTSIndex];
	File latestFile = new File(lastFile);

	// Check the latest file
	if (getDateTime(dirFiles[lastTSIndex]).before(new
						      Date(System.currentTimeMillis() - MAXAGE)) ||
	    latestFile.length() > MAXLENGTH) {
	  compressFile = lastFile;
	  fname = fileName;
	} else fname = lastFile;
      }

      File meFile = new File(fname);
      FileOutputStream fos = new FileOutputStream(fname, true);
      ObjectOutputStream oos = (meFile.length() > 0) ? new NoHeaderOutputStream(fos) : new ObjectOutputStream(fos);
      for (int u = 0; u < data.size(); u++) {
	oos.writeObject(((PointData)data.elementAt(u)).getTimestamp());
	oos.writeObject(((PointData)data.elementAt(u)).getData());
      }
      oos.flush();
      oos.close();
      fos.close();

      // Now lets compress the last file if we need to
      if (compressFile != null) {
	StringBuffer cmdLine = new StringBuffer(MonitorConfig.getProperty("ArchiverArg"));
	for (int i = 0; i < cmdLine.length() - 1; i++)
	  if (cmdLine.charAt(i) == '$' && cmdLine.charAt(i+1) == '1')
	    cmdLine.replace(i, i+2, compressFile);
	Process p = Runtime.getRuntime().exec(cmdLine.toString());
	//	    p.waitFor();
	//	    if (p.exitValue() != 0) System.out.println("Error in compressing: "+cmdLine);

	/*	       BufferedInputStream bis = new BufferedInputStream(p.getErrorStream());
	 ByteArrayOutputStream baos = new ByteArrayOutputStream();
	 int a = bis.read();
	 while (a != -1) {
	 baos.write(a);
	 a = bis.read();
	 }

	 p.waitFor();
	 p.destroy();
	 //	       System.out.println(new String(baos.toByteArray()));
	 if (p.exitValue() != 0) {
	 System.out.println("Error in Compressing! "+cmdLine);
	 //		  System.exit(-1);
	 }
	 */       }
    } catch (Exception e) {e.printStackTrace();}
  }


   /** Extract data from the archive.
    * @param pm Point to extract data for.
    * @param start Earliest time in the range of interest.
    * @param end Most recent time in the range of interest.
    * @param undersample Undersampling factor.
    * @return Vector containing all data for the point over the time range. */
   public synchronized
   Vector
   extract(PointMonitor pm, AbsTime start, AbsTime end, int undersample)
   {
     Vector res = new Vector();
     itsUndersampleCount = 0;
     itsUndersampleFactor = undersample;

     //get the archive directory for the given point
     String dir = getDir(pm);

     //Get all the archive files relevant to the period of interest
     Vector files = getFiles(dir, start, end);

     //Try to load data from each of the files
     for (int j=0; j<files.size(); j++) {
       String filename = (String)files.get(j);

       if (isCompressed(filename)) {
	 try {
	   //COMPRESSED - we need to make a copy and decompress
	   String tempFile = getTempFile(filename);
	   File outFile = new File(tempFile);
	   File myFile = new File(dir + FSEP + filename);
	   FileInputStream fis = new FileInputStream(myFile);
	   FileOutputStream fos = new FileOutputStream(outFile);
	   byte[] buf = new byte[1024];
	   int bytes_read = 0;
	   while ((bytes_read = fis.read(buf)) != -1) fos.write(buf,0,bytes_read);
	   fos.flush();
	   fis.close();
	   fos.close();

	   // Now lets decompress the temporary file
	   StringBuffer cmdLine = new StringBuffer(MonitorConfig.getProperty("DeArchiverArg"));
	   for (int i = 0; i < cmdLine.length() - 1; i++)
	     if (cmdLine.charAt(i) == '$' && cmdLine.charAt(i+1) == '1')
	       cmdLine.replace(i, i+2, tempFile);
	   Process p = Runtime.getRuntime().exec(cmdLine.toString());
	   p.waitFor();

	   // Append the extension to get the file decompressed filename
	   String decompressedFile = tempFile + "." + MonitorConfig.getProperty("DeArchiverExt");

           //Load the relevant data from the file
           loadFile(res, decompressedFile, start, end);

	   // OK, get rid of the temp files
	   outFile.delete();
	   (new File(decompressedFile)).delete();
	 } catch (Exception e) {e.printStackTrace();}

       } else {
	 //NOT COMPRESSED - we can just load the file
	 loadFile(res, dir+FSEP+filename, start, end);
       }
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
    String[] files = (new File(dir)).list();
    //Can't do anything if there were no files!
    if (files==null || files.length==0) return res;

    //Map the filenames to dates, and sort
    for (int i = 0; i < files.length; i++) {
      String dateName = null;
      //Ignore compression extension for purpose of getting timestamp
      if (files[i].length() >= 13) dateName = files[i].substring(0, 13);
      else continue; //The file isn't an archive file
      Date date = PointArchiver.getDateTime(dateName);
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
	  if (prevkey!=null) res.add(map.get(prevkey)); //(if there was one)
        }
	//No point looking further into the future
	break;

      } else {
	//We need this file it is within the range
	if (!hit) {
	  //This file's start time is in the range,
	  //so the previous file may also contain useful data
	  hit = true;
	  if (prevkey!=null) res.add(map.get(prevkey)); //(if there was one)
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


  /** Return a temporary file name.
   * @param prepend String to be prepended to the temporary file name.
   * @return Temporary filename. */
  private static
  String
  getTempFile(String prepend)
  {
    try {
      String path = MonitorConfig.getProperty("TempDir");
      String fileName = path+FSEP+prepend + PointArchiver.getDateTimeNow();
      File myDir = new File(path);
      if (!myDir.isDirectory()) myDir.mkdirs();
      return fileName;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }


  /** Check the filename to see if the file is compressed.
   * @param fname The name of the file to check.
   * @return <tt>True</tt> if the file is compressed, <tt>False</tt> if not.
   */
  private static
  boolean
  isCompressed(String fname)
  {
    //Just encapsulates a very simple check
    if (fname.length() > 13) return true;
    return false;
  }


  /** Load data within the given time range from the file. This method
   * uses the instance variables <tt>itsUndersampleFactor</tt> and
   * <tt>itsUndersampleCount</tt> to manage undersampling if required.
   * @param res Vector which holds the loaded data.
   * @param fname Full path to the file to load data from.
   * @param pname Name of the monitor point we are loading.
   * @param start The earliest time of interest.
   * @param end The most recent time of interest. */
  private
  void
  loadFile(Vector res, String fname, AbsTime start, AbsTime end)
  {
    try {
      int num = 0;
      FileInputStream fis = new FileInputStream(fname);
      ObjectInputStream ois = new ObjectInputStream(fis);

      while (fis.available()>0) {
	//Read the next data item from the archive file
	AbsTime ts = (AbsTime)ois.readObject();
	Object data = ois.readObject();

	if (ts.isBefore(start)) continue; //Data's too early
	if (ts.isAfter(end)) break; //No more useful data in this file

	//Only keep the appropriate samples
	if (itsUndersampleCount >= itsUndersampleFactor) {
          num++;
	  itsUndersampleCount = 0;
	  res.add(new PointData(null, ts, data));
	}
	itsUndersampleCount++;
      }

      ois.close();
      fis.close();
      if (itsDebug) System.err.println("PointArchiverCompress:loadFile: "
				       + "LOADED " + num + " FROM " + fname);
    } catch (Exception e) {
      System.err.println("PointArchiverCompress:loadFile: " + fname + " "
			 + e.getMessage());
    }
  }
}
