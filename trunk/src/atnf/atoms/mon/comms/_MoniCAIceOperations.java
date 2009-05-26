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

public interface _MoniCAIceOperations
{
    String[] getAllPointNames(Ice.Current __current);

    PointDescriptionIce[] getPoints(String[] names, Ice.Current __current);

    PointDescriptionIce[] getAllPoints(Ice.Current __current);

    boolean addPoints(PointDescriptionIce[] newpoints, String username, String passwd, Ice.Current __current);

    PointDataIce[][] getArchiveData(String[] names, long start, long end, long maxsamples, Ice.Current __current);

    PointDataIce[] getData(String[] names, Ice.Current __current);

    boolean setData(String[] names, PointDataIce[] rawvalues, String username, String passwd, Ice.Current __current);

    String[] getAllSetups(Ice.Current __current);

    boolean addSetup(String setup, String username, String passwd, Ice.Current __current);

    String[] getEncryptionInfo(Ice.Current __current);

    long getCurrentTime(Ice.Current __current);
}
