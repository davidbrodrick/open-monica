package atnf.atoms.mon.externalsystem;

import org.apache.log4j.Logger;
import org.snmp4j.*;
import org.snmp4j.smi.*;
import org.snmp4j.event.*;
import org.snmp4j.security.*;
import org.snmp4j.mp.*;
import org.snmp4j.transport.*;
import org.snmp4j.util.DefaultPDUFactory;

import atnf.atoms.mon.*;
import atnf.atoms.mon.transaction.TransactionStrings;

/**
 * Generic SNMP interface supporting SNMPv1, v2c and NOAUTH, NOPRIV SNMPv3
 * requests, using a provided username. It should be straightforward to extend
 * the class to support write operations and enable fully encrypted SNMPv3
 * support. This class uses the SNMP4J library.
 * 
 * <P>
 * The constructor expects the following arguments:
 * <ul>
 * <li><b>Host Name:</b> The name or IP address of the agent.
 * <li><b>UDP Port:</b> The UDP port (usually 161).
 * <li><b>SNMP Version:</b> "v1", "v2c" or "v3".
 * <li><b>Ident:</b> The username or community, depending on which SNMP version
 * you are using.
 * </ul>
 * 
 * <P>
 * Here is an example entry for <tt>monitor-sources.txt</tt> which connects to
 * "labswitch" using username "dlink".
 * <P>
 * <tt>SNMP labswitch:161:v3:dlink</tt>
 * 
 * <P>
 * The ExternalSystem instances register their channel id's as "snmp-host:port",
 * where host and port are the values provided.
 * 
 * <P>
 * Any points which use SNMP to collect their data need to use a
 * <tt>TransactionStrings</tt> and set the first argument after the channel id
 * to be the OID of the data point to be collected in dot notation. For instance
 * like this <tt>Strings-"snmp-$1:161""1.3.6.1.2.1.1.3.0"</tt>.
 * 
 * @author David Brodrick
 */
public class SNMP extends ExternalSystem {
  /** Logger. */
  private static Logger theirLogger = Logger.getLogger(SNMP.class.getName());

  /** The remote host name. */
  protected String itsHostName;

  /** The remote port. */
  protected int itsPort;

  /** The SNMPv3 user name or v1/2 community. */
  protected String itsIdent;

  /** The SNMP Target. */
  protected Target itsTarget;

  /** The SNMP instance. */
  protected Snmp itsSNMP;
 
  /** The different SNMP versions supported. */
  public static enum SNMPVersion{ v1, v2c, v3 };
  
  /** The SNMP version to use. */
  protected SNMPVersion itsVersion;

  /** String representing SNMP v1. */
  protected final static String theirV1 = "v1";

  /** String representing SNMP v2c. */
  protected final static String theirV2c = "v2c";

  /** String representing SNMP v3. */
  protected final static String theirV3 = "v3";

  public SNMP(String[] args) {
    super("snmp-" + args[0] + ":" + args[1]);
    itsHostName = args[0];
    itsPort = Integer.parseInt(args[1]);
    itsVersion = SNMPVersion.valueOf(SNMPVersion.class, args[2]);
    itsIdent = args[3];

    try {
      TransportMapping transport = new DefaultUdpTransportMapping();
      itsSNMP = new Snmp(transport);

      if (itsVersion==SNMPVersion.v3) {
        USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
        SecurityModels.getInstance().addSecurityModel(usm);
        itsSNMP.getUSM().addUser(new OctetString(itsIdent), new UsmUser(new OctetString(itsIdent), null, null, null, null));
      }
      transport.listen();

      Address targetAddress = GenericAddress.parse("udp:" + itsHostName + "/" + itsPort);

      if (itsVersion==SNMPVersion.v3) {
        itsTarget = new UserTarget();
        itsTarget.setVersion(SnmpConstants.version3);
        ((UserTarget) itsTarget).setSecurityLevel(SecurityLevel.NOAUTH_NOPRIV);
        ((UserTarget) itsTarget).setSecurityName(new OctetString(itsIdent));
      } else {
        itsTarget = new CommunityTarget();
        if (itsVersion==SNMPVersion.v1) {
          itsTarget.setVersion(SnmpConstants.version1);
        } else {
          itsTarget.setVersion(SnmpConstants.version2c);
        }
        ((CommunityTarget) itsTarget).setCommunity(new OctetString(itsIdent));
      }

      itsTarget.setAddress(targetAddress);
      itsTarget.setRetries(1);
      itsTarget.setTimeout(5000);

      itsConnected = true;
    } catch (Exception e) {
      theirLogger.fatal("Error while creating SNMP classes: " + e);
      itsConnected = false;
    }
  }

  public void getData(PointDescription[] points) throws Exception {
    for (int i = 0; i < points.length; i++) {
      PointDescription pm = points[i];
      TransactionStrings tds = (TransactionStrings) getMyTransactions(pm.getInputTransactions()).get(0);

      // Check we have correct number of arguments
      if (tds.getNumStrings() < 1) {
        theirLogger.error("(" + itsHostName + "): Expect OID argument in Transaction for point \"" + pm.getFullName() + "\"");
        throw new IllegalArgumentException("Missing OID argument in Transaction");
      }
      // Create an OID from the string argument
      OID oid = new OID(tds.getString());

      // Send the SNMP request
      PDU pdu = DefaultPDUFactory.createPDU(itsTarget, PDU.GET);
      pdu.setType(PDU.GET);
      pdu.add(new VariableBinding(oid));
      ResponseEvent response = itsSNMP.send(pdu, itsTarget);

      // Process response
      PDU responsePDU = response.getResponse();
      PointData newdata;
      if (responsePDU == null || responsePDU.getErrorStatus() != SnmpConstants.SNMP_ERROR_SUCCESS) {
        // Response timed out or was in error, so fire event with null data
        newdata = new PointData(pm.getFullName());
      } else {
        // Fire event with new data value (always as a string)
        newdata = new PointData(pm.getFullName(), responsePDU.get(0).getVariable().toString());
      }
      pm.firePointEvent(new PointEvent(this, newdata, true));

      // Increment the transaction counter for this ExternalSystem
      itsNumTransactions++;
    }
  }
}
