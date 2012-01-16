// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.archiver;


import java.util.*;
import atnf.atoms.mon.*;
import atnf.atoms.time.*;

/**
 * Archiver which doesn't archive.
 * 
 * @author David Brodrick
 */
public class PointArchiverNone extends PointArchiver {
  /** Constructor. */
  public PointArchiverNone() {
    super();
  }

  /**
   * Purge all data for the given point that is older than the specified age in days.
   * 
   * @param point
   *          The point whos data we wish to purge.
   */
  protected void purgeOldData(PointDescription point) {
  }

  /**
   * Method to do the actual archiving.
   * 
   * @param pm
   *          The point whos data we wish to archive.
   * @param data
   *          Vector of data to be archived.
   */
  protected void saveNow(PointDescription pm, Vector<PointData> alldata) {
    // Finished archiving this data
    alldata.clear();
  }

  /**
   * Extract data from the archive.
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
    return null;
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
    return null;
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
    return null;
  }
}
