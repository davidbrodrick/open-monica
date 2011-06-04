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
import atnf.atoms.mon.archiver.PointArchiver;

/**
 * Maintains a buffer of PointData, and makes them available to processes which require them.
 * 
 * @author Le Cuong Nguyen
 * @author David Brodrick
 * @version $Id: PointBuffer.java,v 1.5 2008/12/18 00:47:02 bro764 Exp bro764 $
 */
public class PointBuffer {
  /** Stores Vectors of past data. Keyed by PointMonitors */
  protected static Hashtable<PointDescription, Vector<PointData>> theirBufferTable = new Hashtable<PointDescription, Vector<PointData>>();

  /** Allocate buffer storage space for the new monitor point. */
  private static void newPoint(PointDescription pm) {
    synchronized (theirBufferTable) {
      // Add new key/storage to the hash
      theirBufferTable.put(pm, new Vector<PointData>(pm.getMaxBufferSize() + 1));
      // Wake any waiting threads
      theirBufferTable.notifyAll();
    }
  }

  /**
   * Add new data to the buffer for the given point.
   * 
   * @param pm
   *          The point to add the new data for.
   * @param data
   *          The new data for the given point.
   */
  public static void updateData(PointDescription pm, PointData data) {
    if (data != null) {
      Vector<PointData> thisbuf = theirBufferTable.get(pm);
      if (thisbuf == null) {
        synchronized (theirBufferTable) {
          // Lock buffer and try again or create if need be
          thisbuf = theirBufferTable.get(pm);
          if (thisbuf == null) {
            // New point, add it to the table
            newPoint(pm);
            thisbuf = theirBufferTable.get(pm);
          }
        }
      }

      synchronized (thisbuf) {
        // Ensure the buffer hasn't grown too large
        while (thisbuf.size() > pm.getMaxBufferSize()) {
          thisbuf.remove(0);
        }

        // Add the new data to the buffer
        thisbuf.add(data);
        // bufferTable.notifyAll();
      }
    }
  }

  /**
   * Return the latest data for the specified point.
   * 
   * @param point
   *          Source/name of the monitor point to retrieve. This is expected to be in the format <tt>source.name</tt>.
   * @return Latest data for the specified point.
   */
  public static PointData getPointData(String point) {
    PointDescription pm = PointDescription.getPoint(point);
    if (pm == null) {
      return null;
    }
    return getPointData(pm);
  }

  /**
   * Return the latest data for the specified point.
   * 
   * @param pm
   *          Monitor point to get the latest data for.
   * @return Latest data for the specified point.
   */
  public static PointData getPointData(PointDescription pm) {
    synchronized (theirBufferTable) {
      if (pm == null) {
        return null;
      }
      Vector data = (Vector) theirBufferTable.get(pm);
      if (data == null) {
        return null;
      }
      if (data.size() < 1) {
        return null;
      }
      return (PointData) ((Vector) theirBufferTable.get(pm)).lastElement();
    }
  }

  /**
   * Return all data in the specified time range for the given point. This will access the memory buffer and/or the data archive on
   * disk in order to gather all data between the specified times. A <tt>sample_rate</tt> parameter can be used to undersample the
   * available data to reduce the amount of data which is returned.
   * 
   * @param pm
   *          The point to get the data for.
   * @param start_time
   *          The earliest time in the range of interest.
   * @param end_time
   *          The most recent time in the range of interest.
   * @param maxsamples
   *          The maximum number of samples to be returned.
   * @return Vector containing all data in the specified time range. <tt>null</tt> will be returned if no data were found.
   */
  public static Vector<PointData> getPointData(PointDescription pm, AbsTime start_time, AbsTime end_time, int maxsamples) {
    // If all data is in memory buffer then return it from there
    if (isAfterOrEqualsFirstData(pm, start_time)) {
      return getPointDataBuffer(pm, start_time, end_time);
    }

    // Some data may be in memory buffer so try there first
    Vector<PointData> bufdata = getPointDataBuffer(pm, start_time, end_time);
    if (bufdata == null) {
      bufdata = new Vector<PointData>(); // Ensure not null
    }

    // Then request rest of data from disk archive
    PointArchiver arc = PointArchiver.getPointArchiver();
    Vector<PointData> arcdata = arc.extract(pm, start_time, end_time);
    if (arcdata == null) {
      arcdata = new Vector<PointData>(); // Ensure not null
    }

    boolean mergebuffer = true;
    if (arcdata.size() > 0) {
      // This is what goes on here:
      // The archive may have a limit on the maximum number of points
      // it can return to a single query, therefore the data that has
      // just been retrieved may not be the entire collection within
      // the time range we requested. If this is the case then we must
      // not append the latest data from the memory buffer. However
      // if the archive retrieval is complete, then we may need to append
      // the updates still buffered in memory.

      // If the result has been clipped at the maximum size then assume
      // it is incomplete.
      if (arcdata.size() >= arc.getMaxNumRecords()) {
        mergebuffer = false;
      }
    }

    if (mergebuffer) {
      // Remove any overlap data (data duplicated in archive and buffer)
      int cnt = 0;
      if (bufdata.size() > 0) {
        AbsTime buffer_start = ((PointData) bufdata.firstElement()).getTimestamp();
        while (arcdata.size() > 0 && ((PointData) arcdata.lastElement()).getTimestamp().isAfterOrEquals(buffer_start)) {
          arcdata.remove(arcdata.lastElement());
          cnt++;
        }
        // Add the buffer data to the archive data
        arcdata.addAll(bufdata);
      }
    }

    // If a maximum number of samples was specified then down-sample
    if (arcdata.size() > 0 && maxsamples > 1 && arcdata.size() > maxsamples) {
      // System.err.println("PointBuffer: Need to down-sample from " +
      // arcdata.size() + " to " + maxsamples);
      AbsTime nextsamp = start_time;
      RelTime increment = RelTime.factory((end_time.getValue() - start_time.getValue()) / maxsamples);
      Vector<PointData> newres = new Vector<PointData>(maxsamples);

      int i = 0;
      while (i < arcdata.size() && nextsamp.isBeforeOrEquals(end_time)) {
        // Find the next sample which needs to be kept
        while (i < arcdata.size() && ((PointData) arcdata.get(i)).getTimestamp().isBefore(nextsamp)) {
          i++;
        }
        // If we've exhausted the data then exit the loop
        if (i >= arcdata.size()) {
          break;
        }

        // We need to keep this sample
        newres.add(arcdata.get(i));
        nextsamp = nextsamp.add(increment);
        i++;
      }
      arcdata = newres;
    }

    // Ensure null result if no data were found.
    if (arcdata.size() == 0) {
      arcdata = null;
    }
    return arcdata;
  }

  /**
   * Return all data in the specified time range for the given point. This will access the memory buffer and/or the data archive on
   * disk in order to gather all data between the specified times. A <tt>sample_rate</tt> parameter can be used to undersample the
   * available data to reduce the amount of data which is returned.
   * 
   * @param point
   *          Source and point name to get the data for. This must be in the format <tt>source.pointname</tt>.
   * @param start_time
   *          The earliest time in the range of interest.
   * @param end_time
   *          The most recent time in the range of interest.
   * @param sample_rate
   *          Not yet sure how it works...
   * @return Vector containing all data in the specified time range. <tt>null</tt> will be returned if no data were found or if the
   *         source/point name were invalid.
   */
  public static Vector<PointData> getPointData(String point, AbsTime start, AbsTime end, int sample_rate) {
    // Try to get the specified point and check if it was found
    PointDescription pm = PointDescription.getPoint(point);
    if (pm == null) {
      return null;
    }

    return getPointData(pm, start, end, sample_rate);
  }

  /**
   * Return the last record who's timestamp is <= the timestamp argument.
   * 
   * @param point
   *          Monitor point to request data for.
   * @param timestamp
   *          Get data before or equalling this time.
   * @return The data, or null if no record was found.
   */
  public static PointData getPreceding(String point, AbsTime timestamp) {
    // Try to get the specified point and check if it was found
    PointDescription pm = PointDescription.getPoint(point);
    if (pm == null) {
      return null;
    }

    PointData res = null;

    // Check if the requested data is still in our memory buffer
    synchronized (theirBufferTable) {
      Vector bufferdata = theirBufferTable.get(pm);

      if (bufferdata != null && bufferdata.size() > 1 && ((PointData) bufferdata.get(0)).getTimestamp().isBeforeOrEquals(timestamp)) {
        // That which we seek is buffered
        for (int i = 0; i < bufferdata.size(); i++) {
          PointData pd = ((PointData) bufferdata.get(i));
          if (pd.getTimestamp().isAfter(timestamp)) {
            // Stop now
            break;
          } else {
            // This record satisfies our criteria
            res = pd;
          }
        }
      }
    }

    if (res == null) {
      // The data is no longer buffered - need to ask the archive
      PointArchiver arc = PointArchiver.getPointArchiver();
      res = arc.getPreceding(pm, timestamp);
    }

    return res;
  }

  /**
   * Return the last record who's timestamp is >= the timestamp argument.
   * 
   * @param point
   *          Monitor point to request data for.
   * @param timestamp
   *          Get data after or equalling this time.
   * @return The data, or null if no record was found.
   */
  public static PointData getFollowing(String point, AbsTime timestamp) {
    // Try to get the specified point and check if it was found
    PointDescription pm = PointDescription.getPoint(point);
    if (pm == null) {
      return null;
    }

    PointData res = null;
    PointData temp = null;

    // Check if the requested data is still in our memory buffer
    synchronized (theirBufferTable) {
      Vector bufferdata = theirBufferTable.get(pm);
      if (bufferdata != null && bufferdata.size() > 0) {
        if (((PointData) bufferdata.get(0)).getTimestamp().isBeforeOrEquals(timestamp)) {
          // That which we seek is certainly in the buffer
          for (int i = bufferdata.size() - 1; i >= 0; i--) {
            PointData pd = ((PointData) bufferdata.get(i));
            if (pd.getTimestamp().isBefore(timestamp)) {
              // Stop now
              break;
            } else {
              // This record satisfies our criteria
              res = pd;
            }
          }
        } else {
          // Can't be certain it is in buffer, but might be depending on what
          // data
          // the archive contains.
          temp = ((PointData) bufferdata.get(0));
        }
      }
    }

    if (res == null) {
      // The data may not be buffered so ask the archive
      PointArchiver arc = PointArchiver.getPointArchiver();
      res = arc.getFollowing(pm, timestamp);
      if (res == null) {
        // Nothing from archive so oldest data in buffer is best match
        res = temp;
      }
    }

    return res;
  }

  /**
   * Check if the specified timestamp is after or equal to to the most recent data in the buffer for the specified point.
   * 
   * @param pm
   *          The point to check.
   * @param time
   *          The timestamp to be checked against the buffer.
   * @return True if the timestamp is equal to or after the most recent buffered data, False otherwise.
   */
  private static boolean isAfterOrEqualsLastData(PointDescription pm, AbsTime time) {
    synchronized (theirBufferTable) {
      Vector<PointData> data = theirBufferTable.get(pm);
      if (data == null) {
        return false;
      }
      if (data.size() < 1) {
        return false;
      }
      if (time.isAfterOrEquals(data.lastElement().getTimestamp())) {
        return true;
      } else {
        return false;
      }
    }
  }

  /**
   * Check if the specified timestamp is after or equal to the oldest data in the buffer for the specified point.
   * 
   * @param pm
   *          The point to check.
   * @param time
   *          The timestamp to be checked against the buffer.
   * @return True if the timestamp is equal to or more recent than the oldest buffered data, False otherwise.
   */
  private static boolean isAfterOrEqualsFirstData(PointDescription pm, AbsTime time) {
    synchronized (theirBufferTable) {
      Vector<PointData> data = theirBufferTable.get(pm);
      if (data == null) {
        return false;
      }
      if (data.size() < 1) {
        return false;
      }
      if (time.isAfterOrEquals(data.firstElement().getTimestamp())) {
        return true;
      } else {
        return false;
      }
    }
  }

  /**
   * Return data for the given point from the memory buffer between the specified times. NOTE: This data is from the memory buffer
   * only, the disk archive is not accessed for this operation.
   * 
   * @param pm
   *          The point to get the data for.
   * @param start_time
   *          The earliest time in the range of interest.
   * @param end_time
   *          The most recent time in the range of interest.
   * @return Vector of buffer data in the given time range. <tt>null</tt> will be returned if no data were found.
   */
  private static Vector<PointData> getPointDataBuffer(PointDescription pm, AbsTime start_time, AbsTime end_time) {
    synchronized (theirBufferTable) {
      Vector<PointData> data = theirBufferTable.get(pm);
      if (data == null) {
        return null;
      }
      if (data.size() < 1) {
        return null;
      }
      Vector<PointData> res = new Vector<PointData>();
      // / Should do this in a more efficient way
      res.addAll(data);
      while (res.size() > 0 && ((PointData) res.firstElement()).getTimestamp().isBefore(start_time)) {
        res.remove(0);
      }
      while (res.size() > 0 && ((PointData) res.lastElement()).getTimestamp().isAfter(end_time)) {
        res.remove(res.lastElement());
      }
      return res;
    }
  }
}
