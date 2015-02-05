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

public abstract class _MoniCAIceDisp extends Ice.ObjectImpl implements MoniCAIce
{
    protected void
    ice_copyStateFrom(Ice.Object __obj)
        throws java.lang.CloneNotSupportedException
    {
        throw new java.lang.CloneNotSupportedException();
    }

    public static final String[] __ids =
    {
        "::Ice::Object",
        "::atnf::atoms::mon::comms::MoniCAIce"
    };

    public boolean ice_isA(String s)
    {
        return java.util.Arrays.binarySearch(__ids, s) >= 0;
    }

    public boolean ice_isA(String s, Ice.Current __current)
    {
        return java.util.Arrays.binarySearch(__ids, s) >= 0;
    }

    public String[] ice_ids()
    {
        return __ids;
    }

    public String[] ice_ids(Ice.Current __current)
    {
        return __ids;
    }

    public String ice_id()
    {
        return __ids[1];
    }

    public String ice_id(Ice.Current __current)
    {
        return __ids[1];
    }

    public static String ice_staticId()
    {
        return __ids[1];
    }

    public final boolean acknowledgeAlarms(String[] pointnames, boolean ack, String username, String passwd)
    {
        return acknowledgeAlarms(pointnames, ack, username, passwd, null);
    }

    public final boolean addPoints(PointDescriptionIce[] newpoints, String username, String passwd)
    {
        return addPoints(newpoints, username, passwd, null);
    }

    public final boolean addSetup(String setup, String username, String passwd)
    {
        return addSetup(setup, username, passwd, null);
    }

    public final PointDataIce[] getAfter(String[] names, long t)
    {
        return getAfter(names, t, null);
    }

    public final AlarmIce[] getAllAlarms()
    {
        return getAllAlarms(null);
    }

    public final String[] getAllPointNames()
    {
        return getAllPointNames(null);
    }

    public final String[] getAllPointNamesChunk(int start, int num)
    {
        return getAllPointNamesChunk(start, num, null);
    }

    public final PointDescriptionIce[] getAllPoints()
    {
        return getAllPoints(null);
    }

    public final PointDescriptionIce[] getAllPointsChunk(int start, int num)
    {
        return getAllPointsChunk(start, num, null);
    }

    public final String[] getAllSetups()
    {
        return getAllSetups(null);
    }

    public final PointDataIce[][] getArchiveData(String[] names, long start, long end, long maxsamples)
    {
        return getArchiveData(names, start, end, maxsamples, null);
    }

    public final PointDataIce[] getBefore(String[] names, long t)
    {
        return getBefore(names, t, null);
    }

    public final AlarmIce[] getCurrentAlarms()
    {
        return getCurrentAlarms(null);
    }

    public final long getCurrentTime()
    {
        return getCurrentTime(null);
    }

    public final PointDataIce[] getData(String[] names)
    {
        return getData(names, null);
    }

    public final String[] getEncryptionInfo()
    {
        return getEncryptionInfo(null);
    }

    public final dUTCEntry[] getLeapSeconds()
    {
        return getLeapSeconds(null);
    }

    public final PointDescriptionIce[] getPoints(String[] names)
    {
        return getPoints(names, null);
    }

    public final boolean setData(String[] names, PointDataIce[] values, String username, String passwd)
    {
        return setData(names, values, username, passwd, null);
    }

    public final boolean shelveAlarms(String[] pointnames, boolean shelve, String username, String passwd)
    {
        return shelveAlarms(pointnames, shelve, username, passwd, null);
    }

    public static Ice.DispatchStatus ___getAllPointNames(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Idempotent, __current.mode);
        __inS.readEmptyParams();
        String[] __ret = __obj.getAllPointNames(__current);
        IceInternal.BasicStream __os = __inS.__startWriteParams(Ice.FormatType.DefaultFormat);
        stringarrayHelper.write(__os, __ret);
        __inS.__endWriteParams(true);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus ___getAllPointNamesChunk(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Idempotent, __current.mode);
        IceInternal.BasicStream __is = __inS.startReadParams();
        int start;
        int num;
        start = __is.readInt();
        num = __is.readInt();
        __inS.endReadParams();
        String[] __ret = __obj.getAllPointNamesChunk(start, num, __current);
        IceInternal.BasicStream __os = __inS.__startWriteParams(Ice.FormatType.DefaultFormat);
        stringarrayHelper.write(__os, __ret);
        __inS.__endWriteParams(true);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus ___getPoints(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Idempotent, __current.mode);
        IceInternal.BasicStream __is = __inS.startReadParams();
        String[] names;
        names = stringarrayHelper.read(__is);
        __inS.endReadParams();
        PointDescriptionIce[] __ret = __obj.getPoints(names, __current);
        IceInternal.BasicStream __os = __inS.__startWriteParams(Ice.FormatType.DefaultFormat);
        pointarrayHelper.write(__os, __ret);
        __inS.__endWriteParams(true);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus ___getAllPoints(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Idempotent, __current.mode);
        __inS.readEmptyParams();
        PointDescriptionIce[] __ret = __obj.getAllPoints(__current);
        IceInternal.BasicStream __os = __inS.__startWriteParams(Ice.FormatType.DefaultFormat);
        pointarrayHelper.write(__os, __ret);
        __inS.__endWriteParams(true);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus ___getAllPointsChunk(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Idempotent, __current.mode);
        IceInternal.BasicStream __is = __inS.startReadParams();
        int start;
        int num;
        start = __is.readInt();
        num = __is.readInt();
        __inS.endReadParams();
        PointDescriptionIce[] __ret = __obj.getAllPointsChunk(start, num, __current);
        IceInternal.BasicStream __os = __inS.__startWriteParams(Ice.FormatType.DefaultFormat);
        pointarrayHelper.write(__os, __ret);
        __inS.__endWriteParams(true);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus ___addPoints(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Normal, __current.mode);
        IceInternal.BasicStream __is = __inS.startReadParams();
        PointDescriptionIce[] newpoints;
        String username;
        String passwd;
        newpoints = pointarrayHelper.read(__is);
        username = __is.readString();
        passwd = __is.readString();
        __inS.endReadParams();
        boolean __ret = __obj.addPoints(newpoints, username, passwd, __current);
        IceInternal.BasicStream __os = __inS.__startWriteParams(Ice.FormatType.DefaultFormat);
        __os.writeBool(__ret);
        __inS.__endWriteParams(true);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus ___getArchiveData(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Idempotent, __current.mode);
        IceInternal.BasicStream __is = __inS.startReadParams();
        String[] names;
        long start;
        long end;
        long maxsamples;
        names = stringarrayHelper.read(__is);
        start = __is.readLong();
        end = __is.readLong();
        maxsamples = __is.readLong();
        __inS.endReadParams();
        PointDataIce[][] __ret = __obj.getArchiveData(names, start, end, maxsamples, __current);
        IceInternal.BasicStream __os = __inS.__startWriteParams(Ice.FormatType.DefaultFormat);
        pointdatasetarrayHelper.write(__os, __ret);
        __os.writePendingObjects();
        __inS.__endWriteParams(true);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus ___getData(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Idempotent, __current.mode);
        IceInternal.BasicStream __is = __inS.startReadParams();
        String[] names;
        names = stringarrayHelper.read(__is);
        __inS.endReadParams();
        PointDataIce[] __ret = __obj.getData(names, __current);
        IceInternal.BasicStream __os = __inS.__startWriteParams(Ice.FormatType.DefaultFormat);
        pointdatasetHelper.write(__os, __ret);
        __os.writePendingObjects();
        __inS.__endWriteParams(true);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus ___getBefore(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Idempotent, __current.mode);
        IceInternal.BasicStream __is = __inS.startReadParams();
        String[] names;
        long t;
        names = stringarrayHelper.read(__is);
        t = __is.readLong();
        __inS.endReadParams();
        PointDataIce[] __ret = __obj.getBefore(names, t, __current);
        IceInternal.BasicStream __os = __inS.__startWriteParams(Ice.FormatType.DefaultFormat);
        pointdatasetHelper.write(__os, __ret);
        __os.writePendingObjects();
        __inS.__endWriteParams(true);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus ___getAfter(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Idempotent, __current.mode);
        IceInternal.BasicStream __is = __inS.startReadParams();
        String[] names;
        long t;
        names = stringarrayHelper.read(__is);
        t = __is.readLong();
        __inS.endReadParams();
        PointDataIce[] __ret = __obj.getAfter(names, t, __current);
        IceInternal.BasicStream __os = __inS.__startWriteParams(Ice.FormatType.DefaultFormat);
        pointdatasetHelper.write(__os, __ret);
        __os.writePendingObjects();
        __inS.__endWriteParams(true);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus ___setData(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Normal, __current.mode);
        IceInternal.BasicStream __is = __inS.startReadParams();
        String[] names;
        PointDataIce[] values;
        String username;
        String passwd;
        names = stringarrayHelper.read(__is);
        values = pointdatasetHelper.read(__is);
        username = __is.readString();
        passwd = __is.readString();
        __is.readPendingObjects();
        __inS.endReadParams();
        boolean __ret = __obj.setData(names, values, username, passwd, __current);
        IceInternal.BasicStream __os = __inS.__startWriteParams(Ice.FormatType.DefaultFormat);
        __os.writeBool(__ret);
        __inS.__endWriteParams(true);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus ___getAllSetups(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Idempotent, __current.mode);
        __inS.readEmptyParams();
        String[] __ret = __obj.getAllSetups(__current);
        IceInternal.BasicStream __os = __inS.__startWriteParams(Ice.FormatType.DefaultFormat);
        stringarrayHelper.write(__os, __ret);
        __inS.__endWriteParams(true);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus ___addSetup(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Normal, __current.mode);
        IceInternal.BasicStream __is = __inS.startReadParams();
        String setup;
        String username;
        String passwd;
        setup = __is.readString();
        username = __is.readString();
        passwd = __is.readString();
        __inS.endReadParams();
        boolean __ret = __obj.addSetup(setup, username, passwd, __current);
        IceInternal.BasicStream __os = __inS.__startWriteParams(Ice.FormatType.DefaultFormat);
        __os.writeBool(__ret);
        __inS.__endWriteParams(true);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus ___getAllAlarms(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Normal, __current.mode);
        __inS.readEmptyParams();
        AlarmIce[] __ret = __obj.getAllAlarms(__current);
        IceInternal.BasicStream __os = __inS.__startWriteParams(Ice.FormatType.DefaultFormat);
        alarmarrayHelper.write(__os, __ret);
        __os.writePendingObjects();
        __inS.__endWriteParams(true);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus ___getCurrentAlarms(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Normal, __current.mode);
        __inS.readEmptyParams();
        AlarmIce[] __ret = __obj.getCurrentAlarms(__current);
        IceInternal.BasicStream __os = __inS.__startWriteParams(Ice.FormatType.DefaultFormat);
        alarmarrayHelper.write(__os, __ret);
        __os.writePendingObjects();
        __inS.__endWriteParams(true);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus ___acknowledgeAlarms(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Normal, __current.mode);
        IceInternal.BasicStream __is = __inS.startReadParams();
        String[] pointnames;
        boolean ack;
        String username;
        String passwd;
        pointnames = stringarrayHelper.read(__is);
        ack = __is.readBool();
        username = __is.readString();
        passwd = __is.readString();
        __inS.endReadParams();
        boolean __ret = __obj.acknowledgeAlarms(pointnames, ack, username, passwd, __current);
        IceInternal.BasicStream __os = __inS.__startWriteParams(Ice.FormatType.DefaultFormat);
        __os.writeBool(__ret);
        __inS.__endWriteParams(true);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus ___shelveAlarms(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Normal, __current.mode);
        IceInternal.BasicStream __is = __inS.startReadParams();
        String[] pointnames;
        boolean shelve;
        String username;
        String passwd;
        pointnames = stringarrayHelper.read(__is);
        shelve = __is.readBool();
        username = __is.readString();
        passwd = __is.readString();
        __inS.endReadParams();
        boolean __ret = __obj.shelveAlarms(pointnames, shelve, username, passwd, __current);
        IceInternal.BasicStream __os = __inS.__startWriteParams(Ice.FormatType.DefaultFormat);
        __os.writeBool(__ret);
        __inS.__endWriteParams(true);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus ___getEncryptionInfo(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Idempotent, __current.mode);
        __inS.readEmptyParams();
        String[] __ret = __obj.getEncryptionInfo(__current);
        IceInternal.BasicStream __os = __inS.__startWriteParams(Ice.FormatType.DefaultFormat);
        stringarrayHelper.write(__os, __ret);
        __inS.__endWriteParams(true);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus ___getCurrentTime(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Idempotent, __current.mode);
        __inS.readEmptyParams();
        long __ret = __obj.getCurrentTime(__current);
        IceInternal.BasicStream __os = __inS.__startWriteParams(Ice.FormatType.DefaultFormat);
        __os.writeLong(__ret);
        __inS.__endWriteParams(true);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus ___getLeapSeconds(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Idempotent, __current.mode);
        __inS.readEmptyParams();
        dUTCEntry[] __ret = __obj.getLeapSeconds(__current);
        IceInternal.BasicStream __os = __inS.__startWriteParams(Ice.FormatType.DefaultFormat);
        dutcarrayHelper.write(__os, __ret);
        __inS.__endWriteParams(true);
        return Ice.DispatchStatus.DispatchOK;
    }

    private final static String[] __all =
    {
        "acknowledgeAlarms",
        "addPoints",
        "addSetup",
        "getAfter",
        "getAllAlarms",
        "getAllPointNames",
        "getAllPointNamesChunk",
        "getAllPoints",
        "getAllPointsChunk",
        "getAllSetups",
        "getArchiveData",
        "getBefore",
        "getCurrentAlarms",
        "getCurrentTime",
        "getData",
        "getEncryptionInfo",
        "getLeapSeconds",
        "getPoints",
        "ice_id",
        "ice_ids",
        "ice_isA",
        "ice_ping",
        "setData",
        "shelveAlarms"
    };

    public Ice.DispatchStatus __dispatch(IceInternal.Incoming in, Ice.Current __current)
    {
        int pos = java.util.Arrays.binarySearch(__all, __current.operation);
        if(pos < 0)
        {
            throw new Ice.OperationNotExistException(__current.id, __current.facet, __current.operation);
        }

        switch(pos)
        {
            case 0:
            {
                return ___acknowledgeAlarms(this, in, __current);
            }
            case 1:
            {
                return ___addPoints(this, in, __current);
            }
            case 2:
            {
                return ___addSetup(this, in, __current);
            }
            case 3:
            {
                return ___getAfter(this, in, __current);
            }
            case 4:
            {
                return ___getAllAlarms(this, in, __current);
            }
            case 5:
            {
                return ___getAllPointNames(this, in, __current);
            }
            case 6:
            {
                return ___getAllPointNamesChunk(this, in, __current);
            }
            case 7:
            {
                return ___getAllPoints(this, in, __current);
            }
            case 8:
            {
                return ___getAllPointsChunk(this, in, __current);
            }
            case 9:
            {
                return ___getAllSetups(this, in, __current);
            }
            case 10:
            {
                return ___getArchiveData(this, in, __current);
            }
            case 11:
            {
                return ___getBefore(this, in, __current);
            }
            case 12:
            {
                return ___getCurrentAlarms(this, in, __current);
            }
            case 13:
            {
                return ___getCurrentTime(this, in, __current);
            }
            case 14:
            {
                return ___getData(this, in, __current);
            }
            case 15:
            {
                return ___getEncryptionInfo(this, in, __current);
            }
            case 16:
            {
                return ___getLeapSeconds(this, in, __current);
            }
            case 17:
            {
                return ___getPoints(this, in, __current);
            }
            case 18:
            {
                return ___ice_id(this, in, __current);
            }
            case 19:
            {
                return ___ice_ids(this, in, __current);
            }
            case 20:
            {
                return ___ice_isA(this, in, __current);
            }
            case 21:
            {
                return ___ice_ping(this, in, __current);
            }
            case 22:
            {
                return ___setData(this, in, __current);
            }
            case 23:
            {
                return ___shelveAlarms(this, in, __current);
            }
        }

        assert(false);
        throw new Ice.OperationNotExistException(__current.id, __current.facet, __current.operation);
    }

    protected void __writeImpl(IceInternal.BasicStream __os)
    {
        __os.startWriteSlice(ice_staticId(), -1, true);
        __os.endWriteSlice();
    }

    protected void __readImpl(IceInternal.BasicStream __is)
    {
        __is.startReadSlice();
        __is.endReadSlice();
    }

    public static final long serialVersionUID = 0L;
}
