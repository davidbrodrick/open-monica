// Copyright (C) Oz Forecast, NSW, Australia.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.datasource;

import atnf.atoms.time.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.transaction.*;

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
 * redirected from the serial port to a socket using something like 'socat'.
 * An example invocation of socat is:
 * <i>socat /dev/ttyS2 TCP4-LISTEN:1234,fork</i>.
 *
 * <P>Sensor offsets can be removed by using a TranslationEQ to apply the
 * appropriate offset.
 *
 * <P>The constructor requires <i>hostname:port</i> arguments. For
 * instance your monitor-sources.txt file might contain:<BR>
 * <tt>atnf.atoms.mon.datasource.DataSourceK145 localhost:1234</tt>
 *
 * <P>The monitor points for the four sensors then need to use a
 * TransactionString with the channel set to <i>hostname:port</i> and
 * the next argument being the sensor number for that point eg <i>"1"</i>
 * or <i>"4"</i>
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

  /** Constructor, expects host:port[:timeout] argument. */  
  public DataSourceK145(String[] args)
  {
    super(args);

    //Start the thread that reads the data as it comes in
    DataReader worker = new DataReader();
    worker.start();
  }

  /** Get the latest temperature reading for the sensor number specified in the
   * monitor point's TransactionString. */
  public
  Object
  parseData(PointDescription requestor)
  throws Exception
  {
    //Get the Transaction which associates the point with us
    TransactionStrings thistrans=(TransactionStrings)getMyTransactions(requestor.getInputTransactions()).get(0);

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
            if (tokens.length!=2) {
              continue;
            }

            //Get sensor id number
            int thissensor;
            try {
              thissensor=Integer.parseInt(tokens[0]);
            } catch (Exception f) {continue;}
            if (thissensor<1 || thissensor>theirNumSensors) {
              continue;
            }
            thissensor--;
						
            //Parse float
            if (tokens[1].indexOf(".")==-1) {
              continue;
            }
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
     //Add static arguments
     String[] args=argv[0].split(":");
     String[] fullargs=new String[2];
     fullargs[0]=args[0];
     fullargs[1]=args[1];
     
     DataSourceK145 k145 = new DataSourceK145(fullargs);

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
