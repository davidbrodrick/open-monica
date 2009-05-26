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

public final class PointDescriptionIce implements java.lang.Cloneable
{
    public String[] names;

    public String source;

    public String description;

    public String shortdescription;

    public String units;

    public boolean enabled;

    public String[] inputtransactions;

    public String[] outputtransactions;

    public String[] translations;

    public String[] limits;

    public String[] archivepolicies;

    public long period;

    public int archivelongevity;

    public PointDescriptionIce()
    {
    }

    public PointDescriptionIce(String[] names, String source, String description, String shortdescription, String units, boolean enabled, String[] inputtransactions, String[] outputtransactions, String[] translations, String[] limits, String[] archivepolicies, long period, int archivelongevity)
    {
        this.names = names;
        this.source = source;
        this.description = description;
        this.shortdescription = shortdescription;
        this.units = units;
        this.enabled = enabled;
        this.inputtransactions = inputtransactions;
        this.outputtransactions = outputtransactions;
        this.translations = translations;
        this.limits = limits;
        this.archivepolicies = archivepolicies;
        this.period = period;
        this.archivelongevity = archivelongevity;
    }

    public boolean
    equals(java.lang.Object rhs)
    {
        if(this == rhs)
        {
            return true;
        }
        PointDescriptionIce _r = null;
        try
        {
            _r = (PointDescriptionIce)rhs;
        }
        catch(ClassCastException ex)
        {
        }

        if(_r != null)
        {
            if(!java.util.Arrays.equals(names, _r.names))
            {
                return false;
            }
            if(source != _r.source && source != null && !source.equals(_r.source))
            {
                return false;
            }
            if(description != _r.description && description != null && !description.equals(_r.description))
            {
                return false;
            }
            if(shortdescription != _r.shortdescription && shortdescription != null && !shortdescription.equals(_r.shortdescription))
            {
                return false;
            }
            if(units != _r.units && units != null && !units.equals(_r.units))
            {
                return false;
            }
            if(enabled != _r.enabled)
            {
                return false;
            }
            if(!java.util.Arrays.equals(inputtransactions, _r.inputtransactions))
            {
                return false;
            }
            if(!java.util.Arrays.equals(outputtransactions, _r.outputtransactions))
            {
                return false;
            }
            if(!java.util.Arrays.equals(translations, _r.translations))
            {
                return false;
            }
            if(!java.util.Arrays.equals(limits, _r.limits))
            {
                return false;
            }
            if(!java.util.Arrays.equals(archivepolicies, _r.archivepolicies))
            {
                return false;
            }
            if(period != _r.period)
            {
                return false;
            }
            if(archivelongevity != _r.archivelongevity)
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
        if(names != null)
        {
            for(int __i0 = 0; __i0 < names.length; __i0++)
            {
                if(names[__i0] != null)
                {
                    __h = 5 * __h + names[__i0].hashCode();
                }
            }
        }
        if(source != null)
        {
            __h = 5 * __h + source.hashCode();
        }
        if(description != null)
        {
            __h = 5 * __h + description.hashCode();
        }
        if(shortdescription != null)
        {
            __h = 5 * __h + shortdescription.hashCode();
        }
        if(units != null)
        {
            __h = 5 * __h + units.hashCode();
        }
        __h = 5 * __h + (enabled ? 1 : 0);
        if(inputtransactions != null)
        {
            for(int __i1 = 0; __i1 < inputtransactions.length; __i1++)
            {
                if(inputtransactions[__i1] != null)
                {
                    __h = 5 * __h + inputtransactions[__i1].hashCode();
                }
            }
        }
        if(outputtransactions != null)
        {
            for(int __i2 = 0; __i2 < outputtransactions.length; __i2++)
            {
                if(outputtransactions[__i2] != null)
                {
                    __h = 5 * __h + outputtransactions[__i2].hashCode();
                }
            }
        }
        if(translations != null)
        {
            for(int __i3 = 0; __i3 < translations.length; __i3++)
            {
                if(translations[__i3] != null)
                {
                    __h = 5 * __h + translations[__i3].hashCode();
                }
            }
        }
        if(limits != null)
        {
            for(int __i4 = 0; __i4 < limits.length; __i4++)
            {
                if(limits[__i4] != null)
                {
                    __h = 5 * __h + limits[__i4].hashCode();
                }
            }
        }
        if(archivepolicies != null)
        {
            for(int __i5 = 0; __i5 < archivepolicies.length; __i5++)
            {
                if(archivepolicies[__i5] != null)
                {
                    __h = 5 * __h + archivepolicies[__i5].hashCode();
                }
            }
        }
        __h = 5 * __h + (int)period;
        __h = 5 * __h + archivelongevity;
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
        stringarrayHelper.write(__os, names);
        __os.writeString(source);
        __os.writeString(description);
        __os.writeString(shortdescription);
        __os.writeString(units);
        __os.writeBool(enabled);
        stringarrayHelper.write(__os, inputtransactions);
        stringarrayHelper.write(__os, outputtransactions);
        stringarrayHelper.write(__os, translations);
        stringarrayHelper.write(__os, limits);
        stringarrayHelper.write(__os, archivepolicies);
        __os.writeLong(period);
        __os.writeInt(archivelongevity);
    }

    public void
    __read(IceInternal.BasicStream __is)
    {
        names = stringarrayHelper.read(__is);
        source = __is.readString();
        description = __is.readString();
        shortdescription = __is.readString();
        units = __is.readString();
        enabled = __is.readBool();
        inputtransactions = stringarrayHelper.read(__is);
        outputtransactions = stringarrayHelper.read(__is);
        translations = stringarrayHelper.read(__is);
        limits = stringarrayHelper.read(__is);
        archivepolicies = stringarrayHelper.read(__is);
        period = __is.readLong();
        archivelongevity = __is.readInt();
    }
}
