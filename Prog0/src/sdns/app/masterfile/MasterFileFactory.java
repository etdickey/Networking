//Contains the MasterFileFactory class (see comments below)
//Created: 10/11/20
package sdns.app.masterfile;

import sdns.app.masterfile.MasterFile;
import sdns.app.masterfile.MasterFileClientToGoogle;

/**
 * Factory for a master file-implementing class
 *
 * @author Ethan Dickey
 *  Credit: Dr. Donahoo of Baylor University for specifications
 * @version 1.0
 */
public class MasterFileFactory {
    /**
     * Factory method for generating a master file
     * @return a MasterFile
     * @throws Exception if anything bad happens
     */
    public static MasterFile makeMasterFile() throws Exception { return new MasterFileClientToGoogle(); }
}
