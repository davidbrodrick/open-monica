//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon;

import atnf.atoms.mon.util.*;


/**
 * Encapsulates a thread which periodically changes the encryption keys.
 * The period at which the encryption keys are changed is determined by
 * the property <tt>KeyLife</tt>. The keys are updated by calling the
 * <tt>MonitorMap::generateNewKeys</tt> method.
 *
 * @author Le Cuong Nguyen
 * @version $Id: KeyKeeper.java,v 1.2 2004/02/13 05:14:01 bro764 Exp $
 */
public class KeyKeeper
implements Runnable
{
  /** Should the thread keep running <tt>True</tt> or stop <tt>False</tt>. */
  protected boolean itsRunning = true;
  
  /** Handles RSA encryption. */
  private static RSA theirRSA = new RSA(1024);

  /** Create a new KeyKeeper thread. */
  public
  KeyKeeper()
  {
    Thread t = new Thread(this, "Encryption Key Keeper");
    t.start();
  }

  /** Return the public RSA key. */
  public static String getPublicKey()
  {
    return new String(theirRSA.getE().toString());
  }

  /** Return the RSA modulus. */
  public static String getModulus()
  {
    return new String(theirRSA.getN().toString());
  }

  /** Decrypt the given ciphertext. */
  public static String decrypt(String ciphertext)
  {
    return theirRSA.decrypt(ciphertext);
  }
  
  /** Main loop for KeyKeeper thread. */
  public
  void
  run()
  {
    while (itsRunning) {
      theirRSA.generateKeys();
      try {
        synchronized (this) {
          this.wait(Long.parseLong(MonitorConfig.getProperty("KeyLife")));
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
