package atnf.atoms.mon.translation;

import java.lang.reflect.Array;

import org.nfunk.jep.JEP;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.util.Angle;

/**
 * Translation class using a mathematical expression.
 * 
 * <P>
 * The "init" argument must be an expression, where x is the current monitor
 * point value and the result of evaluating the expression produces the
 * translated point value.
 * 
 * <P>
 * Standard functions like log(), sqrt(), etc are understood. Check out
 * <code>http://www.singularsys.com/jep/doc/html/index.html</code> for
 * detailed documentation on JEP, the Java Mathematical Expression Parser.
 * 
 * @author Le Cuong Nguyen
 * @author David Brodrick
 * @version $Id: TranslationEQ.java,v 1.3 2005/08/15 01:55:17 bro764 Exp $
 */
public class TranslationEQ extends Translation
{
  /** Used for parsing and evaluating the expression. */
  JEP itsParser = new JEP();

  protected static String itsArgs[] = new String[] { "Translation using equation", "EQ", "Equation, use X as independent variable",
      "java.lang.String" };

  public TranslationEQ(PointDescription parent, String[] init)
  {
    super(parent, init);

    // precondition
    if (init == null || init.length < 1) {
      System.err.println("TranslationEQ for " + itsParent.getFullName() + ": EXPECT EQUATION ARGUMENT!");
    } else {
      itsParser.setImplicitMul(false);
      itsParser.setAllowUndeclared(true);
      itsParser.addStandardConstants();
      itsParser.addStandardFunctions();
      itsParser.parseExpression(init[0]);
    }
  }

  public PointData translate(PointData data)
  {
    if (data == null) {
      return null;
    }

    Object val = data.getData();
    if (data.getData() == null) {
      // Return a null result
      return new PointData(itsParent.getFullName());
    }

    if (val instanceof Boolean) {
      if (((Boolean) val).booleanValue()) {
        itsParser.addVariable("x", 1.0);
      } else {
        itsParser.addVariable("x", 0.0);
      }
    } else if (val instanceof Angle) {
      itsParser.addVariable("x", ((Angle) val).getValue());
    } else {
      itsParser.addVariable("x", val);
    }

    // Parse the expression using new value
    Object res = itsParser.getValueAsObject();

    // Check for parse error
    if (itsParser.hasError()) {
      System.err.println("TranslationEQ(" + itsParent.getFullName() + ": " + itsParser.getErrorInfo());
    }

    return new PointData(itsParent.getFullName(), data.getTimestamp(), res);
  }

  public static String[] getArgs()
  {
    return itsArgs;
  }
}
