//Contains the ValidationUtils class (see comments below)
//Created: 10/29/20
package sdns.app.utils;

import sdns.serialization.Query;
import sdns.serialization.RCode;
import sdns.serialization.Response;
import sdns.serialization.ValidationException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.System.err;
import static java.lang.System.out;

/**
 * SDNS client validation tools (UDP or TCP)
 *
 * @author Ethan Dickey
 *   Reference was drawn from Dr. Donahoo's UDPEchoClientTimeout.java and TCPEchoClient.java
 *     in TCP/IP Sockets in Java version 2
 * @version 1.0
 */
public class ValidationUtils {
    //Max port available
    private static final int MAX_PORT = 65535;

    /* ************** start response validation ************** */
    /**
     * Validate a Response based on specifications.  Removes from EL if it matches with bad response code.
     * @param expectedList expected list to handle accordingly
     * @param r Response to validate
     * @return if it matches specifications
     */
    public static boolean validateResponse(List<Query> expectedList, Response r, boolean silent){
        //find the response in expectedList
        for(var q : expectedList){
            //Check if ID matches
            if(q.getID() == r.getID()){
                //Check if query matches
                if(q.getQuery().equals(r.getQuery())){
                    //handle non-zero error (rcode)
                    if(r.getRCode() != RCode.NOERROR){
                        if(!silent) err.println("ERROR: " + r.getRCode().getRCodeMessage());

                        //remove from EL (this invalidates the list iterators, MUST EXIT LOOP)
                        expectedList.remove(q);

                        return false;
                    } else {
                        return true;
                    }
                } else {
                    if(!silent) err.println("Non-matching query: " + r.toString());
                    break;
                }
            }
        }

        //handle no matching ID in EL
        if(!silent) err.println("No such ID: " + r.toString());

        return false;
    }
    /* ************** end response validation ************** */


    /* ************** start argument parsing ************** */
    /**
     * Gets the Expected List (of queries) from 3rd+ argument(s)
     * @param args args to check and parse
     * @param silent option to print error message to console and exit vs throwing error
     * @return Expected List
     * @throws IllegalArgumentException if args/no valid questions
     */
    public static List<Query> getExpectedListFromArgs(String[] args, boolean silent) throws IllegalArgumentException {
        //Validate args are right length etc.
        validateArgs(args);

        //Record each ID/Question in Expected List (EL)
        //if the number of questions is > MAX_INT, the program won't run in the first place because the
        //   JVM won't allocate that many array spots to this program
        List<Query> el = new ArrayList<>();
        for(int i=2; i<args.length; i++){
            try {
                //construct a query
                el.add(new Query(i, args[i]));
            } catch (ValidationException e) {
                if(!silent) err.println("Malformed question: " + args[i]);
            }
        }

        //Check for no valid questions
        if(el.size() == 0){
            throw new IllegalArgumentException("ERROR: No correctly formed questions, exit");
        }

        return el;
    }

    /**
     * Get the port from the 2nd arg (validates args too)
     * @param args args to check and parse
     * @return port
     * @throws IllegalArgumentException if bad port
     */
    public static int getPortFromArgs(String[] args) throws IllegalArgumentException {
        //Validate args are right length etc.
        validateArgs(args);

        //get server port and validate
        int serverPort = -1;
        try {
            serverPort = Integer.parseInt(args[1]);
            if(serverPort < 0 || serverPort > MAX_PORT){
                throw new IllegalArgumentException("ERROR: Port must be [0, 65535]");
            }
            return serverPort;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ERROR: Malformed port: \"" + args[1] + "\"");
        }
    }

    /**
     * Get the port from the 1st arg (validates args too)
     * @param args args to check and parse
     * @return address
     * @throws IllegalArgumentException if bad address
     */
    public static InetAddress getAddressFromArgs(String[] args) throws IllegalArgumentException {
        //Validate args are right length etc.
        validateArgs(args);

        //get server IP/name and validate
        try {
            return InetAddress.getByName(args[0]);
        } catch (UnknownHostException e) {
            //"if the IP address of the host could not be determined."
            throw new IllegalArgumentException("ERROR: Malformed server identity: \"" + args[0] + "\"");
        }
    }

    /**
     * Validates that at least 3 args are present
     * @param args args to check
     * @throws IllegalArgumentException if less than 3 args are present
     */
    public static void validateArgs(String[] args) throws IllegalArgumentException {
        final String usageError = "Usage: <IPv4/IPv6 server IP/name> <port> <1 or more query strings>";
        //The client command-line parameters are the server IP/name, server port, and one or more query strings
        if(args.length < 3){
            throw new IllegalArgumentException(usageError);
        }
    }
    /* ************** end argument parsing ************** */
}
