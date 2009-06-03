package atnf.atoms.mon.translation;

import atnf.atoms.mon.*;

/** Map a number to a boolean value. The number is cast to an integer and
 * if the value is zero the output will be False, otherwise the output will
 * be True.
 * 
 * @author David Brodrick */
public class TranslationNumberToBool extends Translation {
  public TranslationNumberToBool(PointDescription parent, String[] init)
  {
    super(parent, init);
  }
  
  public PointData translate(PointData data) {
    Boolean result = null;
    if (data.getData()!=null) {
      if (data.getData() instanceof Number) {
        if (((Number)data.getData()).intValue()==0) {
          result = new Boolean(false);
        } else {
          result = new Boolean(true);
        }
      } else {
        MonitorMap.logger.error("TranslationNumberToBool for " + itsParent.getFullName() + ": Expect Number as input, got " + data.getData().getClass());
      }
    }
    return new PointData(itsParent.getFullName(), data.getTimestamp(), result);
  }
}
