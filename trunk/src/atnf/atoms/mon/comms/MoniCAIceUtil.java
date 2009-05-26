//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.comms;

import java.util.Vector;
import atnf.atoms.mon.*;
import atnf.atoms.time.*;

/** Contains various static methods for converting between native MoniCA data types and
 * their Ice representations.
 * 
 * @author David Brodrick */
public class MoniCAIceUtil {
  /** Convert the points to their Ice representation. */
  public static 
  PointDescriptionIce[] 
  getPointDescriptionsAsIce(PointDescription[] points)
  {
    if (points==null || points.length==0) {
      return new PointDescriptionIce[0];
    }
    PointDescriptionIce[] res = new PointDescriptionIce[points.length];
    for (int i=0; i<points.length; i++) {
      res[i]=getPointDescriptionAsIce(points[i]);
    }
    return res;
  }
  
  /** Convert the point to its Ice representation. */
  public static 
  PointDescriptionIce 
  getPointDescriptionAsIce(PointDescription pd)
  {
    String[] names = pd.getAllNames();
    String source = pd.getSource();
    String description = pd.getLongDesc();
    String shortdescription = pd.getShortDesc();
    String units = pd.getUnits();
    boolean enabled = pd.getEnabled();
    String[] inputtransactions = pd.getInputTransactionsAsStrings();
    String[] outputtransactions = pd.getOutputTransactionsAsStrings();
    String[] translations = pd.getTranslationsAsStrings();
    String[] limits = pd.getLimitsAsStrings();
    String[] archivepolicies = pd.getArchivePoliciesAsStrings();
    long period = pd.getPeriod();
    int archivelongevity = pd.getArchiveLongevity();
    return new PointDescriptionIce(names, source, description, shortdescription, units, enabled, inputtransactions, outputtransactions, translations, limits, archivepolicies, period, archivelongevity);
  }
  
  /** Convert the point from its Ice representation. */
  public static 
  Vector<PointDescription> 
  getPointDescriptionsFromIce(PointDescriptionIce[] points)
  {
    if (points==null || points.length==0) {
      return null;
    }
    Vector<PointDescription> res = new Vector<PointDescription>(points.length);
    for (int i=0; i<points.length; i++) {
      res.add(getPointDescriptionFromIce(points[i]));
    }
    return res;
  }
  
  /** Convert the point from its Ice representation. */
  public static 
  PointDescription
  getPointDescriptionFromIce(PointDescriptionIce pd)
  {
    if (pd==null) return null;
    PointDescription res = PointDescription.factory(pd.names, pd.description, 
        pd.shortdescription, pd.units, pd.source, pd.inputtransactions, 
        pd.outputtransactions, pd.translations, pd.limits, pd.archivepolicies, 
        ""+pd.period, ""+pd.archivelongevity, pd.enabled);
    //Populate the appropriate data structures for client-side use
    res.populateClientFields();
    return res;
  }
  
  
  /** Convert the data to its Ice representation. */
  public static
  PointDataIce[]
  getPointDataAsIce(PointData[] data)
  {
    if (data==null || data.length==0) {
      return new PointDataIce[0];
    }   
    PointDataIce[] res = new PointDataIce[data.length];
    for (int i=0; i<data.length; i++) {
      res[i]=getPointDataAsIce(data[i]);
    }
    return res;    
  }

  /** Convert the data to its Ice representation. */
  public static
  PointDataIce[]
  getPointDataAsIce(Vector<PointData> data)
  {
    if (data==null || data.size()==0) {
      return new PointDataIce[0];
    }
    PointDataIce[] res = new PointDataIce[data.size()];
    for (int i=0; i<data.size(); i++) {
      res[i]=getPointDataAsIce(data.get(i));
    }
    return res;    
  }
 
  
  /** Convert the data to its Ice representation. */
  public static
  PointDataIce
  getPointDataAsIce(PointData pd)
  {
    if (pd==null) return null;
    //Have to convert to string/type pair for cross language compatibility
    DataValue value;
    Object data = pd.getData();
    //Lookup type of data object and assign appropriate values
    if (data instanceof Float) {
      value=new DataValueFloat(DataType.DTFloat, ((Number)data).floatValue());
    } else if (data instanceof Double) {
      value=new DataValueDouble(DataType.DTDouble, ((Number)data).doubleValue());
    } else if (data instanceof Long) {
      value=new DataValueLong(DataType.DTLong, ((Number)data).longValue());     
    } else if (data instanceof Number) {
      value=new DataValueInt(DataType.DTInt, ((Number)data).intValue());
    } else if (data instanceof String) {
      value=new DataValueString(DataType.DTString, (String)data);
    } else if (data instanceof Boolean) {
      value=new DataValueBoolean(DataType.DTBoolean, ((Boolean)data).booleanValue());      
    } else if (data instanceof AbsTime) {
      value=new DataValueAbsTime(DataType.DTAbsTime, ((AbsTime)data).getValue());      
    } else if (data instanceof RelTime) {
      value=new DataValueRelTime(DataType.DTRelTime, ((RelTime)data).getValue());      
    } else {
      value=new DataValue(DataType.DTNull);      
    }
    return new PointDataIce(pd.getName(), pd.getTimestamp().getValue(), value);
  }

  /** Convert data from Ice representation. */
  public static
  Vector<PointData>
  getPointDataFromIce(PointDataIce[] data)
  {
    if (data==null || data.length==0) {
      return null;
    }
    Vector<PointData> res = new Vector<PointData>(data.length);
    for (int i=0; i<data.length; i++) {
      res.add(getPointDataFromIce(data[i]));
    }
    return res;    
  }
  
  /** Convert data from Ice representation. */
  public static
  PointData
  getPointDataFromIce(PointDataIce icedata)
  {
    String name = icedata.name;
    AbsTime ts = AbsTime.factory(icedata.timestamp);
    Object value;
    if (icedata.value.type==DataType.DTFloat) {
      value = new Float(((DataValueFloat)icedata.value).value);
    } else if (icedata.value.type==DataType.DTDouble) {
      value = new Double(((DataValueDouble)icedata.value).value);
    } else if (icedata.value.type==DataType.DTInt) {
      value = new Integer(((DataValueInt)icedata.value).value);
    } else if (icedata.value.type==DataType.DTLong) {
      value = new Long(((DataValueLong)icedata.value).value);      
    } else if (icedata.value.type==DataType.DTString) {
      value = ((DataValueString)icedata.value).value;
    } else if (icedata.value.type==DataType.DTBoolean) {
      value = new Boolean(((DataValueBoolean)icedata.value).value);
    } else if (icedata.value.type==DataType.DTAbsTime) {
      value = AbsTime.factory(((DataValueAbsTime)icedata.value).value);
    } else if (icedata.value.type==DataType.DTRelTime) {
      value = RelTime.factory(((DataValueRelTime)icedata.value).value);
    } else {
      value = null;
    }
    return new PointData(name, ts, value);
  }
  
  /** Default port. */
  protected static final int theirDefaultPort = 8052;
  
  /** Get the default port for client server communication. */
  public static
  int
  getDefaultPort()
  {
    return theirDefaultPort;
  }
}
