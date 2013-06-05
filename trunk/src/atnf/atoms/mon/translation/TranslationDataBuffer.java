package atnf.atoms.mon.translation;

import java.util.Vector;

import atnf.atoms.mon.*;
import atnf.atoms.time.*;

/**
 * Abstract base class for points which retain a buffer of historical data. This requires that the first argument is the length of
 * the buffer in seconds.
 * 
 * <P>
 * Subclasses should call <tt>updateBuffer(newpointdata)</tt> in their translate method.
 * 
 * <P>
 * There is a utility method <tt>seedBufferFromArchive()</tt> which sub-classes may choose to call on construction, where
 * appropriate.
 * 
 * @author David Brodrick
 */
public abstract class TranslationDataBuffer extends Translation {

  /** Buffer containing data. */
  protected Vector<PointData> itsBuffer = new Vector<PointData>();

  /** Period to measure the peak over. */
  protected RelTime itsPeriod = RelTime.factory(-60000000l);

  public TranslationDataBuffer(PointDescription parent, String[] init) {
    super(parent, init);

    // Find amount of time to buffer data for
    try {
      float period = Float.parseFloat(init[0]) * 1000000;
      if (period > 0) {
        period = -period;
      }
      itsPeriod = RelTime.factory((long) period);
    } catch (Exception e) {
      throw new IllegalArgumentException("TranslationDataBuffer: " + itsParent.getFullName() + ": Error parsing buffer length argument");
    }
  }

  /** Add new data to buffer and purge old data. */
  protected void updateBuffer(PointData newdata) {
    // Add the new data
    if (newdata != null && newdata.getData() != null) {
      itsBuffer.add(newdata);
    }

    // Purge any old data which has now expired
    AbsTime expiry = (new AbsTime()).add(itsPeriod);
    while (itsBuffer.size() > 0 && ((PointData) itsBuffer.get(0)).getTimestamp().isBefore(expiry)) {
      itsBuffer.remove(0);
    }
  }

  /** Fetch data from the archive and use it to seed the buffer. */
  protected void seedBufferFromArchive() {
    AbsTime now = new AbsTime();
    AbsTime start = now.add(itsPeriod);
    Vector<PointData> arcdata = PointBuffer.getPointData(itsParent, start, now);
    if (arcdata != null && !arcdata.isEmpty()) {
      theirLogger.info("TranslationDataBuffer: " + itsParent.getFullName() + ": Seeding buffer with " + arcdata.size() + " points from archive");
      for (int i = 0; i < arcdata.size(); i++) {
        updateBuffer(arcdata.get(i));
      }
    }
  }
}