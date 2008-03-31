/**
 * Class: MonitorUtils
 * Description: Contains lots of static methods that may be useful
 * @author Le Cuong Nguyen
 **/
package atnf.atoms.mon.util;

//import atnf.atoms.mon.rpc.*;
import atnf.atoms.util.*;
import java.io.*;
import java.math.*;
import java.util.*;
import java.lang.reflect.*;
import atnf.atoms.mon.*;
import javax.crypto.*;
import javax.crypto.spec.*;
//import org.bouncycastle.jce.provider.*;
import java.security.*;
import java.security.spec.*;
import java.util.zip.*;

public abstract class MonitorUtils
{
   private static Hashtable itsMacros = new Hashtable();

   public static byte[] getDecimalAsByteArray(double num) throws IOException
   {
      return getIntAsByteArray(Double.doubleToLongBits(num));
   }
   
   public static byte[] getIntAsByteArray(int num) throws IOException
   {
      return getIntAsByteArray((long)num);
   }
   
   public static byte[] getIntAsByteArray(long num) throws IOException
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      BigInteger bi = new BigInteger(num+"");
      byte[] bia = bi.toByteArray();
      baos.write(bia.length);
      baos.write(bia);
      byte[] res = baos.toByteArray();
      baos.reset();
      baos.close();
      return res;
   }
   
   public static byte[] getObjectAsByteArray(Object data) throws IOException
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(data);
      oos.flush();
      byte[] res = baos.toByteArray();
      oos.reset();
      oos.close();
      baos.reset();
      baos.close();
      return res;
   }

   public static String[] toStringArray(Object[] data)
   {
      String[] res = new String[data.length];
      for (int i = 0; i < res.length; i++) res[i] = (String)(data[i]);
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
      for (int i = 0; tok.hasMoreTokens(); i++) res[i] = tok.nextToken();
      return res;
   }
   
   /** Scales and Offsets a Number value */
   public static Object scaleOffset(Number val, double scale, double offset)
   {
      try {
	 Constructor con = val.getClass().getConstructor(new Class[]{String.class});
	 double res = val.doubleValue() * scale + offset;
	 Object resObj = null;
	 if (val instanceof Double || val instanceof Float)
	    resObj = con.newInstance(new Object[]{res+""});
	 else resObj = con.newInstance(new Object[]{(long)res+""});
	 return resObj;
      } catch (Exception e) {e.printStackTrace();}
      return null;
   }

   /** Scales and Offsets an Angle */
   public static Object scaleOffset(Angle val, double scale, double offset)
   {
      double val_d = val.getValue();
      val_d = val_d * scale + offset;
      return Angle.factory(val_d);
   }
   
   /** breaks a line up into tokens, uses whitespaces and braces are token
   markers */
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
	    while (startPos < endPos && line.charAt(startPos) != '}') startPos++;
	    res.add(line.substring(start, startPos));
	    startPos++;
	 } else {
	    // Found a string literal
	    if (line.charAt(startPos) == '\"') {
	       start++;
	       startPos++;
	       while (startPos < endPos && line.charAt(startPos) != '\"') startPos++;
	       if (startPos <= start) res.add("");
	       else res.add(line.substring(start, startPos));
	       startPos++;
	    } else {
	       while (startPos < endPos && line.charAt(startPos) != ' ' && line.charAt(startPos) != '\t') startPos++;
	       res.add(line.substring(start, startPos));
	    }
	 }
      }
      String[] res_str = new String[res.size()];
      for (int i = 0; i < res_str.length; i++) res_str[i] = (String)(res.elementAt(i));
      return res_str;
   }
   
   public static String deTokenStringArray(String line, String token)
   {
      StringTokenizer tok = new StringTokenizer(line, token);
      if (tok.countTokens() < 1) return line;
      String res = tok.nextToken();
      while (tok.hasMoreTokens())
         if (!tok.nextToken().equals(res)) return line;
      return res;
   }

   public static void savePoint(String comment, String point)
   {
      String fname = MonitorConfig.getProperty("PointSaveFile");
      try {
         FileWriter fw = new FileWriter(fname, true);
	 if (comment != null && comment.trim().length() > 0) fw.write("# "+comment+"\r\n");
	 fw.write(point+"\r\n");
	 fw.flush();
	 fw.close();
      } catch (Exception e) {e.printStackTrace();}
   }

   public static byte[] compress(Object data)
   {
      if (data == null) return null;
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
      if (data == null) return null;
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
         for (int k = 0; k < tok.length(); k++)
	    if (res.charAt(i+k) != tokChars[k]) {
	       match = false;
	       break;
	    }
	 if (match) res.replace(i, i+tok.length(), replacement);
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
           if (line.length()==0) continue;
	   // Ignore Comments
           if (line.startsWith("#")) continue;
	   if (line.startsWith("//")) continue;
	   int tempPos = line.indexOf("/*");
	   if (tempPos > -1) {
	      commentDepth++;
	      if (line.indexOf("*/", tempPos) > -1) commentDepth--;
	      else continue;
	   }
	   if (line.indexOf("*/") > -1) {
	      commentDepth--;
	      continue;
	   }
	   if (commentDepth > 0) continue;

           // Commands
	   if (line.startsWith("!")) parseCommand(line);
	   else {
	      if (itsMacros.size() < 1) result.add(line);
	      else {
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
