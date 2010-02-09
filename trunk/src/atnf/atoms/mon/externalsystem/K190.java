// Copyright (C) CSIRO Australia Telescope National Facility.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.externalsystem;

import atnf.atoms.mon.*;
import atnf.atoms.mon.transaction.*;
import atnf.atoms.time.RelTime;
import java.util.Vector;

/**
 * Reads temperature data and controls relays on a widely available kit based on a PIC
 * microcontroller. The board supports four DS18S20 or DS18B20 temperature sensors and
 * four relays for control.
 *
 * <P>The kits are available, for instance, from http://www.ozitronics.com/ or
 * http://www.oceancontrols.com.au/controllers/diy/serial_temp_k190.html
 * 
 * <P>The MoniCA interface to the microcontroller is via a socket, so the board needs 
 * to be plugged in to a serial/ethernet converter or redirected from the serial port
 * to a socket using something like 'socat'. An example invocation of socat, listening
 * on port 1234, is: <i>socat /dev/ttyS0 TCP4-LISTEN:1234,fork</i>.
 *
 * <P>The constructor requires <i>hostname:port</i> arguments. For instance your
 * monitor-sources.txt file might contain:<BR>
 * <tt>K190 localhost:1234</tt>
 *
 * <P>The monitor points for the four sensors need to use a TransactionString with the
 * channel set to <i>hostname:port</i> and the next argument being the specific sensor
 * number to use, eg <i>"1"</i> or <i>"4"</i>. Temperature sensor offsets can be removed
 * by using a TranslationEQ to apply the appropriate offset.
 * 
 * <P>The four relays require the same Transaction settings as the sensors, but are
 * defined as output transactions rather than inputs. A zero or False data value will 
 * turn the specified relay OFF, any non-zero value or True will turn the relay ON.
 *
 * @author David Brodrick
 * @version $Id: $
 **/
public class K190
extends ASCIISocket
{
  /** Number of sensors that a K190 kit supports. */
  private final static int theirNumSensors = 4;
  /** Number of sensors that a K190 kit supports. */
  private final static int theirNumRelays = 4;

  /** Constructor, expects host:port[:timeout] argument. */  
  public K190(String[] args)
  {
    super(args);
  }

  /** Get the latest temperature reading for the sensor number specified in the
   * point's input transaction. */
  public synchronized
  Object
  parseData(PointDescription desc)
  throws Exception 
  {
    // Get the Transaction which associates the point with us
    TransactionStrings thistrans = (TransactionStrings) getMyTransactions(desc.getInputTransactions()).get(0);

    // The Transaction should contain a numeric channel id
    if (thistrans.getNumStrings() < 1) {
      throw new Exception("K190(" + itsHostName + ":" + itsPort + "): Not enough arguments in input Transaction for "
          + desc.getFullName());
    }
    int thischan = Integer.parseInt(thistrans.getString(0));
    if (thischan < 1 || thischan > theirNumSensors) {
      throw new Exception("K190(" + itsHostName + ":" + itsPort + "): Illegal sensor number requested (" + thischan + ") for "
          + desc.getFullName());
    }

    // Purge the input stream
    while (itsReader.ready()) {
      itsReader.readLine();  
    }
    // Request the value for this sensor
    itsWriter.write("T" + thischan);
    itsWriter.flush();

    // Sleep for a short while. Introduced to stop spurious relay switching
    RelTime.factory(50000l).sleep();

    // Parse response
    String response = itsReader.readLine();
    //System.err.println("K190(" + itsHostName + ":" + itsPort + "): Got response \"" + response + "\"");
    Float result = null;
    if (response.startsWith("T"+thischan) && response.indexOf("?")==-1) {
      result = new Float(response.substring(2));
    }
    return result;
  }
  
  /** Set the state of the relay(s) specified in the points output transaction(s). A zero
   * data argument will open the relay while any non-zero argument will set the relay. */ 
  public synchronized
  void
  putData(PointDescription desc, PointData pd)
  throws Exception
  {
    //Ensure we are connected
    if (!itsConnected) {
      connect();
    }
    
    // Determine desired state for relay(s)
    boolean relayset;
    if (pd.getData() instanceof Boolean) {
      relayset = ((Boolean)pd.getData()).booleanValue(); 
    } else {
      relayset = ((Number)pd.getData()).intValue()==0?false:true;
    }

    // Get the Transactions which associates the point with us
    Vector<Transaction> alltrans = getMyTransactions(desc.getOutputTransactions());

    // Point may control more than one relay
    for (int i = 0; i < alltrans.size(); i++) {
      TransactionStrings thistrans = (TransactionStrings) alltrans.get(i);

      // The Transaction should contain a numeric channel id
      if (thistrans.getNumStrings() < 1) {
        throw new Exception("K190(" + itsHostName + ":" + itsPort + "): Not enough arguments in output Transaction for "
            + desc.getFullName());
      }
      int thischan = Integer.parseInt(thistrans.getString(0));
      if (thischan < 1 || thischan > theirNumRelays) {
        throw new Exception("K190(" + itsHostName + ":" + itsPort + "): Illegal relay number requested (" + thischan + ") for "
            + desc.getFullName());
      }

      try {
        // Write setting for this relay
        if (relayset) {
          // Turn ON
          itsWriter.write("N" + thischan);
        } else {
          // Turn OFF
          itsWriter.write("F" + thischan);
        }
        itsWriter.flush();
        
        // Sleep for a short while. Introduced to stop spurious relay switching
        RelTime.factory(50000l).sleep();
      } catch (Exception e) {
        disconnect();
        throw e;
      }
    }
  }
}
