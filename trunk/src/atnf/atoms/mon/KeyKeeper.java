//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon;

import org.apache.log4j.Logger;
import atnf.atoms.mon.util.*;

/**
 * Encapsulates a thread which periodically changes the encryption keys. The period at which the encryption keys are changed is
 * determined by the property <tt>KeyLife</tt>.
 * 
 * @author Le Cuong Nguyen
 * @author David Brodrick
 */
public class KeyKeeper implements Runnable
{
  /** Should the thread keep running <tt>True</tt> or stop <tt>False</tt>. */
  protected boolean itsRunning = true;

  /** Logger. */
  protected Logger itsLogger = Logger.getLogger(this.getClass().getName());

  /** Handles RSA encryption. */
  private static RSA theirRSA = new RSA(1024);

  /** Create a new KeyKeeper thread. */
  public KeyKeeper()
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
  public void run()
  {
    while (itsRunning) {
      itsLogger.trace("Generating new encryption keys");
      theirRSA.generateKeys();
      try {
        synchronized (this) {
          this.wait(Long.parseLong(MonitorConfig.getProperty("KeyLife")));
        }
      } catch (Exception e) {
        itsLogger.error(e);
      }
    }
  }
}
