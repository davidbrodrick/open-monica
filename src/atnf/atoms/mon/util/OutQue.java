// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.


package atnf.atoms.mon.util;

import java.net.*;
import java.io.*;
import java.util.Vector;

/**
 * Class representing the VMON output queue.
 *
 * @author
 *  David G Loone
 *
 * @version $Id: $
 */
public
class OutQue
{

  /**
   * The RCS id.
   */
  final public static
  String RCSID = "$Id: $";

  /**
   * The antenna id.
   */
  String itsLabel;

  /**
   * The running count of points in output message.
   */
  private int itsCount;
  
  /**
   * The buffer to be sent.
   */
  private  
  StringBuffer itsMess;

  private static int MAXCOUNT = 50;
  private static int MAXINDEX = 1499;

  /**
   * The host to send all the data to.
   */
  private static String theirHost = null;

  /**
   * The port to send all the data to.
   */
  private static int theirPort = 5100;

  /**
   * The socket for data output.
   */
  private static Socket theirSocket = null;

  /**
   * PrintWriter used for sending data.
   */
  private static PrintWriter theirWriter = null;

  /**
   * Hack, static list of all OutQueues
   */
  private static Vector theirOutQues = new Vector();

  /**
   * Specify the host/port to send the data to.
   * @param host The name of the host to send the data to.
   * @param port The TCP/IP port to send the data to.
   */
  public static
  void
  setDestination(String host, int port)
  {
    theirHost = host;
    theirPort = port;
    theirSocket = null;
  }


  /**
   * Constructor an output queue that writes to standard out.
   */
  public
  OutQue()
  {}

  /**
   * Construct an output queue that writes to an IP port.
   *
   * @param host
   *  The host to send data to.
   *
   * @param port
   *  The port on <code>host</code> to use.
   */
  public
  OutQue(
    String host,
    int port
  )
  {

    itsCount = 0;
    itsLabel = "";
    theirOutQues.add(this);
  }

  /**
   * Construct an output queue that writes to an IP port.
   *
   * @param label
   *  The monitor source code to be included in the message.
   */
  public
  OutQue(
    String label
  )
  {
    itsCount = 0;
    itsLabel = label;
    theirOutQues.add(this);
  }


  /**
   * Construct an output queue that writes to an IP port.
   *
   * @param host
   *  The host to send data to.
   *
   * @param port
   *  The port on <code>host</code> to use.
   *
   * @param label
   *  The monitor source code to be included in the message.
   */
  public
  OutQue(
    String host,
    int port,
    String label
  )
  {
    itsCount = 0;
    itsLabel = label;
    theirOutQues.add(this);
  }

  /**
   * Send the given string to the destination
   */
  private
  void
  sendStr(String message)
  {
    //Connect if not already connected
    if (theirSocket==null && theirHost!=null) {
      try {
	theirSocket = new Socket(theirHost, theirPort);
	theirSocket.setSoTimeout(5000);
	theirWriter = new PrintWriter(theirSocket.getOutputStream());
      } catch (Exception e) {
	//Couldn't connect - can't send data
	theirSocket = null;
	theirWriter = null;
	return;
      }
    }

    synchronized (theirSocket) {
      //Send data
      try {
	theirWriter.print(message + "\n");
	theirWriter.flush();
      } catch (Exception e) {
	//Transmission error - connection is dead
	theirSocket = null;
	theirWriter = null;
	return;
      }
    }
  }


  /**
   * Send data to wherever it is to go. This method builds a message from each
   * input and transmits it when the maximum message size is reached.
   *
   * @param index
   *  The index (within OBSCOM) of the data.
   *
   * @param data
   *  The data.
   */
  public synchronized
  void
  put(
    int index,
    Number data
  )
  {
    if (data instanceof Integer) {
      put(index, data.intValue());
    } else {
      put(index, data.floatValue());
    }
  }


  /**
   * Send data to wherever it is to go. This method builds a message from each
   * input and transmits it when the maximum message size is reached.
   *
   * @param index
   *  The index (within OBSCOM) of the data.
   *
   * @param data
   *  The data.
   */
  public synchronized
  void
  put(
    int index,
    int data
  )
  {
    // do nothing if index >= MAXINDEX
    if(index < MAXINDEX) {
      if (itsCount == 0) {
        itsMess = new StringBuffer(4);
      }
      // make sure data is less than max signed integer*2 in fortran
      if (data>32767) {
        data-=65536;
      }
      itsMess.append(" " + index + " " + data + " ");
      itsCount++;

      if (itsCount == MAXCOUNT) {
      	//Insert point count and restrict label to 5 characters: e.g., ant04
        itsMess.insert(0,"PUT " + itsLabel.substring(0,5) + " " + MAXCOUNT);
        itsCount = 0;
	String message = new String(itsMess);
	sendStr(message);
      }
    }
  }

  /**
   * Send data to wherever it is to go. This method is called for floating point
   * data which is translated into a pair of integers and passed to put(int, int)
   * with successive index numbers. 
   *
   * @param index
   *  The index (within OBSCOM) of the data.
   *
   * @param data
   *  The data.
   */
  public synchronized
  void
  put(
    int index,
    float data
  )
  {
    // do nothing if index >= MAXINDEX
    if(index < MAXINDEX) {
      int[] words = new int[2];
      words = OutQue.toVAXFloat(data);
      put(index, words[0]);
      put(index+1, words[1]);
    }
  }

  /**
   * Method to generate VAX format floating point numbers as
   * a pair of integers.
   *
   */
   private static int[] toVAXFloat(float f){
     int lsw=0;
     int msw=0;
     int exp = 0;
     if (f != 0.0f && f !=-0.0f) {
       float tst = 1.0f;
       if(f < 0.0f) {
         lsw = 32768;
         f = f*-1.0f;
       }
       if(tst < f) {
         while(tst < f){
           exp = exp+1;
           tst = tst*2.0f;
         }
       } else {
         while(tst > f*2.0f){
           exp = exp-1;
           tst = tst/2.0f;
         }
       }     
       float a1 = f/tst;
       exp = 128 + exp; 
       if (exp>255) {
        exp=255;
      }
       if (exp<0) {
        exp=0;
      }
       exp = exp*128;
        
       int a2 = (new Float((a1-0.5f)*16777216.0f)).intValue();
       int a3 = a2/65536;
       int a4 = a2 - (a3*65536);
       lsw = lsw+exp+a3;
       msw = a4;
     }
     int[] ret = {lsw,msw};
     return ret;
   }
   
  /**
   * Send data to wherever it is to go. This method transmits the input
   * character message immediately.
   *
   * @param message
   *  The character message.
   *
   */
  public synchronized
  void
  put(
    String message
  )
  {
    sendStr("PUT " + itsLabel + " " + message);
  }
  
  public static void main(String[] args){
    float f = 9.641110f;
 
    int[] words = new int[2];
    words = OutQue.toVAXFloat(f);
    System.out.println(words[0] + "," + words[1]);
    System.out.println(Integer.toHexString(words[0]) + "," + Integer.toHexString(words[1]));
    words = OutQue.toVAXFloat(1.03f);
    System.out.println(words[0] + "," + words[1]);
    System.out.println(Integer.toHexString(words[0]) + "," + Integer.toHexString(words[1]));
    words = OutQue.toVAXFloat(0.000057f);
    System.out.println(words[0] + "," + words[1]);
    System.out.println(Integer.toHexString(words[0]) + "," + Integer.toHexString(words[1]));
    words = OutQue.toVAXFloat(-33.33f);
    System.out.println(words[0] + "," + words[1]);
    System.out.println(Integer.toHexString(words[0]) + "," + Integer.toHexString(words[1]));
    words = OutQue.toVAXFloat(0.0f);
    System.out.println(words[0] + "," + words[1]);
    System.out.println(Integer.toHexString(words[0]) + "," + Integer.toHexString(words[1]));
    words = OutQue.toVAXFloat(-8.0e36f);
    System.out.println(words[0] + "," + words[1]);
    System.out.println(Integer.toHexString(words[0]) + "," + Integer.toHexString(words[1]));
  
  }

  /**
   * Flush all enqueued data to VMS.
   *
   */
  public synchronized
  void
  flush()
  {
    if (itsCount>0) {
      //Insert the correct count at the start of the message
      if (itsCount<10) {
	itsMess.insert(0,"PUT " + itsLabel + "  " + itsCount);
      } else {
	itsMess.insert(0,"PUT " + itsLabel + " " + itsCount);
      }
      //Build the packet to send to VMS
      String message = new String(itsMess);
      sendStr(message);
      itsCount = 0;
    }
  }

  /**
   * Return the label for this OutQueue
   */
  public String getLabel()
  {
    return itsLabel;
  }
}
