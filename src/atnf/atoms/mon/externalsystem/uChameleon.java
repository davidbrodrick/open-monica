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
 * 
 * @author David Brodrick
 */
public class uChameleon extends ASCIISocket {
  /** Constructor. */
  public uChameleon(String[] args) {
    super(args);
  }

  public synchronized void putData(PointDescription desc, PointData pd) throws Exception {
    // Determine desired state for relay(s)
    boolean relayset;
    if (pd.getData() instanceof Boolean) {
      relayset = ((Boolean) pd.getData()).booleanValue();
    } else {
      relayset = ((Number) pd.getData()).intValue() == 0 ? false : true;
    }

    // Get the Transactions which associates the point with us
    Vector<Transaction> alltrans = getMyTransactions(desc.getOutputTransactions());

    // Point may control more than one relay
    for (int i = 0; i < alltrans.size(); i++) {
      TransactionStrings thistrans = (TransactionStrings) alltrans.get(i);
    }
  }

  public Object parseData(PointDescription requestor) throws Exception {
    Object result;
    
    // Get the Transactions which associates the point with us
    Vector<Transaction> alltrans = getMyTransactions(requestor.getInputTransactions());
    
    TransactionStrings thistrans = (TransactionStrings) alltrans.get(0);
    if (thistrans.getNumStrings()!=2) {
      throw new Exception("Insufficient arguments in TransactionStrings for " + requestor.getFullName());
    }

    int pin = Integer.parseInt(thistrans.getString(1));

    // Figure out what the appropriate query string is for this point
    String query;
    if (thistrans.getString(0).equals("pin")) {
      query = "pin " + pin + " state" + "\r";
      itsWriter.write(query);
      itsWriter.flush();    
      String response = itsReader.readLine();
      result = new Integer(response.split(" ")[2]);
    } else if (thistrans.getString(0).equals("adc")) {
      query = "adc " + pin + "\r";
      itsWriter.write(query);
      itsWriter.flush();
      String response = itsReader.readLine();
      result = new Float(response.split(" ")[2]);
    } else {
      throw new Exception("Unexpected values in TransactionStrings for " + requestor.getFullName());
    }

    return result;
  }
}
