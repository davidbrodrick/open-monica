package cass.monica.rest;

import java.lang.reflect.Type;

import atnf.atoms.time.AbsTime;
import atnf.atoms.time.AbsTime.Format;

import com.google.gson.*;

public class AbsTimeSerializer implements JsonSerializer<AbsTime> {
	private Format format;
	public AbsTimeSerializer(Format format) {
		this.format= format;
	}
	public JsonElement serialize(AbsTime src, Type typeOfSrc, 
			                     JsonSerializationContext context) {
	    return new JsonPrimitive(src.toString(this.format));
	  }
}
