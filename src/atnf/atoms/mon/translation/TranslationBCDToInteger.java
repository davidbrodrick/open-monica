package atnf.atoms.mon.translation;

import org.apache.log4j.Logger;

import atnf.atoms.mon.*;

/**
 * Read an integer containing BCD nibbles and return the plain integer value.
 *  
 * @author David Brodrick
 */
public class TranslationBCDToInteger extends Translation
{  
  public TranslationBCDToInteger(PointDescription parent, String[] init)
  {
    super(parent, init);
  }

  public PointData translate(PointData data)
  {  
    if (data.getData() == null || !(data.getData() instanceof Number)) {
        Logger logger = Logger.getLogger(this.getClass().getName());
        logger.error("(" + itsParent.getFullName() + ") Expect Number as input");
        return null;
    }

    // Process each nibble
    int inp = ((Number)data.getData()).intValue();
    int outp = 0;
    for (int n=0; n<8; n++) {
      int thisn=(inp>>(4*n)&0xF);
      outp=outp+thisn*(int)Math.pow(10, n);
    }
    
    return new PointData(itsParent.getFullName(), data.getTimestamp(), new Integer(outp), data.getAlarm());
  }
}
