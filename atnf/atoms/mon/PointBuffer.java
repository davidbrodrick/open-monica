//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon;

import java.util.*;
import atnf.atoms.time.*;
import atnf.atoms.mon.transaction.*;
import atnf.atoms.mon.translation.*;
import atnf.atoms.mon.archiver.PointArchiver;

/**
 * Maintains a buffer of PointData, and makes them available to processes
 * which require them.
 *
 * @author Le Cuong Nguyen
 * @author David Brodrick
 * @version $Id: PointBuffer.java,v 1.4 2006/05/11 23:40:55 bro764 Exp $
 */
public class PointBuffer
{
  /** Stores Vectors of past data. Keyed by PointMonitors */
  protected static Hashtable bufferTable = new Hashtable();
   
  /** Stores the loaded Translation classes */
  protected static HashSet itsTranslations = new HashSet();
   
  /** Stores the loaded Transaction classes */
  protected static HashSet itsTransactions = new HashSet();

  //protected PointBuffer() { }


  /** Allocate buffer storage space for the new monitor point. */
  private static
  void
  newPoint(PointMonitor pm)
  {
    synchronized(bufferTable) {
      //Add new key/storage to the hash
      bufferTable.put(pm, new Vector());
      //Wake any waiting threads
      bufferTable.notifyAll();
    }
  }


  /** Add new data to the buffer for the given point.
   * @param pm The point to add the new data for.
   * @param data The new data for the given point. */
  public static
  void
  updateData(PointMonitor pm, PointData data)
  {
    if (data!=null && data.getData()!=null) {
      synchronized(bufferTable) {
	Vector buf = (Vector)bufferTable.get(pm);
	if (buf==null) {
	  //New point, add it to the table
	  newPoint(pm);
	  buf = (Vector)bufferTable.get(pm);
	} else {
	  //Existing point, update sequence number
	  data.setSequence(((PointData)buf.lastElement()).getSequence()+1);
	}

	//Ensure the buffer hasn't grown too large
	while (buf.size()>pm.getMaxBufferSize()) buf.remove(0);

	//Add the new data to the buffer
	buf.add(data);
	bufferTable.notifyAll();
      }
    }
  }
   

  /** Return the latest data for the specified point.
   * @param point Source/name of the monitor point to retrieve. This
   *        is expected to be in the format <tt>source.name</tt>.
   * @return Latest data for the specified point. */
  public static
  PointData
  getPointData(String point)
  {
    PointMonitor pm = MonitorMap.getPointMonitor(point);
    if (pm==null) return null;
    return getPointData(pm);
  }


  /** Return the latest data for the specified point/source.
   * @param name Name of the monitor point to retrieve.
   * @param source Source to get the data for.
   * @return Latest data for the specified point. */
  public static
  PointData
  getPointData(String name,
	       String source)
  {
    PointMonitor pm = MonitorMap.getPointMonitor(source+"."+name);
    return getPointData(pm);
  }


  /** Return the latest data for the specified point.
   * @param pm Monitor point to get the latest data for.
   * @return Latest data for the specified point. */
  public static
  PointData
  getPointData(PointMonitor pm)
  {
    synchronized(bufferTable) {
      if (pm==null) return null;
      Vector data = (Vector)bufferTable.get(pm);
      if (data == null) return null;
      if (data.size() < 1) return null;
      return (PointData)((Vector)bufferTable.get(pm)).lastElement();
    }
  }


  /** Return all data in the specified time range for the given point.
   * This will access the memory buffer and/or the data archive on disk
   * in order to gather all data between the specified times. A
   * <tt>sample_rate</tt> parameter can be used to undersample the
   * available data to reduce the amount of data which is returned.
   * @param pm The point to get the data for.
   * @param start_time The earliest time in the range of interest.
   * @param end_time The most recent time in the range of interest.
   * @param sample_rate Not yet sure how it works...
   * @return Vector containing all data in the specified time range.
   *         <tt>null</tt> will be returned if no data were found. */
  public static
  Vector
  getPointData(PointMonitor pm,
	       AbsTime start_time,
	       AbsTime end_time,
	       int maxsamples)
  {
    //Get data from the memory buffer first
    Vector bufdata = getPointDataBuffer(pm, start_time, end_time);
    if (bufdata==null) bufdata = new Vector(); //Ensure not null

    //If all data were in the memory buffer there's no point in
    //searching the disk archive for additional data.
    if (bufdata.size()>0 && start_time.isAfterOrEquals(((PointData)bufdata.firstElement()).getTimestamp())) {
      return bufdata;
    }

    //OK, we need archived data as well
    PointArchiver arc = MonitorMap.getPointArchiver(PointArchiver.name());
    Vector arcdata = arc.extract(pm, start_time, end_time, 1);
    if (arcdata==null) arcdata = new Vector(); //Ensure not null

    //Remove any overlap data (data duplicated in archive and buffer)
    int cnt = 0;
    if (bufdata.size()>0) {
      AbsTime buffer_start = ((PointData)bufdata.firstElement()).getTimestamp();
      while (arcdata.size()>0 &&
             ((PointData)arcdata.lastElement()).getTimestamp().isAfterOrEquals(buffer_start)) {
	arcdata.remove(arcdata.lastElement());
        cnt++;
      }
      //Add the buffer data to the archive data
      arcdata.addAll(bufdata);
    }

    //If a maximum number of samples was specified then down-sample
    if (arcdata.size()>0 && maxsamples>1 && arcdata.size()>maxsamples) {
      //System.err.println("PointBuffer: Need to down-sample from " + arcdata.size() + " to " + maxsamples);
      AbsTime nextsamp = start_time;
      RelTime increment = RelTime.factory((end_time.getValue()-start_time.getValue())/maxsamples);
      Vector newres = new Vector(maxsamples);

      int i=0;
      while (i<arcdata.size()&&nextsamp.isBeforeOrEquals(end_time)) {
	//Find the next sample which needs to be kept
	while (i<arcdata.size() &&
	       ((PointData)arcdata.get(i)).getTimestamp().isBefore(nextsamp)) {
	  i++;
	}
	//If we've exhausted the data then exit the loop
	if (i>=arcdata.size()) break;

	//We need to keep this sample
	newres.add(arcdata.get(i));
	nextsamp = nextsamp.add(increment);
        i++;
      }
      arcdata=newres;
    }

    //Ensure null result if no data were found.
    if (arcdata.size()==0) arcdata = null;
    return arcdata;
  }


  /** Return all data in the specified time range for the given point.
   * This will access the memory buffer and/or the data archive on disk
   * in order to gather all data between the specified times. A
   * <tt>sample_rate</tt> parameter can be used to undersample the
   * available data to reduce the amount of data which is returned.
   * @param point Source and point name to get the data for. This must
   *              be in the format <tt>source.pointname</tt>.
   * @param start_time The earliest time in the range of interest.
   * @param end_time The most recent time in the range of interest.
   * @param sample_rate Not yet sure how it works...
   * @return Vector containing all data in the specified time range.
   *         <tt>null</tt> will be returned if no data were found
   *         or if the source/point name were invalid. */
  public static
  Vector
  getPointData(String point,
	       AbsTime start,
	       AbsTime end,
	       int sample_rate)
  {
    //Try to get the specified point and check if it was found
    PointMonitor pm = MonitorMap.getPointMonitor(point);
    if (pm==null) return null;

    return getPointData(pm, start, end, sample_rate);
  }


  /** Return all data between the given time and NOW for the specified
   * point from the specified source. This will access the memory buffer
   * and/or the data archive on disk in order to gather all data in the
   * specified time range.
   * @param name Name of the monitor point to retrieve.
   * @param source Name of the data source to get the data for.
   * @param timestamp Oldest data to return.
   * @return All data between the given time and now. <tt>null</tt> will
   *         be returned if no data were available. */
  public static
  Vector
  getPointData(String name,
	       String source,
	       AbsTime timestamp)
  {
    PointMonitor pm = MonitorMap.getPointMonitor(source+"."+name);
    if (timestamp.isASAP()) return getBufferData(pm);
    return getPointData(pm, timestamp, AbsTime.factory(), 0);
  }


  /** Return all data between the given time and NOW for the specified
   * point/source. This will access the memory buffer and/or the data
   * archive on disk in order to gather all data in the specified time range.
   * @param point Source and point name to get the data for. This must
   *              be in the format <tt>source.pointname</tt>.
   * @param source Name of the data source to get the data for.
   * @param timestamp Oldest data to return.
   * @return All data between the given time and now. <tt>null</tt> will
   *         be returned if no data were available. */
  public static
  Vector
  getPointData(String point,
	       AbsTime timestamp)
  {
    //Try to get the specified point and check if it was found
    PointMonitor pm = MonitorMap.getPointMonitor(point);
    if (pm==null) return null;

    if (timestamp.isASAP()) return getBufferData(pm);
    return getPointData(pm, timestamp, AbsTime.factory(), 0);
  }


  /** Return the latest data for the specified point. This call will block
   * until at least one piece of data is available for the given point.
   * @param pm Monitor point to get the latest data for.
   * @return Latest data for the specified point. */
  public static
  PointData
  getPointDataBlock(PointMonitor pm)
  {
    synchronized(bufferTable) {
      blockOn(pm);
      return (PointData)((Vector)bufferTable.get(pm)).lastElement();
    }
  }


  /** Return data for the given point from the memory buffer between
   * the specified times. NOTE: This data is from the memory buffer only,
   * the disk archive is not accessed for this operation.
   * @param pm The point to get the data for.
   * @param start_time The earliest time in the range of interest.
   * @param end_time The most recent time in the range of interest.
   * @return Vector of buffer data in the given time range. <tt>null</tt>
   *                will be returned if no data were found. */
  public static
  Vector
  getPointDataBuffer(PointMonitor pm,
		     AbsTime start_time,
		     AbsTime end_time)
  {
    synchronized(bufferTable) {
      Vector data = (Vector)bufferTable.get(pm);
      if (data == null) return null;
      if (data.size() < 1) return null;
      Vector res = new Vector();
      /// Should do this in a more efficient way
      res.addAll(data);
      while (res.size()>0 && ((PointData)res.firstElement()).getTimestamp().isBefore(start_time)) res.remove(0);
      while (res.size()>0 && ((PointData)res.lastElement()).getTimestamp().isAfter(end_time)) res.remove(res.lastElement());
      return res;
    }
  }


  /** Return all buffered data for the given point. NOTE: This data is
   * from the memory buffer only, the disk archive is not accessed for
   * this operation.
   * @param pm The point to get the data for.
   * @return Vector of all buffer data. <tt>null</tt> will be returned
   *         if no data were found in the memory buffer. */
  public static
  Vector
  getBufferData(PointMonitor pm)
  {
    return (Vector)bufferTable.get(pm);
  }


  /** Return all buffered data for the given point. NOTE: This data is
   * from the memory buffer only, the disk archive is not accessed for
   * this operation. This call will block until at least one piece of
   * data has been processed for the specified point.
   * @param pm The point to get the data for.
   * @return Vector of all buffer data. */
  public static
  Vector
  getBufferDataBlock(PointMonitor pm)
  {
    synchronized(bufferTable) {
      blockOn(pm);
      return (Vector)bufferTable.get(pm);
    }
  }


  /** Waits until data becomes available for the given point.
   * @param pm The point to wait on. */
  public static
  void
  blockOn(PointMonitor pm)
  {
    synchronized(bufferTable) {
      while (bufferTable.get(pm) == null) {
	try {
	  bufferTable.wait();
	} catch (Exception e) {}
      }
    }
  }


   /** The following have not been implemented yet. They should provide the
    ability for a client to get all the policy classes from the server

    OK, but why is it in this class??
    **/
  public static
  void
  add(Translation trans)
  {
    itsTranslations.add(trans.getClass());
  }

  public static
  void
  add(Transaction trans)
  {
    itsTransactions.add(trans.getClass());
  }
   
  public static
  Iterator
  getTransaction()
  {
    return itsTransactions.iterator();
  }

  public static
  Iterator
  getTranslation()
  {
    return itsTranslations.iterator();
  }
}