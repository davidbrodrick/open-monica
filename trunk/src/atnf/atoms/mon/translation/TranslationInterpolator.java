package atnf.atoms.mon.translation;

import atnf.atoms.mon.*;
import atnf.atoms.time.*;

/**
 * Abstract base class for use by interpolators. Each subclass only needs to implement the <tt>interpolate</tt> method, which
 * returns the value for the specified number of seconds into the future (or past if negative).
 * 
 * <P>
 * Instances expect the following arguments:
 * <ul>
 * <li><b>Period:</b> The buffer length for past values in seconds.
 * <li><b>Prediction Time:</b> The time into the future (or past if negative) to extrapolate/interpolate the value for.
 * <li><b>Preload:</b> Optional argument, if set to "true" the buffer will be pre-seeded from the archive at construction time.
 * </ul>
 * 
 * @author David Brodrick
 */
public abstract class TranslationInterpolator extends TranslationDataBuffer {

  /** How many seconds into the future to make the prediction for. */
  protected RelTime itsPredictionTime = RelTime.factory(-60000000l);

  public TranslationInterpolator(PointDescription parent, String[] init) {
    super(parent, init);
    try {
      // Find when to make the prediction for
      float pred = Float.parseFloat(init[1]) * 1000000;
      itsPredictionTime = RelTime.factory((long) pred);

      // Check whether to seed buffer from archive
      if (init.length >= 3 && init[2].equalsIgnoreCase("true")) {
        seedBufferFromArchive();
      }
    } catch (Exception e) {
      throw new IllegalArgumentException("TranslationInterpolator: " + itsParent.getFullName() + ": Error parsing time offset argument");
    }
  }

  /** Calculate the average and return an averaged value. */
  public PointData translate(PointData data) {
    // Check data type
    if (data.getData() == null || !(data.getData() instanceof Number)) {
      theirLogger.warn("TranslationInterpolator: " + itsParent.getFullName() + ": Can't use non-numeric data");
    }
    // Add new data to buffer and remove any expired data
    updateBuffer(data);

    // If insufficient data then can't calculate result
    if (itsBuffer.size() < 1) {
      return null;
    }

    // Get the value for the requested time offset
    double reqtime = Time.diff(data.getTimestamp().add(itsPredictionTime), itsBuffer.firstElement().getTimestamp()).getAsSeconds();
    double fitval = interpolate(reqtime);

    // Return the interpolated value
    return new PointData(itsParent.getFullName(), data.getTimestamp(), fitval);
  }

  /** Return the interpolated value for the specified time offset. */
  protected abstract double interpolate(double reqtime);
}