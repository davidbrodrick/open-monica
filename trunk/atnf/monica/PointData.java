// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon;

import atnf.atoms.time.*;
import java.io.*;

/**
 * Class: PointData
 * <P>Description: Used to store data for a monitor/control point
 * @author Le Cuong Nguyen
 * @author David Brodrick
 **/
public class PointData implements Serializable
{
  /** Timestamp for when the data was collected. */
  private AbsTime itsTimestamp = null;

  /** The translated real-world data value. */
  private Object itsData = null;

  /** The pre-translation raw data value. */
  private Object itsRaw = null;

  /** Name of the monitor point that we contain data for. */
  private String itsName = null;

  /** Name of the source from which this data were collected. */
  private String itsSource = null;

  /** The sequence number for this data. The sequence number is incremented
   * by the monitor server/data collector each time a new value is
   * read for the point. */
  private int itsSequence = 0;
   
  public
  PointData()
  {
  }

  public
  PointData(String name,
	    String source,
	    AbsTime timestamp,
	    Object raw,
	    Object data,
	    int sequence)
  {
    itsName = name;
    itsSource = source;
    itsTimestamp = timestamp;
    itsRaw = raw;
    itsData = data;
    itsSequence = sequence;
  }

  public
  PointData(String name,
	    String source,
	    AbsTime timestamp,
	    Object raw,
	    Object data)
  {
    itsName = name;
    itsSource = source;
    itsTimestamp = timestamp;
    itsRaw = raw;
    itsData = data;
  }


  public
  PointData(String name,
	    String source,
	    AbsTime timestamp,
	    Object data,
	    int sequence)
  {
    itsName = name;
    itsSource = source;
    itsTimestamp = timestamp;
    itsData = data;
    itsRaw = data;
    itsSequence = sequence;
  }


   public PointData(String name, AbsTime timestamp, Object data, int sequence)
   {
      itsName = name;
      itsTimestamp = timestamp;
      itsData = data;
      itsRaw = data;
      itsSequence = sequence;
   }

   public PointData(String name, String source,
		    AbsTime timestamp, Object data)
   {
     itsName = name;
     itsSource = source;
     itsTimestamp = timestamp;
     itsData = data;
     itsRaw = data;
   }


   public PointData(String name, AbsTime timestamp, Object data)
   {
     itsName = name;
     itsTimestamp = timestamp;
     itsData = data;
     itsRaw = data;
   }

   public PointData(String name, String source, AbsTime timestamp)
   {
     itsName = name;
     itsSource = source;
     itsTimestamp = timestamp;
     itsData = null;
     itsRaw = null;
   }

   public PointData(String name, String source)
   {
     itsName = name;
     itsSource = source;
     itsTimestamp = AbsTime.factory();
     itsData = null;
   }

   public PointData(String name, String source, Object data)
   {
     itsName = name;
     itsSource = source;
     itsTimestamp = AbsTime.factory();
     itsData = data;
   }

   public PointData(Object data)
   {
     itsData = data;
     itsTimestamp = AbsTime.factory();
   }


   /** Specify the name of the monitor point for which we hold data. */
   public
   void
   setName(String name)
   {
     itsName = name;
   }

   /** Specify the name of the source that this data relates to. */
   public
   void
   setSource(String source)
   {
     itsSource = source;
   }

   /** Specify the timestamp for the data value we hold. */
   public
   void
   setTimestamp(AbsTime timestamp)
   {
     itsTimestamp = timestamp;
   }

   /** Specify the actual data to be stored. */
   public
   void
   setData(Object data)
   {
     itsData = data;
   }

   /** Specify the raw data value to be stored. */
   public
   void
   setRaw(Object raw)
   {
     itsRaw = raw;
   }

   /** Specify the sequence number for this data value. */
   public
   void
   setSequence(int sequence)
   {
     itsSequence = sequence;
   }

   /** Get the name of the monitor point for which we hold data. */
   public
   String
   getName()
   {
     return itsName;
   }

   /** Get the name of the source that this data relates to. */
   public
   String
   getSource()
   {
     return itsSource;
   }

   /** Get the timestamp for the data value we hold. */
   public
   AbsTime
   getTimestamp()
   {
     return itsTimestamp;
   }

  /** Get the raw data stored by this PointData. The raw value is the
   * original value of this point prior to value translation. */
   public
   Object
   getRaw()
   {
     if (itsRaw!=null) return itsRaw;
     else return itsData;
   }

   /** Get the actual translated data stored by this PointData. */
   public
   Object
   getData()
   {
     return itsData;
   }

   /** Get the sequence number for this value. The sequence number is
    * incremented by the monitor server/data collector each time a new
    * value is collected for the monitor point. The sequence number will
    * be useful for checking to see if a client is updating too infrequently
    * and is consequently failing to collect every data value. */
   public
   int
   getSequence()
   {
      return itsSequence;
   }

   /** Check if the data field contains valid data. This simply checks to see
    * if the data field is null, a null data field indicates that there was
    * some problem updating the value for this point and that we no longer
    * have a valid value to display for the point. */
   public
   boolean
   isValid()
   {
     if (itsData==null) return false;
     else return true;
   }

   /** Get a String representation of this Object. */
   public
   String
   toString()
   {
     return "{" + getSource() + "." + getName() + " "
       + getTimestamp().toString(AbsTime.Format.UTC_STRING)
       + " " + getSequence() + " " + getRaw() + " -> " + getData() + "}";
   }
}
