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

public final class DataValueAnglePrxHelper extends Ice.ObjectPrxHelperBase implements DataValueAnglePrx
{
    public static DataValueAnglePrx checkedCast(Ice.ObjectPrx __obj)
    {
        DataValueAnglePrx __d = null;
        if(__obj != null)
        {
            if(__obj instanceof DataValueAnglePrx)
            {
                __d = (DataValueAnglePrx)__obj;
            }
            else
            {
                if(__obj.ice_isA(ice_staticId()))
                {
                    DataValueAnglePrxHelper __h = new DataValueAnglePrxHelper();
                    __h.__copyFrom(__obj);
                    __d = __h;
                }
            }
        }
        return __d;
    }

    public static DataValueAnglePrx checkedCast(Ice.ObjectPrx __obj, java.util.Map<String, String> __ctx)
    {
        DataValueAnglePrx __d = null;
        if(__obj != null)
        {
            if(__obj instanceof DataValueAnglePrx)
            {
                __d = (DataValueAnglePrx)__obj;
            }
            else
            {
                if(__obj.ice_isA(ice_staticId(), __ctx))
                {
                    DataValueAnglePrxHelper __h = new DataValueAnglePrxHelper();
                    __h.__copyFrom(__obj);
                    __d = __h;
                }
            }
        }
        return __d;
    }

    public static DataValueAnglePrx checkedCast(Ice.ObjectPrx __obj, String __facet)
    {
        DataValueAnglePrx __d = null;
        if(__obj != null)
        {
            Ice.ObjectPrx __bb = __obj.ice_facet(__facet);
            try
            {
                if(__bb.ice_isA(ice_staticId()))
                {
                    DataValueAnglePrxHelper __h = new DataValueAnglePrxHelper();
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

    public static DataValueAnglePrx checkedCast(Ice.ObjectPrx __obj, String __facet, java.util.Map<String, String> __ctx)
    {
        DataValueAnglePrx __d = null;
        if(__obj != null)
        {
            Ice.ObjectPrx __bb = __obj.ice_facet(__facet);
            try
            {
                if(__bb.ice_isA(ice_staticId(), __ctx))
                {
                    DataValueAnglePrxHelper __h = new DataValueAnglePrxHelper();
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

    public static DataValueAnglePrx uncheckedCast(Ice.ObjectPrx __obj)
    {
        DataValueAnglePrx __d = null;
        if(__obj != null)
        {
            if(__obj instanceof DataValueAnglePrx)
            {
                __d = (DataValueAnglePrx)__obj;
            }
            else
            {
                DataValueAnglePrxHelper __h = new DataValueAnglePrxHelper();
                __h.__copyFrom(__obj);
                __d = __h;
            }
        }
        return __d;
    }

    public static DataValueAnglePrx uncheckedCast(Ice.ObjectPrx __obj, String __facet)
    {
        DataValueAnglePrx __d = null;
        if(__obj != null)
        {
            Ice.ObjectPrx __bb = __obj.ice_facet(__facet);
            DataValueAnglePrxHelper __h = new DataValueAnglePrxHelper();
            __h.__copyFrom(__bb);
            __d = __h;
        }
        return __d;
    }

    public static final String[] __ids =
    {
        "::Ice::Object",
        "::atnf::atoms::mon::comms::DataValue",
        "::atnf::atoms::mon::comms::DataValueAngle"
    };

    public static String ice_staticId()
    {
        return __ids[2];
    }

    protected Ice._ObjectDelM __createDelegateM()
    {
        return new _DataValueAngleDelM();
    }

    protected Ice._ObjectDelD __createDelegateD()
    {
        return new _DataValueAngleDelD();
    }

    public static void __write(IceInternal.BasicStream __os, DataValueAnglePrx v)
    {
        __os.writeProxy(v);
    }

    public static DataValueAnglePrx __read(IceInternal.BasicStream __is)
    {
        Ice.ObjectPrx proxy = __is.readProxy();
        if(proxy != null)
        {
            DataValueAnglePrxHelper result = new DataValueAnglePrxHelper();
            result.__copyFrom(proxy);
            return result;
        }
        return null;
    }

    public static final long serialVersionUID = 0L;
}
