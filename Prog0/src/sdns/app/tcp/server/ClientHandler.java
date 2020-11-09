package sdns.app.tcp.server;

import sdns.app.utils.ServerProtocol;
import sdns.serialization.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import static sdns.app.utils.LoggingUtils.*;

public class ClientHandler extends ServerProtocol implements Runnable {
    private Socket client = null;
    private OutputStream sout = null;

    /**
     * Constructs the client handler
     * @param client client to handle
     */
    public ClientHandler(Socket client){
        this.client = client;
    }

    /**
     * Sends a Response through sout
     * @param r Response to send
     * @throws IOException if sending error
     */
    @Override
    protected void sendResponse(Response r) throws IOException {
        try {
            sout.write(Framer.frameMsg(r.encode()));
        } catch (ValidationException e) {
            //ack
        }
    }

    /**
     * Logs the current client with the given message
     * @param message message to log
     */
    @Override
    protected void logNewClient(String message){
        logNewTCPClient(client, message);
    }

    /**
     * When an object implementing interface {@code Runnable} is used
     * to create a thread, starting the thread causes the object's
     * {@code run} method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method {@code run} is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        //deal with client
        try {
            //get input and output streams
            this.sout = this.client.getOutputStream();

            //get message
            while(true){
                byte[] data = Framer.nextMsg(this.client.getInputStream());

                this.processResponse(data);
            }
        } catch(IOException e){
            logCommunicationError(e.getMessage());
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                logCommunicationError(e.getMessage());
            }
        }
    }
}
