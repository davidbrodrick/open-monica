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

import atnf.atoms.mon.PointData;
import atnf.atoms.time.AbsTime;

import com.google.gson.*;

/**
 * Handle serialisation/deserialisation of PointDescription objects to JSON.
 * 
 * @author David Brodrick
 */
public class PointDataSerializer implements JsonSerializer<PointData> {
  /** Serialise from a PointDescription to JSON, in the appropriate format. */
  public JsonElement serialize(PointData src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject res = new JsonObject();
    if (src.getName() != null) {
      res.addProperty("name", src.getName());
    }
    // How to use the AbsTime serialiser class from here?
    res.addProperty("ts", src.getTimestamp().toString(AbsTime.Format.UTC_STRING));
    if (src.getData() != null) {
      res.add("value", context.serialize(src.getData()));
    }
    if (src.getAlarm()) {
      res.addProperty("alarm", src.getAlarm());
    }
    return res;
  }
}
