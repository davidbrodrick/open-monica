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

public final class DataValueRelTimePrxHelper extends Ice.ObjectPrxHelperBase implements DataValueRelTimePrx
{
    public static DataValueRelTimePrx
    checkedCast(Ice.ObjectPrx __obj)
    {
        DataValueRelTimePrx __d = null;
        if(__obj != null)
        {
            try
            {
                __d = (DataValueRelTimePrx)__obj;
            }
            catch(ClassCastException ex)
            {
                if(__obj.ice_isA("::atnf::atoms::mon::comms::DataValueRelTime"))
                {
                    DataValueRelTimePrxHelper __h = new DataValueRelTimePrxHelper();
                    __h.__copyFrom(__obj);
                    __d = __h;
                }
            }
        }
        return __d;
    }

    public static DataValueRelTimePrx
    checkedCast(Ice.ObjectPrx __obj, java.util.Map<String, String> __ctx)
    {
        DataValueRelTimePrx __d = null;
        if(__obj != null)
        {
            try
            {
                __d = (DataValueRelTimePrx)__obj;
            }
            catch(ClassCastException ex)
            {
                if(__obj.ice_isA("::atnf::atoms::mon::comms::DataValueRelTime", __ctx))
                {
                    DataValueRelTimePrxHelper __h = new DataValueRelTimePrxHelper();
                    __h.__copyFrom(__obj);
                    __d = __h;
                }
            }
        }
        return __d;
    }

    public static DataValueRelTimePrx
    checkedCast(Ice.ObjectPrx __obj, String __facet)
    {
        DataValueRelTimePrx __d = null;
        if(__obj != null)
        {
            Ice.ObjectPrx __bb = __obj.ice_facet(__facet);
            try
            {
                if(__bb.ice_isA("::atnf::atoms::mon::comms::DataValueRelTime"))
                {
                    DataValueRelTimePrxHelper __h = new DataValueRelTimePrxHelper();
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

    public static DataValueRelTimePrx
    checkedCast(Ice.ObjectPrx __obj, String __facet, java.util.Map<String, String> __ctx)
    {
        DataValueRelTimePrx __d = null;
        if(__obj != null)
        {
            Ice.ObjectPrx __bb = __obj.ice_facet(__facet);
            try
            {
                if(__bb.ice_isA("::atnf::atoms::mon::comms::DataValueRelTime", __ctx))
                {
                    DataValueRelTimePrxHelper __h = new DataValueRelTimePrxHelper();
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

    public static DataValueRelTimePrx
    uncheckedCast(Ice.ObjectPrx __obj)
    {
        DataValueRelTimePrx __d = null;
        if(__obj != null)
        {
            try
            {
                __d = (DataValueRelTimePrx)__obj;
            }
            catch(ClassCastException ex)
            {
                DataValueRelTimePrxHelper __h = new DataValueRelTimePrxHelper();
                __h.__copyFrom(__obj);
                __d = __h;
            }
        }
        return __d;
    }

    public static DataValueRelTimePrx
    uncheckedCast(Ice.ObjectPrx __obj, String __facet)
    {
        DataValueRelTimePrx __d = null;
        if(__obj != null)
        {
            Ice.ObjectPrx __bb = __obj.ice_facet(__facet);
            DataValueRelTimePrxHelper __h = new DataValueRelTimePrxHelper();
            __h.__copyFrom(__bb);
            __d = __h;
        }
        return __d;
    }

    protected Ice._ObjectDelM
    __createDelegateM()
    {
        return new _DataValueRelTimeDelM();
    }

    protected Ice._ObjectDelD
    __createDelegateD()
    {
        return new _DataValueRelTimeDelD();
    }

    public static void
    __write(IceInternal.BasicStream __os, DataValueRelTimePrx v)
    {
        __os.writeProxy(v);
    }

    public static DataValueRelTimePrx
    __read(IceInternal.BasicStream __is)
    {
        Ice.ObjectPrx proxy = __is.readProxy();
        if(proxy != null)
        {
            DataValueRelTimePrxHelper result = new DataValueRelTimePrxHelper();
            result.__copyFrom(proxy);
            return result;
        }
        return null;
    }
}
