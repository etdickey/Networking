//Contains the IOUtils class (see comments below)
//Created: 9/18/20
package sdns.serialization;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.List;
import java.util.Objects;

import static sdns.serialization.ValidationUtils.validateDomainName;

/**
 * Contains a set of utility functions for reading and writing bytes from an input stream and/or byte array
 *   (most commonly types useful in the sdns.serialization package)
 * @author Ethan Dickey
 * @version 1.0
 */
class IOUtils {
    //Bit mask to get a short from an int
    public static final int SHORT_BIT_MASK = 0x0000FFFF;
    //Bit mask to get a byte from an int
    public static final int BYTE_BIT_MASK = 0x000000FF;

    /**
     * Checks if the top two bits are set (indicating the end of a label stream)
     * @param b byte to check
     * @return if the top two bits are set
     */
    private static boolean checkEndOfLabelsBitsSet(byte b){ return (byte)(b & 0xC0) == (byte)(-64); }

    //output
    /**
     * Serializes the given domain name.  First checks that the name is a valid domain name though...
     * @param name the domain name to validate
     * @param b the byte arraylist to append to
     */
    static void serializeDomainName(String name, List<Byte> b) throws ValidationException {
        if(!validateDomainName(name)){
            throw new ValidationException("ERROR: name provided to internal serialize function not a valid domain name", name);
        }

        if(name.length() == 1){
            b.add((byte)0);
            return;
        }

        //Parse name into labels and
        String[] labels = name.substring(0, name.length()-1).split("\\.");

        //Put each length + label into the byte array
        for(var l : labels){
            b.add((byte) l.length());
            for(int i=0; i<l.length(); ++i){
                b.add((byte) l.charAt(i));
            }
        }

        //Add the final '0' to signal the end of the array
        b.add((byte)0);
    }

    /**
     * Writes a short (2 bytes) in a byte array
     * @param k the int to write
     * @return the short in Big Endian in a Byte array
     */
    static Byte[] writeShortBigEndian(short k) {
        Byte[] buff = new Byte[2];
        for(int i=0;i<2;i++){
            buff[i] = (byte)((k >> (1-i)*8) & 0x00FF);
        }

        return buff;
    }

    /**
     * Encodes a list of RRs into an output stream (repeated a few times in code so abstracted even though it is simple)
     * @param rrs RRs to encode
     * @param out Output stream to write to
     * @throws IOException if some sort of IO error
     */
    static void encodeRRList(List<ResourceRecord> rrs, OutputStream out) throws IOException {
        for(var rr : rrs){
            if(!(rr instanceof Unknown)){
                rr.encode(out);
            }
        }
    }

    //input
    /**
     * Alias for readDomainName(in, -1) <-sentinel value
     * @param in the InputStream to read from
     * @return a string representing the deserialized domain name read
     * @throws ValidationException if parse or validation problem
     * @throws IOException if I/O problem
     */
    static String readDomainName(InputStream in) throws ValidationException, IOException { return readDomainName(in, -1); }

    /**
     * Reads a domain name in from the given input stream
     * @param in the InputStream to read from
     * @param maxSize if the number of bytes read doesn't match the maxSize, a ValidationException is thrown
     * @return a string representing the deserialized domain name read
     * @throws ValidationException if parse or validation problem
     * @throws IOException if I/O problem
     */
    static String readDomainName(InputStream in, int maxSize) throws ValidationException, IOException {
        Objects.requireNonNull(in, "Input stream cannot be null");

        int numBytes = 0;
        byte llen;
        char ch;
        StringBuilder dname = new StringBuilder();
        StringBuilder finalString = new StringBuilder();

        //Check if maxSize is 0, in which case throw a validation exception
        if(maxSize == 0){
            throw new ValidationException("ERROR: Max size of a domain name cannot be 0", maxSize + "");
        }

        //read the first length
        llen = readByte(in);//discards the top 3 bytes, might output a negative
        numBytes++;

        //read the label length, label values, repeat until you read the end
        while(llen > 0 && !checkEndOfLabelsBitsSet(llen)){//0 is end of labels, -1 is end of stream
            for(int i=0; i<llen; i++){
                //read a character
                ch = (char)(readByte(in));
                numBytes++;
                dname.append(ch);
            }
            dname.append('.');

            if(dname.length()-1 > ValidationUtils.DOMAIN_NAME_LABEL_MAX_LEN+1){//-1 for the '.'
                throw new ValidationException("Label cannot exceed " + ValidationUtils.DOMAIN_NAME_LABEL_MAX_LEN +
                        " characters (including .)", dname.length() + "");
            }

            //spew into the output buffer
            finalString.append(dname.toString());
            dname.setLength(0);

            llen = readByte(in);
            numBytes++;
        }

//        if(llen == -1){
//            throw new EOFException("ERROR: Premature end of input stream");
//        } else
        if(checkEndOfLabelsBitsSet(llen)){//clear the next byte too, according to the specifications
            readByte(in, "reading trash byte after top two bits set");
            numBytes++;
        } else if(llen < -1){
            throw new ValidationException("ERROR: label length < 0", llen + "");
        }


        //Check for max size violations
        if(maxSize > 0 && maxSize != numBytes){
            if(maxSize > numBytes){
                throw new EOFException("ERROR: Premature end of input stream (RDLENGTH does not match RDATA " +
                        "length (rdlen=" + maxSize + ", rdata.length()=" + numBytes);
            } else {
                throw new ValidationException("ERROR: RDLENGTH does not match RDATA length (rdlen=" + maxSize
                        + ", rdata.length()=" + numBytes, maxSize + "");
            }
        }

        if(numBytes == 1 || numBytes == 2){//then we have the '.' case ([0] or [-64, 1])
            finalString.append('.');
        }

        return finalString.toString();
    }

    /**
     * Alias for readByte(in, "")
     * @param in the input stream to read from
     * @return a single byte
     * @throws IOException if premature EOF or other IO error
     */
    static byte readByte(InputStream in) throws IOException { return readByte(in, ""); }

    /**
     * Reads a single byte and checks for EOF
     * @param in the input stream to read from
     * @param when a message to append to the error stating what part of the calling code you're in
     * @return a single byte
     * @throws IOException if premature EOF or other IO error
     */
    static byte readByte(InputStream in, String when) throws IOException {
        int tempInt;
        tempInt = in.read();
        if(tempInt == -1){ throw new EOFException("ERROR: Premature EOF" + (when.equals("") ? "" : " " + when)); }
        return (byte) tempInt;
    }

    /**
     * Reads an int (4 bytes) from an input stream
     * @param in the input stream to read from
     * @return the int
     * @throws IOException if premature EOF or other IO error
     */
    static int readIntBigEndian(InputStream in) throws IOException {
        int toReturn = 0;

        for(int i=0;i<4;i++){
            toReturn = toReturn << 8;
            toReturn |= (readByte(in, "when reading int in big endian") & 0x00FF);
        }

        return toReturn;
    }

    /**
     * Reads an unsigned short (2 bytes) from an input stream and returns it as an int (because java doesn't
     *  really do unsigned)
     * @param in the input stream to read from
     * @return the unsigned short as an int
     * @throws IOException if premature EOF or other IO error
     */
    static int readUnsignedShortBigEndian(InputStream in) throws IOException {
        int toReturn = 0;

        for(int i=0;i<2;i++){
            toReturn = toReturn << 8;
            toReturn |= (readByte(in, "when reading unsigned short in big endian") & 0x00FF);
        }

        return toReturn;
    }

    /**
     * Reads X bytes from an input stream and throws an exception if they're not available
     * @param in the input stream to read from
     * @param x the number of bytes to try to read
     * @throws IOException if premature EOF or other IO error
     */
    static void readXBytes(InputStream in, int x) throws IOException {
        int numRead = 0;
        while(numRead < x && in.read() >= 0){//readByte(in, ", expected: " + x + " got: " + numRead)
            numRead++;
        }

        if(numRead != x){
            throw new EOFException("ERROR: Premature end of input stream, expected: " + x + " got: " + numRead);
        }
    }

    /**
     * Reads an IPv4 (4 bytes) from an input stream
     * @param in the input stream to read from
     * @return the IPv4 address
     * @throws IOException if premature EOF or other IO error
     */
    static Inet4Address readIPv4(InputStream in) throws IOException {
        //Number of bytes in an encoded IPv4 address
        final int NUM_BYTES_IPV4 = 4;
        byte[] buff = readIPHelper(in, NUM_BYTES_IPV4);
        return (Inet4Address) Inet4Address.getByAddress(buff);
    }

    /**
     * Reads an IPv6 (16 bytes) from an input stream
     * @param in the input stream to read from
     * @return the IPv6 address
     * @throws IOException if premature EOF or other IO error
     */
    static Inet6Address readIPv6(InputStream in) throws IOException {
        //Number of bytes in an encoded IPv6 address
        final int NUM_BYTES_IPV6 = 16;
        byte[] buff = readIPHelper(in, NUM_BYTES_IPV6);
        return (Inet6Address) Inet6Address.getByAddress(buff);
    }

    /**
     * Reads NUM_BYTES_IP in from buff
     * @param in input stream to read from
     * @param NUM_BYTES_IP number of bytes to read
     * @throws IOException if EOF or other IO error
     */
    private static byte[] readIPHelper(InputStream in, int NUM_BYTES_IP) throws IOException {
        byte[] buff = new byte[NUM_BYTES_IP];
        int i = 0, numRead = -1;
        while(i < NUM_BYTES_IP && (numRead = in.read(buff, i, 1)) != -1){
            i += numRead;
        }
        if(numRead == -1 && i < NUM_BYTES_IP){
            throw new EOFException("ERROR: Premature EOF when reading IPv4 " + numRead);
        }
        return buff;
    }
}
