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


public final class pointdatasetHelper
{
    public static void
    write(IceInternal.BasicStream __os, PointDataIce[] __v)
    {
        if(__v == null)
        {
            __os.writeSize(0);
        }
        else
        {
            __os.writeSize(__v.length);
            for(int __i0 = 0; __i0 < __v.length; __i0++)
            {
                __v[__i0].__write(__os);
            }
        }
    }

    public static PointDataIce[]
    read(IceInternal.BasicStream __is)
    {
        PointDataIce[] __v;
        final int __len0 = __is.readAndCheckSeqSize(14);
        __v = new PointDataIce[__len0];
        for(int __i0 = 0; __i0 < __len0; __i0++)
        {
            __v[__i0] = new PointDataIce();
            __v[__i0].__read(__is);
        }
        return __v;
    }
}
