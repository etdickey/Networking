//Contains the ServerValidationUtils class (see comments below)
//Created: 11/7/20
package sdns.app.utils;

import sdns.app.masterfile.MasterFile;
import sdns.app.masterfile.MasterFileFactory;
//import sdns.app.masterfile.MasterFileFactoryMock;

import static sdns.app.utils.LoggingUtils.logErrorAndExit;
import static sdns.app.utils.ValidationUtils.getPort;

/**
 * SDNS server validation tools (UDP or TCP)
 *
 * @author Ethan Dickey
 *   Reference was drawn from Dr. Donahoo's UDPEchoClientTimeout.java and TCPEchoClient.java
 *     in TCP/IP Sockets in Java version 2
 * @version 1.0
 */
public class ServerValidationUtils {
    /**
     * Get a port from the argument and handle errors according to server protocol
     * @param port port to parse and validate
     * @return port
     */
    public static int getAndHandlePort(String port) {
        int p = -1;
        try{
            p = getPort(port);
        } catch(IllegalArgumentException e){
            logErrorAndExit("Unable to start: " + e.getMessage());
        }
        return p;
    }

    /**
     * Get the master file and validate it
     * @return master file instance
     */
    public static MasterFile getAndCheckMasterFile(){
        MasterFile mf = null;
        try {
//            mf = MasterFileFactoryMock.makeMasterFile();
            mf = MasterFileFactory.makeMasterFile();
        } catch (Exception e) {
            logErrorAndExit("Unable to start: Error in creating the master file: " + e.getMessage());
        }

        //Verify master file
        if(mf == null){
            logErrorAndExit("Unable to start: Null master file");
        }

        return mf;
    }
}
