//Contains the SocketUtils class (see comments below)
//Created: 10/29/20
package sdns.app.utils;


import sdns.app.masterfile.MasterFile;
import sdns.serialization.*;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import static java.lang.System.err;
import static sdns.app.utils.ClientValidationUtils.validateResponse;

/**
 * SDNS client socket tools (UDP or TCP)
 *
 * @author Ethan Dickey
 *   Reference was drawn from Dr. Donahoo's UDPEchoClientTimeout.java and TCPEchoClient.java
 *     in TCP/IP Sockets in Java version 2
 * @version 1.0
 */
public class SocketUtils {
    //Max socket timeout is 3 seconds
    private static final int MAX_TIMEOUT_MS = 3000;
    //The maximum allowed number of bytes in a DNS through UDP response (12 are taken by the header)
    private static final int MAX_DNS_UDP_BYTES = 512;//*256;

    /* ************** Begin socket creation ************** */
    /**
     * Construct UDP socket (optional silent mode which throws an error vs exiting)
     * @param silent option to print error message to console and exit vs throwing error
     * @return constructed DatagramSocket socket
     */
    public static DatagramSocket createUDPSocket(boolean silent) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(MAX_TIMEOUT_MS);//set timeout
        } catch (SocketException e) {
            //"if the socket could not be opened, or the socket could not bind to the specified local port"
            if(!silent){
                err.println("ERROR: I/O Error while creating the socket or output stream: " + e.getMessage());
                System.exit(1);
            } else {
                throw new RuntimeException("ERROR: I/O Error while creating the socket or output stream: " + e.getMessage(), e);
            }
        }
        return socket;
    }

    /**
     * Construct TCP socket (optional silent mode which throws an error vs exiting)
     * @param addr server address
     * @param port server port
     * @param silent option to print error message to console and exit vs throwing error
     * @return constructed Socket socket
     */
    public static Socket createTCPSocket(InetAddress addr, int port, boolean silent) {
        Socket socket = null;
        try {
            socket = new Socket(addr, port);
            socket.setSoTimeout(MAX_TIMEOUT_MS);//set timeout
        } catch (IOException e) {//SocketException is a subclass
            //"if the socket could not be opened, or the socket could not bind to the specified local port"
            if(!silent){
                err.println("ERROR: I/O Error while creating the socket or output stream: " + e.getMessage());
                System.exit(1);
            } else {
                throw new RuntimeException("ERROR: I/O Error while creating the socket or output stream: " + e.getMessage(), e);
            }
        }
        return socket;
    }
    /* ************** End socket creation ************** */


    /* ************** Begin send queries ************** */
    /**
     * Sends all of the queries in @param expectedList to the UDP DatagramSocket sout in serialized form.
     * @param sout datagram socket to transmit to
     * @param servAddr server address
     * @param servPort server port
     * @param expectedList list of queries to send
     * @param silent option to print error message to console and exit vs throwing error
     */
    public static void sendQueriesUDP(DatagramSocket sout, InetAddress servAddr, int servPort,
                                      List<Query> expectedList, boolean silent) {
        try {
            //Send all queries
            for(var query : expectedList){
                byte[] toSend = query.encode();
                DatagramPacket pack = new DatagramPacket(toSend, toSend.length, servAddr, servPort);
                sout.send(pack);
            }
        } catch (IOException e) {
            if(!silent){
                err.println("ERROR: I/O error while writing to socket: " + e.getMessage());
                System.exit(1);
            } else {
                throw new RuntimeException("ERROR: I/O error while writing to socket: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Sends all of the queries in @param expectedList to the OutputStream sout (normally a socket) in serialized form.
     * @param sout outputstream/socket to transmit to
     * @param expectedList list of queries to send
     * @param silent option to print error message to console and exit vs throwing error
     */
    public static void sendQueriesTCP(OutputStream sout, List<Query> expectedList, boolean silent) {
        try {
            //Send all queries
            for(var query : expectedList){
                byte[] toSend = query.encode();
                sout.write(Framer.frameMsg(toSend));
            }
        } catch (IOException | ValidationException e) {
            if(!silent){
                err.println("ERROR: I/O error while writing to socket: " + e.getMessage());
                System.exit(1);
            } else {
                throw new RuntimeException("ERROR: I/O error while writing to socket: " + e.getMessage(), e);
            }
        }
    }
    /* ************** End send queries ************** */


    /* ************** Begin receive queries ************** */
    /**
     * Handles second SocketTimeoutException according to the specification.
     * @param expectedList expected list to handle accordingly
     * @param silent option to print error message to console and exit vs throwing error
     */
    public static void handleSecondTimeout(List<Query> expectedList, boolean silent){
        //If retransmitted previously, print "No response: <question>" for each Question in the EL and terminate
        if(!silent) {
            for (Query q : expectedList) {
                err.println("No response: " + q.getQuery());
            }
            System.exit(1);
        } else {
            throw new RuntimeException("No responses for queries");
        }
    }

    /**
     * Processes a byte[] message and parses it into response RRs and validates each response
     * @param message byte[] to process
     * @param expectedList expected list to handle accordingly
     * @param silent option to print error message to console and exit vs throwing error
     * @return Response object with all RRs in it
     */
    private static Response processMessage(byte[] message, List<Query> expectedList, boolean silent){
        //have a packet!
        Message m;
        try {
            m = Message.decode(message);

            //handle bad type
            Response r = null;
            if(m instanceof Response && validateResponse(expectedList, (Response)m, silent)){
                r = (Response)m;
            } else {
                if(!silent) err.println("Unexpected query: " + m.toString());
            }

            return r;
        } catch (ValidationException e) {
            //Deserialization failure
            //This part of the specification smells.  The ValidationException makes no promises as to how it will
            //  indicate too short/long or error in header content.  Knowing this is equivalent to opening the black
            //  box which is Message.decode().  This part of the specification should just print the error and continue,
            //  not have to parse through the error.
            //As such, it is almost impossible to correctly identify every time .decode() does any of the specified
            //  behaviors and so it is not attempted to catch all of them, just most of them.
            if(e.getBadToken().toLowerCase().contains("too many")){
                if(!silent) err.println("Packet too long");
            } else if(e.getCause() instanceof EOFException || e.getBadToken().toLowerCase().contains("not long")){
                if(!silent) err.println("Packet too short");
            } else {
                if(!silent) err.println("ERROR: " + e.getMessage());
            }

            return null;
        }
    }

    /**
     * Receives a UDP response, handling all errors in the specification
     * @param sout datagram socket to retransmit to if needed
     * @param servAddr server address
     * @param servPort server port
     * @param expectedList expected list to handle accordingly
     * @param silent option to print error message to console and exit vs throwing error
     * @return the Response received, or null
     * @throws SocketTimeoutException if the socket times out (up to the caller to handle according to specs)
     */
    public static Response receiveResponseUDP(DatagramSocket sout, InetAddress servAddr, int servPort,
                                              List<Query> expectedList, boolean silent)
            throws SocketTimeoutException {
        //Construct a datagram packet to store the data
        DatagramPacket pack = new DatagramPacket(new byte[MAX_DNS_UDP_BYTES], MAX_DNS_UDP_BYTES);
        try {
            sout.receive(pack);
        } catch(SocketTimeoutException e){//ensure it gets thrown (because it's of type IOException)
            throw e;
        } catch (IOException e) {
            if(!silent){
                err.println("ERROR: I/O error while writing to socket: " + e.getMessage());
                System.exit(1);
            } else {
                throw new RuntimeException("ERROR: I/O error while writing to socket: " + e.getMessage(), e);
            }
        }

        if (!pack.getAddress().equals(servAddr)) {// Check source
            if(!silent){
                err.println("Received packet from an unknown source");
                System.exit(1);
            } else {
                throw new RuntimeException("Received packet from an unknown source");
            }
        }

        return processMessage(Arrays.copyOfRange(pack.getData(), 0, pack.getLength()), expectedList, silent);
    }

    /**
     * Receives a TCP response, handling all errors in the specification
     * @param in input stream to read from
     * @param expectedList expected list to handle accordingly
     * @param silent option to print error message to console and exit vs throwing error
     * @return the Response received, or null
     * @throws SocketTimeoutException if the socket times out (up to the caller to handle according to specs)
     */
    public static Response receiveResponseTCP(InputStream in, List<Query> expectedList, boolean silent)
            throws SocketTimeoutException {
        byte[] data = null;
        try {
            //Get all bytes from socket connection
            data = Framer.nextMsg(in);//minimum length to decode the Framed length field = 2
        } catch (SocketTimeoutException e){
            throw e;//ensure it gets thrown (because it's of type IOException)
        } catch (IOException e) {
            if(!silent){
                err.println("ERROR: I/O error while writing to socket: " + e.getMessage());
                System.exit(1);
            } else {
                throw new RuntimeException("ERROR: I/O error while writing to socket: " + e.getMessage(), e);
            }
        }

        return processMessage(data, expectedList, silent);
    }
    /* ************** End receive queries ************** */
}
