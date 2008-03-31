/**
 * Class: NoHeaderOutputStream
 * Description: A simple extension of the ObjectOutputStream
 * that will not write a header to file
 * @author Le Cuong Nguyen
 **/
package atnf.atoms.mon.util;

import java.io.*;

public class NoHeaderOutputStream extends ObjectOutputStream
{
   public NoHeaderOutputStream(OutputStream os) throws IOException
   {
      super(os);
   }
   
   public void writeStreamHeader() throws IOException
   {
   }
}
