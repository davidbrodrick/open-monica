// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.externalsystem;

import java.util.HashMap;
import atnf.atoms.mon.*;

/**
 * Reads data from a Protek 506 digital multimeter. The RS232 interface of the meter must
 * be available over a TCP socket, for instance by using a serial/ethernet converter or a
 * utility like socat to map to map from a serial port to a socket. The meter uses a 7n2
 * serial format at 1200 baud, an example invocation of socat is:<BR>
 * <tt>socat /dev/ttyS0,b1200,raw,cs7,cstopb=1 TCP4-LISTEN:2222</tt>.
 * 
 * <P>
 * The monitor-sources.txt must specify the hostname and port arguments, for instance:<BR>
 * <tt>Protek506 localhost:2222</tt>
 * 
 * <P>
 * This Protek506 driver publishes data as a HashMap with three fields: <bl>
 * <li><b>VAL</b> The current multimeter value, eg "1.46" or "HIGH".
 * <li><b>MODE</b> The current multimeter mode, eg "DC" or "CAP".
 * <li><b>UNITS</b> The measurement units, eg "mV" or "" for dimensionless modes. </bl>
 * 
 * A typical set of monitor point definitions looks like:<BR>
 * <tt>
 * hidden.protek     "Raw multimeter data"  "" "" meter1 T Generic-"localhost:2222"  - -          - - 1000000 -
 * test.protek.Mode  "Multimeter Mode"      "" "" meter1 T Listen-"$1.hidden.protek" - NV-"MODE"  - - 1000000 -
 * test.protek.Value "Multimeter Reading"   "" "" meter1 T Listen-"$1.hidden.protek" - NV-"VAL"   - - 1000000 -
 * test.protek.Units "Multimeter Units"     "" "" meter1 T Listen-"$1.hidden.protek" - NV-"UNITS" - - 1000000 -
 * </tt>
 * 
 * @author David Brodrick
 */
public class Protek506 extends ASCIISocket
{
    /** Constructor, expects host:port[:timeout] argument. */
    public Protek506(String[] args)
    {
        super(args);
    }

    /** Get a new value from the digital multimeter. */
    public Object parseData(PointDescription requestor) throws Exception
    {
        // Purge the read buffer
        while (itsReader.ready()) {
            itsReader.read();
        }
        
        // Write any character to request a new value
        itsWriter.write(" ");
        itsWriter.flush();

        // Read and store response
        final int MAXRESP = 15;
        char[] resp = new char[MAXRESP];
        for (int i = 0; i < MAXRESP; i++) {
            int thischar = itsReader.read();
            if (thischar == -1) {
                // Encountered EOF
                throw new Exception("Encountered EOF");
            } else if (((char) thischar) == '\r') {
                // End of this reading
                break;
            }
            resp[i] = (char) thischar;
        }

        // Parse the response line
        String respstr = new String(resp).trim();
        String[] tokens = respstr.split(" ");
        if (tokens.length<2) {
            // Something obviously went wrong
            throw new Exception("Parse Error");
        }
        HashMap<String, Object> res = new HashMap<String, Object>(3);
        res.put("MODE", tokens[0]);
        try {
            // Tryt o parse the value as a number
            Float floatval = new Float(tokens[1]);
            res.put("VAL", floatval);
        } catch (Exception e) {
            res.put("VAL", tokens[1]);
        }
        if (tokens.length > 2) {
            res.put("UNITS", tokens[2]);
        } else {
            res.put("UNITS", "");
        }

        return res;
    }
}
