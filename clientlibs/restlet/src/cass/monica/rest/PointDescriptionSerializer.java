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

import atnf.atoms.mon.PointDescription;

import com.google.gson.*;

/**
 * Handle serialisation/deserialisation of PointDescription objects to JSON.
 * 
 * @author David Brodrick
 */
public class PointDescriptionSerializer implements JsonSerializer<PointDescription> {
	/** Serialise from a PointDescription to JSON, in the appropriate format. */
	public JsonElement serialize(PointDescription src, Type typeOfSrc,
			JsonSerializationContext context) {
	  JsonObject res = new JsonObject();
    res.addProperty("name", src.getFullName());
    String desc = src.getLongDesc(); 
    if (desc!=null && !desc.equals("")) {
      res.addProperty("desc", desc);
    }
    String units = src.getUnits(); 
    if (units!=null && !units.equals("")) {
      res.addProperty("units", units);
    }
    long period= src.getPeriod();
    if (period>0) {
      res.addProperty("period", period/1000000.0);
    }
		return res;
	}
}
