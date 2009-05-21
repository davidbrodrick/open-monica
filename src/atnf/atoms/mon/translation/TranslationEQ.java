package atnf.atoms.mon.translation;

import java.lang.reflect.Array;

import org.nfunk.jep.JEP;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.util.Angle;

/**
 * Translation class using a mathematical expression.
 *
 * <P>The "init" argument must be an expression, where x is the current
 * monitor point value and the result of evaluating the expression produces
 * the translated point value.
 *
 * <P>Standard functions like log(), sqrt(), etc are understood.
 * Check out <code>http://www.singularsys.com/jep/doc/html/index.html</code>
 * for detailed documentation on JEP, the Java Mathematical Expression Parser.
 *
 * @author Le Cuong Nguyen
 * @author David Brodrick
 * @version $Id: TranslationEQ.java,v 1.3 2005/08/15 01:55:17 bro764 Exp $
 */
public class
TranslationEQ
extends Translation
{
  /** Used for parsing and evaluating the expression. */
  JEP itsParser = new JEP();

  protected static String itsArgs[] = new String[]{
    "Translation using equation", "EQ",
    "Equation, use X as independent variable", "java.lang.String"};

   public TranslationEQ(PointDescription parent, String[] init)
   {
     super(parent, init);

     //precondition
     if (init==null || init.length<1) {
       System.err.println("TranslationEQ for " + itsParent.getLongName() +
			  ": EXPECT EQUATION ARGUMENT!");
     } else {
       itsParser.setImplicitMul(true);
       itsParser.setAllowUndeclared(true);
       itsParser.addStandardConstants();
       itsParser.addStandardFunctions();
       itsParser.parseExpression(init[0]);
     }
   }


   public
   PointData
   translate(PointData data)
   {
     if (data==null) {
      return null;
    }
     if (data.getData()==null) {
      //Return a null result
      return new PointData(itsParent.getFullName());
     }

     Object dataCopy = data.getData();
      if (!(dataCopy instanceof Number) && !(dataCopy instanceof Angle)) {
	if (dataCopy instanceof Object[]) {
    return new PointData(itsParent.getFullName(), 
    						       data.getTimestamp(), translateArray((Object[])dataCopy));
  } else {
    return null;
  }
      }
      // Really atrocious solution, but I believe its fast
      if (dataCopy instanceof Angle) {
          itsParser.addVariable("x",((Angle)data.getData()).getValue());
      } else {
	itsParser.addVariable("x",((Number)data.getData()).doubleValue());
      }
      double resNum = itsParser.getValue();

      return new PointData(itsParent.getFullName(), data.getTimestamp(), new Double(resNum));
   }


   /** Translate an array. */
   private
   Object
   translateArray(Object[] data)
   {
      Object[] resData = (Object[])Array.newInstance(data[0].getClass(), data.length);

      for (int i = 0; i < data.length; i++) {
         itsParser.addVariable("x", ((Number)data[i]).doubleValue());
	 double res = itsParser.getValue();
	 if (data[0] instanceof Byte) {
    resData[i] = new Byte((byte)res);
  } else if (data[0] instanceof Short) {
    resData[i] = new Short((short)res);
  } else if (data[0] instanceof Long) {
    resData[i] = new Long((long)res);
  } else if (data[0] instanceof Integer) {
    resData[i] = new Integer((int)res);
  } else if (data[0] instanceof Float) {
    resData[i] = new Float((float)res);
  } else {
    resData[i] = new Double(res);
  }
      }
      return resData;
   }

  public static String[] getArgs()
  {
     return itsArgs;
  }
}
