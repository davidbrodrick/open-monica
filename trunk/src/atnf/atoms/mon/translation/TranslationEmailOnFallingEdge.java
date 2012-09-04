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
 * Sends an email when the numeric/boolean input changes from a high/mark state to a low/space state. The email is sent using the
 * local hosts standard email transport.
 * 
 * <P>
 * The definition requires three or four string arguments:
 * <ul>
 * <li><b>Recipient:</b> The email address to send the email to, eg "fred@email.com".
 * <li><b>Sender:</b> Optional argument. The email address of the sender "mary@email.com". If no address is specified, the current
 * user's default address from the system will be used.
 * <li><b>Subject:</b> The subject line of the email, eg "Warning from MoniCA".
 * <li><b>Body:</b> The body text of the email, eg "Warning\nThe new value is $V".
 * </ul>
 * 
 * The subject and body templates can macros which will be substituted before the email is sent. These are documented in the
 * MonitorUtils.doSubstituion method.
 * 
 * @author David Brodrick
 */
public class TranslationEmailOnFallingEdge extends TranslationEmailOnChange {
  /** The previous data value interpreted as a boolean. */
  protected boolean itsLastBoolValue;

  public TranslationEmailOnFallingEdge(PointDescription parent, String[] init) {
    super(parent, init);
  }

  /** Detects when the data value has changed. */
  protected boolean detectTrigger(PointData pd) {
    boolean res;
    boolean newval = MonitorUtils.parseAsBoolean(pd.getData());
    if (newval == false && itsLastBoolValue == true) {
      // Falling edge. Trigger email now.
      res = true;
    } else {
      res = false;
    }
    itsLastBoolValue = newval;
    return res;
  }
}
