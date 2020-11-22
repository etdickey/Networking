//Contains the (TCP) ServerAIO class (see comments below)
//Created: 11/14/20
package sdns.app.tcp.server;

import sdns.app.utils.ServerProtocol;
import sdns.serialization.Framer;
import sdns.serialization.NIODeframer;
import sdns.serialization.Response;
import sdns.serialization.ValidationException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

import static sdns.app.utils.LoggingUtils.*;
import static sdns.app.utils.ServerValidationUtils.getAndHandlePort;

/**
 * The server will take the command-line parameter of the server port. The server should repeatedly receive
 *   a Query and send a Response with a thread pool according to the server protocol.  The server should use
 *   asynchronous I/O
 *
 * @author Ethan Dickey
 *   Credit: Dr. Donahoo of Baylor University for comments and API, Oracle for AsynchronousServerSocketChannel sample code
 * @version 1.0
 */
public class ServerAIO {
    /**
     * Timeout in milliseconds
     */
    private static final int TIMEOUT = 20;
    /**
     * Buffer size (bytes)
     *   arbitrary, since the reads are buffered
     */
    private static final int BUFFSIZE = 256;

    /**
     * Main
     *
     * @param args arguments
     */
    public static void main(String[] args){
        final String usageError = "Usage: <server port>";

        //Set up logger to specifications
        setupLogger();

        ///////////////////////////////////////
        //Validate program arguments///////////
        //"The AOI server takes the command-line argument of the port of the server."
        if(args.length != 1){
            logErrorAndExit("Unable to start: Bad usage: " + usageError);
        }

        //get server port and validate
        int serverPort = getAndHandlePort(args[0]);

        try (final AsynchronousServerSocketChannel listenChannel =
                     AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(serverPort))) {// Bind local port

            // Create accept handler
            listenChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
                /**
                 * Invoked when an operation has completed.
                 *
                 * @param   clntChan
                 *          The result of the I/O operation.
                 * @param   attachment
                 *          The object attached to the I/O operation when it was initiated.
                 */
                @Override
                public void completed(AsynchronousSocketChannel clntChan, Void attachment) {
                    //accept the next connection
                    listenChannel.accept(null, this);

                    // handle this connection
                    handleAccept(clntChan);
                }

                /**
                 * Invoked when an operation fails.
                 *
                 * @param   e
                 *          The exception to indicate why the I/O operation failed
                 * @param   attachment
                 *          The object attached to the I/O operation when it was initiated.
                 */
                @Override
                public void failed(Throwable e, Void attachment) {
                    logWarning("Failed to close the connection: " + e.getMessage());
                }
            });
            // Block until current thread dies
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            logWarning("Server Interrupted: " + e.getMessage());
        } catch(IOException e){
            logErrorAndExit("Unable to start: bad port (or socket error): " + e.getMessage());
        }
    }

    /**
     * Called after each accept completion
     *
     * @param clntChan channel of new client
     */
    private static void handleAccept(final AsynchronousSocketChannel clntChan) {
        final NIODeframer deframer = new NIODeframer();
        final ByteBuffer readBuff = ByteBuffer.allocateDirect(BUFFSIZE);

        //This will handle process the client's message and responding to it, as well as setting up callbacks
        //  for afterwards
        ServerProtocol sp = new ServerProtocol() {
            /**
             * Sends a Response
             * @param r Response to send
             * @throws IOException if sending error
             */
            @Override
            protected boolean sendResponse(Response r) throws IOException {
                byte[] resp;
                try {
                    resp = Framer.frameMsg(r.encode());
                } catch (ValidationException e) {
                    //ack
                    return false;
                }

                //Spawn off a writer handler
                ByteBuffer toWrite = ByteBuffer.wrap(resp);
                clntChan.write(toWrite, toWrite, makeWriteCompletionHandler(clntChan, deframer, this, readBuff));
                return true;
            }

            /**
             * Runs upon failure to respond to the client during processResponse
             *   (primarily for possible use in derived classes)
             */
            @Override
            protected void handleFailedSend() {
                //spawn off a read completion handler
                readBuff.clear();
                clntChan.read(readBuff, TIMEOUT, TimeUnit.SECONDS, readBuff, makeReadCompletionHandler(clntChan, deframer, this));
            }

            /**
             * Logs the current client with the given message
             * @param message message to log
             */
            @Override
            protected void logNewClient(String message) {
                logNewASyncClient(clntChan, message);
            }
        };

        //try to read bytes, add handler once finished (non-blocking)
        readBuff.clear();
        clntChan.read(readBuff, TIMEOUT, TimeUnit.SECONDS, readBuff, makeReadCompletionHandler(clntChan, deframer, sp));
    }

    /**
     * Called after each read completion
     *
     * @param clntChan channel of new client
     * @param deframer deframer to use as the buffer
     * @param sp ServerProtocol object to handle responses
     * @param readBuff byte buffer used in read
     * @param bytesRead number of bytes read
     * @throws IOException if I/O problem
     */
    public static void handleRead(final AsynchronousSocketChannel clntChan, NIODeframer deframer,
                                  ServerProtocol sp, ByteBuffer readBuff, int bytesRead) throws IOException {
        if (bytesRead == -1) { // Did the other end close?
            clntChan.close();
        } else if (bytesRead > 0) {
            //Handle bytes read -- buf.array() is optional in implementation, so unsafe to use
            byte[] tempBuff = new byte[bytesRead];
            readBuff.flip();
            readBuff.get(tempBuff);

            //get any potential messages existing in the buffer
            byte[] buffer = deframer.getMessage(tempBuff);

            //if no readable message yet, go back to reading
            if(buffer == null){
                //go back to reading
                readBuff.clear();
                clntChan.read(readBuff, TIMEOUT, TimeUnit.SECONDS, readBuff, makeReadCompletionHandler(clntChan, deframer, sp));
            } else {//if readable message, decode/etc, then go back to reading
                //handle according to specifications -- sets up a new write completion handler
                sp.processResponse(buffer);
                //once this finishes, either a writer will have spawned off from sendResponse or from handleFailedSend
            }
        }
    }

    /**
     * Called after each write
     *
     * @param clntChan channel of new client
     * @param deframer the deframer to pass around
     * @param sp ServerProtocol object to handle responses
     * @param writeBuff byte buffer used in write
     * @param readBuff reading buffer to reuse
     * @throws IOException if IO error while communicating with client
     */
    public static void handleWrite(final AsynchronousSocketChannel clntChan, NIODeframer deframer,
                                   ServerProtocol sp, ByteBuffer writeBuff, ByteBuffer readBuff) throws IOException {
        if (writeBuff.hasRemaining()) { // More to write
            clntChan.write(writeBuff, writeBuff, makeWriteCompletionHandler(clntChan, deframer, sp, readBuff));
        } else {
            //check for more to write from deframer
            byte[] buffer = deframer.getMessage(new byte[0]);

            if(buffer == null){
                // Back to reading
                readBuff.clear();
                clntChan.read(readBuff, TIMEOUT, TimeUnit.SECONDS, readBuff, makeReadCompletionHandler(clntChan, deframer, sp));
            } else {//if readable message, decode/etc, then go back to reading
                //handle according to specifications -- sets up a new write completion handler
                sp.processResponse(buffer);
                //once this finishes, either a writer will have spawned off from sendResponse or from handleFailedSend
            }
        }
    }

    /**
     * Returns a new completion handler for when reading finishes
     * @param clntChan the async socket channel to use
     * @param deframer the deframer to pass around
     * @param sp ServerProtocol object to handle responses
     * @return new completion handler for when reading finishes
     */
    private static CompletionHandler<Integer, ByteBuffer> makeReadCompletionHandler(
            final AsynchronousSocketChannel clntChan, NIODeframer deframer, ServerProtocol sp){
        return new CompletionHandler<Integer, ByteBuffer>() {//non-blocking
            /**
             * Invoked when an operation has completed.
             *
             * @param   bytesRead
             *          The result of the I/O operation.
             * @param   buf
             *          The object attached to the I/O operation when it was initiated.
             */
            @Override
            public void completed(Integer bytesRead, ByteBuffer buf) {
                try {
                    handleRead(clntChan, deframer, sp, buf, bytesRead);
                } catch (IOException e) {
                    logCommunicationError(e.getMessage());
                }
            }

            /**
             * Invoked when an operation fails.
             *
             * @param   ex
             *          The exception to indicate why the I/O operation failed
             * @param   v
             *          The object attached to the I/O operation when it was initiated.
             */
            @Override
            public void failed(Throwable ex, ByteBuffer v) {
                try {
                    //if nothing more to read, guaranteed that we already responded to everything we could
                    clntChan.shutdownInput();
                    handleRead(clntChan, deframer, sp, null, -1);
                } catch (IOException e) {
                    logWarning("Failed to close the connection: " + e.getMessage());
                }
            }
        };
    }

    /**
     * Returns a new completion handler for when reading finishes
     * @param clntChan the async socket channel to use
     * @param deframer the deframer to pass around
     * @param sp ServerProtocol object to handle responses
     * @param readBuff reading buffer to reuse
     * @return new completion handler for when reading finishes
     */
    private static CompletionHandler<Integer, ByteBuffer> makeWriteCompletionHandler(
            final AsynchronousSocketChannel clntChan, NIODeframer deframer, ServerProtocol sp, ByteBuffer readBuff) {
        return new CompletionHandler<Integer, ByteBuffer>() {//non-blocking
            /**
             * Invoked when an operation has completed.
             *
             * @param bytesWritten The result of the I/O operation.
             * @param buf          The object attached to the I/O operation when it was initiated.
             */
            @Override
            public void completed(Integer bytesWritten, ByteBuffer buf) {
                try {
                    handleWrite(clntChan, deframer, sp, buf, readBuff);
                } catch (IOException e) {
                    logCommunicationError(e.getMessage());
                }
            }

            /**
             * Invoked when an operation fails.
             *
             * @param ex  The exception to indicate why the I/O operation failed
             * @param buf The object attached to the I/O operation when it was initiated.
             */
            @Override
            public void failed(Throwable ex, ByteBuffer buf) {
                try {//if can't write, close connection
                    clntChan.close();
                } catch (IOException e) {
                    logWarning("Failed to close the connection: " + e.getMessage());
                }
            }
        };
    }
}
