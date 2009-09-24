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

public final class DataValueStringPrxHelper extends Ice.ObjectPrxHelperBase implements DataValueStringPrx
{
    public static DataValueStringPrx
    checkedCast(Ice.ObjectPrx __obj)
    {
        DataValueStringPrx __d = null;
        if(__obj != null)
        {
            try
            {
                __d = (DataValueStringPrx)__obj;
            }
            catch(ClassCastException ex)
            {
                if(__obj.ice_isA("::atnf::atoms::mon::comms::DataValueString"))
                {
                    DataValueStringPrxHelper __h = new DataValueStringPrxHelper();
                    __h.__copyFrom(__obj);
                    __d = __h;
                }
            }
        }
        return __d;
    }

    public static DataValueStringPrx
    checkedCast(Ice.ObjectPrx __obj, java.util.Map<String, String> __ctx)
    {
        DataValueStringPrx __d = null;
        if(__obj != null)
        {
            try
            {
                __d = (DataValueStringPrx)__obj;
            }
            catch(ClassCastException ex)
            {
                if(__obj.ice_isA("::atnf::atoms::mon::comms::DataValueString", __ctx))
                {
                    DataValueStringPrxHelper __h = new DataValueStringPrxHelper();
                    __h.__copyFrom(__obj);
                    __d = __h;
                }
            }
        }
        return __d;
    }

    public static DataValueStringPrx
    checkedCast(Ice.ObjectPrx __obj, String __facet)
    {
        DataValueStringPrx __d = null;
        if(__obj != null)
        {
            Ice.ObjectPrx __bb = __obj.ice_facet(__facet);
            try
            {
                if(__bb.ice_isA("::atnf::atoms::mon::comms::DataValueString"))
                {
                    DataValueStringPrxHelper __h = new DataValueStringPrxHelper();
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

    public static DataValueStringPrx
    checkedCast(Ice.ObjectPrx __obj, String __facet, java.util.Map<String, String> __ctx)
    {
        DataValueStringPrx __d = null;
        if(__obj != null)
        {
            Ice.ObjectPrx __bb = __obj.ice_facet(__facet);
            try
            {
                if(__bb.ice_isA("::atnf::atoms::mon::comms::DataValueString", __ctx))
                {
                    DataValueStringPrxHelper __h = new DataValueStringPrxHelper();
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

    public static DataValueStringPrx
    uncheckedCast(Ice.ObjectPrx __obj)
    {
        DataValueStringPrx __d = null;
        if(__obj != null)
        {
            try
            {
                __d = (DataValueStringPrx)__obj;
            }
            catch(ClassCastException ex)
            {
                DataValueStringPrxHelper __h = new DataValueStringPrxHelper();
                __h.__copyFrom(__obj);
                __d = __h;
            }
        }
        return __d;
    }

    public static DataValueStringPrx
    uncheckedCast(Ice.ObjectPrx __obj, String __facet)
    {
        DataValueStringPrx __d = null;
        if(__obj != null)
        {
            Ice.ObjectPrx __bb = __obj.ice_facet(__facet);
            DataValueStringPrxHelper __h = new DataValueStringPrxHelper();
            __h.__copyFrom(__bb);
            __d = __h;
        }
        return __d;
    }

    protected Ice._ObjectDelM
    __createDelegateM()
    {
        return new _DataValueStringDelM();
    }

    protected Ice._ObjectDelD
    __createDelegateD()
    {
        return new _DataValueStringDelD();
    }

    public static void
    __write(IceInternal.BasicStream __os, DataValueStringPrx v)
    {
        __os.writeProxy(v);
    }

    public static DataValueStringPrx
    __read(IceInternal.BasicStream __is)
    {
        Ice.ObjectPrx proxy = __is.readProxy();
        if(proxy != null)
        {
            DataValueStringPrxHelper result = new DataValueStringPrxHelper();
            result.__copyFrom(proxy);
            return result;
        }
        return null;
    }
}