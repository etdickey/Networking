//Contains the Framer class (see comments below)
//Created: 10/25/20
package sdns.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static sdns.serialization.IOUtils.*;
import static sdns.serialization.ValidationUtils.MAX_UNSIGNED_SHORT;

/**
 * Frame/Deframe message
 * @author Ethan Dickey
 *   Credit: Dr. Donahoo of Baylor University for comments and API
 * @version 1.0
 */
public class Framer {
    /**
     * Prevent anyone from using the default constructor
     * @throws UnsupportedOperationException every time used (DON'T USE IT!)
     */
    private Framer() throws UnsupportedOperationException { throw new UnsupportedOperationException("Don't you dare"); }

    /**
     * Frame given message
     * @param message bytes to frame
     * @return framed byte[]
     * @throws ValidationException  if message too long (greater than 65535)
     * @throws NullPointerException if message is null
     */
    public static byte[] frameMsg(byte[] message) throws ValidationException, NullPointerException {
        //Null check
        Objects.requireNonNull(message);

        //Validate length
        if(message.length > MAX_UNSIGNED_SHORT){
            throw new ValidationException("Message longer than " + MAX_UNSIGNED_SHORT + ": " + message.length, message.length + "");
        }

        //Prefix the message with a 2-byte length field,
        //  which gives the message length in octets, excluidng the two-byte length field
        //These two bytes represent a binary-encoded, big endian, unsigned integer
        byte[] length = writeShortBigEndian((short)message.length);
        byte[] result = new byte[length.length + message.length];
        System.arraycopy(length, 0, result, 0, length.length);
        System.arraycopy(message, 0, result, length.length, message.length);

        return result;
    }

    /**
     * Get next message
     * @param in byte input source
     * @return message (without prefix)
     * @throws NullPointerException if in is null
     * @throws IOException          if I/O (EOFException if premature EoS)
     */
    public static byte[] nextMsg(InputStream in) throws NullPointerException, IOException {
        //Null check
        Objects.requireNonNull(in);

        //Read length
        int length = readUnsignedShortBigEndian(in);

        //Read length bytes from the input stream
        return readXBytes(in, length);
    }
}
