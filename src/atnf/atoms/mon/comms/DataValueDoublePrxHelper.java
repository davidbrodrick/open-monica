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

public final class DataValueDoublePrxHelper extends Ice.ObjectPrxHelperBase implements DataValueDoublePrx
{
    public static DataValueDoublePrx
    checkedCast(Ice.ObjectPrx __obj)
    {
        DataValueDoublePrx __d = null;
        if(__obj != null)
        {
            try
            {
                __d = (DataValueDoublePrx)__obj;
            }
            catch(ClassCastException ex)
            {
                if(__obj.ice_isA("::atnf::atoms::mon::comms::DataValueDouble"))
                {
                    DataValueDoublePrxHelper __h = new DataValueDoublePrxHelper();
                    __h.__copyFrom(__obj);
                    __d = __h;
                }
            }
        }
        return __d;
    }

    public static DataValueDoublePrx
    checkedCast(Ice.ObjectPrx __obj, java.util.Map<String, String> __ctx)
    {
        DataValueDoublePrx __d = null;
        if(__obj != null)
        {
            try
            {
                __d = (DataValueDoublePrx)__obj;
            }
            catch(ClassCastException ex)
            {
                if(__obj.ice_isA("::atnf::atoms::mon::comms::DataValueDouble", __ctx))
                {
                    DataValueDoublePrxHelper __h = new DataValueDoublePrxHelper();
                    __h.__copyFrom(__obj);
                    __d = __h;
                }
            }
        }
        return __d;
    }

    public static DataValueDoublePrx
    checkedCast(Ice.ObjectPrx __obj, String __facet)
    {
        DataValueDoublePrx __d = null;
        if(__obj != null)
        {
            Ice.ObjectPrx __bb = __obj.ice_facet(__facet);
            try
            {
                if(__bb.ice_isA("::atnf::atoms::mon::comms::DataValueDouble"))
                {
                    DataValueDoublePrxHelper __h = new DataValueDoublePrxHelper();
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

    public static DataValueDoublePrx
    checkedCast(Ice.ObjectPrx __obj, String __facet, java.util.Map<String, String> __ctx)
    {
        DataValueDoublePrx __d = null;
        if(__obj != null)
        {
            Ice.ObjectPrx __bb = __obj.ice_facet(__facet);
            try
            {
                if(__bb.ice_isA("::atnf::atoms::mon::comms::DataValueDouble", __ctx))
                {
                    DataValueDoublePrxHelper __h = new DataValueDoublePrxHelper();
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

    public static DataValueDoublePrx
    uncheckedCast(Ice.ObjectPrx __obj)
    {
        DataValueDoublePrx __d = null;
        if(__obj != null)
        {
            try
            {
                __d = (DataValueDoublePrx)__obj;
            }
            catch(ClassCastException ex)
            {
                DataValueDoublePrxHelper __h = new DataValueDoublePrxHelper();
                __h.__copyFrom(__obj);
                __d = __h;
            }
        }
        return __d;
    }

    public static DataValueDoublePrx
    uncheckedCast(Ice.ObjectPrx __obj, String __facet)
    {
        DataValueDoublePrx __d = null;
        if(__obj != null)
        {
            Ice.ObjectPrx __bb = __obj.ice_facet(__facet);
            DataValueDoublePrxHelper __h = new DataValueDoublePrxHelper();
            __h.__copyFrom(__bb);
            __d = __h;
        }
        return __d;
    }

    protected Ice._ObjectDelM
    __createDelegateM()
    {
        return new _DataValueDoubleDelM();
    }

    protected Ice._ObjectDelD
    __createDelegateD()
    {
        return new _DataValueDoubleDelD();
    }

    public static void
    __write(IceInternal.BasicStream __os, DataValueDoublePrx v)
    {
        __os.writeProxy(v);
    }

    public static DataValueDoublePrx
    __read(IceInternal.BasicStream __is)
    {
        Ice.ObjectPrx proxy = __is.readProxy();
        if(proxy != null)
        {
            DataValueDoublePrxHelper result = new DataValueDoublePrxHelper();
            result.__copyFrom(proxy);
            return result;
        }
        return null;
    }
}