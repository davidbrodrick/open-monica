// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import atnf.atoms.mon.PointBuffer;
import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.time.AbsTime;
import atnf.atoms.time.RelTime;

/**
 * Contains static methods that may be useful.
 */
public abstract class MonitorUtils {
	private static Hashtable<String, String> theirMacros = new Hashtable<String, String>();

	public static String[] toStringArray(Object[] data) {
		String[] res = new String[data.length];
		for (int i = 0; i < res.length; i++) {
			res[i] = (String) (data[i]);
		}
		return res;
	}

	public static Object deSerialize(byte[] data) throws Exception {
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		ObjectInputStream ois = new ObjectInputStream(bais);
		Object res = ois.readObject();
		bais.close();
		ois.close();
		return res;
	}

	public static String[] tokToStringArray(String line) {
		StringTokenizer tok = new StringTokenizer(line, "\"");
		String[] res = new String[tok.countTokens()];
		for (int i = 0; tok.hasMoreTokens(); i++) {
			res[i] = tok.nextToken();
		}
		return res;
	}

	/**
	 * Break a line into tokens, uses whitespaces and braces as token markers.
	 */
	public static String[] getTokens(String line) {
		Vector<String> res = new Vector<String>();
		int startPos = 0;
		int endPos = line.length();
		int start = 0;
		for (; startPos < endPos; startPos++) {
			char c = line.charAt(startPos);
			while (c == ' ' || c == '\t') {
				// Found a whitespace
				startPos++;
				c = line.charAt(startPos);
			}
			start = startPos;
			if (line.charAt(startPos) == '{') {
				// Composite
				start++;
				while (startPos < endPos && line.charAt(startPos) != '}') {
					startPos++;
				}
				res.add(line.substring(start, startPos));
				startPos++;
			} else {
				if (line.charAt(startPos) == '\"') {
					// String literal
					start++;
					startPos++;
					while (startPos < endPos && line.charAt(startPos) != '\"') {
						startPos++;
					}
					if (startPos <= start) {
						res.add("");
					} else {
						res.add(line.substring(start, startPos));
					}
					startPos++;
				} else {
					while (startPos < endPos && line.charAt(startPos) != ' ' && line.charAt(startPos) != '\t' && line.charAt(startPos) != ',') {
						if (line.charAt(startPos) == '\"') {
							// String literal attached to a string
							startPos++;
							while (startPos < endPos && line.charAt(startPos) != '\"') {
								// Find the end of it
								startPos++;
							}
							if (startPos != endPos) {
								startPos++;
							}
						} else {
							startPos++;
						}
					}
					res.add(line.substring(start, startPos));
				}
			}
		}

		String[] res_str = new String[res.size()];
		for (int i = 0; i < res_str.length; i++) {
			res_str[i] = (String) (res.elementAt(i));
		}
		return res_str;
	}

	public static byte[] compress(Object data) {
		if (data == null) {
			return null;
		}
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPOutputStream gout = new GZIPOutputStream(baos);
			ObjectOutputStream oos = new ObjectOutputStream(gout);
			oos.writeObject(data);
			oos.flush();
			gout.finish();
			return baos.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Object decompress(byte[] data) {
		if (data == null) {
			return null;
		}
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			GZIPInputStream gin = new GZIPInputStream(bais);
			ObjectInputStream ois = new ObjectInputStream(gin);
			return ois.readObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String replaceTok(String line, String replacement) {
		return replaceTok(line, replacement, "$1");
	}

	/** Replaces a particular token in a string with another token */
	public static String replaceTok(String line, String replacement, String tok) {
		StringBuffer res = new StringBuffer(line);
		char[] tokChars = tok.toCharArray();
		for (int i = 0; i < res.length() - tok.length() + 1; i++) {
			boolean match = true;
			for (int k = 0; k < tok.length(); k++) {
				if (res.charAt(i + k) != tokChars[k]) {
					match = false;
					break;
				}
			}
			if (match) {
				res.replace(i, i + tok.length(), replacement);
			}
		}
		return res.toString();
	}

	/** Reads and parses a file */
	public static String[] parseFile(Reader reader) {
		ArrayList<String> result = new ArrayList<String>();
		theirMacros = new Hashtable<String, String>();

		try {
			LineNumberReader lnr = new LineNumberReader(reader);
			String line = null;
			int commentDepth = 0;
			int lineNum = 0;
			while ((line = lnr.readLine()) != null) {
				lineNum++;
				// Leading and trailing spaces
				line = line.trim();
				// Ignore blank lines
				if (line.length() == 0) {
					continue;
				}
				// Ignore Comments
				if (line.startsWith("#")) {
					continue;
				}
				if (line.startsWith("//")) {
					continue;
				}
				int tempPos = line.indexOf("/*");
				if (tempPos > -1) {
					commentDepth++;
					if (line.indexOf("*/", tempPos) > -1) {
						commentDepth--;
					} else {
						continue;
					}
				}
				if (line.indexOf("*/") > -1) {
					commentDepth--;
					continue;
				}
				if (commentDepth > 0) {
					continue;
				}

				// Commands
				if (line.startsWith("!")) {
					parseCommand(line);
				} else {
					if (theirMacros.size() < 1) {
						result.add(line);
					} else {
						Enumeration<String> keys = theirMacros.keys();
						while (keys.hasMoreElements()) {
							String key = (String) keys.nextElement();
							line = MonitorUtils.replaceTok(line, (String) theirMacros.get(key), key);
						}
						result.add(line);
					}
				}
			}
		} catch (Exception e) {
			System.err.println("MonitorUtils.parseFile: " + e.getClass());
			e.printStackTrace();
			return null;
		}
		return toStringArray(result.toArray());
	}

	public static String[] parseFile(String filename) {
		try {
			FileReader fr = new FileReader(filename);
			return parseFile(fr);
		} catch (Exception e) {
			System.err.println("MonitorUtils.parseFile(" + filename + "): " + e.getClass());
			e.printStackTrace();
		}
		return null;
	}

	protected static void parseCommand(String line) {
		StringTokenizer tok = new StringTokenizer(line);
		String command = tok.nextToken().trim();
		if (command.equalsIgnoreCase("!define")) {
			String macro = tok.nextToken().trim();
			if (tok.nextToken().trim().equals("=")) {
				String replacement = line.substring(line.indexOf("=") + 1).trim();
				theirMacros.put(macro, replacement);
			}
		}
	}

	/**
	 * Parse type code and value strings and return the appropriate object.
	 * 
	 * @param type
	 *          One of <tt>int</tt>, <tt>flt</tt>, <tt>dbl</tt>, <tt>str</tt>, <tt>bool</tt>.
	 * @param strval
	 *          The string representation of the value, eg "3.141", or "true".
	 * @return The appropriate Object.
	 * @throws IllegalArgumentException
	 *           If the type code is invalid or the string value cannot be parsed.
	 */
	public static Object parseFixedValue(String type, String strval) throws IllegalArgumentException {
		Object res;
		try {
			if (type.equals("dbl")) {
				res = new Double(strval);
			} else if (type.equals("flt")) {
				res = new Float(strval);
			} else if (type.equals("int")) {
				res = new Integer(strval);
			} else if (type.equals("str")) {
				res = strval;
			} else if (type.equals("bool")) {
				res = new Boolean(strval);
			} else if (type.equals("abst")) {
				long foo = Long.parseLong(strval, 16); // Hex
				res = AbsTime.factory(foo);
			} else if (type.equals("relt")) {
				long foo = Long.parseLong(strval); // Decimal
				res = RelTime.factory(foo);
			} else {
				throw new IllegalArgumentException("Unknown type code for value data type");
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to parse data value");
		}
		return res;
	}

	/**
	 * Try to interpret the input as a boolean.
	 * 
	 * If the input is a boolean then this is trivial. If the input is numeric then we interpret 0 as false and any other value as
	 * true.
	 * 
	 * An exception will be thrown if the input cannot be parsed.
	 */
	public static boolean parseAsBoolean(Object in) throws IllegalArgumentException {
		boolean res;
		if (in == null) {
			throw new IllegalArgumentException("Input must not be null");
		} else if (in instanceof Boolean) {
			res = ((Boolean) in).booleanValue();
		} else if (in instanceof Number) {
			if (((Number) in).doubleValue() == 0.0) {
				res = false;
			} else {
				res = true;
			}
		} else if (in instanceof String) {
			String instr = (String) in;
			if (instr.equalsIgnoreCase("true")) {
				res = true;
			} else if (instr.equalsIgnoreCase("false")) {
				res = false;
			} else {
				throw new IllegalArgumentException("String \"" + instr + "\" could not be parsed as a boolean: expect \"true\" or \"false\"");
			}
		} else {
			throw new IllegalArgumentException("Could not interpret input as a boolean");
		}
		return res;
	}

	/**
	 * Find the index of the first PointData with a timestamp after the one specified.
	 * 
	 * @return -1 if no suitable data could be found.
	 */
	public static int getNextPointData(Vector<PointData> data, AbsTime ts) {
		synchronized (data) {
			if (data.isEmpty()) {
				return -1;
			}
			/*
			 * System.err.println("Seeking first point after " + ts.toString(AbsTime.Format.UTC_STRING)); for (int i=0; i<data.size(); i++
			 * ) { //System.err.println(i + " " + data.get(i).getTimestamp().toString(AbsTime.Format.UTC_STRING)); }
			 */
			int fullsize = data.size();
			// Handle special cases
			if (data.get(0).getTimestamp().isAfter(ts)) {
				// All data is after the reference time
				return 0;
			}
			if (data.get(fullsize - 1).getTimestamp().isBeforeOrEquals(ts)) {
				// No data is after the reference time
				return -1;
			}

			int start = 0;
			int end = fullsize - 1;

			while ((end - start) > 1) {
				int mid = start + (end - start) / 2;
				if (data.get(mid).getTimestamp().isBeforeOrEquals(ts)) {
					start = mid + 1;
				} else {
					end = mid;
				}
				// System.err.println("Checking span start=" + start + ", end=" + end);
			}
			if (data.get(start).getTimestamp().isBeforeOrEquals(ts)) {
				// Next element is the final result
				start++;
			}
			// System.err.println("Found result at " + start + " " + data.get(start).getTimestamp().toString(AbsTime.Format.UTC_STRING));
			return start;
		}
	}

	/**
	 * Find the index of the first PointData with a timestamp before or equal to the one specified.
	 * 
	 * @return -1 if no suitable data could be found.
	 */
	public static int getPrevEqualsPointData(Vector<PointData> data, AbsTime ts) {
		synchronized (data) {
			if (data.isEmpty()) {
				return -1;
			}
			/*
			 * System.err.println("Seeking first point before or equals " + ts.toString(AbsTime.Format.UTC_STRING)); for (int i=0;
			 * i<data.size(); i++ ) { //System.err.println(i + " " + data.get(i).getTimestamp().toString(AbsTime.Format.UTC_STRING)); }
			 */
			int fullsize = data.size();
			// Handle special cases
			if (data.get(fullsize - 1).getTimestamp().isBeforeOrEquals(ts)) {
				// All data is before the reference time
				return fullsize - 1;
			}
			if (data.get(0).getTimestamp().isAfter(ts)) {
				// No data is before the reference time
				return -1;
			}

			int start = 0;
			int end = fullsize - 1;

			while ((end - start) > 1) {
				int mid = start + (end - start) / 2;
				if (data.get(mid).getTimestamp().isBeforeOrEquals(ts)) {
					start = mid;
				} else {
					end = mid - 1;
				}
				// System.err.println("Checking span start=" + start + ", end=" + end);
			}
			if (data.size() > start + 1 && data.get(start + 1).getTimestamp().isBeforeOrEquals(ts)) {
				// Next element is the final result
				start++;
			}
			// System.err.println("Found result at " + start + " " + data.get(start).getTimestamp().toString(AbsTime.Format.UTC_STRING));
			return start;
		}
	}

	/**
	 * Substitute parameters for macro flags in the string.
	 * 
	 * <P>
	 * The supported substitutions are:
	 * <ul>
	 * <li>$V The current value of the data.
	 * <li>$V[point.name] The current value of the specified point.
	 * <li>$U The units of the data.
	 * <li>$N The name of the point.
	 * <li>$S The source part of the point name ($1 is also supported for this purpose).
	 * <li>$D The point's description.
	 * <li>$T The data's timestamp.
	 * <li>$A The alarm status of the data (true or false).
	 * <li>$a The alarm status of the data (ALARMING or OK).
	 * </ul>
	 * */
	public static String doSubstitutions(String template, PointData data, PointDescription point) {
		String res = template;

		// Substitute source first so it can be used in lookup of other points
		String source = null;
		if (point != null) {
			source = point.getSource();
		} else if (data != null) {
			source = data.getSource();
		}
		if (source != null) {
			res = res.replace("$S", source);
			res = res.replace("$1", source);
		}

		while (res.indexOf("$V[") != -1) {
			int start = res.indexOf("$V[");
			int end = res.indexOf(']', start);
			if (end == -1) {
				// Cannot parse this template
				break;
			}
			String pointname = res.substring(start + 3, end);
			PointDescription pointref = PointDescription.getPoint(pointname);
			if (pointref == null) {
				res = res.replace(res.substring(start, end + 1), "[point not found]");
			} else {
				PointData pointdata = PointBuffer.getPointData(pointref);
				if (pointdata == null) {
					res = res.replace(res.substring(start, end + 1), "null");
				} else {
					res = res.replace(res.substring(start, end + 1), "" + pointdata.getData());
				}
			}
		}

		if (data != null) {
			// Substitute our value
		  if (data.getData()==null) {
		    res = res.replace("$V", "null");
		  } else {
			  res = res.replace("$V", data.getData().toString());
		  }
			// Substitute time stamp
			res = res.replace("$T", data.getTimestamp().toString(AbsTime.Format.UTC_STRING));
			// Alarm status
			res = res.replace("$A", "" + data.getAlarm());
			if (data.getAlarm()) {
				res = res.replace("$a", "ALARMING");
			} else {
				res = res.replace("$a", "OK");
			}
		}

		// Substitute units
		res = res.replace("$U", point.getUnits());
		// Substitute point name
		res = res.replace("$N", point.getFullName());
		// Substitute point description
		res = res.replace("$D", point.getLongDesc());

		return res;
	}

	/** Prune a list of point names back to a minimal pattern which exactly matches all of the points. */
	/*public static String prunePointTree(String point, HashSet<String> selected, HashSet<String> allpoints) {
    String res = point;
    int lastdot = point.lastIndexOf('.');
    if (lastdot == -1) {
      System.err.println("no . in point name");
      return point;
    }
    String branch = point.substring(0, lastdot);
    System.err.println("for point " + point + " branch is " + branch);
    boolean allmatch = true;
    Iterator<String> i = allpoints.iterator();
    while (allmatch && i.hasNext()) {
      String thispoint = i.next();
      if (thispoint.startsWith(branch) && !selected.contains(thispoint)) {
        System.err.println("selected set doesn't contain " + thispoint);
        allmatch = false;
      } else if (thispoint.startsWith(branch)) {
        System.err.println("selected set contains " + thispoint);
      }
    }
    if (allmatch) {
      res = branch;
    }
    System.err.println("result is " + res);
    return res;
  }*/

	/**
	 * Method for pruning a set of points down to a minimal pattern with wildcards to indicate that
	 * all potential points from that tree are selected. Use with {@link #sprout(HashSet, HashSet)}
	 * to return the pruned list to its original format
	 * @param selections The Set of selected point names
	 * @param allPoints The Set of all point names
	 * @return A pruned Set of point names in a minimal pattern
	 */
	public static HashSet<String> prunePointTree(HashSet<String> selections, HashSet<String> allPoints){
		HashSet<String> res = new HashSet<String>();
		HashSet<String> prep = prunePointTreePrep(selections, allPoints);
		HashSet<String> newAll = new HashSet<String>();
		for (String s : allPoints){
			String str = s.substring(0, s.lastIndexOf('.'));
			newAll.add(str);
		}
		res = prune(prep, newAll);
		return res;
	}

	/**
	 * Preparatory step to ensure that only points with a potential to be shortened are
	 * acted upon
	 * @param selections The initial selected points
	 * @param allPoints The all-encompassing total list of points
	 * @return A HashSet containing a slightly trimmed list of points
	 */
	private static HashSet<String> prunePointTreePrep(HashSet<String> selections, HashSet<String> allPoints){
		HashSet<String> res = new HashSet<String>();
		@SuppressWarnings ("unchecked")
		HashSet<String> selected = (HashSet<String>) selections.clone();
		for (String s : selected){
			int lastDot = s.lastIndexOf('.');
			if (lastDot == -1){
				res.add(s);
				continue;
			}
			String branch = s.substring(0, lastDot);
			if (res.contains(branch + ".*")) continue; //go to next point s
			//			System.err.println("for point " + s + " branch is " + branch);
			boolean allmatch = true;
			Iterator<String> i = allPoints.iterator();
			while (allmatch && i.hasNext()) {
				String thispoint = i.next();
				if (thispoint.startsWith(branch) && !selections.contains(thispoint)) {
					//					System.err.println("selected set doesn't contain " + thispoint);
					allmatch = false;
				} else if (thispoint.startsWith(branch)) {
					//					System.err.println("selected set contains " + thispoint);
				}
			}
			if (allmatch) {
				res.add(branch + ".*");
			} else {
				res.add(s);
			}
		}

		return res;
	}

	/**
	 * The main recursive pruning method used by {@link #prunePointTree(HashSet, HashSet)}.
	 * @param prep The HashSet that has been "prepared" by the prunePointTreePrep() method
	 * @param allPoints The HashSet of all points in their full form
	 * @return A fully pruned HashSet
	 */
	private static HashSet<String> prune(HashSet<String> prep,
			HashSet<String> allPoints) {
		HashSet<String> res = new HashSet<String>();
		HashSet<String> doOvers = new HashSet<String>();
		for (String s : prep){
			if (!s.endsWith(".*")){
				res.add(s);
				continue;
			} else {
				String branch = s.substring(0, s.lastIndexOf('.')); //chop off ".*"
				try {
					branch = branch.substring(0, branch.lastIndexOf('.')); //chop off next segment
				} catch (IndexOutOfBoundsException e){
					res.add(branch + ".*");
					continue;
				}
				if (doOvers.contains(branch + ".*") || res.contains(branch + ".*")) continue; //go to next point s
				boolean allmatch = true;
				Iterator<String> i = allPoints.iterator();
				while (allmatch && i.hasNext()) {
					String thispoint = i.next();
					if (thispoint.startsWith(branch)){
						if (!prep.contains(thispoint + ".*")) {
							allmatch = false;
						}
					}
				}
				if (allmatch) {
					branch += ".*";
					doOvers.add(branch);
				} else {
					res.add(s);
				}
			}
		}
		HashSet<String> newAll = new HashSet<String>();
		boolean allfine = true;
		for (String s : allPoints){
			try {
				String str = s.substring(0, s.lastIndexOf('.'));
				newAll.add(str);
			} catch (StringIndexOutOfBoundsException ex){
				allfine = false;
			}
		}
		if (!allfine){
			return res;
		}
		HashSet<String> pruned = new HashSet<String>();
		if (doOvers.size() > 0) pruned = prune(doOvers, newAll);
		res.addAll(pruned);
		return res;
	}
	
	/**
	 * Reversal method to retrieve the selected points from an already pruned point tree. 
	 * Designed to be backwards-compatible with old Setups of points that use a single list
	 * of many individual Strings.
	 * @param prunedSet A HashSet of Strings that has been pruned already by {@link #prunePointTree(HashSet, HashSet)}
	 * @param allPoints A HashSet of String containing a set of all possible defined points
	 * @return The expanded HashSet containing all the originally selected points
	 */
	public static HashSet<String> sprout(HashSet<String> prunedSet, HashSet<String> allPoints){
		HashSet<String> res = new HashSet<String>();
		HashSet<String> bad = new HashSet<String>();
		for (String s : prunedSet){
			if (!s.endsWith(".*")){
				res.add(s);
				bad.add(s);
			}
		}
		for (String s : bad){
			prunedSet.remove(s);
		}
		for (String s : prunedSet){
			String prunedNub = s.substring(0, s.lastIndexOf('.'));
			for (String str: allPoints){
				if (str.startsWith(prunedNub + ".")){
					res.add(str);
				}
			}
		}
		return res;
	}

	/**
	 * Simple test for pruning and sprouting
	 * @param args
	 */
	public static void main(String[] args){
		HashSet<String> selections = new HashSet<String>();
		HashSet<String> allPoints = new HashSet<String>();
		allPoints.add("x.x.x.x");
		allPoints.add("x.x.x.y");
		allPoints.add("x.x.x.z");
		allPoints.add("x.x.y.a");
		allPoints.add("x.x.y.b");
		allPoints.add("x.z.y.a");
		allPoints.add("a.b.c.d");
		allPoints.add("a.b.c.e");

		selections.add("x.x.x.x");
		selections.add("x.x.x.y");
		selections.add("x.x.x.z");
		selections.add("x.x.y.a");
		selections.add("x.x.y.b");
		selections.add("a.b.c.d");
//		selections.add("x.z.y.a");
//		selections.add("a.b.c.e");

		HashSet<String> res = prunePointTree(selections, allPoints);
		for (String s : res){
			System.out.println(s);
		}
		HashSet<String> sprouted = sprout(res, allPoints);
		System.out.println();
		for (String s : sprouted){
			System.out.println(s);
		}
	}
}
