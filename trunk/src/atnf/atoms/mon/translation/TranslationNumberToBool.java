package atnf.atoms.mon.translation;

import org.apache.log4j.Logger;

import atnf.atoms.mon.*;

/** Map a number to a boolean value. The number is cast to an integer and
 * if the value is zero the output will be False, otherwise the output will
 * be True.
 * 
 * <P>If an optional argument is given as "true" then this will invert the logic of the result.
 * 
 * @author David Brodrick */
public class TranslationNumberToBool extends Translation {
  /** Records whether the logic should be inverted. */
  private boolean itsInverted = false;
  
  public TranslationNumberToBool(PointDescription parent, String[] init)
  {
    super(parent, init);
    
    if (init.length>0 && Boolean.parseBoolean(init[0])) {
      itsInverted = true;
    }
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
        Logger logger = Logger.getLogger(this.getClass().getName());
        logger.error("(" + itsParent.getFullName() + ") Expect Number as input, got " + data.getData().getClass());
      }
    }
    
    // Invert output if required
    if (itsInverted && result!=null) {
      result = new Boolean(!result.booleanValue());
    }
    
    return new PointData(itsParent.getFullName(), data.getTimestamp(), result);
  }
}
