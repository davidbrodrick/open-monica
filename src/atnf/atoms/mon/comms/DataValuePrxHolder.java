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

public final class DataValuePrxHolder
{
    public
    DataValuePrxHolder()
    {
    }

    public
    DataValuePrxHolder(DataValuePrx value)
    {
        this.value = value;
    }

    public DataValuePrx value;
}