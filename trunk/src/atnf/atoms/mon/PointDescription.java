// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon;

import java.util.*;
import java.io.*;
import java.awt.event.*;
import javax.swing.event.*;
import atnf.atoms.time.*;
import atnf.atoms.util.*;
import atnf.atoms.mon.notification.Notification;
import atnf.atoms.mon.translation.*;
import atnf.atoms.mon.transaction.*;
import atnf.atoms.mon.externalsystem.*;
import atnf.atoms.mon.alarmcheck.*;
import atnf.atoms.mon.archivepolicy.*;
import atnf.atoms.mon.archiver.*;
import atnf.atoms.mon.util.*;
import org.apache.log4j.Logger;

/**
 * Class which encapsulates all of the meta-information about a point.
 * 
 * @author David Brodrick
 * @author Le Cuong Nguyen
 */
public class PointDescription implements ActionListener, NamedObject, Comparable {
  /** Logger. */
  protected static Logger theirLogger = Logger.getLogger(PointDescription.class.getName());

  /** Records if initialisation of statically defined points is complete. */
  private static boolean theirPointsCreated = false;

  /**
   * Array of names and aliases that belong to this point in dot "." delimited heirarchical form.
   */
  protected String[] itsNames = null;

  /** The source name of the system this point relates to. */
  protected String itsSource = null;

  /** The description of what this monitor point represents. */
  protected String itsLongDesc = "";

  /**
   * A short description of this point for use by space limited client programs.
   */
  protected String itsShortDesc = "";

  /**
   * Maximum length of the short description string. Any descriptions longer than this will be truncated at initialisation time.
   */
  protected static final int theirMaxShortDescLen = 10;

  /**
   * The engineering units of the point's data. For instance this might be "Volts" or "Amps" or "dBm". Leave as null for
   * dimensionless points.
   */
  protected String itsUnits = null;

  /**
   * Should this point be active in the system or does it exist as metadata only.
   */
  protected boolean itsEnabled = false;

  /** Transactions used to collect the data. */
  protected Transaction[] itsInputTransactions = null;

  /** String representation of input Transactions. */
  protected String[] itsInputTransactionStrings = {};

  /** String representation of input Transactions. */
  protected String itsInputTransactionString = "";

  /** Transactions used to output the data. */
  protected Transaction[] itsOutputTransactions = null;

  /** String representation of output Transactions. */
  protected String[] itsOutputTransactionStrings = {};

  /** String representation of output Transactions. */
  protected String itsOutputTransactionString = "";

  /** Translations used to transform the data. */
  protected Translation[] itsTranslations = null;

  /** String representation of Translations. */
  protected String[] itsTranslationStrings = {};

  /** String representation of Translations. */
  protected String itsTranslationString = "";

  /** The alarm criteria for this point. */
  protected AlarmCheck[] itsAlarmChecks = null;

  /** String representation of the individual alarm criteria. */
  protected String[] itsAlarmCheckStrings = {};

  /** Complete string representation of the alarm criteria. */
  protected String itsAlarmCheckString = "";

  /** The policies that define when to archive this point. */
  protected ArchivePolicy[] itsArchive = null;

  /** String representation of the archive policies. */
  protected String[] itsArchiveStrings = {};

  /** String representation of the archive policies. */
  protected String itsArchiveString = "";

  /** The archiver used to store data for this point. */
  protected PointArchiver itsArchiver = null;

  /** The number of days to keep archived data, or -1 for indefinitely. */
  protected int itsArchiveLongevity = -1;

  /** The policies that define when to archive this point. */
  protected Notification[] itsNotifications = null;

  /** String representation of the notifications. */
  protected String[] itsNotificationStrings = {};

  /** String representation of the notifications. */
  protected String itsNotificationString = "";

  /** The alarm priority level. */
  protected int itsPriority = -1;

  /** The alarm guidance text message. */
  protected String itsGuidance = "";

  /** Listeners for data value updates. */
  protected EventListenerList itsListenerList = new EventListenerList();

  /**
   * The interval between updates. If the period is set to 0, then this point has no scheduled update frequency.
   */
  long itsPeriod = 0;

  /** Indicates if the point is currently in the process of being updated. */
  boolean itsCollecting = false;

  /** The time the point should next be updated. */
  protected transient long itsNextEpoch = 0;

  /**
   * Return the period between updates for this monitor point. A period of zero has the special meaning that the update frequency is
   * being handled through some other mechanism.
   */
  public long getPeriod() {
    return itsPeriod;
  }

  /** Set the update interval. */
  public void setPeriod(RelTime newperiod) {
    itsPeriod = newperiod.getValue();
  }

  /** Set the update interval. */
  public void setPeriod(String newperiod) {
    if (newperiod.equalsIgnoreCase("null") || newperiod.trim().equals("-")) {
      itsPeriod = 0;
    } else {
      try {
        itsPeriod = Long.parseLong(newperiod);
        if (itsPeriod < 0) {
          itsPeriod = 0;
        }
      } catch (Exception e) {
        theirLogger.error("(" + getFullName() + "): setPeriod: " + e);
        itsPeriod = 0;
      }
    }
  }

  /** Get the alarm priority. */
  public int getPriority() {
    return itsPriority;
  }

  /** Set the alarm priority. */
  public void setPriority(String priority) {
    if (priority.equals("-")) {
      itsPriority = -1;
    } else {
      try {
        itsPriority = Integer.parseInt(priority);
        if (itsPriority < -1) {
          itsPriority = -1;
        }
      } catch (Exception e) {
        theirLogger.error("(" + getFullName() + "): setPriority: " + e);
        itsPriority = -1;
      }
    }
  }

  /**
   * Get the archive longevity. This is the period in days to keep archived data, or -1 for indefinitely.
   */
  public int getArchiveLongevity() {
    return itsArchiveLongevity;
  }

  /**
   * Set the archive longevity. This is the period in days to keep archived data, or -1 for indefinitely.
   */
  public void setArchiveLongevity(int length) {
    itsArchiveLongevity = length;
  }

  /**
   * Set the archive longevity. This is the period in days to keep archived data, or "-1" or "null" or "-" to archive indefinitely.
   */
  public void setArchiveLongevity(String newperiod) {
    if (newperiod.equalsIgnoreCase("null") || newperiod.trim().equals("-")) {
      itsArchiveLongevity = -1;
    } else {
      try {
        itsArchiveLongevity = Integer.parseInt(newperiod);
      } catch (Exception e) {
        theirLogger.error("(" + getFullName() + "): setArchiveLongevity: " + e);
        itsPeriod = -1;
      }
    }
  }

  public Transaction[] getInputTransactions() {
    return itsInputTransactions;
  }

  public String getInputTransactionString() {
    return itsInputTransactionString;
  }

  public String[] getInputTransactionsAsStrings() {
    return itsInputTransactionStrings;
  }

  protected void setInputTransactionString(String[] transactions) {
    itsInputTransactionStrings = transactions;
    if (transactions == null || transactions.length == 0) {
      itsInputTransactionString = "";
    } else if (transactions.length == 1) {
      itsInputTransactionString = transactions[0];
    } else {
      itsInputTransactionString = "{";
      for (int i = 0; i < transactions.length - 1; i++) {
        itsInputTransactionString += transactions[i] + ",";
      }
      itsInputTransactionString += transactions[transactions.length - 1] + "}";
    }
  }

  public Transaction[] getOutputTransactions() {
    return itsOutputTransactions;
  }

  public String getOutputTransactionString() {
    return itsOutputTransactionString;
  }

  public String[] getOutputTransactionsAsStrings() {
    return itsOutputTransactionStrings;
  }

  protected void setOutputTransactionString(String[] transactions) {
    itsOutputTransactionStrings = transactions;
    if (transactions == null || transactions.length == 0) {
      itsOutputTransactionString = null;
    } else if (transactions.length == 1) {
      itsOutputTransactionString = transactions[0];
    } else {
      itsOutputTransactionString = "{";
      for (int i = 0; i < transactions.length - 1; i++) {
        itsOutputTransactionString += transactions[i] + ",";
      }
      itsOutputTransactionString += transactions[transactions.length - 1] + "}";
    }
  }

  /** Construct the input and output transactions used by this point. */
  protected void makeTransactions() {
    Transaction[] inputtrans = new Transaction[itsInputTransactionStrings.length];
    for (int i = 0; i < itsInputTransactionStrings.length; i++) {
      inputtrans[i] = (Transaction) Factory.factory(this, itsInputTransactionStrings[i], "atnf.atoms.mon.transaction.Transaction");
    }
    Transaction[] outputtrans = new Transaction[itsOutputTransactionStrings.length];
    for (int i = 0; i < itsOutputTransactionStrings.length; i++) {
      outputtrans[i] = (Transaction) Factory.factory(this, itsOutputTransactionStrings[i], "atnf.atoms.mon.transaction.Transaction");
    }
    itsInputTransactions = inputtrans;
    itsOutputTransactions = outputtrans;
  }

  /**
   * Return the Translation objects used by this PointInteraction.
   */
  public Translation[] getTranslations() {
    return itsTranslations;
  }

  public String getTranslationString() {
    return itsTranslationString;
  }

  public String[] getTranslationsAsStrings() {
    return itsTranslationStrings;
  }

  /** Build the Translation objects for this point. */
  protected void makeTranslations() {
    Translation[] translations = new Translation[itsTranslationStrings.length];
    for (int i = 0; i < itsTranslationStrings.length; i++) {
      translations[i] = (Translation) Factory.factory(this, itsTranslationStrings[i], "atnf.atoms.mon.translation.Translation");
    }
    itsTranslations = translations;
  }

  /**
   * Set the Translation objects for this point.
   */
  protected void setTranslations(Translation[] t) {
    itsTranslations = t;
  }

  protected void setTranslationString(String[] translations) {
    itsTranslationStrings = translations;
    if (translations == null || translations.length == 0) {
      itsTranslationString = null;
    } else if (translations.length == 1) {
      itsTranslationString = translations[0];
    } else {
      itsTranslationString = "{";
      for (int i = 0; i < translations.length - 1; i++) {
        itsTranslationString += translations[i] + ",";
      }
      itsTranslationString += translations[translations.length - 1] + "}";
    }
  }

  /**
   * Return the source for this point. The source field is used to indicate what real-world system the information contained in this
   * point pertains to. For instance this could indicate which antenna some monitor data was collected from.
   */
  public String getSource() {
    return itsSource;
  }

  public void setSource(String source) {
    itsSource = source;
  }

  /**
   * Set the names for this point. The objective of this is that all points sharing a common set of names can share a reference to
   * the same set of names in memory.
   */
  protected void setNames(String[] newnames) {
    itsNames = newnames;
  }

  public String[] getAllNames() {
    return itsNames;
  }

  /** Gets the total number of names this object has */
  public int getNumNames() {
    return itsNames.length;
  }

  /** Gets the name at the index specified. */
  public String getName(int i) {
    return itsNames[i];
  }

  /** Gets the primary name of this point. */
  public String getName() {
    return itsNames[0];
  }

  /** Gets the long name of the object */
  public String getLongName() {
    return itsNames[0];
  }

  /** Gets the full source.name name of this point. */
  public String getFullName() {
    return itsSource + "." + itsNames[0];
  }

  /** Other unique Strings that you might use */
  public String[] getFullNames() {
    String[] res = new String[itsNames.length];
    for (int i = 0; i < itsNames.length; i++) {
      res[i] = itsSource + "." + itsNames[i];
    }
    return res;
  }

  /**
   * Get the name component from the m'th full stop delimiter from the end, to the end of the name. If the name has less than n
   * components then the full name will be returned. The source component is not included in this counting.
   */
  public static String getNameComponentFromEnd(String name, int n) {
    String[] components = name.split("\\.");
    if (components.length <= n) {
      // Just return the name
      return name;
    }

    String res = components[components.length - 1];
    for (int i = components.length - 2; i >= 0 && i >= (components.length - n); i--) {
      res = components[i] + "." + res;
    }
    return res;
  }

  public int getNumListeners() {
    return itsListenerList.getListenerCount();
  }

  public void addPointListener(PointListener listener) {
    itsListenerList.add(PointListener.class, listener);
  }

  public void removePointListener(PointListener listener) {
    itsListenerList.remove(PointListener.class, listener);
  }

  public void actionPerformed(ActionEvent e) {
  }

  /** Get next scheduled collection time as an AbsTime. */
  public AbsTime getNextEpoch_AbsTime() {
    return AbsTime.factory(getNextEpoch());
  }

  public boolean getEnabled() {
    return itsEnabled;
  }

  public void setEnabled(boolean enabled) {
    itsEnabled = enabled;
  }

  /**
   * Compare the next-collection timestamp with another PointInteraction or an AbsTime.
   */
  public int compareTo(Object obj) {
    if (obj instanceof PointDescription) {
      if (((PointDescription) obj).getNextEpoch() < getNextEpoch()) {
        return 1;
      }
      if (((PointDescription) obj).getNextEpoch() > getNextEpoch()) {
        return -1;
      }
      return 0;
    } else if (obj instanceof AbsTime) {
      if (((AbsTime) obj).getValue() < getNextEpoch()) {
        return 1;
      }
      if (((AbsTime) obj).getValue() > getNextEpoch()) {
        return -1;
      }
      return 0;
    } else {
      System.err.println("PointInteraction: compareTo: UNKNOWN TYPE!");
      return -1;
    }
  }

  /** Get the alarm checking criteria used by this point. */
  public AlarmCheck[] getAlarmChecks() {
    return itsAlarmChecks;
  }

  public String getAlarmCheckString() {
    return itsAlarmCheckString;
  }

  public String[] getAlarmChecksAsStrings() {
    return itsAlarmCheckStrings;
  }

  /** Set the alarm criteri string. */
  public void setAlarmCheckString(String[] alarms) {
    itsAlarmCheckStrings = alarms;
    if (alarms == null || alarms.length == 0) {
      itsAlarmCheckString = "-";
    } else if (alarms.length == 1) {
      itsAlarmCheckString = alarms[0];
    } else {
      itsAlarmCheckString = "{";
      for (int i = 0; i < alarms.length - 1; i++) {
        itsAlarmCheckString += alarms[i] + ",";
      }
      itsAlarmCheckString += alarms[alarms.length - 1] + "}";
    }
  }

  /** Make the point's alarm check objects. */
  protected void makeAlarmChecks() {
    try {
      AlarmCheck[] alarms = new AlarmCheck[itsAlarmCheckStrings.length];
      for (int i = 0; i < itsAlarmCheckStrings.length; i++) {
        alarms[i] = (AlarmCheck) Factory.factory(this, itsAlarmCheckStrings[i], "atnf.atoms.mon.alarmcheck.AlarmCheck");
      }
      itsAlarmChecks = alarms;
    } catch (Exception e) {
      theirLogger.error("Encountered " + e + " while making AlarmCheck objects for point " + getFullName());
    }
  }

  /**
   * Check the data value against the alarm criteria and set the alarm field if the point is in an alarm state, otherwise leave the
   * current alarm value unchanged.
   */
  public void evaluateAlarms(PointData pd) {
    if (itsAlarmChecks != null && itsAlarmChecks.length > 0) {
      for (int i = 0; i < itsAlarmChecks.length && pd.getAlarm() == false; i++) {
        if (itsAlarmChecks[i] != null) {
          if (!itsAlarmChecks[i].checkAlarm(pd)) {
            // This instance has flagged that we should not evaluate any subsequent alarms for this update
            break;
          }
        }
      }
    }
  }

  /** Get the archive policies used by this point. */
  public ArchivePolicy[] getArchivePolicies() {
    return itsArchive;
  }

  /** Set the string representation of the archive policies. */
  public void setArchiveString(String[] archive) {
    itsArchiveStrings = archive;
    if (archive == null || archive.length == 0) {
      itsArchiveString = null;
    } else if (archive.length == 1) {
      itsArchiveString = archive[0];
    } else {
      itsArchiveString = "{";
      for (int i = 0; i < archive.length - 1; i++) {
        itsArchiveString += archive[i] + ",";
      }
      itsArchiveString += archive[archive.length - 1] + "}";
    }
  }

  /** Get the string representation of the archive policies. */
  public String getArchivePolicyString() {
    return itsArchiveString;
  }

  /** Get the string representation of the archive policies. */
  public String[] getArchivePoliciesAsStrings() {
    return itsArchiveStrings;
  }

  /** Build the ArchivePolicies from their string representation. */
  protected void makeArchivePolicies() {
    ArchivePolicy[] archives = new ArchivePolicy[itsArchiveStrings.length];
    for (int i = 0; i < archives.length; i++) {
      archives[i] = (ArchivePolicy) Factory.factory(this, itsArchiveStrings[i], "atnf.atoms.mon.archivepolicy.ArchivePolicy");
    }
    itsArchive = archives;
  }

  /** Get the notifications used by this point. */
  public Notification[] getNotifications() {
    return itsNotifications;
  }

  /** Set the string representation of the notifications. */
  public void setNotificationString(String[] notifications) {
    itsNotificationStrings = notifications;
    if (notifications == null || notifications.length == 0) {
      itsNotificationString = null;
    } else if (notifications.length == 1) {
      itsNotificationString = notifications[0];
    } else {
      itsNotificationString = "{";
      for (int i = 0; i < notifications.length - 1; i++) {
        itsNotificationString += notifications[i] + ",";
      }
      itsNotificationString += notifications[notifications.length - 1] + "}";
    }
  }

  /** Get the string representation of the notifications. */
  public String getNotificationString() {
    return itsNotificationString;
  }

  /** Get the string representation of the notifications. */
  public String[] getNotificationsAsStrings() {
    return itsNotificationStrings;
  }

  /** Build the Notifications from their string representations. */
  protected void makeNotifications() {
    Notification[] notifications = new Notification[itsNotificationStrings.length];
    for (int i = 0; i < notifications.length; i++) {
      notifications[i] = (Notification) Factory.factory(this, itsNotificationStrings[i], "atnf.atoms.mon.notification.Notification");
    }
    itsNotifications = notifications;
  }

  /**
   * Return the time when this monitor point was last sampled. If the monitor point hasn't yet been sampled "NEVER" is returned.
   */
  public long getLastEpoch() {
    PointData data = PointBuffer.getPointData(this);
    if (data == null) {
      return -1;
    } else {
      return data.getTimestamp().getValue();
    }
  }

  /**
   * Return the time when this monitor point will next be sampled. If the monitor point hasn't yet been sampled, "ASAP" will be
   * returned.
   */
  public long getNextEpoch() {
    return itsNextEpoch;
  }

  /** Allows the manual setting of the next epoch */
  public void setNextEpoch(long nextEpoch) {
    itsNextEpoch = nextEpoch;
  }

  /** Allows the manual setting of the next epoch */
  public void setNextEpoch(AbsTime nextEpoch) {
    itsNextEpoch = nextEpoch.getValue();
  }

  /**
   * Indicate if the point is in the process of being updated. This is useful for points which are updated asynchronously to prevent
   * a subsequent collection from being scheduled while another collection is still happening.
   */
  public void isCollecting(boolean collecting) {
    itsCollecting = collecting;
  }

  /** Indicates if the point is in the process of being updated. */
  public boolean isCollecting() {
    return itsCollecting;
  }

  /** Set the description of this point. */
  public void setLongDesc(String desc) {
    itsLongDesc = desc.replace('\"', '\0');
  }

  /** Get the description. */
  public String getLongDesc() {
    return itsLongDesc;
  }

  /** Set the short description of this point. */
  public void setShortDesc(String desc) {
    itsShortDesc = desc.replace('\"', '\0');
    if (itsShortDesc.length() > theirMaxShortDescLen) {
      itsShortDesc = itsShortDesc.substring(0, theirMaxShortDescLen);
    }
  }

  /** Get the short description. */
  public String getShortDesc() {
    return itsShortDesc;
  }

  /** Set the alarm guidance string for this point. */
  public void setGuidance(String guidance) {
    itsGuidance = guidance.replace('\"', '\0');
  }

  /** Get the alarm guidance string. */
  public String getGuidance() {
    return itsGuidance;
  }

  /**
   * Return the units of the monitor point's value. This string may be null if the point has no units.
   */
  public String getUnits() {
    return itsUnits;
  }

  /** Specify the units of the monitor point's value. */
  public void setUnits(String units) {
    itsUnits = units;
  }

  /**
   * Populate all point fields and manipulate any perform any other operations to make the point active on the server.
   */
  public void populateServerFields() {
    // Populate fields
    makeTransactions();
    makeTranslations();
    makeArchivePolicies();
    makeAlarmChecks();
    makeNotifications();

    // Register with the alarm manager
    if (itsPriority > -1) {
      AlarmManager.setAlarm(this, new PointData(getFullName()));
    }

    // Assign to appropriate ExternalSystem(s) for data collection
    for (int i = 0; i < itsInputTransactions.length; i++) {
      Transaction thistrans = itsInputTransactions[i];
      if (thistrans != null && thistrans.getChannel() != null && !thistrans.getChannel().equals("NONE")) {
        ExternalSystem ds = ExternalSystem.getExternalSystem(thistrans.getChannel());
        if (ds != null) {
          ds.addPoint(this);
        } else {
          theirLogger.warn("(" + getFullName() + ") No ExternalSystem found for Channel: " + thistrans.getChannel());
        }
      }
    }

    // Assign archiver for this point
    setArchiver(PointArchiver.getPointArchiver());
  }

  /**
   * Parse a point definitions file and return all the points defined.
   */
  public static ArrayList parseFile(String fname) {
    try {
      return parseFile(new FileReader(fname));
    } catch (Exception e) {
      theirLogger.error("While parsing point definition file: " + e);
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Parse a point definitions file and return all the points defined.
   */
  public static ArrayList<PointDescription> parseFile(Reader pointsfile) {
    ArrayList<PointDescription> result = new ArrayList<PointDescription>();
    String[] lines = MonitorUtils.parseFile(pointsfile);
    if (lines != null) {
      for (int i = 0; i < lines.length; i++) {
        ArrayList<PointDescription> al = null;
        try {
          al = parseLine(lines[i]);
        } catch (Exception e) {
          theirLogger.error("Exception \"" + e + "\" while parsing point definition line " + (i + 1) + ": " + lines[i]);
        }
        if (al != null) {
          result.addAll(al);
        }
      }
    }
    return result;
  }

  public static ArrayList<PointDescription> parseLine(String line) throws Exception {
    // Number of tokens we expect for each point definition
    final int NUMTOKENS = 13;
    final int NUMTOKENSv2 = 16;

    ArrayList<PointDescription> result = new ArrayList<PointDescription>();

    // Extract appropriate information and make point/s
    String[] toks = MonitorUtils.getTokens(line);
    if (toks.length != NUMTOKENS && toks.length != NUMTOKENSv2) {
      throw new Exception("Expect " + NUMTOKENS + " or " + NUMTOKENSv2 + " tokens, found " + toks.length);
    }

    String[] pointNameArray = MonitorUtils.getTokens(toks[0]);
    String pointLongDesc = toks[1];
    String pointShortDesc = toks[2];
    String pointUnits = toks[3];
    String[] pointSourceArray = MonitorUtils.getTokens(toks[4]);
    String pointEnabled = toks[5];
    String[] pointInputArray = MonitorUtils.getTokens(toks[6]);
    String[] pointOutputArray = MonitorUtils.getTokens(toks[7]);
    String[] pointTranslateArray = MonitorUtils.getTokens(toks[8]);
    String[] pointLimitsArray = MonitorUtils.getTokens(toks[9]);
    String[] pointArchiveArray = MonitorUtils.getTokens(toks[10]);
    String pointPeriod = toks[11];
    String archiveLife = toks[12];
    String[] notificationArray = new String[0];
    String priority = "-";
    String guidance = "";
    if (toks.length == NUMTOKENSv2) {
      notificationArray = MonitorUtils.getTokens(toks[13]);
      priority = toks[14];
      guidance = toks[15];
    }
    boolean[] pointEnabledArray = parseBoolean(pointEnabled);
    if (pointEnabled.length() < pointSourceArray.length) {
      boolean[] temp = new boolean[pointSourceArray.length];
      for (int i = 0; i < temp.length; i++) {
        temp[i] = pointEnabledArray[0];
      }
      pointEnabledArray = temp;
    }

    for (int i = 0; i < pointSourceArray.length; i++) {
      result.add(PointDescription.factory(pointNameArray, pointLongDesc, pointShortDesc, pointUnits, pointSourceArray[i], pointInputArray, pointOutputArray,
          pointTranslateArray, pointLimitsArray, pointArchiveArray, notificationArray, pointPeriod, archiveLife, guidance, priority, pointEnabledArray[i]));
    }

    return result;
  }

  /** Converts a TTFT string into the appropriate array */
  public static boolean[] parseBoolean(String token) {
    boolean[] res = new boolean[token.length()];
    for (int i = 0; i < res.length; i++) {
      if (token.charAt(i) == 't' || token.charAt(i) == 'T') {
        res[i] = true;
      } else {
        res[i] = false;
      }
    }
    return res;
  }

  /** Construct a new monitor point from the given fields. */
  public static PointDescription factory(String[] names, String longdesc, String shortdesc, String units, String source, String[] inputs, String[] outputs,
      String[] translate, String[] limits, String[] archives, String[] notifications, String period, String archivelife, String guidance, String priority,
      boolean enabled) {
    PointDescription result = new PointDescription();
    result.setNames(names);
    result.setLongDesc(longdesc);
    result.setShortDesc(shortdesc);
    result.setUnits(units);
    result.setSource(source);
    result.setInputTransactionString(inputs);
    result.setOutputTransactionString(outputs);
    result.setTranslationString(translate);
    result.setAlarmCheckString(limits);
    result.setArchiveString(archives);
    result.setNotificationString(notifications);
    result.setArchiveLongevity(archivelife);
    result.setPeriod(period);
    result.setGuidance(guidance);
    result.setPriority(priority);
    result.setEnabled(enabled);
    addPoint(result);
    return result;
  }

  /** Distribute data to listeners. */
  public synchronized void distributeData(PointEvent pe) {
    // Pass the event on to all listeners
    Object[] listeners = itsListenerList.getListenerList();
    for (int i = 0; i < listeners.length; i += 2) {
      if (listeners[i] == PointListener.class) {
        try {
          ((PointListener) listeners[i + 1]).onPointEvent(this, pe);
        } catch (Exception e) {
          theirLogger.warn(getFullName() + ": Error distributing data to listener, class " + listeners[i + 1].getClass().getCanonicalName() + " (" + e + ")");
          e.printStackTrace();
        }
      }
    }
  }

  /** OK, maybe new raw data has been collected */
  public synchronized void firePointEvent(PointEvent pe) {
    PointData data = pe.getPointData();
    if (pe.isRaw()) {
      // This is a raw event, we need to translate the data
      // Don't translate if there was nothing to translate
      if (data != null && itsTranslations != null) {
        for (int i = 0; i < itsTranslations.length; i++) {
          try {
            // Apply the next translation if it is defined
            if (itsTranslations[i] != null) {
              data = itsTranslations[i].translate(data);
            }
          } catch (Throwable e) {
            theirLogger.error("(" + getFullName() + ") Error on Translation " + (i + 1) + "/" + itsTranslations.length + ": " + e);
            e.printStackTrace();
            data = null;
          }
          // If null was returned then stop translation process
          if (data == null) {
            break;
          }
        }
      }
      // Ensure data has our name on it (eg not name of a listened-to
      // point)
      if (data != null && !data.getName().equals(getFullName())) {
        data = new PointData(data);
        data.setName(getFullName());
      }

      // Translation has been completed so prepare new event and fire
      pe = new PointEvent(this, data, false);
    }

    if (data != null && data.isValid()) {
      // Check alarm criteria
      evaluateAlarms(data);

      // Perform any required output transactions
      if (getEnabled() && itsOutputTransactions != null && itsOutputTransactions.length > 0) {
        for (int i = 0; i < itsOutputTransactions.length; i++) {
          Transaction thistrans = itsOutputTransactions[i];
          if (thistrans != null) {
            // Find the ExternalSystem responsible for handling this
            // control
            // operation
            ExternalSystem ds = ExternalSystem.getExternalSystem(thistrans.getChannel());
            if (ds == null) {
              theirLogger.warn("(" + getFullName() + ") No ExternalSystem for output Transaction channel " + thistrans.getChannel());
            } else if (!ds.isConnected()) {
              // Could connect here but might block for too long?
              theirLogger.warn("(" + getFullName() + ") While writing output data: ExternalSystem " + thistrans.getChannel() + " is not connected");
            } else {
              try {
                ds.putData(this, data);
              } catch (Exception e) {
                theirLogger.warn("(" + getFullName() + ") while writing output data, ExternalSystem " + ds.getName() + " threw exception \"" + e + "\"");
              }
            }
          }
        }
      }

      // Change registered alarm status if required
      if (itsPriority > -1) {
        AlarmManager.setAlarm(this, data);
      }

      // Send any required notifications, unless acknowledged or shelved
      if (itsNotifications != null && itsNotifications.length > 0) {
        Alarm alarm = AlarmManager.getAlarm(this);
        if (alarm == null || (!alarm.isAcknowledged() && !alarm.isShelved())) {
          for (int i = 0; i < itsNotifications.length; i++) {
            try {
              if (itsNotifications[i] != null) {
                itsNotifications[i].checkNotify(data);
              }
            } catch (Exception e) {
              theirLogger.error("(" + getFullName() + ") Error on Notification " + (i + 1) + "/" + itsNotifications.length + ": " + e);
              e.printStackTrace();
            }
          }
        }
      }

      // Archive data
      if (itsArchiver != null && itsEnabled) {
        for (int i = 0; i < itsArchive.length; i++) {
          if (itsArchive[i] != null && itsArchive[i].checkArchiveThis(data)) {
            itsArchiver.archiveData(this, data);
            break;
          }
        }
      }
    }

    // Add the updated value to the data buffer
    if (data != null) {
      PointBuffer.updateData(this, data);
    }

    // Pass the event on to all listeners
    distributeData(pe);

    // Schedule the next collection
    if (itsPeriod > 0) {
      if (data != null && data.isValid()) {
        itsNextEpoch = data.getTimestamp().getValue() + itsPeriod;
      } else {
        itsNextEpoch = (new AbsTime()).getValue() + itsPeriod;
      }
    }
  }

  /**
   * Specify the PointArchiver to archive data for this point.
   * 
   * @param archiver
   *          The PointArchiver to be used.
   */
  public void setArchiver(PointArchiver archiver) {
    itsArchiver = archiver;
  }

  /**
   * Return the PointArchiver which archives data for this point.
   */
  public PointArchiver getArchiver() {
    return itsArchiver;
  }

  /**
   * Converts this point into a string which can be used to re-create an identical point
   */
  public String getStringEquiv() {
    StringBuffer res = new StringBuffer();
    if (itsNames.length > 1) {
      res.append('{');
      for (int i = 0; i < itsNames.length - 1; i++) {
        res.append(itsNames[i] + ",");
      }
      res.append(itsNames[itsNames.length - 1] + "}");
    } else {
      res.append(itsNames[0]);
    }
    res.append(' ');
    res.append('"');
    res.append(itsLongDesc);
    res.append('"');
    res.append(' ');
    res.append('"');
    res.append(itsShortDesc);
    res.append('"');
    res.append(' ');
    res.append('"');
    res.append(itsUnits);
    res.append('"');
    res.append(' ');
    res.append(itsSource);
    res.append(' ');
    res.append(itsEnabled ? 'T' : 'F');
    res.append(' ');
    res.append(itsInputTransactionString);
    res.append(' ');
    res.append(itsOutputTransactionString);
    res.append(' ');
    res.append(itsTranslationString);
    res.append(' ');
    res.append(itsAlarmCheckString);
    res.append(' ');
    res.append(itsArchiveString);
    res.append(' ');
    res.append(itsPeriod);
    res.append(' ');
    res.append(itsArchiveLongevity);
    res.append(' ');
    res.append(itsNotificationString);
    res.append(' ');
    res.append(itsPriority);
    res.append(' ');
    res.append('"');
    res.append(itsGuidance);
    res.append('"');

    return res.toString();
  }

  /** Get a basic string representation. */
  public String toString() {
    return "{" + itsSource + "." + itsNames[0] + " " + getNextEpoch_AbsTime().toString(AbsTime.Format.UTC_STRING) + "}";
  }

  /** Map of all points (including aliases) indexed by name. */
  private static TreeMap<String, PointDescription> theirPoints = new TreeMap<String, PointDescription>();

  /** Map of all points (excluding aliases) indexed by name. */
  private static TreeMap<String, PointDescription> theirUniquePoints = new TreeMap<String, PointDescription>();

  /** Add a new point to the running system. */
  public static synchronized void addPoint(PointDescription pm) {
    String[] names = pm.getFullNames();
    if (!theirUniquePoints.containsKey(names[0])) {
      theirUniquePoints.put(names[0], pm);
    }
    for (int i = 0; i < names.length; i++) {
      if (!theirPoints.containsKey(names[i])) {
        theirPoints.put(names[i], pm);
      }
    }
  }

  /** Returns all the point names (including aliases) in the system */
  public static synchronized String[] getAllPointNames() {
    return MonitorUtils.toStringArray(theirPoints.keySet().toArray());
  }

  /** Returns all the point names (excluding aliases) in the system */
  public static synchronized String[] getAllUniqueNames() {
    return MonitorUtils.toStringArray(theirUniquePoints.keySet().toArray());
  }

  /** Get the point with the specified name. */
  public static synchronized PointDescription getPoint(String name) {
    return theirPoints.get(name);
  }

  /** Get all points (including aliases). */
  public static synchronized Collection<PointDescription> getAllPoints() {
    return theirPoints.values();
  }

  /** Get all points (excluding aliases). */
  public static synchronized PointDescription[] getAllUniquePoints() {
    Collection<PointDescription> values = theirUniquePoints.values();
    PointDescription[] res = new PointDescription[values.size()];
    theirUniquePoints.values().toArray(res);
    return res;
  }

  /** Check if the point with the specified name exists */
  public static boolean checkPointName(String name) {
    if (theirPoints.containsKey(name)) {
      return true;
    } else {
      return false;
    }
  }

  /** Check if the point if the given name if an alias, rather than a primary point name. */
  public static boolean checkPointAlias(String name) {
    if (theirPoints.containsKey(name) && !theirUniquePoints.containsKey(name)) {
      return true;
    } else {
      return false;
    }
  }

  /** Check if the point is a valid primary name (0), a valid alias (1) or doesn't exist (-1). */
  public static int checkPointNameType(String name) {
    if (!theirPoints.containsKey(name)) {
      // Point doesn't exist
      return -1;
    } else if (!theirUniquePoints.containsKey(name)) {
      // Point is an alias
      return 1;
    } else {
      // Point is a valid primary name
      return 0;
    }
  }

  /**
   * Flag that initialisation of statically defined points has been completed.
   */
  public static void setPointsCreated() {
    theirPointsCreated = true;
  }

  /** Check if initialisation of statically defined points has been completed. */
  public static boolean getPointsCreated() {
    return theirPointsCreated;
  }
}
