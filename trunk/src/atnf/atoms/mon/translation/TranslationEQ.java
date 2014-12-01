package atnf.atoms.mon.translation;

import org.nfunk.jep.JEP;

import org.apache.log4j.Logger;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.util.Angle;

/**
 * Translation class using a mathematical expression.
 * 
 * <P>
 * The "init" argument must be an expression, where x is the current monitor point value and the result of evaluating the expression
 * produces the translated point value.
 *
 * An optional default value may be specified after the expression, which will be the value generated if the input if null.
 * 
 * <P>
 * Standard functions like log(), sqrt(), etc are understood. Check out
 * <code>http://www.singularsys.com/jep/doc/html/index.html</code> for detailed documentation on JEP, the Java Mathematical
 * Expression Parser.
 * 
 * <P>
 * One difference from the JEP documentation is that strings in the expression must be quoted in single quotes not double quotes.
 * 
 * @author Le Cuong Nguyen
 * @author David Brodrick
 */
public class TranslationEQ extends Translation {
  /** Used for parsing and evaluating the expression. */
  protected JEP itsParser = new JEP();

  /** The expression in String form. */
  protected String itsExpression;

  /** Default value, if specified. */
  protected Double itsDefaultValue;

  /** Logger. */
  protected static Logger theirLogger = Logger.getLogger(TranslationEQ.class.getName());

  public TranslationEQ(PointDescription parent, String[] init) {
    super(parent, init);

    // precondition
    if (init == null || init.length < 1) {
      String msg = itsParent.getFullName() + ": Require Equation Argument!";
      theirLogger.error(msg);
      throw new IllegalArgumentException(msg);
    }
    itsParser.setImplicitMul(false);
    itsParser.setAllowUndeclared(true);
    itsParser.addStandardConstants();
    itsParser.addStandardFunctions();
    itsExpression = init[0].replaceAll("'", "\"");
    itsParser.parseExpression(itsExpression);
    if (init.length>1) {
      itsDefaultValue = new Double(init[1]);
    }
  }

  public PointData translate(PointData data) {
    PointData pdres = new PointData(itsParent.getFullName());
    pdres.setTimestamp(data.getTimestamp());
    pdres.setAlarm(data.getAlarm());

    if (data == null) {
      return null;
    }

    Object val = data.getData();
    if (val == null) {
      if (itsDefaultValue!=null) {     
        pdres.setData(itsDefaultValue);
      }
      return pdres;
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
    pdres.setData(res);

    // Check for parse error
    if (itsParser.hasError()) {
      theirLogger.warn(itsParent.getFullName() + ": " + itsParser.getErrorInfo());
    }

    return pdres;
  }
}
