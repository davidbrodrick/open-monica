//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.externalsystem;

import java.io.*;
import java.util.*;
import java.net.*;

import atnf.atoms.time.*;
import atnf.atoms.mon.*;

/**
 *
 * @author David Brodrick
 * @version $Id: $
 **/
class ThyconUPS
extends ExternalSystem
{
  /** The socket connection to the serial/ethernet converter. */
  private Socket itsSocket = null;
  /** Network port to connect to. */
  private int itsPort = 6000;
  /** Name of remote host to connect to. */
  private String itsHost = null;

  /** InputStream for reading data. */
  protected BufferedReader itsReader = null;
  /** OutputStream for sending commands. */
  protected PrintStream itsWriter = null;
  /** THYNET address of the UPS. */
  protected byte itsAddress = 0x20;

  final static byte[] INVOLTS    = {0x00};
  final static byte[] INCUR      = {0x01};
  final static byte[] INPOW      = {0x02};
  final static byte[] INPERIOD   = {0x03};
  final static byte[] OUTVOLTS   = {0x04};
  final static byte[] OUTCUR     = {0x05};
  final static byte[] OUTPOW     = {0x06};
  final static byte[] OUTPERIOD  = {0x07};
  final static byte[] BYPERIOD   = {0x0B};
  final static byte[] STATUS     = {0x10};
  final static byte[] BATTHIST   = {0x19};


  /** Constructor.
   * @param args
   */
  public ThyconUPS(String[] args)
  {
    super(args[0]+":"+args[1]);

    itsHost = args[1];
  }


  /** Establish a new network connection to the UPS. */
  public
  boolean
  connect()
  throws Exception
  {
    try {
      itsSocket = new Socket(itsHost, itsPort);
      itsSocket.setSoTimeout(10000); ///Ten second timeout.
      itsConnected = true;
      itsReader = new BufferedReader(new InputStreamReader(itsSocket.getInputStream()));
      itsWriter = new PrintStream(itsSocket.getOutputStream());

      System.err.println("ThyconUPS: Connected to " + itsHost);
      MonitorMap.logger.information("ThyconUPS: Connected to "
				    + itsHost);
      //Reset the transaction counter
      itsNumTransactions = 0;
    } catch (Exception e) {
      try {
	if (itsSocket!=null) {
    itsSocket.close();
  }
      } catch (Exception f) { }
      itsSocket = null;
      itsReader = null;
      itsConnected  = false;
      System.err.println("ThyconUPS: Couldn't Connect: "
			 + e.getMessage());
    }
    return itsConnected;
  }


  /** Close the network connection to the lightning detector. */
  public
  void
  disconnect()
  throws Exception
  {
    System.err.println("ThyconUPS: Lost Connection");
    itsConnected = false;
    if (itsSocket!=null) {
      itsSocket.close();
    }
    itsSocket = null;
    itsReader = null;
    itsWriter = null;
  }

  /** Convert the nibble to its BCD equivalent. */
  private static
  String
  nibble2BCD(byte nibble)
  {
    switch ((nibble)&0x0F) {
    case  0: return "0";
    case  1: return "1";
    case  2: return "2";
    case  3: return "3";
    case  4: return "4";
    case  5: return "5";
    case  6: return "6";
    case  7: return "7";
    case  8: return "8";
    case  9: return "9";
    case 10: return "A";
    case 11: return "B";
    case 12: return "C";
    case 13: return "D";
    case 14: return "E";
    case 15: return "F";
    default: return "?"; //Can't happen
    }
  }

  /** Convert the byte to its BCD equivalent. */
  private static
  String
  byte2BCD(byte b)
  {
    return nibble2BCD((byte)(b>>4)) + nibble2BCD(b);
  }

  /** Convert the BCD digit into its numeric equivalent. */
  private static
  byte
  BCD2byte(byte b)
  {
    if (b=='0') {
      return 0;
    } else if (b=='1') {
      return 1;
    } else if (b=='2') {
      return 2;
    } else if (b=='3') {
      return 3;
    } else if (b=='4') {
      return 4;
    } else if (b=='5') {
      return 5;
    } else if (b=='6') {
      return 6;
    } else if (b=='7') {
      return 7;
    } else if (b=='8') {
      return 8;
    } else if (b=='9') {
      return 9;
    } else if (b=='a') {
      return 10;
    } else if (b=='b') {
      return 11;
    } else if (b=='c') {
      return 12;
    } else if (b=='d') {
      return 13;
    } else if (b=='e') {
      return 14;
    } else if (b=='f') {
      return 15;
    } else if (b=='A') {
      return 10;
    } else if (b=='B') {
      return 11;
    } else if (b=='C') {
      return 12;
    } else if (b=='D') {
      return 13;
    } else if (b=='E') {
      return 14;
    } else {
      return 15;
    }
  }


  /** Convert a Thycon date into an AbsTime. */
  private
  AbsTime
  toAbsTime(byte a, byte b, byte c, byte d, byte e, byte f,
	    byte g, byte h, byte i, byte j, byte k, byte l)
  {
    int year =2000 + BCD2byte(b) + BCD2byte(a)*10;
    int mon  =BCD2byte(d) + BCD2byte(c)*10 - 1;
    int day  =BCD2byte(f) + BCD2byte(e)*10;
    int hr   =BCD2byte(h) + BCD2byte(g)*10;
    int min  =BCD2byte(j) + BCD2byte(i)*10;
    int sec  =BCD2byte(l) + BCD2byte(k)*10;

    GregorianCalendar cal = new GregorianCalendar(year, mon, day,
						  hr, min, sec);
    cal.setTimeZone(SimpleTimeZone.getTimeZone("AEST"));
    return AbsTime.factory(cal.getTime());
  }


  /** Convert a Thycon floating point number into a Java Integer. */
  private
  Integer
  toInt(byte a, byte b, byte c, byte d)
  {
    int i1=BCD2byte(b) + (BCD2byte(a)<<4) +
           (BCD2byte(d)<<8) + (BCD2byte(c)<<12);
    return new Integer(i1);
  }


  /** Convert a Thycon floating point number into a Java Float. */
  private
  Float
  toFloat(byte a, byte b, byte c, byte d)
  {
    int i1=BCD2byte(b) + (BCD2byte(a)<<4) +
           (BCD2byte(d)<<8) + (BCD2byte(c)<<12);
    if (i1==0) {
      return new Float(0.0);
    }

    int e=(i1&0x7c00)>>10; //exponent
    float f=((i1&0x03ff)/1024.0f)+1.0f;;

//    System.out.println("e=" + e + "\tf=" + f);
    if (e>0 && e<31) {
      e-=8;
    } else if (e==31) {
      e=255;
//    else if (e==0 && f!=0) {
//      e = 127 - 8;
//      f = f<<1;
//    }
    }

    float res = (float)(f * Math.pow(2, e));
//    System.out.println("Float value=" + res);
    return new Float(res);
  }


  /** Test the response packet for valid format and checksum. */
  private
  boolean
  checkResponse(String resp)
  {
    if (resp==null || resp.length()<5) {
      return false;
    }

    byte[] bytes = resp.getBytes();

    if (bytes[0]!='!') {
      return false;
    }

    ///Still needs to verify checksum

    return true;
  }

  /** Send a request packet using the given request code. If a valid
   * response is obtained it will be returned as a String, otherwise
   * an Exception will be thrown. */
  private
  String
  sendRequest(byte[] req)
  throws Exception
  {
    if (!itsConnected) {
      throw new Exception("Not connected to UPS");
    }

    //Build request array of bytes
    byte[] allbytes = new byte[req.length + 2];
    allbytes[0] = itsAddress;
    for (int i=0; i<req.length; i++) {
      allbytes[i+1] = req[i];
    }
    //Calculate checksum
    byte checksum = 0;
    for (int i=0; i<allbytes.length; i++) {
      checksum = (byte)(checksum + allbytes[i]);
    }
    checksum = (byte)(-checksum);
    allbytes[allbytes.length-1] = checksum;
    //Convert to string
    String reqstr = "!";
    for (int i=0; i<allbytes.length; i++) {
      reqstr = reqstr + byte2BCD(allbytes[i]);
    }
    reqstr = reqstr + "\r";
//    System.out.println("Request is: \t" + reqstr);
    itsWriter.print(reqstr);
    itsWriter.flush();

    String line = itsReader.readLine();
    if (!checkResponse(line)) {
      throw new Exception("Invalid response from UPS!");
    }

//    System.out.println("Response is:\t" + line);
    return line;
  }


  /** Extract meaningful information from a response packet from the UPS
   * and insert the information into the HashMap. */
  private
  void
  parseResponse(String resp, HashMap<String,Object> map)
  {
    byte[] bytes = resp.getBytes();

    if (bytes[3]=='8' && bytes[4]=='0') {
      //Input 3 phase voltage
      map.put("V1IN", toFloat(bytes[5], bytes[6], bytes[7], bytes[8]));
      map.put("V2IN", toFloat(bytes[9], bytes[10], bytes[11], bytes[12]));
      map.put("V3IN", toFloat(bytes[13], bytes[14], bytes[15], bytes[16]));
    } else if (bytes[3]=='8' && bytes[4]=='1') {
      //Input 3 phase current
      map.put("I1IN", toFloat(bytes[5], bytes[6], bytes[7], bytes[8]));
      map.put("I2IN", toFloat(bytes[9], bytes[10], bytes[11], bytes[12]));
      map.put("I3IN", toFloat(bytes[13], bytes[14], bytes[15], bytes[16]));
    } else if (bytes[3]=='8' && bytes[4]=='2') {
      //Input 3 phase power
      map.put("P1INW", toFloat(bytes[5], bytes[6], bytes[7], bytes[8]));
      map.put("P2INW", toFloat(bytes[9], bytes[10], bytes[11], bytes[12]));
      map.put("P3INW", toFloat(bytes[13], bytes[14], bytes[15], bytes[16]));
      map.put("P1INVA", toFloat(bytes[17], bytes[18], bytes[19], bytes[20]));
      map.put("P2INVA", toFloat(bytes[21], bytes[22], bytes[23], bytes[24]));
      map.put("P3INVA", toFloat(bytes[25], bytes[26], bytes[27], bytes[28]));
    } else if (bytes[3]=='8' && bytes[4]=='3') {
      //Input period
    } else if (bytes[3]=='8' && bytes[4]=='4') {
      //Output 3 phase voltage
      map.put("V1OUT", toFloat(bytes[5], bytes[6], bytes[7], bytes[8]));
      map.put("V2OUT", toFloat(bytes[9], bytes[10], bytes[11], bytes[12]));
      map.put("V3OUT", toFloat(bytes[13], bytes[14], bytes[15], bytes[16]));
    } else if (bytes[3]=='8' && bytes[4]=='5') {
      //Output 3 phase current
      map.put("I1OUT", toFloat(bytes[5], bytes[6], bytes[7], bytes[8]));
      map.put("I2OUT", toFloat(bytes[9], bytes[10], bytes[11], bytes[12]));
      map.put("I3OUT", toFloat(bytes[13], bytes[14], bytes[15], bytes[16]));
      /*System.out.println((new AbsTime()).toString(AbsTime.Format.UTC_STRING) + "\t" +
		       toFloat(bytes[5], bytes[6], bytes[7], bytes[8]) + "\t" +
		       toFloat(bytes[9], bytes[10], bytes[11], bytes[12]) + "\t" +
		       toFloat(bytes[13], bytes[14], bytes[15], bytes[16]));*/
    } else if (bytes[3]=='8' && bytes[4]=='6') {
      //Output 3 phase power
      map.put("P1OUT", toFloat(bytes[5], bytes[6], bytes[7], bytes[8]));
      map.put("P2OUT", toFloat(bytes[9], bytes[10], bytes[11], bytes[12]));
      map.put("P3OUT", toFloat(bytes[13], bytes[14], bytes[15], bytes[16]));
      map.put("P1OUTVA", toFloat(bytes[17], bytes[18], bytes[19], bytes[20]));
      map.put("P2OUTVA", toFloat(bytes[21], bytes[22], bytes[23], bytes[24]));
      map.put("P3OUTVA", toFloat(bytes[25], bytes[26], bytes[27], bytes[28]));
    } else if (bytes[3]=='9' && bytes[4]=='0') {
      //Status message
      byte b = (byte)((bytes[5]<<4) + bytes[6]);
      if ((b&1)==0) {
        map.put("CB1", new Boolean(false));
      } else {
        map.put("CB1", new Boolean(true));
      }
      if ((b&2)==0) {
        map.put("CB2", new Boolean(false));
      } else {
        map.put("CB2", new Boolean(true));
      }
      if ((b&4)==0) {
        map.put("CB3", new Boolean(false));
      } else {
        map.put("CB3", new Boolean(true));
      }
      if ((b&8)==0) {
        map.put("CB4", new Boolean(false));
      } else {
        map.put("CB4", new Boolean(true));
      }
      if ((b&16)==0) {
        map.put("CB5", new Boolean(false));
      } else {
        map.put("CB5", new Boolean(true));
      }
      if ((b&32)==0) {
        map.put("CB6", new Boolean(false));
      } else {
        map.put("CB6", new Boolean(true));
      }
      if ((b&64)==0) {
        map.put("CB7", new Boolean(false));
      } else {
        map.put("CB7", new Boolean(true));
      }
      if ((b&128)==0) {
        map.put("CB8", new Boolean(false));
      } else {
        map.put("CB8", new Boolean(true));
      }

/*      map.put("LCD1", "-"); //Initialise status strings to default values
      map.put("LCD2", "-");
      map.put("LCD3", "-");
      map.put("LCD4", "-");
      map.put("LCD5", "-");
      map.put("LCD6", "-");
      map.put("LCD7", "-");
      map.put("LCD8", "-");
      map.put("LCD9", "-");
      map.put("LCD10", "-");
      b = (byte)((bytes[9]<<4) + bytes[10]);
      int lcd=1;
      if ((b&1)!=0) {map.put("LCD"+lcd,   "NBPS online - normal"); lcd++;}
      if ((b&2)!=0) {map.put("LCD"+lcd,   "Load on UPS power"); lcd++;}
      if ((b&4)!=0) {map.put("LCD"+lcd,   "Load on BYPASS power"); lcd++;}
      if ((b&8)!=0) {map.put("LCD"+lcd,   "Input supply off tolerance"); lcd++;}
      if ((b&16)!=0) {map.put("LCD"+lcd,  "Input frequency off tolerance"); lcd++;}
      if ((b&32)!=0) {map.put("LCD"+lcd,  "Input phase current imbalance"); lcd++;}
      if ((b&64)!=0) {map.put("LCD"+lcd,  "Generator online"); lcd++;}
      if ((b&128)!=0) {map.put("LCD"+lcd, "Bypass supply off tolerance"); lcd++;}
      b = (byte)((bytes[11]<<4) + bytes[12]);
      if ((b&1)!=0) {map.put("LCD"+lcd,   "Bypass frequency off tolerance"); lcd++;}
      if ((b&2)!=0) {map.put("LCD"+lcd,   "Inverter is off"); lcd++;}
      if ((b&4)!=0) {map.put("LCD"+lcd,   "Inverter is on"); lcd++;}
      if ((b&8)!=0) {map.put("LCD"+lcd,   "Inverter supply off tolerance"); lcd++;}
      if ((b&16)!=0) {map.put("LCD"+lcd,  "Inverter frequency off tolerance"); lcd++;}
      if ((b&32)!=0) {map.put("LCD"+lcd,  "Inverter/bypass not in sync"); lcd++;}
      if ((b&64)!=0) {map.put("LCD"+lcd,  "Inverter DC current excess"); lcd++;}
      if ((b&128)!=0) {map.put("LCD"+lcd, "Inverter on internal frequency reference"); lcd++;}
      b = (byte)((bytes[13]<<4) + bytes[14]);
      if ((b&1)!=0) {map.put("LCD"+lcd,   "UPS supply off tolerance"); lcd++;}
      if ((b&2)!=0) {map.put("LCD"+lcd,   "UPS frequency off tolerance"); lcd++;}
      if ((b&4)!=0) {map.put("LCD"+lcd,   "UPS in overload"); lcd++;}
      if ((b&8)!=0) {map.put("LCD"+lcd,   "UPS supply fail"); lcd++;}
      if ((b&16)!=0) {map.put("LCD"+lcd,  "Rectifier is off"); lcd++;}
      if ((b&32)!=0) {map.put("LCD"+lcd,  "Rectifier is on"); lcd++;}
      if ((b&64)!=0) {map.put("LCD"+lcd,  "Rectifier tripped overcurrent"); lcd++;}
      if ((b&128)!=0) {map.put("LCD"+lcd, "Rectifier in overload"); lcd++;}
      b = (byte)((bytes[15]<<4) + bytes[16]);
      if ((b&1)!=0) map.put("LCD",        "Rectifier overvoltage DC high");
      else if ((b&2)!=0) map.put("LCD",   "Rectifier has fault");
      else if ((b&4)!=0) map.put("LCD",   "Rectifier DC earth leak");
      else if ((b&8)!=0) map.put("LCD",   "Batteries on boost");
      else if ((b&16)!=0) map.put("LCD",  "Battery voltage high");
      else if ((b&32)!=0) map.put("LCD",  "Battery discharging");
      else if ((b&64)!=0) map.put("LCD",  "Battery low warning");
      else if ((b&128)!=0) map.put("LCD", "Battery discharged");
      b = (byte)((bytes[9]<<4) + bytes[10]);
      if ((b&1)!=0) map.put("LCD",        "");
      else if ((b&2)!=0) map.put("LCD",   "");
      else if ((b&4)!=0) map.put("LCD",   "");
      else if ((b&8)!=0) map.put("LCD",   "");
      else if ((b&16)!=0) map.put("LCD",  "");
      else if ((b&32)!=0) map.put("LCD",  "");
      else if ((b&64)!=0) map.put("LCD",  "");
      else if ((b&128)!=0) map.put("LCD", "");
      b = (byte)((bytes[9]<<4) + bytes[10]);
      if ((b&1)!=0) map.put("LCD",        "");
      else if ((b&2)!=0) map.put("LCD",   "");
      else if ((b&4)!=0) map.put("LCD",   "");
      else if ((b&8)!=0) map.put("LCD",   "");
      else if ((b&16)!=0) map.put("LCD",  "");
      else if ((b&32)!=0) map.put("LCD",  "");
      else if ((b&64)!=0) map.put("LCD",  "");
      else if ((b&128)!=0) map.put("LCD", "");
      b = (byte)((bytes[9]<<4) + bytes[10]);
      if ((b&1)!=0) map.put("LCD",        "");
      else if ((b&2)!=0) map.put("LCD",   "");
      else if ((b&4)!=0) map.put("LCD",   "");
      else if ((b&8)!=0) map.put("LCD",   "");
      else if ((b&16)!=0) map.put("LCD",  "");
      else if ((b&32)!=0) map.put("LCD",  "");
      else if ((b&64)!=0) map.put("LCD",  "");
      else if ((b&128)!=0) map.put("LCD", "");
      b = (byte)((bytes[9]<<4) + bytes[10]);
      if ((b&1)!=0) map.put("LCD",        "");
      else if ((b&2)!=0) map.put("LCD",   "");
      else if ((b&4)!=0) map.put("LCD",   "");
      else if ((b&8)!=0) map.put("LCD",   "");
      else if ((b&16)!=0) map.put("LCD",  "");
      else if ((b&32)!=0) map.put("LCD",  "");
      else if ((b&64)!=0) map.put("LCD",  "");
      else if ((b&128)!=0) map.put("LCD", "");*/

    } else if (bytes[3]=='9' && bytes[4]=='8') {
      //Outage history
      map.put("OUTTIM", toAbsTime(bytes[5],  bytes[6],  bytes[7],  bytes[8],
				  bytes[9],  bytes[10], bytes[11], bytes[12],
				  bytes[13], bytes[14], bytes[15], bytes[16]));
      map.put("OUTDUR", toInt(bytes[17], bytes[18], bytes[19], bytes[20]));
      map.put("OUTNUM", toInt(bytes[21], bytes[22], bytes[23], bytes[24]));
      map.put("OUTTOT", toInt(bytes[25], bytes[26], bytes[27], bytes[28]));
    } else if (bytes[3]=='9' && bytes[4]=='9') {
      //Battery discharge history
      map.put("DISCTIM", toAbsTime(bytes[5],  bytes[6],  bytes[7],  bytes[8],
				   bytes[9],  bytes[10], bytes[11], bytes[12],
				   bytes[13], bytes[14], bytes[15], bytes[16]));
      map.put("DISCDUR", toInt(bytes[17], bytes[18], bytes[19], bytes[20]));
      map.put("DISCNUM", toInt(bytes[21], bytes[22], bytes[23], bytes[24]));
      map.put("DISCTOT", toInt(bytes[25], bytes[26], bytes[27], bytes[28]));
    }
  }

   /** Do the actual network transactions and parse the output of the
    * UPS into a HashMap that can be used by other monitor points. */
   private
   HashMap<String,Object>
   getNewData()
   throws Exception
   {
     if (!itsConnected) {
      throw new Exception("Not connected to UPS");
    }

     HashMap<String,Object> res = new HashMap<String,Object>();
     try {
       parseResponse(sendRequest(INVOLTS), res);
       parseResponse(sendRequest(INCUR), res);
       parseResponse(sendRequest(INPOW), res);
       parseResponse(sendRequest(OUTVOLTS), res);
       parseResponse(sendRequest(OUTCUR), res);
       parseResponse(sendRequest(OUTPOW), res);
       parseResponse(sendRequest(BATTHIST), res);
       parseResponse(sendRequest(STATUS), res);
     } catch (Exception e) {
       res = null;
     }
     return res;
   }

   public 
   void
   getData(PointDescription[] points)
   throws Exception
   {
     //Increment transaction counter
     itsNumTransactions += points.length;

     //Try to get the new data and force a reconnect if the read times out
     HashMap newdata = null;
     try {
       if (itsConnected) {
        newdata = getNewData();
      }
     } catch (Exception e) {
       try {
	 System.err.println("DatasourceThyconUPS: " + e.getMessage());
	 disconnect();
       } catch (Exception f) { }
     }
     //If the response was null then there must have been a parse error
     //this tends to happen after a power glitch when the detector gets
     //power cycled and spits out a heap of rubbish characters that then
     //get buffered by the media converter. Let's force a reconnect and
     //make sure the buffer has been flushed.
     if (newdata==null) {
       try {
	 System.err.println("DatasourceThyconUPS: Parse error..");
	 disconnect();
       } catch (Exception e) { }
     }

     //Fire off the new data
     for (int i=0; i<points.length; i++) {
       PointDescription pm = points[i];
       PointData pd = new PointData(pm.getFullName(), newdata);
       pm.firePointEvent(new atnf.atoms.mon.PointEvent(this, pd, true));
     }
   }

   public final static
   void
   main(String[] argv)
   {
     if (argv.length<1) {
       System.err.println("Missing argument: Needs hostname:port of the socket!");
       System.exit(1);
     }
     //Add static arguments
     String[] args=argv[0].split(":");
     String[] fullargs=new String[4];
     fullargs[0]="thyconups";
     fullargs[1]=args[0];
     fullargs[2]=args[1];
     fullargs[3]="30000";
     
     ThyconUPS ups = new ThyconUPS(fullargs);


     try {
       ups.connect();
       HashMap res = ups.getNewData();
       System.err.println("HashMap=" + res);
     } catch (Exception e) {
       System.err.println(e.getMessage());
       System.exit(1);
     }
     System.exit(0);

     while (true) {
      ;
    }
   }
}
