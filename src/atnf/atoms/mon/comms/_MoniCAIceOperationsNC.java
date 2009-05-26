// **********************************************************************
//
// Copyright (c) 2003-2008 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

// Ice version 3.3.0

package atnf.atoms.mon.comms;

public interface _MoniCAIceOperationsNC
{
    String[] getAllPointNames();

    PointDescriptionIce[] getPoints(String[] names);

    PointDescriptionIce[] getAllPoints();

    boolean addPoints(PointDescriptionIce[] newpoints, String username, String passwd);

    PointDataIce[][] getArchiveData(String[] names, long start, long end, long maxsamples);

    PointDataIce[] getData(String[] names);

    boolean setData(String[] names, PointDataIce[] rawvalues, String username, String passwd);

    String[] getAllSetups();

    boolean addSetup(String setup, String username, String passwd);

    String[] getEncryptionInfo();

    long getCurrentTime();
}
