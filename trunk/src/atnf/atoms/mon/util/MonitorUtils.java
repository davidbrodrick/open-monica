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
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Contains static methods that may be useful.
 *
 * @author Le Cuong Nguyen
 */
public abstract class MonitorUtils
{
   private static Hashtable itsMacros = new Hashtable();

   public static String[] toStringArray(Object[] data)
   {
      String[] res = new String[data.length];
      for (int i = 0; i < res.length; i++) {
        res[i] = (String)(data[i]);
      }
      return res;
   }


   public static Object deSerialize(byte[] data) throws Exception
   {
      ByteArrayInputStream bais = new ByteArrayInputStream(data);
      ObjectInputStream ois = new ObjectInputStream(bais);
      Object res = ois.readObject();
      bais.close();
      ois.close();
      return res;
   }


   public static String[] tokToStringArray(String line)
   {
      StringTokenizer tok = new StringTokenizer(line, "\"");
      String[] res = new String[tok.countTokens()];
      for (int i = 0; tok.hasMoreTokens(); i++) {
        res[i] = tok.nextToken();
      }
      return res;
   }
   

   /** Break a line into tokens, uses whitespaces and braces as token
    * markers. */
   public static String[] getTokens(String line)
   {
      Vector res = new Vector();
      int startPos = 0;
      int endPos = line.length();
      int start = 0;
      for (;startPos < endPos; startPos++) {
         char c = line.charAt(startPos);
         // Found a whitespace
         while (c == ' ' || c == '\t') {
           startPos++;
           c = line.charAt(startPos);
         }
         start = startPos;
         // Composite
         if (line.charAt(startPos) == '{') {
           start++;
           while (startPos < endPos && line.charAt(startPos) != '}') {
            startPos++;
          }
           res.add(line.substring(start, startPos));
           startPos++;
         } else {
           // Found a string literal
           if (line.charAt(startPos) == '\"') {
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
             while (startPos < endPos && line.charAt(startPos) != ' ' && line.charAt(startPos) != '\t') {
              startPos++;
            }
             res.add(line.substring(start, startPos));
           }
         }
      }
      String[] res_str = new String[res.size()];
      for (int i = 0; i < res_str.length; i++) {
        res_str[i] = (String)(res.elementAt(i));
      }
      return res_str;
   }
   

   public static byte[] compress(Object data)
   {
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
      } catch (Exception e) {e.printStackTrace();}
      return null;
   }


   public static Object decompress(byte[] data)
   {
      if (data == null) {
        return null;
      }
      try {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        GZIPInputStream gin = new GZIPInputStream(bais);
        ObjectInputStream ois = new ObjectInputStream(gin);
        return ois.readObject();
      } catch (Exception e) {e.printStackTrace();}
      return null;
   }
   

   public static String replaceTok(String line, String replacement)
   {
      return replaceTok(line, replacement, "$1");
   }


   /** Replaces a particular token in a string with another token */
   public static String replaceTok(String line, String replacement, String tok)
   {
      StringBuffer res = new StringBuffer(line);
      char[] tokChars = tok.toCharArray();
      for (int i = 0; i < res.length() - tok.length() + 1; i++) {
         boolean match = true;
         for (int k = 0; k < tok.length(); k++) {
           if (res.charAt(i+k) != tokChars[k]) {
             match = false;
             break;
           }
         }
         if (match) {
          res.replace(i, i+tok.length(), replacement);
        }
      }
      return res.toString();
   }

   
   /** Reads and parses a file */
   public static String[] parseFile(Reader reader)
   {
     ArrayList result = new ArrayList();
     itsMacros = new Hashtable();

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
         if (line.length()==0) {
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
	         if (itsMacros.size() < 1) {
             result.add(line);
           } else {
             Enumeration keys = itsMacros.keys();
             while (keys.hasMoreElements()) {
               String key = (String)keys.nextElement();
               line = MonitorUtils.replaceTok(line, (String)itsMacros.get(key), key);
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


   public static String[] parseFile(String filename)
   {
      try {
         FileReader fr = new FileReader(filename);
         return parseFile(fr);
      } catch (Exception e) {
        System.err.println("MonitorUtils.parseFile(" + filename + "): " + e.getClass());
        e.printStackTrace();
      }
      return null;
   }


   protected static void parseCommand(String line)
   {
      StringTokenizer tok = new StringTokenizer(line);
      String command = tok.nextToken().trim();
      if (command.equalsIgnoreCase("!define")) {
         String macro = tok.nextToken().trim();
         if (tok.nextToken().trim().equals("=")) {
           String replacement = line.substring(line.indexOf("=")+1).trim();
           itsMacros.put(macro, replacement);
         }
      }
   }
}
