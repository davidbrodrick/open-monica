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

import atnf.atoms.util.Angle;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Handle serialisation/deserialisation of AbsTime objects to JSON.
 * 
 * @author Xinyu Wu
 */
public class AngleSerializer implements JsonSerializer<Angle>,
		JsonDeserializer<Angle> {

	/**
	 * Constructor.
	 */
	public AngleSerializer() {
	}

	/** Serialise from an Angle to JSON, in the appropriate format. */
	public JsonElement serialize(Angle src, Type typeOfSrc,
			JsonSerializationContext context) {
		
		return new JsonPrimitive(src.toString());
	}

	/** Deserialise from JSON to an Angle. */
	public Angle deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {
		
		return Angle.factory(json.getAsJsonPrimitive().getAsString());
	}
}
