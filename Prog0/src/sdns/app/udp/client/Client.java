//Contains the Client class (see comments below)
//Created: 10/1/20
package sdns.app.udp.client;

import sdns.serialization.*;

import java.io.EOFException;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.System.err;

/**
 * SDNS UDP client that first sends all questions, records each ID/Question in ExpectedList (EL), and then
 *   processes responses as described in the Client Protocol in the specifications
 * Reference was drawn from Dr. Donahoo's UDPEchoClientTimeout.java in TCP/IP Sockets in Java version 2
 */
public class Client {
    //Max socket timeout is 3 seconds
    private static final int MAX_TIMEOUT_MS = 3000;
    //Bool to determine if the socket has timed out previously (protocol depends on 1st vs other times)
    private static boolean hasTimedOut = false;
    //The maximum allowed number of bytes in a DNS through UDP response (12 are taken by the header)
    private static final int MAX_DNS_UDP_BYTES = 512;//*256;

    /**
     * Sends all of the queries in @param expectedList to the OutputStream sout (normally a socket) in serialized form.
     *
     *  NOTE: This intentionally does not handle the IOException for expandability purposes
     * @param sout datagram socket to retransmit to if needed
     * @param servAddr server address
     * @param servPort server port
     * @param expectedList expected list to send
     * @throws IOException if IO error
     */
    private static void sendQueries(DatagramSocket sout, InetAddress servAddr, int servPort, List<Query> expectedList) throws IOException {
        //Send all queries
        for(var query : expectedList){
            byte[] toSend = query.encode();
            DatagramPacket pack = new DatagramPacket(toSend, toSend.length, servAddr, servPort);
            sout.send(pack);
        }
    }

    /**
     * Handles SocketTimeoutException according to the specification.
     * @param sout datagram socket to retransmit to if needed
     * @param servAddr server address
     * @param servPort server port
     * @param expectedList expected list to handle accordingly
     */
    private static void handleTimeout(DatagramSocket sout, InetAddress servAddr, int servPort, List<Query> expectedList){
        //If retransmitted previously, print "No response: <question>" for each Question in the EL and terminate
        //  otherwise retransmit all queries in EL
        if(hasTimedOut){
            for(Query q : expectedList){
                err.println("No response: " + q.getQuery());
            }
            System.exit(1);
        } else {
            hasTimedOut = true;
            try {
                sendQueries(sout, servAddr, servPort, expectedList);
            } catch (IOException e) {
                err.println("ERROR: I/O error while writing to socket: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    /**
     * Validate a Response based on specifications.  Removes from EL if it matches with bad response code.
     * @param expectedList expected list to handle accordingly
     * @param r Response to validate
     * @return if it matches specifications
     */
    private static boolean validateResponse(List<Query> expectedList, Response r){
        //find the response in expectedList
        for(var q : expectedList){
            //Check if ID matches
            if(q.getID() == r.getID()){
                //Check if query matches
                if(q.getQuery().equals(r.getQuery())){
                    //handle non-zero error (rcode)
                    if(r.getResponseCode() != RCode.NOERROR){
                        err.println("ERROR: " + r.getResponseCode().getRCodeMessage());

                        //remove from EL (this invalidates the list iterators, MUST EXIT LOOP)
                        expectedList.remove(q);

                        return false;
                    } else {
                        return true;
                    }
                } else {
                    err.println("Non-matching query: " + r.toString());
                    break;
                }
            }
        }

        //handle no matching ID in EL
        err.println("No such ID: " + r.toString());

        return false;
    }

    /**
     * Receives a response, handling all errors in the specification
     * @param sout datagram socket to retransmit to if needed
     * @param servAddr server address
     * @param servPort server port
     * @param expectedList expected list to handle accordingly
     * @return the Response received, or null
     */
    private static Response receiveResponse(DatagramSocket sout, InetAddress servAddr, int servPort, List<Query> expectedList){
        //Construct a datagram packet to store the data
        DatagramPacket pack = new DatagramPacket(new byte[MAX_DNS_UDP_BYTES], MAX_DNS_UDP_BYTES);
        try {
            sout.receive(pack);
        } catch (SocketTimeoutException e) {
            handleTimeout(sout, servAddr, servPort, expectedList);
            return null;
        } catch (IOException e) {
            err.println("ERROR: I/O error while writing to socket: " + e.getMessage());
            System.exit(1);
        }

        if (!pack.getAddress().equals(servAddr)) {// Check source
            err.println("Received packet from an unknown source");
            System.exit(1);
        }

        //have a packet!
        Message m;
        try {
            m = Message.decode(Arrays.copyOfRange(pack.getData(), 0, pack.getLength()));

            //handle bad type
            Response r = null;
            if(m instanceof Response && validateResponse(expectedList, (Response)m)){
                r = (Response)m;
            } else {
                err.println("Unexpected query: " + m.toString());
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
                err.println("Packet too long");
                err.println("Response: " + Arrays.toString(pack.getData()));
            } else if(e.getCause() instanceof EOFException || e.getBadToken().toLowerCase().contains("not long")){
                err.println("Packet too short");
            } else {
                err.println("ERROR: " + e.getMessage());
            }

            return null;
        }
    }

    /**
     * Main runner
     * @param args args
     */
    public static void main(String[] args){
        final String usageError = "Usage: <IPv4/IPv6 server IP/name> <port> <1 or more query strings>";
        //The client command-line parameters are the server IP/name, server port, and one or more query strings
        if(args.length < 3){
            throw new IllegalArgumentException(usageError);
        }

        //get server IP/name and validate
        InetAddress serverAddress = null;
        try {
            serverAddress = InetAddress.getByName(args[0]);
        } catch (UnknownHostException e) {
            //"if the IP address of the host could not be determined."
            throw new IllegalArgumentException("ERROR: Malformed server identity: \"" + args[0] + "\"");
        }

        //get server port and validate
        int serverPort = -1;
        try {
            serverPort = Integer.parseInt(args[1]);
            if(serverPort < 0 || serverPort > 65535){
                throw new IllegalArgumentException("ERROR: Port must be [0, 65535]");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ERROR: Malformed port: \"" + args[1] + "\"");
        }

        //Construct socket
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(MAX_TIMEOUT_MS);//set timeout
        } catch (SocketException e) {
            //"if the socket could not be opened, or the socket could not bind to the specified local port"
            err.println("ERROR: I/O Error while creating the socket or output stream: " + e.getMessage());
            System.exit(1);
        }

        //Record each ID/Question in Expected List (EL)
        //if the number of questions is > MAX_INT, the program won't run in the first place because the
        //   JVM won't allocate that many array spots to this program
        List<Query> el = new ArrayList<>();
        for(int i=2; i<args.length; i++){
            try {
                //construct a query
                el.add(new Query(i, args[i]));
            } catch (ValidationException e) {
                err.println("Malformed question: " + args[i]);
            }
        }

        //Check for no valid questions
        if(el.size() == 0){
            throw new IllegalArgumentException("ERROR: No correctly formed questions, exit");
        }

        //The client should first send all questions
        try {
            sendQueries(socket, serverAddress, serverPort, el);
        } catch (IOException e) {
            err.println("ERROR: I/O error while writing to socket: " + e.getMessage());
            System.exit(1);
        }

        //Then process responses as described in the Client Protocol (in the specification)
        while(0 < el.size()){
            Response r = receiveResponse(socket, serverAddress, serverPort, el);
            if(r != null){
                //handle success
                err.println(r.toString());
                el = el.stream().filter(q -> q.getID() != r.getID()).collect(Collectors.toList());
            }
        }
    }
}
