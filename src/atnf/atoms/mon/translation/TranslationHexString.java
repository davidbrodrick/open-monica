package atnf.atoms.mon.translation;

import org.apache.log4j.Logger;

import atnf.atoms.mon.*;

/**
 * Get a string representing the hex value of the input (which must be an integer type).
 *  
 * @author David Brodrick
 */
public class TranslationHexString extends Translation
{
  public TranslationHexString(PointDescription parent, String[] init)
  {
    super(parent, init);
  }

  public PointData translate(PointData data)
  {
    String result = null;
    if (data.getData() != null) {
      if (data.getData() instanceof Number) {
        result = Long.toHexString(((Number)data.getData()).longValue());
      } else {
        Logger logger = Logger.getLogger(this.getClass().getName());
        logger.error("(" + itsParent.getFullName() + ") Expect Number as input, got " + data.getData().getClass());
      }
    }
    return new PointData(itsParent.getFullName(), data.getTimestamp(), result);
  }
}
