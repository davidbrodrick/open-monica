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

public interface _MoniCAIceDel extends Ice._ObjectDel
{
    String[] getAllPointNames(java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    PointDescriptionIce[] getPoints(String[] names, java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    PointDescriptionIce[] getAllPoints(java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    boolean addPoints(PointDescriptionIce[] newpoints, String username, String passwd, java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    PointDataIce[][] getArchiveData(String[] names, long start, long end, long maxsamples, java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    PointDataIce[] getData(String[] names, java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    boolean setData(String[] names, PointDataIce[] rawvalues, String username, String passwd, java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    String[] getAllSetups(java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    boolean addSetup(String setup, String username, String passwd, java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    String[] getEncryptionInfo(java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    long getCurrentTime(java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;
}
