// Copyright (C) Oz Forecast, NSW, Australia.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.datasource;

import java.util.StringTokenizer;
import java.util.HashMap;
import atnf.atoms.time.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.transaction.TransactionStrings;

/**
 * Reads data from a DS1820 based temperature sensor board, initially
 * designed by James Cameron.  
 *
 * <P>See http://quozl.us.netrek.org/ts/ I bought my board as kit K145 from
 * http://www.ozitronics.com/ but it looks like there are lots of compatible
 * kits available.
 * 
 * <P>The output from the microcontroller is read via a socket, so the
 * board needs to be plugged in to a serial/ethernet converter or
 * redirected from the serial port to a socket using something like sredird.
 *
 * <P>Sensor offsets can be removed by using a TranslationEQ to apply the
 * appropriate offset. DS1820B sensors can be used if you use the EQ to
 * divide the temperature by 8 (the output is shifted by 3 bits wrt to the
 * DS1820 sensors). I haven't tested what happens if the temp goes negative
 * with the B sensors yet.
 *
 * <P>The constructor only requires <i>hostname:port</i> arguments. For
 * instance your monitor-sources.txt file might contain:<BR>
 * <tt>atnf.atoms.mon.datasource.DataSourceK145 K145://localhost:10000</tt>
 *
 * @author David Brodrick
 * @version $Id: $
 **/
public class DataSourceK145
extends DataSourceASCIISocket
{
  /** Number of sensors that a K145 kit supports. */
  private final static int theirNumSensors = 4;
	/** Maximum age before we consider a reading to be stale. */
	private final static AbsTime theirMaxAge = AbsTime.factory(30000000);

  /** Latest temperature readings from each sensor. */
  private Float[] itsTemps = new Float[theirNumSensors];
	/** Times we last acquired data for each sensor. */
	private AbsTime[] itsTimes = new AbsTime[theirNumSensors];

  /** Constructor, expects host:port argument. */  
  public DataSourceK145(String args)
  {
    super(args);

    //Start the thread that reads the data as it comes in
    DataReader worker = new DataReader();
    worker.start();
  }

  /** Data is pushed, so this method is redundant in this class. */
  public
  Object
  parseData(PointMonitor requestor)
  throws Exception
  {
		TransactionStrings thistrans=(TransactionStrings)requestor.getTransaction();
		
		//The Transaction should contain a numeric channel id
		if (thistrans.getNumStrings()<1) {
		  throw new Exception("DataSourceK145: Not enough arguments in Transaction");
		}
		int thischan=Integer.parseInt(thistrans.getString(0));
		if (thischan<1 || thischan>theirNumSensors) {
		  throw new Exception("DataSourceK145: Illegal sensor number requested");
		}
		
		//Return null if data is stale
		AbsTime now=new AbsTime();
		if (itsTimes[thischan-1]==null || 
		    Time.diff(now,itsTimes[thischan-1]).getValue()>theirMaxAge.getValue()) 
		{
		  return null;
		} else {
		  return itsTemps[thischan-1];
		}
  }
  
  ////////////////////////////////////////////////////////////////////////
  /** Nested class to collect data. */
  public class
  DataReader
  extends Thread
  {
    public void	run()
    {
      boolean firstread=true;
      while (true) {
        //If we're not connected then try to reconnect
        if (!itsConnected) {
          try {
            if (!connect()) {
              //Sleep for a while then try again
              try { RelTime.factory(5000000).sleep(); } catch (Exception f) {}
              continue;
            }
            firstread=true;
          } catch (Exception e) {
            //Sleep for a while then try again
            try { RelTime.factory(5000000).sleep(); } catch (Exception f) {}
            continue;
          }
        }
        
        try {
          while (true) {
            String line = itsReader.readLine();
            String[] tokens=line.split(" ");
	    
            //Check correct number of tokens in string
            if (tokens.length!=2) continue;

            //Get sensor id number
            int thissensor;
            try {
              thissensor=Integer.parseInt(tokens[0]);
            } catch (Exception f) {continue;}
            if (thissensor<1 || thissensor>theirNumSensors) continue;
						thissensor--;
						
						//Parse float
            if (tokens[1].indexOf(".")==-1) continue;
            Float thistemp;
            try {
              thistemp=new Float(tokens[1]);
            } catch (Exception f) {continue;}
	    
            //Looks like a good reading, save this update
						itsTemps[thissensor]=thistemp;
						itsTimes[thissensor]=new AbsTime();
          }
        } catch (Exception e) {
          System.err.println("DataSourceK145:DataReader.run (" + itsHostName 
                             + ":" + itsPort + "): " + e.getClass());
          try { disconnect(); } catch (Exception f) { }
        }
      }
    }
  }

  public final static
  void
  main(String[] argv)
  {
     if (argv.length<1) {
       System.err.println("Missing argument: Needs IP:port of the K145 server");
       System.exit(1);
     }
     DataSourceK145 k145 = new DataSourceK145("K145://" + argv[0]);

     try {
       k145.connect();
     } catch (Exception e) {
       System.err.println(e.getMessage());
       System.exit(1);
     }
     while (true) {
       try {
         RelTime.factory(5000000).sleep();
       } catch (Exception e) {}
     }
  }

}
