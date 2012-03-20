// **********************************************************************
//
// Copyright (c) 2003-2010 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

// Ice version 3.4.1

package atnf.atoms.mon.comms;

// <auto-generated>
//
// Generated from file `MoniCA.ice'
//
// Warning: do not edit this file.
//
// </auto-generated>


public final class DataValueIntHolder extends Ice.ObjectHolderBase<DataValueInt>
{
    public
    DataValueIntHolder()
    {
    }

    public
    DataValueIntHolder(DataValueInt value)
    {
        this.value = value;
    }

    public void
    patch(Ice.Object v)
    {
        try
        {
            value = (DataValueInt)v;
        }
        catch(ClassCastException ex)
        {
            IceInternal.Ex.throwUOE(type(), v.ice_id());
        }
    }

    public String
    type()
    {
        return DataValueInt.ice_staticId();
    }
}
