//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import atnf.atoms.mon.*;
import atnf.atoms.mon.util.MailSender;
import atnf.atoms.mon.util.MonitorUtils;
import atnf.atoms.time.AbsTime;

import java.lang.reflect.*;
import org.apache.log4j.Logger;

/**
 * Sends an email when the numeric/boolean input changes from a low/space state to a high/mark state. The email is sent using the
 * local hosts standard email transport, using the current user as the From field.
 * 
 * <P>
 * The definition requires three string arguments:
 * <ul>
 * <li><b>Recipient:</b> The email address to send the email to, eg "fred@email.com".
 * <li><b>Subject:</b> The subject line of the email, eg "Warning from MoniCA".
 * <li><b>Body:</b> The body text of the email, eg "Warning\nThe new value is $V".
 * </ul>
 * 
 * The subject and body templates can include the following macros which will be substituted before the email is sent:
 * <ul>
 * <li><b>$V</b> Will be substituted for the latest value of the point, eg "3.141".
 * <li><b>$U</b> Substituted for the units, eg "Amps".
 * <li><b>$N</b> Substituted for the name of the point , eg "site.power.current".
 * <li><b>$S</b> Substituted for the source name, eg "site".
 * <li><b>$D</b> Substituted for the long description of the point, eg "Site main feeder current consumption".
 * <li><b>$T</b> Substituted for the current UTC time, eg "2011-10-21 08:39:25.234".
 * </ul>
 * 
 * @author David Brodrick
 */
public class TranslationEmailOnRisingEdge extends TranslationEmailOnChange {
  /** The previous data value interpreted as a boolean. */
  protected boolean itsLastBoolValue;
  
  public TranslationEmailOnRisingEdge(PointDescription parent, String[] init) {
    super(parent, init);
  }

  /** Detects when the data value has changed. */
  protected boolean detectTrigger(PointData pd) {
    boolean res;
    boolean newval = MonitorUtils.parseAsBoolean(pd.getData());
    if (newval == true && itsLastBoolValue == false) {
      // Rising edge. Trigger email now.
      res = true;
    } else {
      res = false;
    }
    itsLastBoolValue = newval;
    return res;
  }
}
