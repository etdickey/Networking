//Contains the Server class (see comments below)
//Created: 10/11/20
package sdns.app.udp.server;

import sdns.app.utils.ServerProtocol;
import sdns.serialization.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

import static sdns.app.utils.LoggingUtils.*;
import static sdns.app.utils.ServerValidationUtils.*;

/**
 * The server will take the command-line parameter of the server port. The server should repeatedly receive
 *   a Query and send a Response according to the server protocol.
 *
 * @author Ethan Dickey
 *   Credit: Dr. Donahoo of Baylor University for comments and API
 * @version 1.0
 */
public class Server extends ServerProtocol {
    //Max bytes in a DNS UDP packet
    private static final int MAX_PACKET_SIZE = 512;
    private DatagramSocket sout = null;
    private DatagramPacket source;

    /**
     * Constructs a server on the specified port and process responses
     * @param serverPort port to host on
     */
    protected Server(int serverPort) {
        super();

        ///////////////////////////////////////
        //Start up server!/////////////////////
        try {
            sout = new DatagramSocket(serverPort);
            logUDPServerStart(sout);
        } catch (SocketException e) {
            logErrorAndExit("Unable to start: bad port (or socket error): " + e.getMessage());
        }

        //Create a packet for processing
        source = new DatagramPacket(new byte[MAX_PACKET_SIZE], MAX_PACKET_SIZE);
        while(true){
            //Make sure to catch any IO errors
            try {
                sout.receive(source);//blocking

                this.processResponse(Arrays.copyOfRange(source.getData(), 0, source.getLength()));
            } catch (IOException e) {
                logCommunicationError(e.getMessage());
            } catch (Exception e){//just in case.....
                logSevereError("WARN WARN WARN CRITICAL INTERNAL ERROR: " + e.getMessage());
            }
        }
    }

    /**
     * Sends a Response to servAddr:servPort through sout
     * @param r Response to send
     * @throws IOException if sending error
     */
    @Override
    protected boolean sendResponse(Response r) throws IOException {
        byte[] encoded = r.encode();
        DatagramPacket toSend = new DatagramPacket(encoded, encoded.length, source.getAddress(), source.getPort());
        sout.send(toSend);
        return true;
    }

    /**
     * Logs the current client with the given message
     * @param message message to log
     */
    @Override
    protected void logNewClient(String message){
        logNewUDPClient(source, message);
    }

    /**
     * Main
     *   dig @localhost -p 1999 www.google.com ANY +noedns +notcp +noadflag
     * @param args arguments
     */
    public static void main(String[] args) {
        final String usageError = "Usage: <server port>";

        //Set up logger to specifications
        setupLogger();


        ///////////////////////////////////////
        //Validate program arguments///////////
        //The client command-line parameter is the server port
        if (args.length != 1) {
            logErrorAndExit("Unable to start: Bad usage: " + usageError);
        }

        //get server port and validate
        int serverPort = getAndHandlePort(args[0]);

        //Start up server
        new Server(serverPort);
    }
}
