//Contains the Query class (see comments below)
//Created: 11/8/20
package load;

import org.apache.jmeter.protocol.tcp.sampler.AbstractTCPClient;
import org.apache.jmeter.protocol.tcp.sampler.ReadException;
import sdns.serialization.Framer;
import sdns.serialization.Message;
import sdns.serialization.Query;
import sdns.serialization.ValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static java.lang.System.err;

/**
 * Object to use for load testing
 * @author Ethan Dickey
 * @version 1.0
 */
public class TCPClientImplSDNS extends AbstractTCPClient {
    /**
     * Write to the output stream (UNSUPPORTED)
     * @param outputStream output stream
     * @param inputStream input stream
     * @throws IOException if IO exception
     */
    @Override
    public void write(OutputStream outputStream, InputStream inputStream) throws IOException {
        throw new UnsupportedOperationException("Unused");
    }

    /**
     * Writes the domain name as a query to the output stream
     * @param outputStream output stream
     * @param s domain name to write
     * @throws IOException if io exception
     */
    @Override
    public void write(OutputStream outputStream, String s) throws IOException {
        try {
            Query q = new Query((int)(Math.random()*65535), s);
            outputStream.write(Framer.frameMsg(q.encode()));
        } catch (ValidationException e) {
            err.println("ERROR: Invalid domain name: " + s);
            throw new IOException("Validation Exception: " + e.getMessage(), e);
        }
    }

    /**
     * @param inputStream
     * @deprecated
     */
    @Override
    public String read(InputStream inputStream) throws ReadException {
        try {
            Message m = Message.decode(Framer.nextMsg(inputStream));
            return m.toString();
        } catch (ValidationException | IOException e) {
            err.println("ERROR: Invalid response: " + e.getMessage());
            return "Invalid response";
        }
    }
}
