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

public final class DataValueIntPrxHelper extends Ice.ObjectPrxHelperBase implements DataValueIntPrx
{
    public static DataValueIntPrx
    checkedCast(Ice.ObjectPrx __obj)
    {
        DataValueIntPrx __d = null;
        if(__obj != null)
        {
            try
            {
                __d = (DataValueIntPrx)__obj;
            }
            catch(ClassCastException ex)
            {
                if(__obj.ice_isA("::atnf::atoms::mon::comms::DataValueInt"))
                {
                    DataValueIntPrxHelper __h = new DataValueIntPrxHelper();
                    __h.__copyFrom(__obj);
                    __d = __h;
                }
            }
        }
        return __d;
    }

    public static DataValueIntPrx
    checkedCast(Ice.ObjectPrx __obj, java.util.Map<String, String> __ctx)
    {
        DataValueIntPrx __d = null;
        if(__obj != null)
        {
            try
            {
                __d = (DataValueIntPrx)__obj;
            }
            catch(ClassCastException ex)
            {
                if(__obj.ice_isA("::atnf::atoms::mon::comms::DataValueInt", __ctx))
                {
                    DataValueIntPrxHelper __h = new DataValueIntPrxHelper();
                    __h.__copyFrom(__obj);
                    __d = __h;
                }
            }
        }
        return __d;
    }

    public static DataValueIntPrx
    checkedCast(Ice.ObjectPrx __obj, String __facet)
    {
        DataValueIntPrx __d = null;
        if(__obj != null)
        {
            Ice.ObjectPrx __bb = __obj.ice_facet(__facet);
            try
            {
                if(__bb.ice_isA("::atnf::atoms::mon::comms::DataValueInt"))
                {
                    DataValueIntPrxHelper __h = new DataValueIntPrxHelper();
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

    public static DataValueIntPrx
    checkedCast(Ice.ObjectPrx __obj, String __facet, java.util.Map<String, String> __ctx)
    {
        DataValueIntPrx __d = null;
        if(__obj != null)
        {
            Ice.ObjectPrx __bb = __obj.ice_facet(__facet);
            try
            {
                if(__bb.ice_isA("::atnf::atoms::mon::comms::DataValueInt", __ctx))
                {
                    DataValueIntPrxHelper __h = new DataValueIntPrxHelper();
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

    public static DataValueIntPrx
    uncheckedCast(Ice.ObjectPrx __obj)
    {
        DataValueIntPrx __d = null;
        if(__obj != null)
        {
            try
            {
                __d = (DataValueIntPrx)__obj;
            }
            catch(ClassCastException ex)
            {
                DataValueIntPrxHelper __h = new DataValueIntPrxHelper();
                __h.__copyFrom(__obj);
                __d = __h;
            }
        }
        return __d;
    }

    public static DataValueIntPrx
    uncheckedCast(Ice.ObjectPrx __obj, String __facet)
    {
        DataValueIntPrx __d = null;
        if(__obj != null)
        {
            Ice.ObjectPrx __bb = __obj.ice_facet(__facet);
            DataValueIntPrxHelper __h = new DataValueIntPrxHelper();
            __h.__copyFrom(__bb);
            __d = __h;
        }
        return __d;
    }

    protected Ice._ObjectDelM
    __createDelegateM()
    {
        return new _DataValueIntDelM();
    }

    protected Ice._ObjectDelD
    __createDelegateD()
    {
        return new _DataValueIntDelD();
    }

    public static void
    __write(IceInternal.BasicStream __os, DataValueIntPrx v)
    {
        __os.writeProxy(v);
    }

    public static DataValueIntPrx
    __read(IceInternal.BasicStream __is)
    {
        Ice.ObjectPrx proxy = __is.readProxy();
        if(proxy != null)
        {
            DataValueIntPrxHelper result = new DataValueIntPrxHelper();
            result.__copyFrom(proxy);
            return result;
        }
        return null;
    }
}
