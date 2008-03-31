/**
 * Class: Authenticator
 * Description: Uses directory lookup to authenticate a username and password
 * @author Le Cuong Nguyen
 **/
package atnf.atoms.mon.util;

import java.util.*;
import javax.naming.*;
import javax.naming.directory.*;

public class Authenticator
{
   private static Hashtable itsEnv = new Hashtable();
   private static DirContext itsContext = null;
   private static final String itsServiceProvider = "com.sun.jndi.nis.NISCtxFactory";
   private static final String itsHost = "nis://"+MonitorConfig.getProperty("NISHost")+"/"+MonitorConfig.getProperty("NISDomain");
   private static final String itsPrePass = "system/passwd/";
   private static final String itsPass = "nisMapEntry";
   private static final String itsGroup = MonitorConfig.getProperty("NISGroup");
   
   static {
      reinit();
   }
   
   /** Re-initialises the static variables */
   public static void reinit()
   {
      synchronized (itsEnv) {
	 itsEnv.clear();
	 itsEnv.put(Context.INITIAL_CONTEXT_FACTORY, itsServiceProvider);
         itsEnv.put(Context.PROVIDER_URL, itsHost);
	 try {
	    itsContext = new InitialDirContext(itsEnv);
	 } catch (Exception e) {e.printStackTrace();}
      }
   }
   
   public static boolean checkAll(String username, String password)
   {
      return (check(username, password) && checkGroup(username));
   }
   
   public static boolean checkGroup(String username)
   {
      try {
         // Lets check for group membership
	 Attributes info = itsContext.getAttributes("system/group/"+itsGroup);
	 Attribute members = info.get("memberUid");
	 return (members.remove(username));
      } catch (Exception e) {e.printStackTrace();}
      return false;
   }
   
   public static boolean check(String username, String password)
   {
      try {
         Attributes info = itsContext.getAttributes(itsPrePass+username);
         String encPass = (String)(info.get(itsPass).get(0));
	 StringTokenizer tok = new StringTokenizer(encPass,":");
	 tok.nextToken();
	 encPass = tok.nextToken();
	 String myPass = Crypt.crypt(encPass.substring(0, 2), password);
	 return encPass.equals(myPass);
      } catch (Exception e) {e.printStackTrace();}
      return false;
   }
   
   public static void getpw()
   {
      try {
         StringTokenizer tok = null;
	 Attributes info = null;
	 NamingEnumeration ne = itsContext.list("user");
	 while (ne.hasMore()) {
	    tok = new StringTokenizer(ne.next().toString(),":");
	    info = itsContext.getAttributes(itsPrePass+tok.nextToken());
	    System.out.println(info.get("nisMapEntry").get(0));
	 }
      } catch (Exception e) {e.printStackTrace();}
   }
   
   public static void main(String args[])
   {
      if (args.length < 1) getpw();
      else {
	 if (Authenticator.checkAll(args[0], args[1])) System.out.println("PASSED");
	 else System.out.println("FAILED");
      }
   }
}
