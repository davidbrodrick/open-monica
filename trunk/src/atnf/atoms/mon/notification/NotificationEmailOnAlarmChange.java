//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.notification;

import atnf.atoms.mon.*;
import atnf.atoms.mon.util.MailSender;
import atnf.atoms.mon.util.MonitorUtils;
import org.apache.log4j.Logger;

/**
 * Sends an email when the data changes in to or out of an alarm state.
 * 
 * <P>
 * The definition requires three string arguments:
 * <ul>
 * <li><b>Recipient:</b> The email address to send the email to, eg "fred@email.com".
 * <li><b>Sender:</b> The email address of the sender "fred@email.com". If no address is specified, a default address of
 * MoniCA@localhost will be supplied.
 * <li><b>Subject:</b> The subject line of the email, eg "Warning from MoniCA".
 * <li><b>Body:</b> The body text of the email, eg "Warning\nThe new value is $V".
 * </ul>
 * 
 * The subject and body templates can macros which will be substituted before the email is sent. These are documented in the
 * MonitorUtils.doSubstituion method.
 * 
 * @author David Brodrick
 */
public class NotificationEmailOnAlarmChange extends AbstractNotificationEmail {
  /** The previous data value. */
  protected PointData itsLastData;

  /** Logger. */
  protected static Logger theirLogger = Logger.getLogger(NotificationEmailOnAlarmChange.class);

  public NotificationEmailOnAlarmChange(PointDescription parent, String[] init) {
    super(parent, init);
  }

  /** Send an email if the alarm status changed. */
  public void checkNotify(PointData data) {
    if (itsLastData != null) {
      if (itsLastData.getAlarm() != data.getAlarm()) {
        sendEmail(data);
      }
    }
    itsLastData = data;
  }
}
