//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui;

import atnf.atoms.mon.client.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.limit.*;
import atnf.atoms.time.*;

import javax.swing.table.AbstractTableModel;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.Component;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Vector;
import java.util.Collection;
import java.io.PrintStream;


/**
 * @author David Brodrick
 * @version $Id: PointTableModel.java,v 1.7 2006/02/07 22:05:52 bro764 Exp $
 * @see DataMaintainer
 */
public class PointTableModel
extends AbstractTableModel
implements PointListener, TableCellRenderer
{
  static {
    AbsTime.setTheirDefaultFormat(AbsTime.Format.UTC_STRING);
    RelTime.setTheirDefaultFormat(RelTime.Format.BRIEF);
  }

  /** Names of all PointInteractions to be displayed in the table.
   * A null entry in this Vector is interpreted as indicating that
   * we should display an empty row at that position to improve the
   * layout and readability of the table. */
  protected Vector itsPoints = new Vector();

  /** Names of sources selected for display in the table. */
  protected Vector itsSources  = new Vector();

  /** Determines whether we display short names or long names for the
   * points. If this is <tt>true</tt> we use short names, otherwise we use
   * the long names for the monitor points. */
  protected boolean itsUseShortNames = false;


  public
  PointTableModel()
  {
    super();
  }


  public
  PointTableModel(Vector points, Vector sources)
  {
    super();
    set(points, sources);
  }


  /**  */
  public
  Vector
  getPoints()
  {
    return itsPoints;
  }


  /**  */
  public
  Vector
  getSources()
  {
    return itsSources;
  }


  /** */
  public
  boolean
  getUseShortNames()
  {
    return itsUseShortNames;
  }


  /** */
  public
  void
  setUseShortNames(boolean useshort)
  {
    itsUseShortNames = useshort;
    fireTableStructureChanged(); //Is there a better event?
  }


  /** */
  public
  void
  set(Vector points, Vector sources, boolean useshort)
  {
    itsUseShortNames = useshort;
    set(points, sources);
  }


  /** Cease displaying any currently selected points and start displaying
   * the points contained in the specified page. */
  public
  void
  set(Vector points, Vector sources)
  {
    //First unsubscribe from the old points, if any
    if (itsPoints!=null && itsPoints.size()>0) {
      for (int i=0; i<itsPoints.size(); i++) {
	String pname = (String)itsPoints.get(i);
	if (pname==null || pname.equals("")) continue; //Blank row
	for (int j=0; j<itsSources.size(); j++) {
	  DataMaintainer.unsubscribe(pname,
				     (String)itsSources.get(j),
				     this);
	}
      }
    }

    //Record new settings
    itsPoints  = points;
    itsSources = sources;
    fireTableStructureChanged();

    if (itsPoints!=null && itsPoints.size()>0) {
      //This stage might involve some slow network I/O.
      //Rather than hang the GUI, use a thread to do the work
      final PointTableModel themodel = this;
      Thread realWork = new Thread() {
	public void run() {
	  //Build vector containing all point names
	  ///This is pretty dumb since not all sources will have all points
	  Vector pnames = new Vector();
	  for (int i=0; i<itsPoints.size(); i++) {
	    String pname = (String)itsPoints.get(i);
	    if (pname==null || pname.equals("")) continue; //Blank row
	    for (int j=0; j<itsSources.size(); j++) {
	      pnames.add((String)itsSources.get(j) + "." + pname);
	    }
	  }

	  //Subscribe to each of the points
	  DataMaintainer.subscribe(pnames, themodel);

	  //We've finished the slow network I/O, tell GUI to update
	  Runnable tellSwing = new Runnable() {
	    public void run() {
	      //fireTableStructureChanged();
	      //Make table redraw the rows so they get the point names, etc.
	      //fireTableRowsUpdated(0,itsPoints.size()-1);
              fireTableDataChanged();
	    }
	  };
	  try {
	    SwingUtilities.invokeLater(tellSwing);
	  } catch (Exception e) {e.printStackTrace();}
	}
      };
      realWork.start();
    }
  }


  /** Try to come up with sensible column widths. */
  public
  void
  setSizes(JTable table, JScrollPane scroll)
  {
    if (itsPoints!=null && itsPoints.size()>0) {
      //Set sized based on number of rows
      Dimension prefsize = new Dimension(220+80+90*itsSources.size(),
					 18*(itsPoints.size()+1));
      scroll.setPreferredSize(prefsize);
      Dimension maxsize  = new Dimension(220+80+110*itsSources.size(),
                                         22*(itsPoints.size()+1));
      scroll.setMaximumSize(prefsize);
      scroll.setMinimumSize(new Dimension(200,100));

      //Set background colour for unused area - need to access the 'parent'
      Component parent = table.getParent();
      if (parent!=null) {
	parent.setBackground(Color.lightGray);
	if (parent instanceof JComponent) {
          //possibly turned off elsewhere
	  ((JComponent)parent).setOpaque(true);
	}
      }

      TableColumn column = table.getColumnModel().getColumn(getColumnCount()-1);
      column.setPreferredWidth(80);
      column.setMaxWidth(120);
      column.setMinWidth(0);
      column = table.getColumnModel().getColumn(0);
      column.setPreferredWidth(220);
      column.setMinWidth(150);

      //fireTableStructureChanged();
    }
  }


  /** Return the row in which the specified point is being displayed.
   * @return Row index or -1 if there is no row for the named point. */
  protected
  int
  getRowForPoint(String pname)
  {
    int res = -1;
    for (int p=0; p<itsPoints.size(); p++) {
      String thiss = (String)itsPoints.get(p);
      if (thiss!=null && thiss.equals(pname)) {
	res = p;
	break;
      }
    }
    return res;
  }


  /** Return the monitor point structure for the point which is displayed
   * in the specified row. If no point is displayed in the specified row,
   * null will be returned.
   * @param row The row to get the monitor point structure for.
   * @return The monitor point structure for the specified row, or null if
   *  no point is displayed in the specified row. */
  public
  PointMonitor
  getPointForRow(int row)
  {
    PointMonitor res = null;
    if (row>=0 && row<getRowCount()) {
      String pname = (String)itsPoints.get(row);
      if (pname!=null && !pname.equals("")) {
	//Not all sources may have this point, so brute force it until
        //we find a source which has the required point. Pretty dumb.
	for (int s=0; s<itsSources.size(); s++) {
	  PointMonitor temp = null;
	  temp = DataMaintainer.getPointFromMap(pname,
						(String)itsSources.get(s));
	  if (temp!=null) {
            //We found the answer, record it and we're done!
	    res = temp;
	    break;
	  }
	}
      }
    }
    return res;
  }


  /** Return the monitor point structure for the point which is displayed
   * in the specified cell. If no point is displayed there, null will be
   * returned.
   * @param row The row specification for the cell.
   * @param column The column specification for the cell
   * @return The monitor point structure for the specified cell, or null if
   *  no point is displayed in the specified cell. */
  public
  PointMonitor
  getPoint(int row, int column)
  {
    PointMonitor res = null;
    if (row>=0 && row<getRowCount() &&
	column>0 && column<getColumnCount()-1) {
      String pname = (String)itsPoints.get(row);
      if (pname!=null && !pname.equals("")) {
        res = DataMaintainer.getPointFromMap(pname, (String)
					     itsSources.get(column-1));
      }
    }
    return res;
  }


  /** Return the column in which the specified source is being displayed.
   * @return Column index for the given source or -1 if there is no column
   *         for that source. */
  protected
  int
  getColumnForSource(String source)
  {
    int res = -1;
    for (int s=0; s<itsSources.size(); s++) {
      String thiss = (String)itsSources.get(s);
      if (thiss!=null && thiss.equals(source)) {
	res = s+1;
	break;
      }
    }
    return res;
  }


  /** Return the source displayed in the specified column.
   * Will return -1 if there is no source for the column. */
  protected
  String
  getSourceForColumn(int column)
  {
    String res = null;
    if (column-1>=0 && column-1<itsSources.size()) {
      res = (String)itsSources.get(column-1);
    }
    return res;
  }


  /** Return the number of rows in the table. The number of rows will
   * correspond to te number of points we are to display: one point is
   * displayed in each row. We may also have blank rows used for making
   * the table a bit easier to read.
   * @return Number of rows in the table. */
  public
  int
  getRowCount()
  {
    if (itsPoints==null) return 0;
    else return itsPoints.size();
  }


  /** Return the number of columns in the table. This will correspond to the
   * number of information sources selected for display, plus one column
   * for the point name and another column for the units.
   * @return Number of columns in the table. */
  public
  int
  getColumnCount()
  {
    //Column for point name + one column for each source + one for units
    if (itsSources==null) return 2;
    else return itsSources.size() + 2;
  }


  public
  String
  getColumnName(int column)
  {
    String res = null;
    if (column==0) res = "Point";
    else if (column==getColumnCount()-1) res = "Units";
    else res = getSourceForColumn(column);
    return res;
  }


  public
  Object
  getValueAt(int row, int column)
  {
    Object res = null;
    String pname = (String)itsPoints.get(row);
    if (pname==null) {
      //The point for this row is null, leave the row blank for formatting
      return "";
    }

    String source = getSourceForColumn(column);
    if (column==0) {
      //Point name was requested
      PointMonitor pm = getPointForRow(row);
      if (pm!=null) {
	if (itsUseShortNames) {
	  res = pm.getShortDesc();
	} else {
	  res = pm.getLongDesc();
	}
	if (res==null || res.equals("")) {
	  //No useful name is defined, so substitute the points name
	  res = pm.getName();
	}
      }
    } else if (column==getColumnCount()-1) {
      //Units were requested
      res = "";
      PointMonitor pm = getPointForRow(row);
      if (pm!=null && pm.getUnits()!=null) {
	res = new JLabel(pm.getUnits());
      }
    } else {
      PointMonitor pm = getPointForRow(row);
      PointData pd = DataMaintainer.getBuffer(source + "." + pname);
      if (pd!=null && pm!=null) {
	long age = (new AbsTime()).getValue() - pd.getTimestamp().getValue();
        long period = pm.getPeriod();
	if (pd.isValid() && (period==0 || age<5*period)) {
	  res = pd.getData();
	} else {
	  res = new JLabel("?", SwingConstants.CENTER);
	  ((JLabel)res).setToolTipText("Current Value Not Available");
	  ((JLabel)res).setForeground(Color.lightGray);
	  ((JLabel)res).setBackground(Color.white);
	}
      } else {
	res = null;//new JLabel("No Data");
      }
    }
    return res;
  }


  /** Return a useful tool tip for the specified cell. */
  public
  String
  getToolTip(int row, int column)
  {
    String res = null;
    if (column==0) {
      PointMonitor pm = getPointForRow(row);
      if (pm!=null) {
	res = pm.getName();
      }
    } else if (column == getColumnCount()-1) {
      PointMonitor pm = getPointForRow(row);
      if (pm!=null) {
        res = "Units for " + pm.getShortDesc();
      }
    } else {
      PointMonitor pm = getPoint(row, column);
      if (pm!=null) {
	String desc = pm.getLongDesc();
	if (desc==null || desc.equals("")) {
	  //No useful name is defined, so substitute the points name
	  desc = pm.getName();
	}
	res = desc + " for \"" + pm.getSource() + "\"";
      }
    }
    return res;
  }


  /** Called whenever a new value is available for one of the points we are
   * displaying in the table. */
  public
  void
  onPointEvent(Object source, PointEvent evt)
  {
    if (!evt.isRaw()) {
      PointData newval = evt.getPointData();
      if (newval!=null) {
	int row = getRowForPoint(newval.getName());
	int col = getColumnForSource(newval.getSource());
        if (row>=0 && col>=0) fireTableCellUpdated(row, col);
      }
    }
    //System.err.println("PointTableModel: Event!");
  }


  public
  Component
  getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus,
				int row, int column)
  {
    Component res = null;
    if (value==null) return null;

    if (value instanceof Component) res = (Component)value;
    else {
      //System.err.println("Creating new Component!");
      res = new JLabel(value.toString());
    }

    //Check if this monitor point defines it's limits
    PointMonitor pm = getPoint(row, column);
    if (pm!=null) {
      //Limits are defined, get the raw data and check it
      PointData pd = DataMaintainer.getBuffer(pm.getSource() + "." +
					      pm.getName());
      if (pd!=null) {
	long age = (new AbsTime()).getValue() - pd.getTimestamp().getValue();
	long period = pm.getPeriod();
	if (period!=0 && age>2*period && age<5*period) {
	  //The point is old, so alter the foreground color
          res.setForeground(Color.lightGray);
	}

	PointLimit limits = pm.getLimits();
	if (limits!=null) {
	  if (pd.isValid() && (period==0 || age<5*period) && !limits.checkLimits(pd)) {
	    //Don't highlight the point if it has expired
	    //Point is outside of limits, so highlight this cell
	    if (age<3*period) res.setForeground(Color.red);
	    else res.setForeground(Color.orange);
	    if (res instanceof JComponent) ((JComponent)res).setOpaque(true);
	    res.setBackground(Color.yellow);
	  }
	}
      }
    }

    //Make it a different colour if it has been clicked
    //but only if is a data display cell
//    if (hasFocus && column>0 && column<getColumnCount()-1) {
//      res.setForeground(Color.black);
//    }

    //This is how to colour a cell's background
/*    if (row==1 && column==1) {
      //Need this <1.4 because component has been set to not opaque
      if (res instanceof JComponent) ((JComponent)res).setOpaque(true);
      res.setBackground(Color.yellow);
    }*/

    if (res instanceof JComponent && ((JComponent)res).getToolTipText()==null) {
      ((JComponent)res).setToolTipText(getToolTip(row, column));
    }
    return res;
  }


  /** Dump current data to the given output stream. This is the mechanism
   * through which data can be exported to a file.
   * @param p The print stream to write the data to. */
  public synchronized
  void
  export(PrintStream p)
  {
    if (itsSources.size()==0 || itsPoints.size()==0) {
      p.println("#There is no data to be exported.");
      return;
    }

    //Print table header
    String line = "Point";
    for (int i=0; i<itsSources.size(); i++) {
      line += ", " + (String)itsSources.get(i);
    }
    p.println(line);

    //Print table data
    for (int i=0; i<itsPoints.size(); i++) {
      PointMonitor pm = getPointForRow(i);
      if (pm==null) continue; //Blank row

      line = pm.getShortDesc();
      for (int j=0; j<itsSources.size(); j++) {
        line += ", ";
	String pname = (String)itsSources.get(j) + "." + (String)itsPoints.get(i);

	PointData pd = DataMaintainer.getBuffer(pname);
	if (pd==null || pd.getData()==null) continue;

	if (pd.isValid()) line += pd.getData().toString();
	else line += "?";
      }
      p.println(line);
    }
  }


  public static void main(String[] argv) {
    Vector pnames = new Vector();
    pnames.add("site.seemon.Lock1");
    pnames.add("site.seemon.Lock2");
    pnames.add("site.seemon.Lock3");
    pnames.add("ant.servo.AzSkyPos");
    pnames.add("ant.servo.ElSkyPos");
    Vector sources = new Vector();
    sources.add("seemon");
    sources.add("benchacc00");
    sources.add("benchacc01");

    PointTableModel ptm = new PointTableModel(pnames, sources);

    JFrame frame = new JFrame("Demo App");
    JTable table = new JTable(ptm);
    table.setDefaultRenderer(Object.class, ptm);
    JScrollPane scroll = new JScrollPane(table);
    frame.getContentPane().add(scroll);
    ptm.setSizes(table, scroll);

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setVisible(true);
  }

}
