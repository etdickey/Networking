//Contains the ServerProtocol class (see comments below)
//Created: 11/8/20
package sdns.app.utils;

import sdns.app.masterfile.MasterFile;
import sdns.serialization.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static sdns.app.utils.LoggingUtils.*;
import static sdns.app.utils.ServerValidationUtils.getAndCheckMasterFile;

/**
 * This class handles the server protocl for SDNS servers, both UDP and TCP, allowing for polymorphic
 *   behavior through dispatch.
 *
 * @author Ethan Dickey
 * @version 1.0
 */
public abstract class ServerProtocol {
    private MasterFile mf;

    /**
     * Constructs the server's masterfile
     */
    protected ServerProtocol(){
        ///////////////////////////////////////
        //Setup internal tools for server use//
        //Get master file
        mf = getAndCheckMasterFile();
    }

    /**
     * Implemnets server protocol for handling message from a client (SDNS specifications)
     * @param message message to process
     * @throws IOException if IO error communicating with client
     */
    protected void processResponse(byte[] message) throws IOException {
        //Parse the message
        try {
            Message m = Message.decode(message);
            //log new client
            this.logNewClient(m.toString());

            //Check for bad message type
            if(m instanceof Query){
                //1. log query
                logQueryReceived((Query)m);

                //2. go get the response
                Response r = doSearch(mf, (Query)m);

                //Check for valid responses
                if(r != null){
                    //Send + log response with same ID and question, RCode = 0, and masterfile's ans/ns/adtl RRs
                    logResponseSend(r);
                    sendResponse(r);
                }
            } else {//bad message type
                handleBadMessage(m, RCode.REFUSED);
            }
        } catch (ValidationException e) {
            logParsingError(e.getMessage());
            logNewClient("Number of bytes received from invalid packet of size " + message.length);
        }
    }

    /**
     * Follows protocol for receiving a bad message (including queries that return nothing)
     * @param m bad message
     * @param responseCode response code to use when sending the response message
     * @throws IOException if socket error
     */
    private void handleBadMessage(Message m, RCode responseCode) throws IOException {
        handleBadMessage(m, responseCode, "Unexpected message type: ");
    }

    /**
     * Follows protocol for receiving a send + log an error
     * @param m bad message
     * @param responseCode response code to use when sending the response message
     * @param logMessage message to log
     * @throws IOException if socket error
     */
    private void handleBadMessage(Message m, RCode responseCode, String logMessage) throws IOException {
        logSevereError(logMessage + m.toString());

        //Handle according to specifications
        //send and log a response with the same ID and question, and empty answer/nameserver/additional
        try {
            Response toRespond = new Response(m.getID(), m.getQuery(), responseCode);
            //log and send
            logResponseSend(toRespond);
            sendResponse(toRespond);
        } catch (ValidationException ignored) {
            //ack!
        }
    }

    /**
     * Performs a search using the query object and follows specifications when responding
     * @param mf master file to search from
     * @param q question
     */
    private Response doSearch(MasterFile mf, Query q) throws IOException {
        //init the storage lists
        List<ResourceRecord> ans = new ArrayList<>(), ns = new ArrayList<>(), adtl = new ArrayList<>();
        Response r;
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

            //set the response rcode to 0
            r.setRCode(RCode.NOERROR);
        } catch (ValidationException | NullPointerException ignore) {
            //if question is invalid or anything else goes wrong while trying to resolve question
            //or if no such domain name
            //or if any parameters are null
            //send + log
            handleBadMessage(q, RCode.SERVERFAILURE, "Problem resolving: ");

            r = null;
        } catch (NoSuchElementException e){
            //send + log
            handleBadMessage(q, RCode.NAMEERROR, "Domain name does not exist: ");

            r = null;
        }
        return r;
    }

    /**
     * Sends a Response
     * @param r Response to send
     * @throws IOException if sending error
     */
    protected abstract void sendResponse(Response r) throws IOException;

    /**
     * Logs the current client with the given message
     * @param message message to log
     */
    protected abstract void logNewClient(String message);
}
