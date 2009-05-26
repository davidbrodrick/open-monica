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

public interface MoniCAIcePrx extends Ice.ObjectPrx
{
    public String[] getAllPointNames();
    public String[] getAllPointNames(java.util.Map<String, String> __ctx);

    public PointDescriptionIce[] getPoints(String[] names);
    public PointDescriptionIce[] getPoints(String[] names, java.util.Map<String, String> __ctx);

    public PointDescriptionIce[] getAllPoints();
    public PointDescriptionIce[] getAllPoints(java.util.Map<String, String> __ctx);

    public boolean addPoints(PointDescriptionIce[] newpoints, String username, String passwd);
    public boolean addPoints(PointDescriptionIce[] newpoints, String username, String passwd, java.util.Map<String, String> __ctx);

    public PointDataIce[][] getArchiveData(String[] names, long start, long end, long maxsamples);
    public PointDataIce[][] getArchiveData(String[] names, long start, long end, long maxsamples, java.util.Map<String, String> __ctx);

    public PointDataIce[] getData(String[] names);
    public PointDataIce[] getData(String[] names, java.util.Map<String, String> __ctx);

    public boolean setData(String[] names, PointDataIce[] rawvalues, String username, String passwd);
    public boolean setData(String[] names, PointDataIce[] rawvalues, String username, String passwd, java.util.Map<String, String> __ctx);

    public String[] getAllSetups();
    public String[] getAllSetups(java.util.Map<String, String> __ctx);

    public boolean addSetup(String setup, String username, String passwd);
    public boolean addSetup(String setup, String username, String passwd, java.util.Map<String, String> __ctx);

    public String[] getEncryptionInfo();
    public String[] getEncryptionInfo(java.util.Map<String, String> __ctx);

    public long getCurrentTime();
    public long getCurrentTime(java.util.Map<String, String> __ctx);
}
