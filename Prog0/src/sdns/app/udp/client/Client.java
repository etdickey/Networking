//Contains the Client class (see comments below)
//Created: 10/1/20
package sdns.app.udp.client;

import sdns.serialization.*;

import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.System.*;
import static sdns.app.utils.SocketUtils.*;
import static sdns.app.utils.ClientValidationUtils.*;

/**
 * SDNS UDP client that first sends all questions, records each ID/Question in ExpectedList (EL), and then
 *   processes responses as described in the Client Protocol in the specifications
 * Reference was drawn from Dr. Donahoo's UDPEchoClientTimeout.java in TCP/IP Sockets in Java version 2
 *
 * @author Ethan Dickey
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
        InetAddress serverAddress = getAddressFromArgs(args);
        int serverPort = getPortFromArgs(args);

        //Construct socket
        DatagramSocket socket = createUDPSocket(silent);

        //Record each ID/Question in Expected List (EL)
        //if the number of questions is > MAX_INT, the program won't run in the first place because the
        //   JVM won't allocate that many array spots to this program
        List<Query> el = getExpectedListFromArgs(args, silent);

        //The client should first send all questions
        sendQueriesUDP(socket, serverAddress, serverPort, el, silent);

        //Then process responses as described in the Client Protocol (in the specification)
        while(0 < el.size()) {
            //handle a socket timeout
            try {
                Response r = receiveResponseUDP(socket, serverAddress, serverPort, el, silent);

                //Check for success
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
                    sendQueriesUDP(socket, serverAddress, serverPort, el, silent);
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
