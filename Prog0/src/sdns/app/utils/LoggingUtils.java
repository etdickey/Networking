package sdns.app.utils;

import sdns.serialization.Query;
import sdns.serialization.Response;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Server logging utils (UDP or TCP)
 *
 * @author Ethan Dickey
 * @version 1.0
 */
public class LoggingUtils {
    //Logger!
    private static final Logger log = Logger.getLogger(LoggingUtils.class.getName());

    /**
     * Configure logger
     */
    public static void setupLogger(){
        try {
            FileHandler f = new FileHandler("connections.log");
            f.setFormatter(new SimpleFormatter());
            log.addHandler(f);
            log.setUseParentHandlers(false);
        } catch (IOException e) {//exit?
            log.log(Level.SEVERE, "Unable to start: Attempt to add logger file handler failed");
        }
    }

    /**
     * Logs the error message and exits the program
     * @param msg error message
     */
    public static void logErrorAndExit(String msg){
        log.log(Level.SEVERE, msg);
        System.exit(1);
    }

    /**
     * Logs info about the server at startup
     * @param socket server socket
     */
    public static void logUDPServerStart(DatagramSocket socket) {
        log.info("SDNS UDP server up an running on port " + socket.getLocalPort());
    }

    /**
     * Logs info about the server at startup
     * @param socket server socket
     */
    public static void logTCPServerStart(ServerSocket socket) {
        log.info("SDNS TCP server up an running on port " + socket.getLocalPort());
    }

    /**
     * Logs a parsing error
     * @param msg error message
     */
    public static void logParsingError(String msg){
        log.log(Level.WARNING, "Unable to parse message: " + msg);
    }

    /**
     * Logs a communication error
     * @param msg error message
     */
    public static void logCommunicationError(String msg){
        log.log(Level.WARNING, "Communication problem: " + msg);
    }

    /**
     * Logs a response
     * @param r Response to log
     */
    public static void logResponseSend(Response r) {
        log.log(Level.INFO, "Sending response: " + r.toString());
    }

    /**
     * Logs a received query
     * @param q Query to log
     */
    public static void logQueryReceived(Query q) {
        log.log(Level.INFO, "Received query: " + q.toString());
    }

    /**
     * Logs a new client's source IP address/port and request
     * @param packet client
     * @param payload request to print
     */
    public static void logNewUDPClient(DatagramPacket packet, String payload) {
        log.log(Level.INFO, "Handling client at " + packet.getAddress().getHostAddress() +
                " on port " + packet.getPort() + "\nRequest:: " + payload);
    }

    /**
     * Logs a new client's source IP address/port and request
     * @param packet client
     * @param payload request to print
     */
    public static void logNewTCPClient(Socket packet, String payload) {
        log.log(Level.INFO, "Handling client at " + packet.getInetAddress().getHostAddress() +
                " on port " + packet.getPort() + "\nRequest:: " + payload);
    }

    /**
     * Logs a new client's source IP address/port and request
     * @param client client
     * @param payload request to print
     */
    public static void logNewASyncClient(AsynchronousSocketChannel client, String payload) {
        try {
            log.log(Level.INFO, "Handling client at " + client.getRemoteAddress().toString()
                    + "\nRequest:: " + payload);
        } catch (IOException e) {
            log.log(Level.INFO, "Handling client.\nRequest:: " + payload);
        }
    }

    /**
     * Logs a severe error
     * @param s severe error message
     */
    public static void logSevereError(String s){ log.severe(s); }

    /**
     * Logs a warning
     * @param s warning message
     */
    public static void logWarning(String s){ log.warning(s); }

}
