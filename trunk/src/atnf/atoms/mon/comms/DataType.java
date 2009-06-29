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

public enum DataType
{
    DTNull,
    DTFloat,
    DTDouble,
    DTInt,
    DTLong,
    DTString,
    DTBoolean,
    DTAbsTime,
    DTRelTime,
    DTAngle;

    public static final int _DTNull = 0;
    public static final int _DTFloat = 1;
    public static final int _DTDouble = 2;
    public static final int _DTInt = 3;
    public static final int _DTLong = 4;
    public static final int _DTString = 5;
    public static final int _DTBoolean = 6;
    public static final int _DTAbsTime = 7;
    public static final int _DTRelTime = 8;
    public static final int _DTAngle = 9;

    public static DataType
    convert(int val)
    {
        assert val >= 0 && val < 10;
        return values()[val];
    }

    public static DataType
    convert(String val)
    {
        try
        {
            return valueOf(val);
        }
        catch(java.lang.IllegalArgumentException ex)
        {
            return null;
        }
    }

    public int
    value()
    {
        return ordinal();
    }

    public void
    __write(IceInternal.BasicStream __os)
    {
        __os.writeByte((byte)value());
    }

    public static DataType
    __read(IceInternal.BasicStream __is)
    {
        int __v = __is.readByte(10);
        return DataType.convert(__v);
    }
}
