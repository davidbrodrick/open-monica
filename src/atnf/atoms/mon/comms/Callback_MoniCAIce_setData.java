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


public abstract class Callback_MoniCAIce_setData extends Ice.TwowayCallback
{
    public abstract void response(boolean __ret);

    public final void __completed(Ice.AsyncResult __result)
    {
        MoniCAIcePrx __proxy = (MoniCAIcePrx)__result.getProxy();
        boolean __ret = false;
        try
        {
            __ret = __proxy.end_setData(__result);
        }
        catch(Ice.LocalException __ex)
        {
            exception(__ex);
            return;
        }
        response(__ret);
    }
}
