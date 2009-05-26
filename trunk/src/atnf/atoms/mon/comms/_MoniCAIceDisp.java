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

    public boolean
    ice_isA(String s)
    {
        return java.util.Arrays.binarySearch(__ids, s) >= 0;
    }

    public boolean
    ice_isA(String s, Ice.Current __current)
    {
        return java.util.Arrays.binarySearch(__ids, s) >= 0;
    }

    public String[]
    ice_ids()
    {
        return __ids;
    }

    public String[]
    ice_ids(Ice.Current __current)
    {
        return __ids;
    }

    public String
    ice_id()
    {
        return __ids[1];
    }

    public String
    ice_id(Ice.Current __current)
    {
        return __ids[1];
    }

    public static String
    ice_staticId()
    {
        return __ids[1];
    }

    public final boolean
    addPoints(PointDescriptionIce[] newpoints, String username, String passwd)
    {
        return addPoints(newpoints, username, passwd, null);
    }

    public final boolean
    addSetup(String setup, String username, String passwd)
    {
        return addSetup(setup, username, passwd, null);
    }

    public final String[]
    getAllPointNames()
    {
        return getAllPointNames(null);
    }

    public final PointDescriptionIce[]
    getAllPoints()
    {
        return getAllPoints(null);
    }

    public final String[]
    getAllSetups()
    {
        return getAllSetups(null);
    }

    public final PointDataIce[][]
    getArchiveData(String[] names, long start, long end, long maxsamples)
    {
        return getArchiveData(names, start, end, maxsamples, null);
    }

    public final long
    getCurrentTime()
    {
        return getCurrentTime(null);
    }

    public final PointDataIce[]
    getData(String[] names)
    {
        return getData(names, null);
    }

    public final String[]
    getEncryptionInfo()
    {
        return getEncryptionInfo(null);
    }

    public final PointDescriptionIce[]
    getPoints(String[] names)
    {
        return getPoints(names, null);
    }

    public final boolean
    setData(String[] names, PointDataIce[] rawvalues, String username, String passwd)
    {
        return setData(names, rawvalues, username, passwd, null);
    }

    public static Ice.DispatchStatus
    ___getAllPointNames(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Idempotent, __current.mode);
        __inS.is().skipEmptyEncaps();
        IceInternal.BasicStream __os = __inS.os();
        String[] __ret = __obj.getAllPointNames(__current);
        stringarrayHelper.write(__os, __ret);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus
    ___getPoints(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Idempotent, __current.mode);
        IceInternal.BasicStream __is = __inS.is();
        __is.startReadEncaps();
        String[] names;
        names = stringarrayHelper.read(__is);
        __is.endReadEncaps();
        IceInternal.BasicStream __os = __inS.os();
        PointDescriptionIce[] __ret = __obj.getPoints(names, __current);
        pointarrayHelper.write(__os, __ret);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus
    ___getAllPoints(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Idempotent, __current.mode);
        __inS.is().skipEmptyEncaps();
        IceInternal.BasicStream __os = __inS.os();
        PointDescriptionIce[] __ret = __obj.getAllPoints(__current);
        pointarrayHelper.write(__os, __ret);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus
    ___addPoints(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Normal, __current.mode);
        IceInternal.BasicStream __is = __inS.is();
        __is.startReadEncaps();
        PointDescriptionIce[] newpoints;
        newpoints = pointarrayHelper.read(__is);
        String username;
        username = __is.readString();
        String passwd;
        passwd = __is.readString();
        __is.endReadEncaps();
        IceInternal.BasicStream __os = __inS.os();
        boolean __ret = __obj.addPoints(newpoints, username, passwd, __current);
        __os.writeBool(__ret);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus
    ___getArchiveData(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Idempotent, __current.mode);
        IceInternal.BasicStream __is = __inS.is();
        __is.startReadEncaps();
        String[] names;
        names = stringarrayHelper.read(__is);
        long start;
        start = __is.readLong();
        long end;
        end = __is.readLong();
        long maxsamples;
        maxsamples = __is.readLong();
        __is.endReadEncaps();
        IceInternal.BasicStream __os = __inS.os();
        PointDataIce[][] __ret = __obj.getArchiveData(names, start, end, maxsamples, __current);
        pointdatasetarrayHelper.write(__os, __ret);
        __os.writePendingObjects();
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus
    ___getData(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Idempotent, __current.mode);
        IceInternal.BasicStream __is = __inS.is();
        __is.startReadEncaps();
        String[] names;
        names = stringarrayHelper.read(__is);
        __is.endReadEncaps();
        IceInternal.BasicStream __os = __inS.os();
        PointDataIce[] __ret = __obj.getData(names, __current);
        pointdatasetHelper.write(__os, __ret);
        __os.writePendingObjects();
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus
    ___setData(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Normal, __current.mode);
        IceInternal.BasicStream __is = __inS.is();
        __is.startReadEncaps();
        String[] names;
        names = stringarrayHelper.read(__is);
        PointDataIce[] rawvalues;
        rawvalues = pointdatasetHelper.read(__is);
        String username;
        username = __is.readString();
        String passwd;
        passwd = __is.readString();
        __is.readPendingObjects();
        __is.endReadEncaps();
        IceInternal.BasicStream __os = __inS.os();
        boolean __ret = __obj.setData(names, rawvalues, username, passwd, __current);
        __os.writeBool(__ret);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus
    ___getAllSetups(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Idempotent, __current.mode);
        __inS.is().skipEmptyEncaps();
        IceInternal.BasicStream __os = __inS.os();
        String[] __ret = __obj.getAllSetups(__current);
        stringarrayHelper.write(__os, __ret);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus
    ___addSetup(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Normal, __current.mode);
        IceInternal.BasicStream __is = __inS.is();
        __is.startReadEncaps();
        String setup;
        setup = __is.readString();
        String username;
        username = __is.readString();
        String passwd;
        passwd = __is.readString();
        __is.endReadEncaps();
        IceInternal.BasicStream __os = __inS.os();
        boolean __ret = __obj.addSetup(setup, username, passwd, __current);
        __os.writeBool(__ret);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus
    ___getEncryptionInfo(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Idempotent, __current.mode);
        __inS.is().skipEmptyEncaps();
        IceInternal.BasicStream __os = __inS.os();
        String[] __ret = __obj.getEncryptionInfo(__current);
        stringarrayHelper.write(__os, __ret);
        return Ice.DispatchStatus.DispatchOK;
    }

    public static Ice.DispatchStatus
    ___getCurrentTime(MoniCAIce __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
        __checkMode(Ice.OperationMode.Idempotent, __current.mode);
        __inS.is().skipEmptyEncaps();
        IceInternal.BasicStream __os = __inS.os();
        long __ret = __obj.getCurrentTime(__current);
        __os.writeLong(__ret);
        return Ice.DispatchStatus.DispatchOK;
    }

    private final static String[] __all =
    {
        "addPoints",
        "addSetup",
        "getAllPointNames",
        "getAllPoints",
        "getAllSetups",
        "getArchiveData",
        "getCurrentTime",
        "getData",
        "getEncryptionInfo",
        "getPoints",
        "ice_id",
        "ice_ids",
        "ice_isA",
        "ice_ping",
        "setData"
    };

    public Ice.DispatchStatus
    __dispatch(IceInternal.Incoming in, Ice.Current __current)
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
                return ___addPoints(this, in, __current);
            }
            case 1:
            {
                return ___addSetup(this, in, __current);
            }
            case 2:
            {
                return ___getAllPointNames(this, in, __current);
            }
            case 3:
            {
                return ___getAllPoints(this, in, __current);
            }
            case 4:
            {
                return ___getAllSetups(this, in, __current);
            }
            case 5:
            {
                return ___getArchiveData(this, in, __current);
            }
            case 6:
            {
                return ___getCurrentTime(this, in, __current);
            }
            case 7:
            {
                return ___getData(this, in, __current);
            }
            case 8:
            {
                return ___getEncryptionInfo(this, in, __current);
            }
            case 9:
            {
                return ___getPoints(this, in, __current);
            }
            case 10:
            {
                return ___ice_id(this, in, __current);
            }
            case 11:
            {
                return ___ice_ids(this, in, __current);
            }
            case 12:
            {
                return ___ice_isA(this, in, __current);
            }
            case 13:
            {
                return ___ice_ping(this, in, __current);
            }
            case 14:
            {
                return ___setData(this, in, __current);
            }
        }

        assert(false);
        throw new Ice.OperationNotExistException(__current.id, __current.facet, __current.operation);
    }

    public void
    __write(IceInternal.BasicStream __os)
    {
        __os.writeTypeId(ice_staticId());
        __os.startWriteSlice();
        __os.endWriteSlice();
        super.__write(__os);
    }

    public void
    __read(IceInternal.BasicStream __is, boolean __rid)
    {
        if(__rid)
        {
            __is.readTypeId();
        }
        __is.startReadSlice();
        __is.endReadSlice();
        super.__read(__is, true);
    }

    public void
    __write(Ice.OutputStream __outS)
    {
        Ice.MarshalException ex = new Ice.MarshalException();
        ex.reason = "type atnf::atoms::mon::comms::MoniCAIce was not generated with stream support";
        throw ex;
    }

    public void
    __read(Ice.InputStream __inS, boolean __rid)
    {
        Ice.MarshalException ex = new Ice.MarshalException();
        ex.reason = "type atnf::atoms::mon::comms::MoniCAIce was not generated with stream support";
        throw ex;
    }
}
