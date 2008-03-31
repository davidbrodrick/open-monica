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

package atnf.atoms.util;

/**
 * An interface class which has the methods required for an object    
 * which has an number of names (in an hierachical notation eg.blah.bleat )   
 * and also can return its short name and description   
 * @author
 *  A Danson
 *
 * @version $Id:$
 */
public interface NamedObject
{

    /** gets the long name of the object */
    public String getLongName();

    /** gets the total number of names this object has */
    public int getNumNames();

    /**gets the name at the index specified. */
    public String getName(int i);

    /** gets the short name associated with this object */
    public String getName();

    /** gets the short string description of the object */
    public String toString();
}
