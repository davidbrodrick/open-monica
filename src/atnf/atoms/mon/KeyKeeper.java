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
 * Holds an RSA instance for encryption of passwords etc.
 * 
 * @author Le Cuong Nguyen
 * @author David Brodrick
 */
public class KeyKeeper {
  /** Logger. */
  private static Logger theirLogger = Logger.getLogger(KeyKeeper.class.getName());

  /** Handles RSA encryption. */
  private static RSA theirRSA;

  static {
    theirRSA = new RSA(1024);
    theirRSA.generateKeys();
  }

  /** Return the exponent. */
  public static String getExponent() {
    return new String(theirRSA.getE().toString());
  }

  /** Return the RSA modulus. */
  public static String getModulus() {
    return new String(theirRSA.getN().toString());
  }

  /** Decrypt the given ciphertext. */
  public static String decrypt(String ciphertext) {
    return theirRSA.decrypt(ciphertext);
  }

  /** Encrypt the given plaintext. */
  public static String encrypt(String plaintext) {
    return theirRSA.encrypt(plaintext);
  }
}
