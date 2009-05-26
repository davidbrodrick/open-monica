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

public final class MoniCAIcePrxHelper extends Ice.ObjectPrxHelperBase implements MoniCAIcePrx
{
    public boolean
    addPoints(PointDescriptionIce[] newpoints, String username, String passwd)
    {
        return addPoints(newpoints, username, passwd, null, false);
    }

    public boolean
    addPoints(PointDescriptionIce[] newpoints, String username, String passwd, java.util.Map<String, String> __ctx)
    {
        return addPoints(newpoints, username, passwd, __ctx, true);
    }

    @SuppressWarnings("unchecked")
    private boolean
    addPoints(PointDescriptionIce[] newpoints, String username, String passwd, java.util.Map<String, String> __ctx, boolean __explicitCtx)
    {
        if(__explicitCtx && __ctx == null)
        {
            __ctx = _emptyContext;
        }
        int __cnt = 0;
        while(true)
        {
            Ice._ObjectDel __delBase = null;
            try
            {
                __checkTwowayOnly("addPoints");
                __delBase = __getDelegate(false);
                _MoniCAIceDel __del = (_MoniCAIceDel)__delBase;
                return __del.addPoints(newpoints, username, passwd, __ctx);
            }
            catch(IceInternal.LocalExceptionWrapper __ex)
            {
                __handleExceptionWrapper(__delBase, __ex, null);
            }
            catch(Ice.LocalException __ex)
            {
                __cnt = __handleException(__delBase, __ex, null, __cnt);
            }
        }
    }

    public boolean
    addSetup(String setup, String username, String passwd)
    {
        return addSetup(setup, username, passwd, null, false);
    }

    public boolean
    addSetup(String setup, String username, String passwd, java.util.Map<String, String> __ctx)
    {
        return addSetup(setup, username, passwd, __ctx, true);
    }

    @SuppressWarnings("unchecked")
    private boolean
    addSetup(String setup, String username, String passwd, java.util.Map<String, String> __ctx, boolean __explicitCtx)
    {
        if(__explicitCtx && __ctx == null)
        {
            __ctx = _emptyContext;
        }
        int __cnt = 0;
        while(true)
        {
            Ice._ObjectDel __delBase = null;
            try
            {
                __checkTwowayOnly("addSetup");
                __delBase = __getDelegate(false);
                _MoniCAIceDel __del = (_MoniCAIceDel)__delBase;
                return __del.addSetup(setup, username, passwd, __ctx);
            }
            catch(IceInternal.LocalExceptionWrapper __ex)
            {
                __handleExceptionWrapper(__delBase, __ex, null);
            }
            catch(Ice.LocalException __ex)
            {
                __cnt = __handleException(__delBase, __ex, null, __cnt);
            }
        }
    }

    public String[]
    getAllPointNames()
    {
        return getAllPointNames(null, false);
    }

    public String[]
    getAllPointNames(java.util.Map<String, String> __ctx)
    {
        return getAllPointNames(__ctx, true);
    }

    @SuppressWarnings("unchecked")
    private String[]
    getAllPointNames(java.util.Map<String, String> __ctx, boolean __explicitCtx)
    {
        if(__explicitCtx && __ctx == null)
        {
            __ctx = _emptyContext;
        }
        int __cnt = 0;
        while(true)
        {
            Ice._ObjectDel __delBase = null;
            try
            {
                __checkTwowayOnly("getAllPointNames");
                __delBase = __getDelegate(false);
                _MoniCAIceDel __del = (_MoniCAIceDel)__delBase;
                return __del.getAllPointNames(__ctx);
            }
            catch(IceInternal.LocalExceptionWrapper __ex)
            {
                __cnt = __handleExceptionWrapperRelaxed(__delBase, __ex, null, __cnt);
            }
            catch(Ice.LocalException __ex)
            {
                __cnt = __handleException(__delBase, __ex, null, __cnt);
            }
        }
    }

    public PointDescriptionIce[]
    getAllPoints()
    {
        return getAllPoints(null, false);
    }

    public PointDescriptionIce[]
    getAllPoints(java.util.Map<String, String> __ctx)
    {
        return getAllPoints(__ctx, true);
    }

    @SuppressWarnings("unchecked")
    private PointDescriptionIce[]
    getAllPoints(java.util.Map<String, String> __ctx, boolean __explicitCtx)
    {
        if(__explicitCtx && __ctx == null)
        {
            __ctx = _emptyContext;
        }
        int __cnt = 0;
        while(true)
        {
            Ice._ObjectDel __delBase = null;
            try
            {
                __checkTwowayOnly("getAllPoints");
                __delBase = __getDelegate(false);
                _MoniCAIceDel __del = (_MoniCAIceDel)__delBase;
                return __del.getAllPoints(__ctx);
            }
            catch(IceInternal.LocalExceptionWrapper __ex)
            {
                __cnt = __handleExceptionWrapperRelaxed(__delBase, __ex, null, __cnt);
            }
            catch(Ice.LocalException __ex)
            {
                __cnt = __handleException(__delBase, __ex, null, __cnt);
            }
        }
    }

    public String[]
    getAllSetups()
    {
        return getAllSetups(null, false);
    }

    public String[]
    getAllSetups(java.util.Map<String, String> __ctx)
    {
        return getAllSetups(__ctx, true);
    }

    @SuppressWarnings("unchecked")
    private String[]
    getAllSetups(java.util.Map<String, String> __ctx, boolean __explicitCtx)
    {
        if(__explicitCtx && __ctx == null)
        {
            __ctx = _emptyContext;
        }
        int __cnt = 0;
        while(true)
        {
            Ice._ObjectDel __delBase = null;
            try
            {
                __checkTwowayOnly("getAllSetups");
                __delBase = __getDelegate(false);
                _MoniCAIceDel __del = (_MoniCAIceDel)__delBase;
                return __del.getAllSetups(__ctx);
            }
            catch(IceInternal.LocalExceptionWrapper __ex)
            {
                __cnt = __handleExceptionWrapperRelaxed(__delBase, __ex, null, __cnt);
            }
            catch(Ice.LocalException __ex)
            {
                __cnt = __handleException(__delBase, __ex, null, __cnt);
            }
        }
    }

    public PointDataIce[][]
    getArchiveData(String[] names, long start, long end, long maxsamples)
    {
        return getArchiveData(names, start, end, maxsamples, null, false);
    }

    public PointDataIce[][]
    getArchiveData(String[] names, long start, long end, long maxsamples, java.util.Map<String, String> __ctx)
    {
        return getArchiveData(names, start, end, maxsamples, __ctx, true);
    }

    @SuppressWarnings("unchecked")
    private PointDataIce[][]
    getArchiveData(String[] names, long start, long end, long maxsamples, java.util.Map<String, String> __ctx, boolean __explicitCtx)
    {
        if(__explicitCtx && __ctx == null)
        {
            __ctx = _emptyContext;
        }
        int __cnt = 0;
        while(true)
        {
            Ice._ObjectDel __delBase = null;
            try
            {
                __checkTwowayOnly("getArchiveData");
                __delBase = __getDelegate(false);
                _MoniCAIceDel __del = (_MoniCAIceDel)__delBase;
                return __del.getArchiveData(names, start, end, maxsamples, __ctx);
            }
            catch(IceInternal.LocalExceptionWrapper __ex)
            {
                __cnt = __handleExceptionWrapperRelaxed(__delBase, __ex, null, __cnt);
            }
            catch(Ice.LocalException __ex)
            {
                __cnt = __handleException(__delBase, __ex, null, __cnt);
            }
        }
    }

    public long
    getCurrentTime()
    {
        return getCurrentTime(null, false);
    }

    public long
    getCurrentTime(java.util.Map<String, String> __ctx)
    {
        return getCurrentTime(__ctx, true);
    }

    @SuppressWarnings("unchecked")
    private long
    getCurrentTime(java.util.Map<String, String> __ctx, boolean __explicitCtx)
    {
        if(__explicitCtx && __ctx == null)
        {
            __ctx = _emptyContext;
        }
        int __cnt = 0;
        while(true)
        {
            Ice._ObjectDel __delBase = null;
            try
            {
                __checkTwowayOnly("getCurrentTime");
                __delBase = __getDelegate(false);
                _MoniCAIceDel __del = (_MoniCAIceDel)__delBase;
                return __del.getCurrentTime(__ctx);
            }
            catch(IceInternal.LocalExceptionWrapper __ex)
            {
                __cnt = __handleExceptionWrapperRelaxed(__delBase, __ex, null, __cnt);
            }
            catch(Ice.LocalException __ex)
            {
                __cnt = __handleException(__delBase, __ex, null, __cnt);
            }
        }
    }

    public PointDataIce[]
    getData(String[] names)
    {
        return getData(names, null, false);
    }

    public PointDataIce[]
    getData(String[] names, java.util.Map<String, String> __ctx)
    {
        return getData(names, __ctx, true);
    }

    @SuppressWarnings("unchecked")
    private PointDataIce[]
    getData(String[] names, java.util.Map<String, String> __ctx, boolean __explicitCtx)
    {
        if(__explicitCtx && __ctx == null)
        {
            __ctx = _emptyContext;
        }
        int __cnt = 0;
        while(true)
        {
            Ice._ObjectDel __delBase = null;
            try
            {
                __checkTwowayOnly("getData");
                __delBase = __getDelegate(false);
                _MoniCAIceDel __del = (_MoniCAIceDel)__delBase;
                return __del.getData(names, __ctx);
            }
            catch(IceInternal.LocalExceptionWrapper __ex)
            {
                __cnt = __handleExceptionWrapperRelaxed(__delBase, __ex, null, __cnt);
            }
            catch(Ice.LocalException __ex)
            {
                __cnt = __handleException(__delBase, __ex, null, __cnt);
            }
        }
    }

    public String[]
    getEncryptionInfo()
    {
        return getEncryptionInfo(null, false);
    }

    public String[]
    getEncryptionInfo(java.util.Map<String, String> __ctx)
    {
        return getEncryptionInfo(__ctx, true);
    }

    @SuppressWarnings("unchecked")
    private String[]
    getEncryptionInfo(java.util.Map<String, String> __ctx, boolean __explicitCtx)
    {
        if(__explicitCtx && __ctx == null)
        {
            __ctx = _emptyContext;
        }
        int __cnt = 0;
        while(true)
        {
            Ice._ObjectDel __delBase = null;
            try
            {
                __checkTwowayOnly("getEncryptionInfo");
                __delBase = __getDelegate(false);
                _MoniCAIceDel __del = (_MoniCAIceDel)__delBase;
                return __del.getEncryptionInfo(__ctx);
            }
            catch(IceInternal.LocalExceptionWrapper __ex)
            {
                __cnt = __handleExceptionWrapperRelaxed(__delBase, __ex, null, __cnt);
            }
            catch(Ice.LocalException __ex)
            {
                __cnt = __handleException(__delBase, __ex, null, __cnt);
            }
        }
    }

    public PointDescriptionIce[]
    getPoints(String[] names)
    {
        return getPoints(names, null, false);
    }

    public PointDescriptionIce[]
    getPoints(String[] names, java.util.Map<String, String> __ctx)
    {
        return getPoints(names, __ctx, true);
    }

    @SuppressWarnings("unchecked")
    private PointDescriptionIce[]
    getPoints(String[] names, java.util.Map<String, String> __ctx, boolean __explicitCtx)
    {
        if(__explicitCtx && __ctx == null)
        {
            __ctx = _emptyContext;
        }
        int __cnt = 0;
        while(true)
        {
            Ice._ObjectDel __delBase = null;
            try
            {
                __checkTwowayOnly("getPoints");
                __delBase = __getDelegate(false);
                _MoniCAIceDel __del = (_MoniCAIceDel)__delBase;
                return __del.getPoints(names, __ctx);
            }
            catch(IceInternal.LocalExceptionWrapper __ex)
            {
                __cnt = __handleExceptionWrapperRelaxed(__delBase, __ex, null, __cnt);
            }
            catch(Ice.LocalException __ex)
            {
                __cnt = __handleException(__delBase, __ex, null, __cnt);
            }
        }
    }

    public boolean
    setData(String[] names, PointDataIce[] rawvalues, String username, String passwd)
    {
        return setData(names, rawvalues, username, passwd, null, false);
    }

    public boolean
    setData(String[] names, PointDataIce[] rawvalues, String username, String passwd, java.util.Map<String, String> __ctx)
    {
        return setData(names, rawvalues, username, passwd, __ctx, true);
    }

    @SuppressWarnings("unchecked")
    private boolean
    setData(String[] names, PointDataIce[] rawvalues, String username, String passwd, java.util.Map<String, String> __ctx, boolean __explicitCtx)
    {
        if(__explicitCtx && __ctx == null)
        {
            __ctx = _emptyContext;
        }
        int __cnt = 0;
        while(true)
        {
            Ice._ObjectDel __delBase = null;
            try
            {
                __checkTwowayOnly("setData");
                __delBase = __getDelegate(false);
                _MoniCAIceDel __del = (_MoniCAIceDel)__delBase;
                return __del.setData(names, rawvalues, username, passwd, __ctx);
            }
            catch(IceInternal.LocalExceptionWrapper __ex)
            {
                __handleExceptionWrapper(__delBase, __ex, null);
            }
            catch(Ice.LocalException __ex)
            {
                __cnt = __handleException(__delBase, __ex, null, __cnt);
            }
        }
    }

    public static MoniCAIcePrx
    checkedCast(Ice.ObjectPrx __obj)
    {
        MoniCAIcePrx __d = null;
        if(__obj != null)
        {
            try
            {
                __d = (MoniCAIcePrx)__obj;
            }
            catch(ClassCastException ex)
            {
                if(__obj.ice_isA("::atnf::atoms::mon::comms::MoniCAIce"))
                {
                    MoniCAIcePrxHelper __h = new MoniCAIcePrxHelper();
                    __h.__copyFrom(__obj);
                    __d = __h;
                }
            }
        }
        return __d;
    }

    public static MoniCAIcePrx
    checkedCast(Ice.ObjectPrx __obj, java.util.Map<String, String> __ctx)
    {
        MoniCAIcePrx __d = null;
        if(__obj != null)
        {
            try
            {
                __d = (MoniCAIcePrx)__obj;
            }
            catch(ClassCastException ex)
            {
                if(__obj.ice_isA("::atnf::atoms::mon::comms::MoniCAIce", __ctx))
                {
                    MoniCAIcePrxHelper __h = new MoniCAIcePrxHelper();
                    __h.__copyFrom(__obj);
                    __d = __h;
                }
            }
        }
        return __d;
    }

    public static MoniCAIcePrx
    checkedCast(Ice.ObjectPrx __obj, String __facet)
    {
        MoniCAIcePrx __d = null;
        if(__obj != null)
        {
            Ice.ObjectPrx __bb = __obj.ice_facet(__facet);
            try
            {
                if(__bb.ice_isA("::atnf::atoms::mon::comms::MoniCAIce"))
                {
                    MoniCAIcePrxHelper __h = new MoniCAIcePrxHelper();
                    __h.__copyFrom(__bb);
                    __d = __h;
                }
            }
            catch(Ice.FacetNotExistException ex)
            {
            }
        }
        return __d;
    }

    public static MoniCAIcePrx
    checkedCast(Ice.ObjectPrx __obj, String __facet, java.util.Map<String, String> __ctx)
    {
        MoniCAIcePrx __d = null;
        if(__obj != null)
        {
            Ice.ObjectPrx __bb = __obj.ice_facet(__facet);
            try
            {
                if(__bb.ice_isA("::atnf::atoms::mon::comms::MoniCAIce", __ctx))
                {
                    MoniCAIcePrxHelper __h = new MoniCAIcePrxHelper();
                    __h.__copyFrom(__bb);
                    __d = __h;
                }
            }
            catch(Ice.FacetNotExistException ex)
            {
            }
        }
        return __d;
    }

    public static MoniCAIcePrx
    uncheckedCast(Ice.ObjectPrx __obj)
    {
        MoniCAIcePrx __d = null;
        if(__obj != null)
        {
            try
            {
                __d = (MoniCAIcePrx)__obj;
            }
            catch(ClassCastException ex)
            {
                MoniCAIcePrxHelper __h = new MoniCAIcePrxHelper();
                __h.__copyFrom(__obj);
                __d = __h;
            }
        }
        return __d;
    }

    public static MoniCAIcePrx
    uncheckedCast(Ice.ObjectPrx __obj, String __facet)
    {
        MoniCAIcePrx __d = null;
        if(__obj != null)
        {
            Ice.ObjectPrx __bb = __obj.ice_facet(__facet);
            MoniCAIcePrxHelper __h = new MoniCAIcePrxHelper();
            __h.__copyFrom(__bb);
            __d = __h;
        }
        return __d;
    }

    protected Ice._ObjectDelM
    __createDelegateM()
    {
        return new _MoniCAIceDelM();
    }

    protected Ice._ObjectDelD
    __createDelegateD()
    {
        return new _MoniCAIceDelD();
    }

    public static void
    __write(IceInternal.BasicStream __os, MoniCAIcePrx v)
    {
        __os.writeProxy(v);
    }

    public static MoniCAIcePrx
    __read(IceInternal.BasicStream __is)
    {
        Ice.ObjectPrx proxy = __is.readProxy();
        if(proxy != null)
        {
            MoniCAIcePrxHelper result = new MoniCAIcePrxHelper();
            result.__copyFrom(proxy);
            return result;
        }
        return null;
    }
}
