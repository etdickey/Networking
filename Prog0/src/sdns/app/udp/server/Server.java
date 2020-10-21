//Contains the Server class (see comments below)
//Created: 10/11/20
package sdns.app.udp.server;

import sdns.serialization.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The server will take the command-line parameter of the server port. The server should repeatedly receive
 *   a Query and send a Response according to the server protocol.
 *
 * @author Ethan Dickey
 *   Credit: Dr. Donahoo of Baylor University for comments and API
 * @version 1.0
 */
public class Server {
    //Logger!
    private static final Logger log = Logger.getLogger(Server.class.getName());
    //Max bytes in a DNS UDP packet
    private static final int MAX_PACKET_SIZE = 512;

    /* The following few functions log with specific error messages or specific information */
    /**
     * Logs the error message and exits the program
     * @param msg error message
     */
    private static void logErrorAndExit(String msg){
        log.log(Level.SEVERE, msg);
        System.exit(1);
    }

    /**
     * Logs info about the server at startup
     * @param socket server socket
     */
    private static void logServerStart(DatagramSocket socket) {
        log.info("SDNS UDP server up an running on port " + socket.getLocalPort());
    }

    /**
     * Logs a parsing error
     * @param msg error message
     */
    private static void logParsingError(String msg){
        log.log(Level.WARNING, "Unable to parse message: " + msg);
    }

    /**
     * Logs a communication error
     * @param msg error message
     */
    private static void logCommunicationError(String msg){
        log.log(Level.WARNING, "Communication problem: " + msg);
    }

    /**
     * Logs when receiving a bad message
     * @param m response object to log
     */
    private static void logBadMessageType(Message m) {
        log.log(Level.SEVERE, "Unexpected message type: " + m.toString());
    }

    /**
     * Logs a response
     * @param r Response to log
     */
    private static void logResponseSend(Response r) {
        log.log(Level.INFO, "Sending response: " + r.toString());
    }

    /**
     * Logs a received query
     * @param q Query to log
     */
    private static void logQueryReceived(Query q) {
        log.log(Level.INFO, "Received query: " + q.toString());
    }

    /**
     * Logs a new client's source IP address/port and request
     * @param packet client
     */
    private static void logNewClient(DatagramPacket packet, String payload) {
        log.log(Level.INFO, "Handling client at " + packet.getAddress().getHostAddress() +
                " on port " + packet.getPort() + "\nRequest:: " + payload);
    }

    /**
     * Configure logger
     */
    private static void setupLogger(){
        try {
            log.addHandler(new FileHandler("connections.log"));
            log.setUseParentHandlers(false);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to start: Attempt to add logger file handler failed");
        }
    }


    /**
     * Sends a Response to servAddr:servPort through sout
     * @param r Response to send
     * @param sout Datagram socket to send from
     * @param servAddr address to send to
     * @param servPort port to send to
     * @throws IOException if sending error
     */
    private static void sendResponse(Response r, DatagramSocket sout, InetAddress servAddr, int servPort) throws IOException {
        byte[] encoded = r.encode();
        DatagramPacket toSend = new DatagramPacket(encoded, encoded.length, servAddr, servPort);
        sout.send(toSend);
    }

    /**
     * Follows protocol for receiving a bad message (including queries that return nothing)
     * @param m bad message
     * @param sout socket to write a response to (see protocol)
     * @param source source to parse information from
     * @param responseCode response code to use when sending the response message
     * @throws IOException if socket error
     */
    private static void handleBadMessage(Message m, DatagramSocket sout, DatagramPacket source, RCode responseCode) throws IOException {
        handleBadMessage(m, sout, source, responseCode, "Unexpected message type: ");
    }

    /**
     * Follows protocol for receiving a send + log an error
     * @param m bad message
     * @param sout socket to write a response to (see protocol)
     * @param source source to parse information from
     * @param responseCode response code to use when sending the response message
     * @param logMessage message to log
     * @throws IOException if socket error
     */
    private static void handleBadMessage(Message m, DatagramSocket sout, DatagramPacket source, RCode responseCode, String logMessage) throws IOException {
        log.severe(logMessage + m.toString());

        //Handle according to specifications
        //send and log a response with the same ID and question, and empty answer/nameserver/additional
        Response toRespond = null;
        try {
            toRespond = new Response(m.getID(), m.getQuery(), responseCode);
            //log and send
            logResponseSend(toRespond);
            sendResponse(toRespond, sout, source.getAddress(), source.getPort());
        } catch (ValidationException e) {
            //ack!
        }
    }

    /**
     * Performs a search using the query object and follows specifications when responding
     * @param mf master file to search from
     * @param q question
     */
    private static Response doSearch(MasterFile mf, Query q, DatagramSocket sout, DatagramPacket source) throws IOException {
        List<ResourceRecord> ans = new ArrayList<>(), ns = new ArrayList<>(), adtl = new ArrayList<>();
        Response r = null;
        try {
            //Do the search
            r = new Response(q.getID(), q.getQuery(), RCode.NOERROR);
            mf.search(q.getQuery(), ans, ns, adtl);

            if(ans.size() == ns.size() && ns.size() == adtl.size() && adtl.size() == 0){
                throw new NoSuchElementException("Empty response");
            }

            //Add the results to the response
            for(ResourceRecord rr : ans){
                r.addAnswer(rr);
            }
            for(ResourceRecord rr : ns){
                r.addNameServer(rr);
            }
            for(ResourceRecord rr : adtl){
                r.addAdditional(rr);
            }

            //set the responsercode to 0
            r.setRCode(RCode.NOERROR);
        } catch (ValidationException | NullPointerException ignore) {
            //if question is invalid or anything else goes wrong while trying to resolve question
            //or if no such domain name
            //or if any parameters are null
            //send + log
            handleBadMessage(q, sout, source, RCode.SERVERFAILURE, "Problem resolving: ");

            r = null;
        } catch (NoSuchElementException e){
            //send + log
            handleBadMessage(q, sout, source, RCode.NAMEERROR, "Domain name does not exist: ");

            r = null;
        }
        return r;
    }


    /**
     * Main
     * @param args arguments
     */
    public static void main(String[] args){
        final String usageError = "Usage: <server port>";

        //Set up logger to specifications
        setupLogger();


        ///////////////////////////////////////
        //Validate program arguments///////////
        //The client command-line parameter is the server port
        if(args.length != 1){
            logErrorAndExit("Unable to start: Bad usage: " + usageError);
        }

        //get server port and validate
        int serverPort = -1;
        try {
            serverPort = Integer.parseInt(args[0]);
            if(serverPort < 0 || serverPort > 65535){
                logErrorAndExit("Unable to start: Port must be [0, 65535]");
            }
        } catch (NumberFormatException e) {
            logErrorAndExit("Unable to start: Malformed port: \"" + args[0] + "\"");
        }


        ///////////////////////////////////////
        //Setup internal tools for server use//
        //Get master file
        MasterFile mf = null;
        try {
            mf = MasterFileFactory.makeMasterFile();
        } catch (Exception e) {
            logErrorAndExit("Unable to start: Error in creating the master file: " + e.getMessage());
        }

        //Verify master file
        if(mf == null){
            logErrorAndExit("Unable to start: Null master file");
        }

        ///////////////////////////////////////
        //Start up server!/////////////////////
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(serverPort);
            logServerStart(socket);
        } catch (SocketException e) {
            logErrorAndExit("Unable to start: bad port (or socket error): " + e.getMessage());
        }

        //Create a packet for processing
        DatagramPacket packet = new DatagramPacket(new byte[MAX_PACKET_SIZE], MAX_PACKET_SIZE);
        while(true){
            //Make sure to catch any IO errors
            try {
                socket.receive(packet);//blocking

                //Parse the message
                try {
                    Message m = Message.decode(Arrays.copyOfRange(packet.getData(), 0, packet.getLength()));
                    //log new client
                    logNewClient(packet, m.toString());

                    //Check for bad message type
                    if(m instanceof Query){
                        //1. log query
                        logQueryReceived((Query)m);

                        //2. go get the response
                        Response r = doSearch(mf, (Query)m, socket, packet);

                        //Check for valid responses
                        if(r != null){
                            //Send + log response with same ID and question, RCode = 0, and masterfile's ans/ns/adtl RRs
                            logResponseSend(r);
                            sendResponse(r, socket, packet.getAddress(), packet.getPort());
                        }
                    } else {//bad message type
                        handleBadMessage(m, socket, packet, RCode.REFUSED);
                    }
                } catch (ValidationException e) {
                    logParsingError(e.getMessage());
                    logNewClient(packet, "Number of bytes received from invalid packet: " + packet.getLength());
                }
            } catch (IOException e) {
                logCommunicationError(e.getMessage());
            } catch (Exception e){//just in case.....
                log.severe("WARN WARN WARN CRITICAL INTERNAL ERROR: " + e.getMessage());
            }
        }
    }
}
