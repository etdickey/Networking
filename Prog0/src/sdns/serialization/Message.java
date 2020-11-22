//Contains the Message class (see comments below)
//Created: 9/18/20
package sdns.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

import static sdns.serialization.IOUtils.*;
import static sdns.serialization.ValidationUtils.QR_BIT_SET;
import static sdns.serialization.ValidationUtils.validateDomainName;

/**
 * Represents generic portion of message and provides serialization/deserialization Note: This is just an API
 * containing the required classes, methods, parameters and associated types. Implementation details such as
 * additional methods, abstract, etc. are TBD by the developer.
 * @author Ethan Dickey
 *  Credit: Dr. Donahoo of Baylor University for comments and API
 * @version 1.0
 */
public abstract class Message implements Cloneable {
    //Maximum unsigned short
    private static final int MAX_UNSIGNED_SHORT = 65535;
    //ID and query fields
    private int id;
    private String query;

    /**
     * Protected constructor for convenience
     * @param id Message ID
     * @param query Message query (domain name
     * @throws ValidationException if invalid ID or query (see specification)
     */
    protected Message(int id, String query) throws ValidationException { this.setID(id); this.setQuery(query); }

    /**
     * Protected constructor for partial construction when decoding:
     * @param id id associated with the SDNS message
     * @throws ValidationException if invalid id
     */
    protected Message(int id) throws ValidationException { this.setID(id); }

    /**
     * Deserializes message from byte source.
     * Note: When deserialing RRs, a well-formed RR with an unknown type is deserialized as an Unknown
     *
     * Spec: Bits   Field (assigned value encode, decode)
     *       16     ID (*, * - must match)
     *       1      Query/Response (0/1, 0/1)
     *       4      Opcode (0, 0)
     *       1      Authoritative Answer (0, ignore)
     *       1      TrunCation (0, ignore)
     *       1      Recursion Desired (1, ignore)
     *       1      Recursion Available (0, ignore)
     *       3      Z (reserved for future) (0, ignore)
     *       4      Response code (0/1/2/3/4/5, 0/1/2/3/4/5)
     *       16     0x0001
     *       16     ANCOUNT (unsigned) query(0, 0), response(*, *)
     *       16     NSCOUNT (unsigned) query(0, 0), response(*, *)
     *       16     ARCOUNT (unsigned) query(0, 0), response(*, *)
     *
     * @param message deserialization byte source
     * @return a specific message resulting from deserialization
     * @throws NullPointerException if message is null
     * @throws ValidationException if parse or validation problem (including too few bytes)
     */
    public static Message decode(byte[] message) throws NullPointerException, ValidationException {
        Objects.requireNonNull(message, "Message cannot be null");

        InputStream buff = new ByteArrayInputStream(message);
        try {
            int id = readUnsignedShortBigEndian(buff);
            byte temp = readByte(buff, "when decoding header");
            //verify Opcode = 0, ignore AA and TC and RD
            if((temp & 0x78) != 0){
                throw new ValidationException("ERROR: Opcode is not 0: " + (temp & 0x78), (temp & 0x78) + "");
            }

            Message toReturn;
            //decode the QR byte
            if((temp & QR_BIT_MASK) == 0){
                toReturn = new Query(id, buff);
            } else if((temp & QR_BIT_MASK) == QR_BIT_SET) {
                toReturn = new Response(id, buff);
            } else {
                throw new RuntimeException("INTERNAL ERROR");
            }

            //verify the bytes have been used up
            if(buff.read() != -1){
                throw new ValidationException("ERROR: too many bytes in Query Message decoding", "Too many bytes");
            }

            return toReturn;
        } catch (IOException e) {
            throw new ValidationException("Message invalid length", e, Arrays.toString(message));
        }
    }

    /**
     * Decodes the Question section in an SDNS message and saves it in query
     * @param in the input stream to read from
     * @throws ValidationException if parse of validation problem (including too few bytes/premature EOF)
     */
    protected void decodeQuestion(InputStream in) throws ValidationException {
        int temp1, temp2;
        try {
            this.setQuery(readDomainName(in));
            temp1 = readUnsignedShortBigEndian(in);
            temp2 = readUnsignedShortBigEndian(in);
        } catch (IOException e) {//because the specifications do not call for IOExceptions
            throw new ValidationException("ERROR: Input not long enough", e.getMessage());
        }
        if(temp1 != 0x00FF){
            throw new ValidationException("ERROR: invalid 0x00FF", temp1 + "");
        }

        if(temp2 != 0x0001){
            throw new ValidationException("ERROR: invalid 0x0001", temp2 + "");
        }
    }

    /**
     * Serialize message as byte array
     * Spec: Bits   Field (assigned value encode, decode)
     *       16     ID (*, * - must match)
     *       1      Query/Response (0/1, 0/1)
     *       4      Opcode (0, 0)
     *       1      Authoritative Answer (0, ignore)
     *       1      TrunCation (0, ignore)
     *       1      Recursion Desired (1, ignore)
     *       1      Recursion Available (0, ignore)
     *       3      Z (reserved for future) (0, 0)
     *       4      Response code (0/1/2/3/4/5, 0/1/2/3/4/5)
     *       16     0x0001
     *       16     ANCOUNT (unsigned) query(0, 0), response(*, *)
     *       16     NSCOUNT (unsigned) query(0, 0), response(*, *)
     *       16     ARCOUNT (unsigned) query(0, 0), response(*, *)
     * @return serialized message
     */
    public byte[] encode(){
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            //ID field in header
            bout.write(new byte[]{(byte) (this.getID() >> 8), (byte) this.getID()});
            //rest of header
            this.writeHeader(bout);

            //Write Query field
            try {
                serializeDomainName(this.getQuery(), bout);
            } catch (ValidationException ignored) { }

            bout.write(new byte[]{
                    0, (byte) 0xFF,//0x00FF
                    0, 1//0x0001
            });

            //Add the Answer, NameServer, and Additional fields if a Response
            this.writeData(bout);
        } catch(IOException ignored) {}

        return bout.toByteArray();
    }

    /**
     * Finishes writing the encoded header based on which subtype it is
     * @param out the output array to write to
     * @throws IOException if error writing to the buffer
     */
    protected abstract void writeHeader(ByteArrayOutputStream out) throws IOException;

    /**
     * Finishes writing the encoded data section based on which subtype it is
     * @param out the output stream to write to
     */
    protected abstract void writeData(ByteArrayOutputStream out);

    /**
     * Get message ID
     * @return message ID
     */
    public int getID() { return this.id; }

    /**
     * Get query (domain name) of message
     * @return message query
     */
    public String getQuery() { return this.query; }

    /**
     * Set ID of message
     * @param id new id of message
     * @return this message with new id
     * @throws ValidationException if new id invalid
     */
    public Message setID(int id) throws ValidationException {
        if(id < 0 || id > MAX_UNSIGNED_SHORT){
            throw new ValidationException("ERROR: invalid id: " + id, id + "");
        }
        this.id = id;
        return this;
    }

    /**
     * Set message query (domain name)
     * @param query in the form of a domain name
     * @return this message with new query
     * @throws ValidationException if new query invalid or null
     */
    public Message setQuery(String query) throws ValidationException {
        if(query == null){ throw new ValidationException("Query cannot be null", "null"); }
        if(!validateDomainName(query)){ throw new ValidationException("Invalid query: " + query, query); }
        this.query = query;
        return this;
    }

    /**
     * Checks for equality
     * @param o the object to compare to
     * @return whether or not the objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return id == message.id && query.equalsIgnoreCase(message.query);
    }

    /**
     * Hashes the object
     * @return the hashed value
     */
    @Override
    public int hashCode() {
        final int prime = 7499;
        int result = 1;
        result = prime * result;
        result = prime * result + id;
        result = prime * result + query.toLowerCase().hashCode();
        return result;
    }

    /**
     * Creates and returns a copy of this object.  The precise meaning
     * of "copy" may depend on the class of the object. The general
     * intent is that, for any object {@code x}, the expression:
     * <blockquote>
     * <pre>
     * x.clone() != x</pre></blockquote>
     * will be true, and that the expression:
     * <blockquote>
     * <pre>
     * x.clone().getClass() == x.getClass()</pre></blockquote>
     * will be {@code true}, but these are not absolute requirements.
     * While it is typically the case that:
     * <blockquote>
     * <pre>
     * x.clone().equals(x)</pre></blockquote>
     * will be {@code true}, this is not an absolute requirement.
     * <p>
     * By convention, the returned object should be obtained by calling
     * {@code super.clone}.  If a class and all of its superclasses (except
     * {@code Object}) obey this convention, it will be the case that
     * {@code x.clone().getClass() == x.getClass()}.
     * <p>
     * By convention, the object returned by this method should be independent
     * of this object (which is being cloned).  To achieve this independence,
     * it may be necessary to modify one or more fields of the object returned
     * by {@code super.clone} before returning it.  Typically, this means
     * copying any mutable objects that comprise the internal "deep structure"
     * of the object being cloned and replacing the references to these
     * objects with references to the copies.  If a class contains only
     * primitive fields or references to immutable objects, then it is usually
     * the case that no fields in the object returned by {@code super.clone}
     * need to be modified.
     * <p>
     * The method {@code clone} for class {@code Object} performs a
     * specific cloning operation. First, if the class of this object does
     * not implement the interface {@code Cloneable}, then a
     * {@code CloneNotSupportedException} is thrown. Note that all arrays
     * are considered to implement the interface {@code Cloneable} and that
     * the return type of the {@code clone} method of an array type {@code T[]}
     * is {@code T[]} where T is any reference or primitive type.
     * Otherwise, this method creates a new instance of the class of this
     * object and initializes all its fields with exactly the contents of
     * the corresponding fields of this object, as if by assignment; the
     * contents of the fields are not themselves cloned. Thus, this method
     * performs a "shallow copy" of this object, not a "deep copy" operation.
     * <p>
     * The class {@code Object} does not itself implement the interface
     * {@code Cloneable}, so calling the {@code clone} method on an object
     * whose class is {@code Object} will result in throwing an
     * exception at run time.
     *
     * @return a clone of this instance.
     * @see Cloneable
     */
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
