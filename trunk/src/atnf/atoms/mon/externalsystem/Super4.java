// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.externalsystem;

import atnf.atoms.mon.*;
import atnf.atoms.mon.transaction.*;
import java.util.Vector;
import java.io.*;

/**
 * Provides an interface to control relays on a TCTEC Super-4 USB relay board.
 * This class is basically just a wrapper which expects the <i>irset</i> Linux executable
 * provided by the manufacturer to be in the current PATH.
 * 
 * <P>The manufacturers website is <tt>http://www.emx.net.au/</tt>
 * 
 * <P>The constructor requires <tt>unique_id:serial_num</tt> arguments. Where:
 * <bl>
 * <li> <tt>unique_id</tt> a short id string which is only used to allow several boards to 
 * coexist in the system, without you needing to change the point definitions if the board is 
 * replaced (and serial number consequently changes).
 * <li> <tt>serial_num</tt> is the serial number of the specific board to be addressed. This
 * is the same number that you use to invoke irset manually and can be determined by running
 * <tt>lsusb -v</tt> and finding the iSerial number for the appropriate Super-4 board.
 * </bl>
 * 
 * An example instantiation of this driver would be to add the following line to your 
 * monitor-sources.txt file:<BR>
 * <tt>atnf.atoms.mon.externalsystem.Super4 1:FTRG4H5V</tt>
 *
 * <P>The control points for the four relays need to define an output Transaction of type
 * TransactionString with the channel set to <i>super4:unique_id</i> where unique_id is the
 * same string used to instanciate the driver. The next argument for the TransactionStrings 
 * must be the specific relay number to be controlled, eg <i>"1"</i> or <i>"4"</i>. A zero 
 * data value will turn the specified relay OFF, any non-zero value will turn the relay ON.
 *
 * @author David Brodrick
 **/
public class Super4
extends ExternalSystem
{
  /** Number of sensors that a single board supports. */
  private final static int theirNumRelays = 4;
  /** The unique identifier for this instance. */
  private String itsIdentifier;
  /** Serial number for the specific board to be controlled. */
  private String itsSerialNum;

  /** Constructor, expects unique_num:serial_num arguments. */  
  public Super4(String[] args)
  {
    super("super4:"+args[0]);
    itsIdentifier = args[0];
    itsSerialNum = args[1];
  }

  /** Set the state of the relay(s) specified in the points output transaction(s). A zero
   * data argument will open the relay while any non-zero argument will close the relay. */ 
  public synchronized
  void
  putData(PointDescription desc, PointData pd)
  throws Exception
  {
  	//Determine desired state for relays
	  boolean relayset = ((Number)pd.getData()).intValue()==0?false:true;
	
  	//Get the Transactions which associates the point with us
	  Vector<Transaction> alltrans = getMyTransactions(desc.getOutputTransactions());

	  //Point may control more than one relay
	  for (int i=0; i<alltrans.size(); i++) {
	    TransactionStrings thistrans = (TransactionStrings)alltrans.get(i);

      //The Transaction should contain a numeric channel id
	    if (thistrans.getNumStrings() < 1) {
	      throw new Exception("Super4(" + itsIdentifier + "): Not enough arguments in output Transaction for " + desc.getFullName());
      }
	    int thischan = Integer.parseInt(thistrans.getString(0));
	    if (thischan < 1 || thischan > theirNumRelays) {
	      throw new Exception("Super4(" + itsIdentifier + "): Illegal sensor number requested (" + thischan + ") for " + desc.getFullName());
	    }
      
      //Get the appropriate number to be toggle this relay
      int relayword = 1;
      for (int j=1; j<thischan; j++) {
        relayword*=2;
      }

	    //Construct appropriate shell command for this relay
      String command;
	    if (relayset) {
		    //Turn ON
        command = "irset -s" + itsSerialNum + "," + relayword;
	    } else {
		    //Turn OFF
        command = "irset -u" + itsSerialNum + "," + relayword;
	    }
      System.err.println("Super4(" + itsIdentifier + "): Executing command \"" + command + "\"");
      
      //Execute the command and write to STDERR if there are any problems
      Process p = Runtime.getRuntime().exec(command);
      BufferedReader Resultset = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line;
      while ((line = Resultset.readLine()) != null) {
        System.err.println("Super4(" + itsIdentifier + "): " + line);
      }
	  }
  }
}
