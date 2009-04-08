/**
 * Class: TransactionNONE
 * Description: Provides communication with the loopback
 * @author Le Cuong Nguyen
 **/

package atnf.atoms.mon.transaction;

import atnf.atoms.mon.PointMonitor;

public class TransactionNONE
extends Transaction
{
  protected static String itsArgs[] = new String[]{"Transaction - Loopback",
  "NONE"};

  public TransactionNONE(PointMonitor parent, String specifics)
  {
    super(parent, specifics);
    setChannel("NONE");
    specifics = specifics.replace('\"','\0').trim();
    itsName = specifics;
  }
}
