package atnf.atoms.mon.util;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Simple RSA public key encryption algorithm implementation.
 * <P>Taken from "Paj's" website:
 * <TT>http://pajhome.org.uk/crypt/rsa/implementation.html</TT>
 * <P>Adapted by David Brodrick
 */
public class RSA
{
  private BigInteger n, d, e;

  private int itsNumBits = 1024;
  
  private static final int theirMinLength = 12; 

  /** Create an instance that can encrypt using someone else's public key. */
  public RSA(BigInteger newn, BigInteger newe)
  {
    n = newn;
    e = newe;
  }

  /** Create an instance that can both encrypt and decrypt. */
  public RSA(int bits)
  {
    itsNumBits = bits;
    SecureRandom r = new SecureRandom();
    BigInteger p = new BigInteger(itsNumBits / 2, 100, r);
    BigInteger q = new BigInteger(itsNumBits / 2, 100, r);
    n = p.multiply(q);
    BigInteger m = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));
    e = new BigInteger("3");
    while(m.gcd(e).intValue() > 1) {
      e = e.add(new BigInteger("2"));
    }
    d = e.modInverse(m);
  }

  /** Encrypt the given plaintext message. */
  public synchronized
  String
  encrypt(String message)
  {
    byte[] msgbytes = message.getBytes();
    if (msgbytes.length<theirMinLength) {
      byte[] temp = new byte[theirMinLength];
      for (int i=0; i<msgbytes.length; i++) {
        // Copy the message
        temp[i]=msgbytes[i];
      }
      for (int i=msgbytes.length; i<theirMinLength; i++) {
        // Pad the message
        temp[i] = 0;
      }
      msgbytes = temp;
    }
    return (new BigInteger(msgbytes)).modPow(e, n).toString();
  }

  /** Decrypt the given ciphertext message. */
  public synchronized
  String
  decrypt(String message)
  {
    String plaintext = new String((new BigInteger(message)).modPow(d, n).toByteArray());
    // Strip any padding characters
    plaintext=plaintext.replaceAll("\0", "");
    return plaintext;
  }

  /** Generate a new public and private key set. */
  public synchronized
  void
  generateKeys()
  {
    SecureRandom r = new SecureRandom();
    BigInteger p = new BigInteger(itsNumBits / 2, 100, r);
    BigInteger q = new BigInteger(itsNumBits / 2, 100, r);
    n = p.multiply(q);
    BigInteger m = (p.subtract(BigInteger.ONE))
                   .multiply(q.subtract(BigInteger.ONE));
    e = new BigInteger("3");
    while(m.gcd(e).intValue() > 1) {
      e = e.add(new BigInteger("2"));
    }
    d = e.modInverse(m);
  }

  /** Return the modulus. */
  public synchronized
  BigInteger
  getN()
  {
    return n;
  }

  /** Return the exponent. */
  public synchronized
  BigInteger
  getE()
  {
    return e;
  }

  /** Trivial test program. */
  public static void main(String[] args)
  {
    RSA rsa = new RSA(1024);

    String plaintext = "Yellow and Black Border Collies";
    System.out.println("Plaintext: " + plaintext);    
    String ciphertext = rsa.encrypt(plaintext);
    System.out.println("Ciphertext: " + ciphertext);
    plaintext = rsa.decrypt(ciphertext);
    System.out.println("Plaintext: " + plaintext);
  }
}
