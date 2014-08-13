package atnf.atoms.mon.translation;

import org.nfunk.jep.JEP;

import org.apache.log4j.Logger;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;

/**
 * Translation for formatting strings.
 * 
 * Pass the string "dhms" to format the number of seconds to "1d 12:00:34". Pass any sprintf formatting
 * string otherwise, e.g. "%02x" to convert the input to a two digit hex number
 * 
 * @author Balthasar Indermuehle balt@inside.net 13/08/2014
 */
public class TranslationFormatString extends Translation {
  /** its format string */
  protected String itsFormat;
  
  /** Logger. */
  protected static Logger theirLogger = Logger.getLogger(TranslationFormatString.class.getName());

  public TranslationFormatString(PointDescription parent, String[] init) {
    super(parent, init);

    // precondition
    if (init == null || init.length < 1) {
      theirLogger.error(itsParent.getFullName() + ": Requires Format Argument!");
    } else {
      // do stuff with the data
      itsFormat = init[0];
    }
  }

  public PointData translate(PointData data) {
    String res = null;
    Object val = data.getData();
    
    //theirLogger.debug("Data: " + val);
    //theirLogger.debug("FormatString: " + itsFormat);
 
    if (data == null) {
      return null;
    }

    if (val == null) {
      // Return a null result
      return new PointData(itsParent.getFullName());
    }
   
    // check if a specific string conversion has been requested:
    // supported currently are "dhms" to create "0d 12:03:02" from a count of seconds
    if ( itsFormat.equals("dhms") ) {
      long value = (Long)val;
      long days = (long)Math.floor(value / 86400);        
      long hours = (long)Math.floor((value - days * 86400 ) / 3600);
      long minutes = (long)Math.floor((value - days * 86400 - hours * 3600) / 60);
      long seconds = (long)Math.floor((value - days * 86400 - hours * 3600 - minutes * 60));
      if ( days == 0 ) {
        res = String.format("%02d:%02d:%02d", hours, minutes, seconds);
      } else {
        res = String.format("%dd %02d:%02d:%02d", days, hours, minutes, seconds);
      }
    } else {
      res = String.format(itsFormat, val);
    }
    
    return new PointData(itsParent.getFullName(), data.getTimestamp(), res, data.getAlarm());
  }
}
