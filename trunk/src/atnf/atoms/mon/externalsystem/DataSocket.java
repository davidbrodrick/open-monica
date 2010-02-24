// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.externalsystem;

import java.io.*;

/**
 * Abstract base class for ExternalSystems which need to read and/or write binary data
 * over TCP socket connection to a remote end-point. This class derives from the TCPSocket
 * class but also provides DataInputStream and DataOutputStream fields.
 * <P>
 * The constructor argument defined in <tt>monitor-sources.txt</tt> must include the
 * remote machine, port and optionally timeout in
 * <tt>host:port:timeout_ms:your_other_args</tt> format.
 * 
 * @author David Brodrick
 */
public class DataSocket extends TCPSocket
{
    /** The output stream for writing data to the remote service. */
    protected DataOutputStream itsWriter = null;

    /** The input stream for reading data from the remote service. */
    protected DataInputStream itsReader = null;

    /** Argument must include host:port and optionally :timeout_ms */
    public DataSocket(String[] args)
    {
        super(args);
    }

    /** Make a new socket connection. */
    public boolean connect() throws Exception
    {
        try {
            super.connect();
            itsWriter = new DataOutputStream(itsSocket.getOutputStream());
            itsReader = new DataInputStream(itsSocket.getInputStream());
        } catch (Exception e) {
            itsReader = null;
            itsWriter = null;
            super.disconnect();
            throw e;
        }
        return itsConnected;
    }

    /** Close the socket, unless it is already closed. */
    public void disconnect() throws Exception
    {
        itsReader = null;
        itsWriter = null;
        super.disconnect();
    }
}
