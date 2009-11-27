//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.externalsystem;

import java.util.HashMap;

import atnf.atoms.mon.*;
import atnf.atoms.time.AbsTime;

/**
 * Superclass for receiving data via messages from an IceStorm Topic. Subclasses need to
 * populate the instance field <i>itsSubscriber</i> in their constructor using an
 * instance of their implementation for the relevant Ice interface. The interface
 * implementation should call the <i>gotNewData</i> method when a new message is
 * received.
 * 
 * <P>
 * The constructor/monitor-sources.txt definition expects four arguments:
 * <ol>
 * <li><b>Host:</b> The host name of the IceGrid Locator service used to find IceStorm.
 * <li><b>Port:</b> The port used to contact the IceGrid Locator service.
 * <li><b>Topic:</b> The name of the IceStorm Topic to subscribe to.
 * <li><b>Point:</b> The name of the MoniCA point to update when messages are received.
 * </ol>
 * 
 * @author David Brodrick
 */
public class IceStormSubscriber extends ExternalSystem
{
    /** Name of the point to fire updates to. */
    protected String itsPointName;

    /** The point to fire updates to. */
    protected PointDescription itsPoint;

    /** Name of the IceStorm Topic to subscribe to. */
    protected String itsTopicName;

    /** The IceStorm Topic to subscribe to. */
    protected IceStorm.TopicPrx itsTopic;

    /** The hostname for the IceGrid Locator service. */
    protected String itsHost;

    /** The port number for the IceGrid Locator service. */
    protected int itsPort;

    /** The Ice Communicator. */
    protected Ice.Communicator itsCommunicator;

    /** The actual interface implementation. */
    protected Ice.ObjectImpl itsSubscriber;

    /** Constructor. */
    public IceStormSubscriber(String[] args)
    {
        super(args[0] + ":" + args[1] + ":" + args[2]);
        itsHost = args[0];
        itsPort = Integer.parseInt(args[1]);
        itsTopicName = args[2];
        itsPointName = args[3];
    }

    /** Called by the message receiver class when new data is received. */
    protected void gotNewData(Object newdata)
    {
        gotNewData(new AbsTime(), newdata);
    }

    /** Called by the message receiver class when new data is received. */
    protected void gotNewData(AbsTime timestamp, Object newdata)
    {
        if (itsPoint == null) {
            // Haven't obtained the reference to the specified point yet
            itsPoint = PointDescription.getPoint(itsPointName);
            if (itsPoint == null) {
                System.err.println("IceStormSubscriber: No point called " + itsPointName);
                MonitorMap.logger.warning("IceStormSubscriber: No point called " + itsPointName);
                return;
            }
        }
        // Fire the updated data to the point
        PointData pd = new PointData(itsPointName, timestamp, newdata);
        itsPoint.firePointEvent(new PointEvent(this, pd, true));
    }

    /** Subscribe to the Topic via IceStorm. */
    public synchronized boolean connect() throws Exception
    {
        try {
            Ice.Properties props = Ice.Util.createProperties();
            String locator = "IceGrid/Locator:tcp -h " + itsHost + " -p " + itsPort;
            props.setProperty("Ice.Default.Locator", locator);
            props.setProperty("MoniCAIceStormAdapter.AdapterId", "MoniCAIceStormAdapter");
            props.setProperty("MoniCAIceStormAdapter.Endpoints", "tcp");
            Ice.InitializationData id = new Ice.InitializationData();
            id.properties = props;
            itsCommunicator = Ice.Util.initialize(id);

            Ice.ObjectPrx obj = itsCommunicator.stringToProxy("IceStorm/TopicManager");
            IceStorm.TopicManagerPrx topicManager = IceStorm.TopicManagerPrxHelper.checkedCast(obj);
            Ice.ObjectAdapter adapter = itsCommunicator.createObjectAdapter("MoniCAIceStormAdapter");
            Ice.ObjectPrx proxy = adapter.addWithUUID(itsSubscriber).ice_twoway();

            try {
                itsTopic = topicManager.retrieve(itsTopicName);
                HashMap<String, String> qos = new HashMap<String, String>(1);
                qos.put("reliability", "ordered");
                itsTopic.subscribeAndGetPublisher(null, proxy);
            } catch (IceStorm.NoSuchTopic e0) {
                try {
                    // Create topic if it doesn't already exist
                    itsTopic = topicManager.create(itsTopicName);
                } catch (IceStorm.TopicExists e1) {
                    itsTopic = topicManager.retrieve(itsTopicName);
                }
            }

            adapter.activate();
            itsConnected = true;
        } catch (Exception e) {
            // Connection failed
            disconnect();
        }
        return itsConnected;
    }

    /** Disconnect from the Topic. */
    public synchronized void disconnect() throws Exception
    {
        try {
            if (itsCommunicator != null) {
                itsCommunicator.shutdown();
            }
        } catch (Exception e) {
        }
        itsConnected = false;
    }
}