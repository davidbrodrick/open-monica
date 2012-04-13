// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.externalsystem;

import java.net.*;

/**
 * Abstract base class for ExternalSystems which need to read and/or write data over TCP
 * socket connection to a remote end-point. This simply handles creation of the socket.
 * <P>
 * The constructor argument defined in <tt>monitor-sources.txt</tt> must include the
 * remote machine, port and optionally timeout in
 * <tt>host:port:timeout_ms:your_other_args</tt> format.
 * 
 * @author David Brodrick
 */
public abstract class TCPSocket extends ExternalSystem
{
  /** The socket used for communicating with the remote service. */
  protected Socket itsSocket = null;

    /** The port to connect to the remote end-point. */
    protected int itsPort = -1;

    /** The hostname or IP of the remote end-point. */
    protected String itsHostName = null;

    /** Socket timeout period, in ms. */
    protected int itsTimeout = 5000;

    /** Argument must include host:port and optionally :timeout_ms */
    public TCPSocket(String[] args)
    {
        super(args[0] + ":" + args[1]);
        itsHostName = args[0];
        itsPort = Integer.parseInt(args[1]);
        if (args.length > 2) {
            itsTimeout = Integer.parseInt(args[2]);
        }
    }

    /** Set the socket timeout to use (ms). */
    protected void setTimeout(int ms)
    {
        itsTimeout = ms;
        if (itsConnected) {
            try {
                itsSocket.setSoTimeout(itsTimeout);
            } catch (Exception e) {
                try {
                    disconnect();
                } catch (Exception f) {
                }
            }
        }
    }

    /** Make a new socket connection. */
    public boolean connect() throws Exception
    {
        try {
            itsSocket = new Socket(itsHostName, itsPort);
            itsSocket.setSoTimeout(itsTimeout);
            itsConnected = true;
            itsNumTransactions = 0;
        } catch (Exception e) {
            itsSocket = null;
            itsConnected = false;
            throw e;
        }
        return itsConnected;
    }

    /** Close the socket, unless it is already closed. */
    public void disconnect() throws Exception
    {
        if (itsSocket != null) {
            itsSocket.close();
        }
        itsSocket = null;
        itsConnected = false;
    }
}
