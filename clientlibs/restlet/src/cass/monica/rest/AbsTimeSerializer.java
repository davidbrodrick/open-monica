/*
 *  Copyright (c) 2012 CSIRO Astronomy and Space Science (CASS), Commonwealth
 * Scientific and Industrial Research Organisation (CSIRO) PO Box 76, Epping NSW 1710,
 * Australia atnf-enquiries@csiro.au
 * 
 * MoniCA is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either 
 * version 2 of the License, or (at your option) any later version.
 */

package cass.monica.rest;

import java.lang.reflect.Type;

import atnf.atoms.time.AbsTime;
import atnf.atoms.time.AbsTime.Format;

import com.google.gson.*;

/**
 * Handle serialisation/deserialisation of AbsTime objects to JSON.
 * 
 * @author Malte Marquarding
 * @author David Brodrick
 */
public class AbsTimeSerializer implements JsonSerializer<AbsTime>,
		JsonDeserializer<AbsTime> {
	/** The AbsTime.Format to use. */
	private Format itsFormat;

	/**
	 * Constructor.
	 * 
	 * @param format
	 *            The AbsTime.Format to serialise to.
	 */
	public AbsTimeSerializer(Format format) {
		itsFormat = format;
	}

	/** Serialise from an AbsTime to JSON, in the appropriate format. */
	public JsonElement serialize(AbsTime src, Type typeOfSrc,
			JsonSerializationContext context) {
		return new JsonPrimitive(src.toString(itsFormat));
	}

	/** Deserialise from JSON to an AbsTime. */
	public AbsTime deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {
		return AbsTime.factory(json.getAsJsonPrimitive().getAsString());
	}
}
