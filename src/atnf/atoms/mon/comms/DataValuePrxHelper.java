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

public final class DataValuePrxHelper extends Ice.ObjectPrxHelperBase implements DataValuePrx
{
    public static DataValuePrx
    checkedCast(Ice.ObjectPrx __obj)
    {
        DataValuePrx __d = null;
        if(__obj != null)
        {
            try
            {
                __d = (DataValuePrx)__obj;
            }
            catch(ClassCastException ex)
            {
                if(__obj.ice_isA("::atnf::atoms::mon::comms::DataValue"))
                {
                    DataValuePrxHelper __h = new DataValuePrxHelper();
                    __h.__copyFrom(__obj);
                    __d = __h;
                }
            }
        }
        return __d;
    }

    public static DataValuePrx
    checkedCast(Ice.ObjectPrx __obj, java.util.Map<String, String> __ctx)
    {
        DataValuePrx __d = null;
        if(__obj != null)
        {
            try
            {
                __d = (DataValuePrx)__obj;
            }
            catch(ClassCastException ex)
            {
                if(__obj.ice_isA("::atnf::atoms::mon::comms::DataValue", __ctx))
                {
                    DataValuePrxHelper __h = new DataValuePrxHelper();
                    __h.__copyFrom(__obj);
                    __d = __h;
                }
            }
        }
        return __d;
    }

    public static DataValuePrx
    checkedCast(Ice.ObjectPrx __obj, String __facet)
    {
        DataValuePrx __d = null;
        if(__obj != null)
        {
            Ice.ObjectPrx __bb = __obj.ice_facet(__facet);
            try
            {
                if(__bb.ice_isA("::atnf::atoms::mon::comms::DataValue"))
                {
                    DataValuePrxHelper __h = new DataValuePrxHelper();
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

    public static DataValuePrx
    checkedCast(Ice.ObjectPrx __obj, String __facet, java.util.Map<String, String> __ctx)
    {
        DataValuePrx __d = null;
        if(__obj != null)
        {
            Ice.ObjectPrx __bb = __obj.ice_facet(__facet);
            try
            {
                if(__bb.ice_isA("::atnf::atoms::mon::comms::DataValue", __ctx))
                {
                    DataValuePrxHelper __h = new DataValuePrxHelper();
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

    public static DataValuePrx
    uncheckedCast(Ice.ObjectPrx __obj)
    {
        DataValuePrx __d = null;
        if(__obj != null)
        {
            try
            {
                __d = (DataValuePrx)__obj;
            }
            catch(ClassCastException ex)
            {
                DataValuePrxHelper __h = new DataValuePrxHelper();
                __h.__copyFrom(__obj);
                __d = __h;
            }
        }
        return __d;
    }

    public static DataValuePrx
    uncheckedCast(Ice.ObjectPrx __obj, String __facet)
    {
        DataValuePrx __d = null;
        if(__obj != null)
        {
            Ice.ObjectPrx __bb = __obj.ice_facet(__facet);
            DataValuePrxHelper __h = new DataValuePrxHelper();
            __h.__copyFrom(__bb);
            __d = __h;
        }
        return __d;
    }

    protected Ice._ObjectDelM
    __createDelegateM()
    {
        return new _DataValueDelM();
    }

    protected Ice._ObjectDelD
    __createDelegateD()
    {
        return new _DataValueDelD();
    }

    public static void
    __write(IceInternal.BasicStream __os, DataValuePrx v)
    {
        __os.writeProxy(v);
    }

    public static DataValuePrx
    __read(IceInternal.BasicStream __is)
    {
        Ice.ObjectPrx proxy = __is.readProxy();
        if(proxy != null)
        {
            DataValuePrxHelper result = new DataValuePrxHelper();
            result.__copyFrom(proxy);
            return result;
        }
        return null;
    }
}
