package atnf.atoms.mon.translation;

import org.apache.log4j.Logger;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;
import java.io.*;

/**
 * Translation class to run an external command and retrieve the output.
 * 
 * <P>
 * Arguments include: "command to execute", "parameters to send to that command"
 * 
 * <P>
 * 
 * <P>
 * 
 * @author Balt Indermuehle
 */
public class TranslationRunCmd extends TranslationListener {
  protected static String[] itsArgs = new String[] { "RunCmd", "Listens to two other points", "NumPoints", "Integer", "MonitorPoint 1", "String",
      "MonitorPoint N", "String", "Command", "String", "Params", "String" };
      
  /** The expression in String form. */
  protected String itsCommand;
  protected String itsParams = null;

  /** Logger. */
  protected static Logger theirLogger = Logger.getLogger(TranslationRunCmd.class.getName());

  public TranslationRunCmd(PointDescription parent, String[] init) {
    super(parent, init);

    try {
      //theirLogger.info(itsParent.getFullName() + ": RunCmd called. itsNumPoints=" + itsNumPoints);
      //theirLogger.info(itsParent.getFullName() + ": init.length=" + init.length);
      //theirLogger.info(itsParent.getFullName() + ": init[0]=" + init[0]);
      //theirLogger.info(itsParent.getFullName() + ": init[1]=" + init[1]);
      
      // precondition
      if (init == null || init.length < 1) {
        theirLogger.error(itsParent.getFullName() + ": Requires a command to run!");
      } else {
  
        itsCommand = init[itsNumPoints + 1];
        //theirLogger.info(itsParent.getFullName() + ": itsCommand=" + itsCommand);      
        if ( init.length > itsNumPoints + 2 ) {
          itsParams = init[itsNumPoints + 2];
          //theirLogger.info(itsParent.getFullName() + ": itsParams=" + itsParams);              
        }        
      }
    
    } catch ( Exception err ) {
      theirLogger.error(itsParent.getFullName() + ": " + err);
    }
  }

  protected String doCalculations() {
    //theirLogger.info(itsParent.getFullName() + ": RunCmd doCalculations called. Attempting to execute '" + itsCommand + " " + itsParams + "'");
    String stdin = "";
    String stderr = "";
    String temp;
    String params = "";
    
    try {
      if ( itsParams != null && itsParams != "" ) {      
        params = itsParams;
        for (int i = 0; i < itsNumPoints; i++) {
          // Update the value for this variable
          String thisvar = "" + ((char) (('a') + i));
          Object thisval = itsValues[i].getData();
          String replace = "\\\u0024" + thisvar;
          
          //theirLogger.info(itsParent.getFullName() + ": replaceAll - thisvar=" + thisvar + " thisval=" + thisval + " replace=" + replace);                        
          params = params.replaceAll(replace, "" + thisval);
          itsValues[i] = null;    
        }
      }
      //theirLogger.info(itsParent.getFullName() + ": " + itsCommand + " " + params);
      Process p = Runtime.getRuntime().exec(itsCommand + " " + params);
      BufferedReader bri = new BufferedReader (new InputStreamReader(p.getInputStream()));
      BufferedReader bre = new BufferedReader (new InputStreamReader(p.getErrorStream()));
      while ((temp = bri.readLine()) != null) {
        stdin = stdin + temp;
      }
      bri.close();
      while ((temp = bre.readLine()) != null) {
        stderr = stderr + temp;
      }
      bre.close();
      p.waitFor();
      //System.out.println("Done.");
    }
    catch (Exception err) {
      theirLogger.error(itsParent.getFullName() + ": Error '" + err + "' caught while attempting to run " + itsCommand);
    }
    // Check for parse error
    if (stderr != "") {
      theirLogger.warn(itsParent.getFullName() + ":Error " + stderr + "occurred. Command: " + itsCommand + " Params: " + itsParams);
    }
    //theirLogger.info(itsParent.getFullName() + ": RunCmd doCalculations called. '" + itsCommand + " " + params + "' returned " + stdin);
    return stdin;
  }
}
