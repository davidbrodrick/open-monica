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

public final class PointDataIce implements java.lang.Cloneable
{
    public String name;

    public long timestamp;

    public DataValue value;

    public PointDataIce()
    {
    }

    public PointDataIce(String name, long timestamp, DataValue value)
    {
        this.name = name;
        this.timestamp = timestamp;
        this.value = value;
    }

    public boolean
    equals(java.lang.Object rhs)
    {
        if(this == rhs)
        {
            return true;
        }
        PointDataIce _r = null;
        try
        {
            _r = (PointDataIce)rhs;
        }
        catch(ClassCastException ex)
        {
        }

        if(_r != null)
        {
            if(name != _r.name && name != null && !name.equals(_r.name))
            {
                return false;
            }
            if(timestamp != _r.timestamp)
            {
                return false;
            }
            if(value != _r.value && value != null && !value.equals(_r.value))
            {
                return false;
            }

            return true;
        }

        return false;
    }

    public int
    hashCode()
    {
        int __h = 0;
        if(name != null)
        {
            __h = 5 * __h + name.hashCode();
        }
        __h = 5 * __h + (int)timestamp;
        if(value != null)
        {
            __h = 5 * __h + value.hashCode();
        }
        return __h;
    }

    public java.lang.Object
    clone()
    {
        java.lang.Object o = null;
        try
        {
            o = super.clone();
        }
        catch(CloneNotSupportedException ex)
        {
            assert false; // impossible
        }
        return o;
    }

    public void
    __write(IceInternal.BasicStream __os)
    {
        __os.writeString(name);
        __os.writeLong(timestamp);
        __os.writeObject(value);
    }

    private class Patcher implements IceInternal.Patcher
    {
        public void
        patch(Ice.Object v)
        {
            try
            {
                value = (DataValue)v;
            }
            catch(ClassCastException ex)
            {
                IceInternal.Ex.throwUOE(type(), v.ice_id());
            }
        }

        public String
        type()
        {
            return "::atnf::atoms::mon::comms::DataValue";
        }
    }

    public void
    __read(IceInternal.BasicStream __is)
    {
        name = __is.readString();
        timestamp = __is.readLong();
        __is.readObject(new Patcher());
    }
}
