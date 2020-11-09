//Contains the (TCP) Server class (see comments below)
//Created: 11/7/20
package sdns.app.tcp.server;


import sdns.app.masterfile.MasterFile;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static sdns.app.utils.LoggingUtils.*;
import static sdns.app.utils.ServerValidationUtils.*;

/**
 * The server will take the command-line parameter of the server port. The server should repeatedly receive
 *   a Query and send a Response with a thread pool according to the server protocol.
 *
 * @author Ethan Dickey
 *   Credit: Dr. Donahoo of Baylor University for comments and API
 * @version 1.0
 */
public class Server {
    private static final int TIMEOUT_MS = 20000;

    /**
     * Main
     *
     * @param args arguments
     */
    public static void main(String[] args){
        final String usageError = "Usage: <server port> <number of threads>";

        //Set up logger to specifications
        setupLogger();

        ///////////////////////////////////////
        //Validate program arguments///////////
        //"The server should take the command-line parameters of the server port and number of threads in pool."
        if(args.length != 2){
            logErrorAndExit("Unable to start: Bad usage: " + usageError);
        }

        //get server port and validate
        int serverPort = getAndHandlePort(args[0]);
        int nThreads = -1;
        try{
            nThreads = Integer.parseInt(args[1]);
        } catch(NumberFormatException e){
            logErrorAndExit("Unable to start: Malformed number of threads: \"" + args[1] + "\"");
        }


        ///////////////////////////////////////
        //Set up thread pool///////////////////
        ExecutorService threadPool = Executors.newFixedThreadPool(nThreads);


        ///////////////////////////////////////
        //Start up server!/////////////////////
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(serverPort);
            logTCPServerStart(serverSocket);
        } catch (IOException e) {
            logErrorAndExit("Unable to start: bad port (or socket error): " + e.getMessage());
        }

        ///////////////////////////////////////
        //Do stuff with server!////////////////
        while(true){
            //make sure server never dies.  ever..
            try{
                Socket client = serverSocket.accept();
                client.setSoTimeout(TIMEOUT_MS);

                //distribute to thread pool
                threadPool.execute(new ClientHandler(client));
            } catch (IOException e) {
                logCommunicationError(e.getMessage());
            } catch(Exception e){//just in case.....
                logSevereError("WARN WARN WARN CRITICAL INTERNAL ERROR: " + e.getMessage());
            }
        }
    }
}
