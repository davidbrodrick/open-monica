// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.archiver;

import java.sql.*;
import java.util.*;

import atnf.atoms.mon.*;
import atnf.atoms.util.*;
import atnf.atoms.time.*;

/**
 * Monitor point archiver which uses a MySQL database as the backend.
 *
 * <P>In the interest of speed and space efficiency each monitor point has 
 * its own table in the database, which is derived from the source and monitor
 * point name with .'s replaced by $'s so as not to conflict with the SQL
 * syntax.
 *
 * <P>Since MoniCA does not require strict typing for values of a particular
 * monitor point, each record includes a type and value specifier. The value
 * is stored as a VARCHAR(255) which means it may not exceed 255 bytes when
 * expressed as a string.
 *
 * <P>The <tt>bin/setupMySQL.sh</tt> script provided will create the user and
 * database required for the archiver to operate.
 *
 * @author David Brodrick
 */
public class PointArchiverMySQL
extends PointArchiver
{
  /** The connection to the MySQL server. */  
  protected Connection itsConnection = null;
  
  /** The RUL to connect to the server/database. */
  protected String itsURL = "jdbc:mysql://localhost/MoniCA?user=monica&useTimezone=false";

  
  /** Constructor. */
  public
  PointArchiverMySQL()
  {
    super();
    
    try {
      itsConnection=DriverManager.getConnection(itsURL);
    } catch (Exception e) {
      System.err.println("PointArchiverMySQL: " + e.getMessage());
      itsConnection=null;
    }
  }
  
  
  /** Method to do the actual archiving.
   * @param pm The point whos data we wish to archive.
   * @param data Vector of data to be archived. */
  protected
  void
  saveNow(PointMonitor pm, Vector data)
  {
    String table = getTableName(pm);
    
    for (int i=0; i<data.size(); i++) {
      insertData(table, (PointData)data.get(i));
    }
  }


  /** Extract data from the archive.
   * @param pm Point to extract data for.
   * @param start Earliest time in the range of interest.
   * @param end Most recent time in the range of interest.
   * @return Vector containing all data for the point over the time range. */
  public
  Vector
  extract(PointMonitor pm, AbsTime start, AbsTime end)
  {
    try {
      //Can't do anything if the server is not running
      if (!checkConnection()) return null;
      
      //Get the table name for this point
      String table=getTableName(pm);
    
      //Build and execute the data request
      String cmd = "SELECT * from " + table + " WHERE ts>="
                   + start.getValue() + " AND ts<="
                   + end.getValue() + " ORDER BY ts "
                   + "LIMIT " + MAXNUMRECORDS + ";";
      //System.err.println(cmd);
      Statement stmt = itsConnection.createStatement();
      stmt.execute(cmd);
    
      //Get the results
      Vector res=new Vector(5000, 10000);
      ResultSet rs = stmt.getResultSet();

      //Ensure we got some data
      if (!rs.first()) return null;
      do {
        PointData pd=getPointDataForRow(pm, rs);
        if (pd!=null) res.add(pd);
      } while (rs.next());
      
      //Finished - return the extracted data
      return res;
    } catch (Exception e) {
      System.err.println("PointArchiverMySQL:extract: " + e.getMessage());
      return null;
    }
  }


  /** Return the last update which preceeds the specified time.
   * We interpret 'preceeds' to mean data_time<=req_time.
   * @param pm Point to extract data for.
   * @param ts Find data preceeding this timestamp.
   * @return PointData for preceeding update or null if none found. */
  public
  PointData
  getPreceeding(PointMonitor pm, AbsTime ts)
  {
    try {
      //Can't do anything if the server is not running
      if (!checkConnection()) return null;
      
      //Get the table name for this point
      String table=getTableName(pm);
    
      //Build and execute the data request
      String cmd = "SELECT * from " + table + " WHERE ts<="
                   + ts.getValue() + " ORDER BY TS DESC LIMIT 1;";
      //System.err.println(cmd);
      Statement stmt = itsConnection.createStatement();
      stmt.execute(cmd);
    
      //Get the results
      ResultSet rs = stmt.getResultSet();
      //Ensure we got some data
      if (!rs.first()) return null;
      PointData res=getPointDataForRow(pm, rs);
      //Finished - return the extracted data
      return res;
    } catch (Exception e) {
      System.err.println("PointArchiverMySQL:getPreceeding: " + e.getMessage());
      return null;
    }    
  }


  /** Return the first update which follows the specified time.
   * We interpret 'follows' to mean data_time>=req_time.
   * @param pm Point to extract data for.
   * @param ts Find data following this timestamp.
   * @return PointData for following update or null if none found. */
  public
  PointData
  getFollowing(PointMonitor pm, AbsTime ts)
  {
        try {
      //Can't do anything if the server is not running
      if (!checkConnection()) return null;
      
      //Get the table name for this point
      String table=getTableName(pm);
    
      //Build and execute the data request
      String cmd = "SELECT * from " + table + " WHERE ts>="
                   + ts.getValue() + " ORDER BY TS LIMIT 1;";
      //System.err.println(cmd);
      Statement stmt = itsConnection.createStatement();
      stmt.execute(cmd);
    
      //Get the results
      ResultSet rs = stmt.getResultSet();
      //Ensure we got some data
      if (!rs.first()) return null;
      PointData res=getPointDataForRow(pm, rs);
      //System.err.println(res.toString());
      //Finished - return the extracted data
      return res;
    } catch (Exception e) {
      System.err.println("PointArchiverMySQL:getPreceeding: " + e.getMessage());
      return null;
    }
  }


  /** Build a PointData from the database row.
   * @param pm Point the data belongs to.
   * @param rs The database record/ResultSet.
   * @return PointData representing the data. null if error. */
  protected
  PointData
  getPointDataForRow(PointMonitor pm, ResultSet rs)
  {
    PointData res=null;
    try {
      AbsTime ts   = AbsTime.factory(rs.getLong(1));
      String  type = rs.getString(2);
      String  val  = rs.getString(3);
      Object  oval = getObjectForString(type, val);
      res=new PointData(pm.getName(), pm.getSource(), ts, oval);
    } catch (Exception e) {
      res=null;
    }
    return res;
  }
  

  /** Insert the given data into the table.
   * @param table Name of the table. 
   * @param data The data to insert. */
  protected
  void
  insertData(String table, PointData data)
  {
    try {
      //Get a string representation of the object type and value
      String typeval=null;
      try {
        typeval=getStringForObject(data.getData());
      } catch (IllegalArgumentException e) {
        //Can't save this object
        System.err.println("PointArchiverMySQL:insertData: " + e.getMessage());
        return;
      }
      String cmd = "INSERT INTO " + table + " VALUES(" +
                   data.getTimestamp().getValue() +
                   ", " + typeval + ");";
      Statement stmt = itsConnection.createStatement();
      stmt.execute(cmd);
    } catch (Exception e) {
      System.err.println("PointArchiverMySQL:insertData: " + e.getMessage());
      checkConnection();
      createTable(table);
    }
  }
  
  
  /** Create the specified table if it doesn't already exist.
   * @param table Name of the table to create. */
  protected
  void
  createTable(String table)
  {
    try {
      System.err.println("PointArchiverMySQL:createTable: Creating " + table);
      Statement stmt = itsConnection.createStatement();
      stmt.execute("CREATE table if not exists " + table +
                   "(ts BIGINT, type CHAR(4), val VARCHAR(255), " +
                   "PRIMARY KEY(`ts`)) ENGINE = MyISAM;");
    } catch (Exception e) {
      System.err.println("PointArchiverMySQL:createTable: " + e.getMessage());
    }
  }


  /** Check if we are connected to the server and reconnect if required.
   @return True if connected (or reconnected). False if not connected. */
  protected
  boolean
  checkConnection()
  {
    boolean res=false;
    try {
      //Do a minimal query to see if connection is valid
      //From Java 1.6 we could use itsConnection.isValid(1)
      if (itsConnection!=null) {
        try {
          Statement stmt = itsConnection.createStatement();
          stmt.execute("select 1;");
        } catch (Exception f) {
          itsConnection=null;
        }
      }
      if (itsConnection==null) {
        itsConnection=DriverManager.getConnection(itsURL);
      } else {
        res=true;
      }
    } catch (Exception e) {
      System.err.println("PointArchiverMySQL:checkConnection: " + e.getMessage());
      itsConnection=null;
    }
    return res;
  }
    
  
  /** Return the SQL table name for given monitor point.
   * @param pm Point to get the table name for.
   * @return String containing SQL table name. */
  protected
  String
  getTableName(PointMonitor pm)
  {
    String rawname = pm.getSource() + "$" + pm.getName();
    //Translate .'s to $'s so as not to conflict with SQL syntax
    return rawname.replace('.', '$');
  }  
  
  
  /** Get a string representation of the Object. The string includes a type
   * specifier as well as an ASCII representation of the data. These fields
   * are separated by tabs. The <i>getObjectForString</i> method is able
   * to decode this representation and recover the original Object.
   * <P><i>null</i> objects are properly handled.
   * @param data The Object to encode into ASCII text. 
   * @return An ASCII String representation of the data. */
  protected
  String
  getStringForObject(Object data)
  throws IllegalArgumentException
  {
    String res = null;
    if (data==null) {
      res = "NULL, NULL";
    } else if (data instanceof Double) {
      res = "'dbl', '" + ((Double)data).doubleValue() + "'";
    } else if (data instanceof Float) {
      res = "'flt', '" + ((Float)data).floatValue() + "'";
    } else if (data instanceof Integer) {
      res = "'int', '" + ((Integer)data).intValue() + "'";  
    } else if (data instanceof String) {
      res = "'str', '" + (String)data + "'";
    } else if (data instanceof HourAngle) {
      res = "'hr', '" + ((Angle)data).getValue() + "'";
    } else if (data instanceof Angle) {
      res = "'ang', '" + ((Angle)data).getValue() + "'";
    } else if (data instanceof Boolean) {
      res = "'bool', '" + ((Boolean)data).booleanValue() + "'";
    } else if (data instanceof Short) {
      res = "'shrt', '" + ((Short)data).shortValue() + "'";
    } else if (data instanceof Long) {
      res = "'long', '" + ((Long)data).longValue() + "'";
    } else if (data instanceof AbsTime) {
      res = "'abst', '" + ((AbsTime)data).toString(AbsTime.Format.HEX_BAT) + "'";
    } else if (data instanceof RelTime) {
      res = "'relt', '" + ((RelTime)data).toString(RelTime.Format.DECIMAL_BAT) + "'";
    //} else if (data instanceof BigInteger) {
    //  res = "'big', '" + ((BigInteger)data).toString() + "'";
    } else {
      //Unhandled data type
      throw new IllegalArgumentException("Unsupported Type: "	+ data.getClass());
    }
    return res;
  }


  /** Use the ASCII <i>type</i> and <i>data</i> to reconstruct the data
   * Object. This method essentially performs the opposite procedure to that
   * implemented by <i>getStringForObject</i>.
   * @param type Short string representing the class of the data.
   * @param data The actual data in ASCII text form.
   * @return The reconstructed object. */
  protected
  Object
  getObjectForString(String type, String data)
  {
    Object res = null;
    if (type.equals("dbl")) {
      res = new Double(data);
    } else if (type.equals("flt")) {
      res = new Float(data);
    } else if (type.equals("int")) {
      res = new Integer(data);
    } else if (type.equals("str")) {
      res = data;  
    } else if (type.equals("ang")) {
      res = Angle.factory(data);
    } else if (type.equals("hr")) {
      res = new HourAngle(Double.parseDouble(data));
    } else if (type.equals("bool")) {
      res = new Boolean(data);
    } else if (type.equals("shrt")) {
      res = new Short(data);
    } else if (type.equals("long")) {
      res = new Long(data);
    } else if (type.equals("abst")) {
      long foo = Long.parseLong(data,16); //Hex
      res = AbsTime.factory(foo);
    } else if (type.equals("relt")) {
      long foo = Long.parseLong(data); //Decimal
      res = RelTime.factory(foo);
    //} else if (type.equals("big")) {
    //  res = new BigInteger(data);
    } else {
      System.err.println("PointArchiverMySQL: Parse error at \""
                         + type + "\"");
      res = null;
    }
    return res;
  }  
}
