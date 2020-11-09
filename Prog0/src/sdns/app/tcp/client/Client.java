//Contains the TCP SDNS Client class (see comments below)
//Created: 10/25/20
package sdns.app.tcp.client;

import sdns.serialization.Query;
import sdns.serialization.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.System.err;
import static java.lang.System.out;
import static sdns.app.utils.SocketUtils.*;
import static sdns.app.utils.ClientValidationUtils.*;

/**
 * Represents a TCP SDNS Client
 * @author Ethan Dickey
 *   Credit: Dr. Donahoo of Baylor University for API
 * @version 1.0
 */
public class Client {
    //Bool to determine if the socket has timed out previously (protocol depends on 1st vs other times)
    private static boolean hasTimedOut = false;

    /**
     * Alias for runClient without recording responses
     * @param args ip, port, and query strings
     */
    public static void runClient(String[] args){
        runClient(args, false, new ArrayList<>());
    }

    /**
     * Queries all questions in args
     * @param args ip, port, and query strings
     * @param silent whether or not to record the responses
     * @param responsesContainer the container to stuff the responses into if recordResponses is true
     */
    public static void runClient(String[] args, boolean silent, List<Response> responsesContainer) throws IllegalArgumentException {
        //Get args/validate
        InetAddress serverAddress = getAddressFromArgs(args);
        int serverPort = getPortFromArgs(args);

        //construct socket and input/output streams
        Socket socket = createTCPSocket(serverAddress, serverPort, silent);
        InputStream in = null;
        OutputStream sout = null;
        try {
            in = socket.getInputStream();
            sout = socket.getOutputStream();
        } catch (IOException e) {
            //"if the socket could not be opened, or the socket could not bind to the specified local port"
            if(!silent){
                err.println("ERROR: I/O Error while creating the socket or output stream: " + e.getMessage());
                System.exit(1);
            } else {
                throw new RuntimeException("ERROR: I/O Error while creating the socket or output stream: " + e.getMessage(), e);
            }
        }

        //Record each ID/Question in Expected List (EL)
        //if the number of questions is > MAX_INT, the program won't run in the first place because the
        //   JVM won't allocate that many array spots to this program
        List<Query> el = getExpectedListFromArgs(args, silent);

        //The client should first send all questions
        sendQueriesTCP(sout, el, silent);

        //Then process responses as described in the Client Protocol (in the specification)
        while(0 < el.size()) {
            //Don't need to handle retransmission protocol, according to the specs,
            // "As TCP is reliable, you do not need to implement an additional retransmission mechanism
            //  for the TCP SDNS client."
            //However, if I took out timeouts, then we wouldn't be able to cover the case where the server never
            //  responds, even if it got the message (which is the only thing that TCP guarantees).
            //AKA, the timeout covers more than just TCP reliability, it covers the protocol.

            //handle a socket timeout (retry requests)
            try {
                Response r = receiveResponseTCP(in, el, silent);

                if(r != null){
                    //handle success
                    if(!silent) {
                        out.println(r.toString());
                    } else {
                        responsesContainer.add(r);
                    }
                    el = el.stream().filter(q -> q.getID() != r.getID()).collect(Collectors.toList());
                }
            } catch (SocketTimeoutException e) {
                if(hasTimedOut){
                    handleSecondTimeout(el, silent);
                } else {//otherwise retransmit all queries in EL
                    hasTimedOut = true;
                    sendQueriesTCP(sout, el, silent);
                }
            }
        }
    }

    /**
     * Main runner
     * @param args args
     */
    public static void main(String[] args){
        runClient(args);
    }
}
