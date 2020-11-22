//Contains the NIODeframer class (see comments below)
//Created: 11/14/20
package sdns.serialization;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;

import static sdns.serialization.IOUtils.readUnsignedShortBigEndian;

/**
 * Non-blocking message deserialization from given buffers
 * @author Ethan Dickey
 *   Credit: Dr. Donahoo of Baylor University for comments and API
 * @version 1.0
 */
public class NIODeframer {
    //Indicators of buffer status
    private int frameSize = -1, nextInsert = 0;
    //start small and grow as needed, starting value = 256 (2^8)
    private byte[] buff = new byte[2 << 8];

    /**
     * Doubles the internal buffer's size.  Could extract to utility function in the future.
     */
    private void doubleBuffSize(){
        byte[] newBuff = new byte[this.buff.length*2];
        System.arraycopy(this.buff, 0, newBuff, 0, nextInsert);
        this.buff = newBuff;//automatically garbage collects old array (at some point)
    }

    /**
     * Adds the given bytes to the internal buffer, increasing the size of the buffer if needed
     * @param toAdd bytes to add
     */
    private void addBytesToBuffer(byte[] toAdd){
        while(buff.length < nextInsert + toAdd.length){
            //increase size
            this.doubleBuffSize();
        }

        //add arrays
        System.arraycopy(toAdd, 0, this.buff, nextInsert, toAdd.length);
        nextInsert += toAdd.length;
    }

    /**
     * Check if buffer length is big enough to have a frame length field
     */
    private void checkAndSetFrameSize(){
        if(nextInsert >= 2){
            //can read the frame size
            ByteArrayInputStream bin = new ByteArrayInputStream(buff);
            try {
                int length = readUnsignedShortBigEndian(bin);
                bin.close();//done using, so close resource
                //check if we can parse a message, if so, set the frameSize
                if(length + 2 <= nextInsert){//length + length of header bytes
                    frameSize = length;
                }//else do nothing
            } catch (IOException ignored) {
                //uh oh...  shouldn't happen
            }
        }
    }


    /**
     * Constructs the NIODeframer object, setting up internal objects
     * Required according to the API, but can remove because it does nothing
     */
    public NIODeframer() {}

    /**
     * Non-blocking call to get the next message (not including prefix) if immediately available.  If a complete
     *   message is not immediately available, return null.  Any bytes not returned should be buffered for use
     *   with future invocations of this method.
     * @param buffer next bytes of message
     * @return next message, excluding the prefix length
     * @throws NullPointerException if buffer is null
     */
    public byte[] getMessage(byte[] buffer) throws NullPointerException {
        //Having a buffer with 0 size is a way to get a new message
        //  if there was more than one message in any previous call
        //check parameter
        Objects.requireNonNull(buffer);

        //shove the new bytes into internal buffer
        this.addBytesToBuffer(buffer);

        //deal with buffer
        this.checkAndSetFrameSize();

        //return something
        byte[] toReturn = null;
        //check if have enough bytes to satisfy frame size (frame size will be set if this is valid)
        if(frameSize != -1 && nextInsert >= frameSize + 2){//second check is superfluous, but just in case
            //if so, return those bytes and remove them from the array, leaving the rest of the array in tact
            toReturn = new byte[frameSize];

            //Copy array over
            System.arraycopy(buff, 2, toReturn, 0, frameSize);

            //copy rest of array forwards
            System.arraycopy(buff, frameSize+2, buff, 0, nextInsert-frameSize-1);

            nextInsert -= (frameSize + 2);
            frameSize = -1;
        }

        return toReturn;
    }
}
