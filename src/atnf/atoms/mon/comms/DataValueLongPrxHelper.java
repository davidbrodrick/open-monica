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

public final class DataValueLongPrxHelper extends Ice.ObjectPrxHelperBase implements DataValueLongPrx
{
    public static DataValueLongPrx
    checkedCast(Ice.ObjectPrx __obj)
    {
        DataValueLongPrx __d = null;
        if(__obj != null)
        {
            try
            {
                __d = (DataValueLongPrx)__obj;
            }
            catch(ClassCastException ex)
            {
                if(__obj.ice_isA("::atnf::atoms::mon::comms::DataValueLong"))
                {
                    DataValueLongPrxHelper __h = new DataValueLongPrxHelper();
                    __h.__copyFrom(__obj);
                    __d = __h;
                }
            }
        }
        return __d;
    }

    public static DataValueLongPrx
    checkedCast(Ice.ObjectPrx __obj, java.util.Map<String, String> __ctx)
    {
        DataValueLongPrx __d = null;
        if(__obj != null)
        {
            try
            {
                __d = (DataValueLongPrx)__obj;
            }
            catch(ClassCastException ex)
            {
                if(__obj.ice_isA("::atnf::atoms::mon::comms::DataValueLong", __ctx))
                {
                    DataValueLongPrxHelper __h = new DataValueLongPrxHelper();
                    __h.__copyFrom(__obj);
                    __d = __h;
                }
            }
        }
        return __d;
    }

    public static DataValueLongPrx
    checkedCast(Ice.ObjectPrx __obj, String __facet)
    {
        DataValueLongPrx __d = null;
        if(__obj != null)
        {
            Ice.ObjectPrx __bb = __obj.ice_facet(__facet);
            try
            {
                if(__bb.ice_isA("::atnf::atoms::mon::comms::DataValueLong"))
                {
                    DataValueLongPrxHelper __h = new DataValueLongPrxHelper();
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

    public static DataValueLongPrx
    checkedCast(Ice.ObjectPrx __obj, String __facet, java.util.Map<String, String> __ctx)
    {
        DataValueLongPrx __d = null;
        if(__obj != null)
        {
            Ice.ObjectPrx __bb = __obj.ice_facet(__facet);
            try
            {
                if(__bb.ice_isA("::atnf::atoms::mon::comms::DataValueLong", __ctx))
                {
                    DataValueLongPrxHelper __h = new DataValueLongPrxHelper();
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

    public static DataValueLongPrx
    uncheckedCast(Ice.ObjectPrx __obj)
    {
        DataValueLongPrx __d = null;
        if(__obj != null)
        {
            try
            {
                __d = (DataValueLongPrx)__obj;
            }
            catch(ClassCastException ex)
            {
                DataValueLongPrxHelper __h = new DataValueLongPrxHelper();
                __h.__copyFrom(__obj);
                __d = __h;
            }
        }
        return __d;
    }

    public static DataValueLongPrx
    uncheckedCast(Ice.ObjectPrx __obj, String __facet)
    {
        DataValueLongPrx __d = null;
        if(__obj != null)
        {
            Ice.ObjectPrx __bb = __obj.ice_facet(__facet);
            DataValueLongPrxHelper __h = new DataValueLongPrxHelper();
            __h.__copyFrom(__bb);
            __d = __h;
        }
        return __d;
    }

    protected Ice._ObjectDelM
    __createDelegateM()
    {
        return new _DataValueLongDelM();
    }

    protected Ice._ObjectDelD
    __createDelegateD()
    {
        return new _DataValueLongDelD();
    }

    public static void
    __write(IceInternal.BasicStream __os, DataValueLongPrx v)
    {
        __os.writeProxy(v);
    }

    public static DataValueLongPrx
    __read(IceInternal.BasicStream __is)
    {
        Ice.ObjectPrx proxy = __is.readProxy();
        if(proxy != null)
        {
            DataValueLongPrxHelper result = new DataValueLongPrxHelper();
            result.__copyFrom(proxy);
            return result;
        }
        return null;
    }
}
