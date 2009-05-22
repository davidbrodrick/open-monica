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
import atnf.atoms.mon.translation.*;
import atnf.atoms.mon.transaction.*;
import atnf.atoms.mon.externalsystem.*;
import atnf.atoms.mon.archivepolicy.*;
import atnf.atoms.mon.archiver.*;
import atnf.atoms.mon.limit.*;
import atnf.atoms.mon.util.*;

/**
 * Class which encapsulates all of the meta-information about a point.
 * 
 * @author David Brodrick
 * @author Le Cuong Nguyen 
 */
public class PointDescription 
implements ActionListener, NamedObject, Comparable
{
  /** Array of names and aliases that belong to this point in dot "." delimited 
   * heirarchical form. */
  protected String[] itsNames = null;

  /** The source name of the system this point relates to. */
  protected String itsSource = null;

  /** The description of what this monitor point represents. */
  protected String itsLongDesc = "";

  /** A short description of this point for use by space limited client programs. */
  protected String itsShortDesc = "";
  
  /** Maximum length of the short description string. Any descriptions longer than
   * this will be truncated at initialisation time. */
  protected static final int theirMaxShortDescLen = 10;

  /** The engineering units of the point's data. For instance this might be "Volts"
   * or "Amps" or "dBm". Leave as null for dimensionless points. */
  protected String itsUnits = null;
  
  /** Should this point be active in the system or does it exist as metadata only. */
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

  /** The limit/alarm criteria for this point. */
  protected PointLimit[] itsLimits = null;
  /** String representation of the limits. */
  protected String[] itsLimitsStrings = {};
  /** String representation of the limits. */
  protected String itsLimitsString = "";

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

  protected EventListenerList itsPLList = new EventListenerList();

  /**
   * The interval between updates. If the period is set to 0, then this point
   * has no scheduled update frequency.
   */
  long itsPeriod = 0;

  /** Indicates if the point is currently in the process of being updated. */
  boolean itsCollecting = false;

  /** The time the point should next be updated. */
  protected transient long itsNextEpoch = 0;

  /**
   * Return the period between updates for this monitor point. A period of zero
   * has the special meaning that the update frequency is being handled through
   * some other mechanism.
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
      itsPeriod = -1;
    } else {
      try {
        itsPeriod = Long.parseLong(newperiod);
      } catch (Exception e) {
        MonitorMap.logger.error("PointDescription: (" + getName()
            + "): setPeriod: " + e.getMessage());
        itsPeriod = -1; // Better than doing nothing..
      }
    }
  }
  
  /** Get the archive longevity. This is the period in days to keep archived data, or -1
   * for indefinitely. */
  public int getArchiveLongevity()
  {
    return itsArchiveLongevity;
  }
  
  /** Set the archive longevity. This is the period in days to keep archived data, or -1
   * for indefinitely. */
  public void setArchiveLongevity(int length)
  {
    itsArchiveLongevity=length;
  }  

  /** Set the archive longevity. This is the period in days to keep archived data, or "-1"
   * or "null" or "-" to archive indefinitely. */
  public void setArchiveLongevity(String newperiod) {
    if (newperiod.equalsIgnoreCase("null") || newperiod.trim().equals("-")) {
      itsArchiveLongevity = -1;
    } else {
      try {
        itsArchiveLongevity = Integer.parseInt(newperiod);
      } catch (Exception e) {
        MonitorMap.logger.error("PointDescription: (" + getName()
            + "): setArchiveLongevity: " + e.getMessage());
        itsPeriod = -1;
      }
    }
  }

  public Transaction[] getInputTransactions()
  {
    return itsInputTransactions;
  }

  public String getInputTransactionString()
  {
    return itsInputTransactionString;
  }

  public String[] getInputTransactionsAsStrings()
  {
    return itsInputTransactionStrings;
  }

  protected
  void
  setInputTransactionString(String[] transactions)
  {
    itsInputTransactionStrings = transactions;
    if (transactions==null || transactions.length==0) {
      itsInputTransactionString = "";
    } else if (transactions.length==1) {
      itsInputTransactionString = transactions[0];
    } else {
      itsInputTransactionString = "{";
      for (int i=0; i<transactions.length-1; i++) {
        itsInputTransactionString += transactions[i] + ",";
      }
      itsInputTransactionString += transactions[transactions.length-1] + "}";
    }
  }  
  public Transaction[] getOutputTransactions()
  {
    return itsOutputTransactions;
  }

  public String getOutputTransactionString()
  {
    return itsOutputTransactionString;
  }

  public String[] getOutputTransactionsAsStrings()
  {
    return itsOutputTransactionStrings;
  }

  protected
  void
  setOutputTransactionString(String[] transactions)
  {
    itsOutputTransactionStrings=transactions;
    if (transactions==null || transactions.length==0) {
      itsOutputTransactionString = null;
    } else if (transactions.length==1) {
      itsOutputTransactionString = transactions[0];
    } else {
      itsOutputTransactionString = "{";
      for (int i=0; i<transactions.length-1; i++) {
        itsOutputTransactionString += transactions[i] + ",";
      }
      itsOutputTransactionString += transactions[transactions.length-1] + "}";
    }
  }
  
  /** Construct the input and output transactions used by this point. */
  protected void
  makeTransactions()
  {
    Transaction[] inputtrans = new Transaction[itsInputTransactionStrings.length];
    for (int i = 0; i < itsInputTransactionStrings.length; i++) {
      inputtrans[i] = Transaction.factory(this, itsInputTransactionStrings[i]);
    }
    Transaction[] outputtrans = new Transaction[itsOutputTransactionStrings.length];
    for (int i = 0; i < itsOutputTransactionStrings.length; i++) {
      outputtrans[i] = Transaction.factory(this, itsOutputTransactionStrings[i]);
    }
    itsInputTransactions = inputtrans;
    itsOutputTransactions = outputtrans;
  }
  
  /**
   * Return the Translation objects used by this PointInteraction.
   */
  public
  Translation[]
  getTranslations()
  {
    return itsTranslations;
  }

  public
  String
  getTranslationString()
  {
    return itsTranslationString;
  }

  public
  String[]
  getTranslationsAsStrings()
  {
    return itsTranslationStrings;
  }
  
  /** Build the Translation objects for this point. */
  protected void
  makeTranslations()
  {
    Translation[] translations = new Translation[itsTranslationStrings.length];
    for (int i = 0; i < itsTranslationStrings.length; i++) {
      translations[i] = Translation.factory(this, itsTranslationStrings[i]);
    }
    itsTranslations=translations;
  }
  
  /**
   * Set the Translation objects for this point.
   */
  protected
  void
  setTranslations(Translation[] t)
  {
    itsTranslations = t;
  }

  protected
  void
  setTranslationString(String[] translations)
  {
    itsTranslationStrings = translations;
    if (translations==null || translations.length==0) {
      itsTranslationString = null;
    } else if (translations.length==1) {
      itsTranslationString = translations[0];
    } else {
      itsTranslationString = "{";
      for (int i=0; i<translations.length-1; i++) {
        itsTranslationString += translations[i] + ",";
      }
      itsTranslationString += translations[translations.length-1] + "}";
    }
  }

   /**
    * Return the source for this point. The source field is used to indicate
    * what real-world system the information contained in this point
    * pertains to. For instance this could indicate which antenna some
    * monitor data was collected from.
    */
   public String
   getSource()
   {
     return itsSource;
   }

   public void setSource(String source)
   {
      itsSource = source;
   }

   /**
    * Set the names for this point. The objective of this is that all
    * points sharing a common set of names can share a reference to
    * the same set of names in memory.
    */
   protected void
   setNames(String[] newnames)
   {
     itsNames = newnames;
   }

   public String[] getAllNames()
   {
      return itsNames;
   }
   /** Gets the total number of names this object has */
   public int
   getNumNames()
   {
     return itsNames.length;
   }

   /** Gets the name at the index specified. */
   public String
   getName(int i)
   {
     return itsNames[i];
   }

   /** Gets the primary name of this point. */
   public String
   getName()
   {
     return itsNames[0];
   }

   /** Gets the long name of the object */
   public String
   getLongName()
   {
     return itsNames[0];
   }

   /** Gets the full source.name name of this point. */
   public String
   getFullName()
   {
     return itsSource+"."+itsNames[0];
   }

   /** Other unique Strings that you might use */
   public String[] getFullNames()
   {
      String[] res = new String[itsNames.length];
      for (int i = 0; i < itsNames.length; i++) {
        res[i] = itsSource+"."+itsNames[i];
      }
      return res;
   }
   public int getNumListeners()
   {
      return itsPLList.getListenerCount();
   }
   
   public void addPointListener(PointListener listener)
   {
      itsPLList.add(PointListener.class, listener);
   }

   public void removePointListener(PointListener listener)
   {
      itsPLList.remove(PointListener.class, listener);
   }

   public void actionPerformed(ActionEvent e) {}


  /** Get next scheduled collection time as an AbsTime. */
  public
  AbsTime
  getNextEpoch_AbsTime()
  {
    return AbsTime.factory(getNextEpoch());
  }


   public boolean getEnabled()
   {
      return itsEnabled;
   }

   public void setEnabled(boolean enabled)
   {
      itsEnabled = enabled;
   }

   /**
    * Compare the next-collection timestamp with another PointInteraction
    * or an AbsTime.
    **/  
   public int compareTo(Object obj)
   {
     if (obj instanceof PointDescription) {
       if (((PointDescription)obj).getNextEpoch() < getNextEpoch()) {
        return 1;
      }
       if (((PointDescription)obj).getNextEpoch() > getNextEpoch()) {
        return -1;
      }
       return 0;
     } else if (obj instanceof AbsTime) {
       if (((AbsTime)obj).getValue() < getNextEpoch()) {
        return 1;
      }
       if (((AbsTime)obj).getValue() > getNextEpoch()) {
        return -1;
      }
       return 0;
     } else {
       System.err.println("PointInteraction: compareTo: UNKNOWN TYPE!");
       return -1;
     }
   }
  /** Get the limit/alarm checking criteria used by this point. */
  public PointLimit[] getLimits() {
    return itsLimits;
  }

  public
  String
  getLimitsString()
  {
    return itsLimitsString;
  }

  public
  String[]
  getLimitsAsStrings()
  {
    return itsLimitsStrings;
  }
  
  /** Set the limit string. */
  public void setLimitsString(String[] limits) {
    itsLimitsStrings=limits;
    if (limits == null || limits.length == 0) {
      itsLimitsString = null;
    } else if (limits.length == 1) {
      itsLimitsString = limits[0];
    } else {
      itsLimitsString = "{";
      for (int i = 0; i < limits.length - 1; i++) {
        itsLimitsString += limits[i] + ",";
      }
      itsLimitsString += limits[limits.length - 1] + "}";
    }
  }
  
  /** Make the point's limit objects. */
  protected void
  makeLimits()
  {
    PointLimit[] limitsa = new PointLimit[itsLimitsStrings.length];
    for (int i = 0; i < itsLimitsStrings.length; i++) {
      limitsa[i] = PointLimit.factory(itsLimitsStrings[i]);
    }
    itsLimits=limitsa;
  }

  /** Check the data against the alarm criteria. */
  public boolean checkLimits(PointData pd) {
    boolean res = false;
    if (itsLimits != null && itsLimits.length > 0) {
      for (int i = 0; i < itsLimits.length; i++) {
        if (itsLimits[i].checkLimits(pd)) {
          res = true;
          break;
        }
      }
    }
    return res;
  }

  /** Get the archive policies used by this point. */
  public ArchivePolicy[] getArchive() {
    return itsArchive;
  }

  /** Specify the archive policies to be used by this point. */
 /* public void setArchive(ArchivePolicy[] archive) {
    itsArchive = archive;
  }*/

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
  public String
  getArchivePolicyString()
  {
     return itsArchiveString;
  }

  /** Get the string representation of the archive policies. */
  public String[]
  getArchivePoliciesAsStrings()
  {
     return itsArchiveStrings;
  }
  
  /** Build the ArchivePolicies from their string representation. */
  protected void
  makeArchivePolicies()
  {
    ArchivePolicy[] archives = new ArchivePolicy[itsArchiveStrings.length];
    for (int i = 0; i < archives.length; i++) {
      archives[i] = ArchivePolicy.factory(itsArchiveStrings[i]);
    }
    itsArchive=archives;
  }
  
  /**
   * Return the time when this monitor point was last sampled. If the monitor
   * point hasn't yet been sampled "NEVER" is returned.
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
   * Return the time when this monitor point will next be sampled. If the
   * monitor point hasn't yet been sampled, "ASAP" will be returned.
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
   * Get the number of data updates to keep buffered in memory. TODO: This
   * currently just returns a fixed number. TODO: Do we want to buffer updates
   * at all?
   */
  public int getMaxBufferSize() {
    return 50;
  }

  /** Indicates if the point is in the process of actively being updated. */
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
    if (itsShortDesc.length()>theirMaxShortDescLen) {
      itsShortDesc=itsShortDesc.substring(theirMaxShortDescLen);
    }
  }

  /** Get the short description. */
  public String getShortDesc() {
    return itsShortDesc;
  }

  /**
   * Return the units of the monitor point's value. This string may be null if
   * the point has no units.
   */
  public String getUnits() {
    return itsUnits;
  }

  /** Specify the units of the monitor point's value. */
  public void setUnits(String units) {
    itsUnits = units;
  }
  
  /** Populate all point fields and manipulate any perform any other operations to
   * make the point active on the server. */
  public void
  populateServerFields()
  {
    makeTransactions();
    makeTranslations();
    makeArchivePolicies();
    makeLimits();
    //Assign to appropriate ExternalSystem(s) for data collection
    for (int i = 0; i < itsInputTransactions.length; i++) {
      Transaction t = itsInputTransactions[i];
      if (t != null && !t.getChannel().equals("NONE")) {
        ExternalSystem ds = ExternalSystem.getExternalSystem(t.getChannel());
        if (ds != null) {
          ds.addPoint(this);
          // System.err.println("PointDescription:addPoint: OK for "
          // + pm + " (" + t.getChannel() + ")");
        } else {
          System.err.println("PointDescription:addPoint: No ExternalSystem for "
                  + this + " (" + t.getChannel() + ")");
        }
      }
    }
    
    //Assign archiver for this point
    setArchiver(MonitorMap.getPointArchiver());
  }

  /** Populate any point fields which may be required on the client. */
  public void
  populateClientFields()
  {
    makeLimits(); 
  }
  
  /**
   * Parse a line from the point definitions file and generate all the
   * points defined there. The returned array may be null if the line
   * does not define any active sources for the point.
   */
  public static PointDescription[]
  parsePoints(String line)
  {
    line = line.trim();
    if (line.startsWith("#")) {
      //Comment line
      return null;
    } else {
      return PointDescription.parsePoints(line);
    }
  }

  /**
   * Parse a point definitions file and return all the points defined.
   */
  public static ArrayList
  parseFile(String fname)
  {
    try {
      return parseFile(new FileReader(fname));
    } catch (Exception e) {
      System.err.println("PointInteraction:parseFile(): " + e.getClass());
      e.printStackTrace();
      return null;
    }
  }
  
  /**
   * Parse a point definitions file and return all the points defined.
   */
  public static ArrayList<PointDescription>
  parseFile(Reader pointsfile)
  {
    ArrayList<PointDescription> result = new ArrayList<PointDescription>();
    String[] lines = MonitorUtils.parseFile(pointsfile);
    if (lines != null) {
      for (int i = 0; i < lines.length; i++) {
        ArrayList<PointDescription> al = parseLine(lines[i]);
        if (al!=null) {
          result.addAll(al);
        } else {
          System.err.println("PointDescription.parseFile: ERROR parsing line "
                             + i + ": " + lines[i]);
        }
      }
    }
    return result;
  }

  public static ArrayList<PointDescription> parseLine(String line) {
    //Number of tokens we expect for each point definition
    final int NUMTOKENS = 13;
    
    ArrayList<PointDescription> result = new ArrayList<PointDescription>();
    try {
      // Extract appropriate information and make point/s
      String[] toks = MonitorUtils.getTokens(line);
      if (toks.length!=NUMTOKENS) {
        return null;
      }
      
      String[] pointNameArray = getTokens(toks[0]);
      String pointLongDesc = toks[1];
      String pointShortDesc = toks[2];
      String pointUnits = toks[3];
      String[] pointSourceArray = getTokens(toks[4]);
      String pointEnabled = toks[5];
      String[] pointInputArray = getTokens(toks[6]);
      String[] pointOutputArray = getTokens(toks[7]);
      String[] pointTranslateArray = getTokens(toks[8]);
      String[] pointLimitsArray = getTokens(toks[9]);
      String[] pointArchiveArray = getTokens(toks[10]);
      String pointPeriod = toks[11];
      String archiveLife = toks[12];

      boolean[] pointEnabledArray = parseBoolean(pointEnabled);
      if (pointEnabled.length() < pointSourceArray.length) {
        boolean[] temp = new boolean[pointSourceArray.length];
        for (int i = 0; i < temp.length; i++) {
          temp[i] = pointEnabledArray[0];
        }
        pointEnabledArray = temp;
      }

      for (int i = 0; i < pointSourceArray.length; i++) {
          result.add(PointDescription.factory(pointNameArray, pointLongDesc,
              pointShortDesc, pointUnits, pointSourceArray[i], pointInputArray,
              pointOutputArray, pointTranslateArray, pointLimitsArray,
              pointArchiveArray, pointPeriod, archiveLife, pointEnabledArray[i]));
      }

    } catch (Exception e) {
      result = null;
    }
    return result;
  }
  
  
  /** Converts a TTFT string into the appropriate array */
  public static boolean[] parseBoolean(String token)
  {
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

  /** Breaks a line into tokens */
  protected static String[] getTokens(String line)
  {
      StringTokenizer tok = new StringTokenizer(line,", \t\r\n");
      String[] result = new String[tok.countTokens()];
      for (int i = 0; i < result.length; i++) {
       result[i] = tok.nextToken().trim();
     }
      return result;
  }
  
  /** Construct a new monitor point from the given fields. */
  public static PointDescription factory(String[] names, String longdesc, String shortdesc, 
      String units, String source, String[] inputs, String[] outputs,
      String[] translate, String[] limits, String[] archive, String period, String archivelife,
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
    result.setLimitsString(limits);
    result.setArchiveString(archive);
    result.setArchiveLongevity(archivelife);
    result.setPeriod(period);
    result.setEnabled(enabled);
    addPoint(result);
    return result;
  }

  /** OK, maybe data has been collected */
  public synchronized void firePointEvent(PointEvent pe) {
    if (pe.isRaw()) {
      // This is a raw event, we need to translate the data
      PointData data = pe.getPointData();
      // Don't translate if there was nothing to translate
      if (data != null) {
        for (int i = 0; i < itsTranslations.length; i++) {
          try {
            // Apply the next translation
            data = itsTranslations[i].translate(data);
          } catch (Throwable e) {
            System.err.println("PointDescription:firePointevent: Translation Error:"
                    + e.getMessage());
            System.err.println("\tPOINT = " + getSource() + "."
                + getName());
            System.err.println("\tTRANSLATION = "
                + itsTranslations[i].getClass());
            System.err.println("\tEXCEPTION = " + e);
            data = null;
          }
          // If null was returned then stop translation process
          if (data == null) {
            break;
          }
        }
      }
      // Translation has been completed so prepare new event and fire
      pe = new PointEvent(this, data, false);
    }

    PointData data = pe.getPointData();
    if (data != null) {
      // Add the updated value to the archive + data buffer
      if (data != null) {
        PointBuffer.updateData(this, data);
      }
      if (data.getData() != null && itsArchiver != null && itsEnabled) {
        // Archive data?
        for (int i = 0; i < itsArchive.length; i++) {
          if (itsArchive[i].newData(data)) {
            itsArchiver.archiveData(this, data);
            break;
          }
        }
      }
      
      // Schedule the next collection
      if (itsPeriod > 0) {
        itsNextEpoch = data.getTimestamp().getValue() + itsPeriod;
      }

      //Perform any required output transactions
      if (data.isValid()) {
      if (itsOutputTransactions!=null && itsOutputTransactions.length>0) {
        for (int i=0; i<itsOutputTransactions.length; i++) {
          Transaction thistransaction = itsOutputTransactions[i];
          if (!(thistransaction instanceof TransactionNONE)) {
            //Find the ExternalSystem responsible for handling this control operation
            ExternalSystem ds = ExternalSystem.getExternalSystem(thistransaction.getChannel());
            if (ds==null) {
              System.err.println("PointDescription (" + getFullName() + "): No ExternalSystem for output Transaction channel " + thistransaction.getChannel());
            } else {
              //System.err.println("PointDescription.firePointEvent (" + getFullName() + "): Using ExternalSystem " + ds.getName() + " for output");
              try {
                ds.putData(this, data);
              } catch (Exception e) {
                System.err.println("ExternalSystem " + ds.getName() + " threw exception \"" + e + "\" for " + getFullName());
                e.printStackTrace();
              }
            }
          }
        }
      }
      }
      
      // Pass the event on to all listeners
      Object[] listeners = itsPLList.getListenerList();
      for (int i = 0; i < listeners.length; i += 2) {
        if (listeners[i] == PointListener.class) {
          ((PointListener) listeners[i + 1]).onPointEvent(this, pe);
        }
      }
    } else {
      // Schedule the next collection
      if (itsPeriod > 0) {
        itsNextEpoch = (new AbsTime()).getValue() + itsPeriod;
      }
    }
    itsCollecting = false;
  }

  /**
   * Specify the PointArchiver to archive data for this point.
   * 
   * @param archiver
   *          The PointArchiver to be used.
   */
  public void setArchiver(PointArchiver archiver) 
  {
    itsArchiver = archiver;
  }

  /**
   * Return the PointArchiver which archives data for this point.
   */
  public PointArchiver getArchiver() 
  {
    return itsArchiver;
  }

  /**
   * Converts this point into a string which can be used to re-create an
   * identical point
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
    res.append(itsLimitsString);
    res.append(' ');
    res.append(itsArchiveString);
    res.append(' ');
    res.append(itsPeriod);
    res.append(' ');
    res.append(itsArchiveLongevity);
    
    return res.toString();
  }

  /** Get a basic string representation. */
  public String toString() {
    return "{" + itsSource + "." + itsNames[0] + " "
        + getNextEpoch_AbsTime().toString(AbsTime.Format.UTC_STRING) + "}";
  }

  
  /** Map of all points (including aliases) indexed by name. */
  private static TreeMap<String,PointDescription> theirPoints = new TreeMap<String,PointDescription>();
  /** Map of all points (excluding aliases) indexed by name. */
  private static TreeMap<String,PointDescription> theirUniquePoints = new TreeMap<String,PointDescription>();

  /** Add a new point to the running system. */
  public static synchronized void addPoint(PointDescription pm)
  {
    String[] names = pm.getFullNames();
    theirUniquePoints.put(names[0], pm);
    for (int i = 0; i < names.length; i++) {
      theirPoints.put(names[i], pm);
    }
  }
  
  /** Returns all the point names (including aliases) in the system */
  public static synchronized String[] getAllPointNames()
  {
     return MonitorUtils.toStringArray(theirPoints.keySet().toArray());
  }

  /** Returns all the point names (excluding aliases) in the system */
  public static synchronized String[] getAllUniqueNames()
  {
     return MonitorUtils.toStringArray(theirUniquePoints.keySet().toArray());
  }
  
  /** Get the point with the specified name. */
  public static synchronized PointDescription getPoint(String name)
  {
     return theirPoints.get(name);
  }

  /** Get all points (including aliases). */
  public static synchronized PointDescription[] getAllPoints()
  {
    return (PointDescription[])theirPoints.values().toArray();
  }
  
  /** Get all points (excluding aliases). */
  public static synchronized PointDescription[] getAllUniquePoints()
  {
    return (PointDescription[])theirUniquePoints.values().toArray();
  }
  
  /** Check if the point with the specified name exists */
  public static
  boolean
  checkPointName(String name)
  {
    if (theirPoints.containsKey(name)) {
      return true;
    } else {
      return false;
    }
  }
}
