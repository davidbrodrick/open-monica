// Copyright (C)1997  CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2 
// of the License, or (at your option) any later version. 
// 
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of 
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
// GNU Library General Public License for more details. 
// 
// A copy of the GNU Library General Public License is available at: 
//     http://wwwatoms.atnf.csiro.au/doc/gnu/GLGPL.htm 
// or, write to the Free Software Foundation, Inc., 59 Temple Place, 
// Suite 330, Boston, MA  02111-1307  USA 

package atnf.atoms.time;

/**
 * Encapsulates dUTC (current, past and future).
 *
 * <p>The value of dUTC changes from time to time. This class encapsulates
 * those changes into an authorative class that will present dUTC for any
 * time in the past or into the near future.</p>
 *
 * <p>The current value of dUTC is obtained from the ATOMS
 * observatory property <code>dUTC</code>.</p>
 *
 * @author
 *  David G Loone
 *
 * @version $Id: DUTC.java,v 1.9 2005/12/06 03:35:56 wie017 Exp $
 */
final public
class DUTC
{

  /**
   * The UTC dates (in MJD days) and amount of the leap second
   */
   private static
   int[][] leap = {
     { 56109, 35 }, // 2012 JUL  1
     { 54832, 34 }, // 2009 JAN  1
     { 53736, 33 }, // 2006 JAN  1
     { 51179, 32 }, // 1999 JAN  1
     { 50630, 31 }, // 1997 JUL  1
     { 50083, 30 }, // 1996 JAN  1
     { 49534, 29 }, // 1994 JUL  1
     { 49169, 28 }, // 1993 JUL  1
     { 48804, 27 }, // 1992 JUL  1
     { 48257, 26 }, // 1991 JAN  1
     { 47892, 25 }, // 1990 JAN  1
     { 47161, 24 }, // 1988 JAN  1
     { 46247, 23 }, // 1985 JUL  1
     { 45516, 22 }, // 1983 JUL  1
     { 45151, 21 }, // 1982 JUL  1
     { 44786, 20 }, // 1981 JUL  1
     { 44239, 19 }, // 1980 JAN  1
     { 43874, 18 }, // 1979 JAN  1
     { 43509, 17 }, // 1978 JAN  1
     { 43144, 16 }, // 1977 JAN  1
     { 42778, 15 }, // 1976 JAN  1
     { 42413, 14 }, // 1975 JAN  1
     { 42048, 13 }, // 1974 JAN  1
     { 41683, 12 }, // 1973 JAN  1
     { 41499, 11 }, // 1972 JUL  1
     { 41317, 10 }  // 1972 JAN  1
     };
            
  /**
   * Constructor.
   *
   * This prevents this class from being instantiated.
   */
  private
  DUTC()
  {}

  /**
   * Get the dUTC for now.
   *
   * @return
   *  The current dUTC (in seconds).
   */
  public static
  int
  get()
  { 
    // To avoid recursion, we base our calculation directly on
    // the system time
    final double MJD1970 = 40587.0; // MJD of 1970/01/01
    double daysSince1970 = System.currentTimeMillis()/1000.0/86400.0;
    return get(daysSince1970 + MJD1970);
    //return Integer.parseInt(ATOMS.getProperty(ATOMS.OBS_PREFIX + "dUTC"));
  }

  /**
   * Get the dUTC for the given utc time (in MJD days)
   *
   * @return
   *  The current dUTC (in seconds).
   */
  public static
  int
  get(double utc) {
    int n = leap.length;
    for (int i=0; i<n; i++) {
      if (utc >= leap[i][0]) {
        return leap[i][1];
      }
    }
    return 0;// different system before then
  } 

  /**
   * Get the dUTC for the given BAT time (in microseconds)
   *
   * @return
   *  The current dUTC (in seconds).
   */
  public static
  int
  get(long bat) {
    double t = bat / 1000000.0; // seconds
    int n = leap.length;
    for (int i=0; i<n; i++) {
      if (t >= (leap[i][0]*86400.0 + leap[i][1])) {
        return leap[i][1];
      }
    }
    return 0;// different system before then
  } 

  /**
   * Print some dUTC values.
   */
  public static
  void
  main(
    String[] argv
  )
  {
    System.out.println("Current DUTC  = "+DUTC.get());
    System.out.println("DUTC at J2000 = "+DUTC.get(51545.0));
    System.out.println("DUTC in  2010 = "+DUTC.get(55198.0));
    System.out.println("DUTC in  1990 = "+DUTC.get(47892.0));
    System.out.println("DUTC at 4000000000000000L (BAT 1985) = "+
        DUTC.get(4000000000000000L));
    System.out.println("DUTC at 3506716800000000L (BAT 1970) = "+
        DUTC.get(3506716800000000L));
  }

}
