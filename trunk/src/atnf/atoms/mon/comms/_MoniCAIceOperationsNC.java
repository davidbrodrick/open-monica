// **********************************************************************
//
// Copyright (c) 2003-2013 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************
//
// Ice version 3.5.0
//
// <auto-generated>
//
// Generated from file `MoniCA.ice'
//
// Warning: do not edit this file.
//
// </auto-generated>
//

package atnf.atoms.mon.comms;

public interface _MoniCAIceOperationsNC
{
    String[] getAllPointNames();

    PointDescriptionIce[] getPoints(String[] names);

    PointDescriptionIce[] getAllPoints();

    boolean addPoints(PointDescriptionIce[] newpoints, String username, String passwd);

    PointDataIce[][] getArchiveData(String[] names, long start, long end, long maxsamples);

    PointDataIce[] getData(String[] names);

    PointDataIce[] getBefore(String[] names, long t);

    PointDataIce[] getAfter(String[] names, long t);

    boolean setData(String[] names, PointDataIce[] values, String username, String passwd);

    String[] getAllSetups();

    boolean addSetup(String setup, String username, String passwd);

    AlarmIce[] getAllAlarms();

    AlarmIce[] getCurrentAlarms();

    boolean acknowledgeAlarms(String[] pointnames, boolean ack, String username, String passwd);

    boolean shelveAlarms(String[] pointnames, boolean shelve, String username, String passwd);

    String[] getEncryptionInfo();

    long getCurrentTime();
}
