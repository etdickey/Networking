//Contains the Response class (see comments below)
//Created: 9/8/20
package sdns.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static sdns.serialization.IOUtils.*;
import static sdns.serialization.ValidationUtils.validateDomainName;

/**
 * Represents generic SDNS RR and provides serialization/deserialization You may make concrete anything listed as
 * abstract in this interface. In other words, abstract is not part of the requirement
 * (while the class, method, and parameters are required).
 *   Credit: Dr. Donahoo of Baylor University for comments and API
 * @version 1.0
 */
public abstract class ResourceRecord implements Cloneable {
    //The type values for various subtypes (CName, NS, A, AAAA, MX)
    protected static final int CN_TYPE_VALUE = 5;
    protected static final int NS_TYPE_VALUE = 2;
    protected static final int A_TYPE_VALUE = 1;
    protected static final int AAAA_TYPE_VALUE = 28;
    protected static final int MX_TYPE_VALUE = 15;
    protected static final int CAA_TYPE_VALUE = 257;
    protected static final int SOA_TYPE_VALUE = 6;

    //RR name and TimeToLive (TTL)
    private String name = null;
    private int ttl = -1;

    /**
     * Finishes reading the 0x0001 and ttl from the input stream and sets up name and ttl for this RR
     * @param name name to set
     * @param in input stream to read from
     * @throws ValidationException if invalid input
     * @throws IOException if premature eof
     */
    protected ResourceRecord(String name, InputStream in) throws ValidationException, IOException {
        if(name == null){
            throw new ValidationException("ERROR: Name null", "null");
        }
        this.setName(name);

        //0x0001
        byte tempByte = readByte(in, "when reading 0x0001");
        if(tempByte != 0){ throw new ValidationException("ERROR: 0x0001 first byte not set correctly: " + tempByte, tempByte + ""); }

        tempByte = readByte(in, "when reading 0x0001");
        if(tempByte != 1){ throw new ValidationException("ERROR: 0x0001 second byte not set correctly: " + tempByte, tempByte + ""); }

        //TTL
        int ttl = readIntBigEndian(in);
        this.setTTL(ttl);
    }

    /**
     * Sets up this RR with name and ttl
     * @param name name to set
     * @param ttl ttl to set
     * @throws ValidationException if invalid name or ttl
     */
    protected ResourceRecord(String name, int ttl) throws ValidationException {
        if(name == null){
            throw new ValidationException("ERROR: Name null", "null");
        }
        this.setName(name);
        this.setTTL(ttl);
    }

    /**
     * Deserializes message from input source
     * @param in deserialization input source
     * @return a specific RR resulting from deserialization
     * @throws ValidationException if parse or validation problem
     * @throws IOException if I/O problem (e.g., premature EoS)
     * @throws NullPointerException if in is null
     */
    public static ResourceRecord decode(InputStream in) throws ValidationException, IOException {
        Objects.requireNonNull(in, "Input stream cannot be null");

        //name
        String name = readDomainName(in);

        //read higher order Type byte
        int type = readByte(in, "when reading type") << 8;
        //read lower order Type byte
        type += readByte(in, "when reading type") & BYTE_BIT_MASK;

        ResourceRecord toReturn;
        switch(type){
            case (short) CN_TYPE_VALUE: toReturn = new CName(name, in); break;
            case (short) NS_TYPE_VALUE: toReturn = new NS(name, in); break;
            case (short) A_TYPE_VALUE: toReturn = new A(name, in); break;
            case (short) AAAA_TYPE_VALUE: toReturn = new AAAA(name, in); break;
            case (short) MX_TYPE_VALUE: toReturn = new MX(name, in); break;
            case (short) CAA_TYPE_VALUE: toReturn = new CAA(name, in); break;
            case (short) SOA_TYPE_VALUE: toReturn = new SOA(name, in); break;
            default: toReturn = new Unknown(name, type, in); break;
        }

        return toReturn;
    }

    /**
     * Serializes RR to given sink
     * @param out serialization sink
     * @throws IOException if I/O problem
     * @throws NullPointerException if out is null
     */
    public void encode(OutputStream out) throws IOException {
        Objects.requireNonNull(out, "Output stream cannot be null");

        /*  "foo." = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            Valid data:
                3, 'f', 'o', 'o', -64, 5,
                0, 2,
                0, 1, //0x0001
                0, 0, 0, 0,
                6,
                3, 'f', 'o', 'o', -64, 5//"foo."
         */
        //Construct the output
        List<Byte> a = new ArrayList<>();

        //Name
        try{
            serializeDomainName(this.getName(), a);
        } catch(ValidationException e){
            //Do nothing
//            throw new ValidationException("WARN WARN WARN: \"NAME\" FIELD DOES NOT CONTAIN A VALID DOMAIN NAME", this.getName());
        }

        //Type -- this is not expandable, and is written acknowledging the "quick and dirty" way was used.
        Collections.addAll(a, (byte)0, (byte)this.getTypeValue());

        //0x0001
        Collections.addAll(a, (byte)0, (byte)1);

        //TTL
        ByteBuffer buff = ByteBuffer.allocate(4);
        buff.order(ByteOrder.BIG_ENDIAN);
        buff.putInt(this.getTTL());
        Collections.addAll(a, buff.get(0), buff.get(1), buff.get(2), buff.get(3));

        //RData and RDLength
        List<Byte> rdataBytes = this.serializeRData();
        short rdlen = (short) rdataBytes.size();

        ByteBuffer buff2 = ByteBuffer.allocate(4);
        buff2.order(ByteOrder.BIG_ENDIAN);
        buff2.putShort(rdlen);

        //RDLength
        Collections.addAll(a, buff2.get(0), buff2.get(1));
        //RData
        a.addAll(rdataBytes);

        //Copy output list to byte array
        byte[] finalBuff = new byte[a.size()];
        for(int i=0;i<a.size();i++){
            finalBuff[i] = a.get(i);
        }
        out.write(finalBuff);
    }

    /**
     * Return type value for specific RR
     * @return type value
     */
    public abstract int getTypeValue();

    /**
     * Get name of RR
     * @return name
     */
    public String getName() { return this.name; }

    /**
     * Get TTL of RR
     * @return TTL
     */
    public int getTTL(){ return this.ttl; }

    /**
     * Set name of RR
     * @param name new name of RR
     * @return this RR with new name
     * @throws ValidationException if new name invalid or null
     */
    public ResourceRecord setName(String name) throws ValidationException {
        if(name == null){
            throw new ValidationException("Name cannot be null", "null");
        }

        //require non null and validate domain name all in one!
        if(validateDomainName(name)){
            this.name = name;
        } else {
            throw new ValidationException("Name did not pass domain name checks", name);
        }

        return this;
    }

    /**
     * Set TTL of RR
     * @param ttl new TTL
     * @return this RR with new TTL
     * @throws ValidationException if new TTL invalid
     */
    public ResourceRecord setTTL(int ttl) throws ValidationException {
        if(ttl < 0){
            throw new ValidationException("TTL < 0", ttl + "");
        }
        this.ttl = ttl;
        return this;
    }

    /**
     * Returns a byte array of the rdata for this object.  For internal use only.
     * @return the serialized version of this objects rdata
     */
    protected abstract List<Byte> serializeRData();

    /**
     * Checks for equality
     * @param o the object to compare to
     * @return whether or not the objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceRecord that = (ResourceRecord) o;
        return ttl == that.ttl && name.equalsIgnoreCase(that.name);
    }

    /**
     * Hashes the object
     * @return the hashed value
     */
    @Override
    public int hashCode() {
        final int prime = 9857;
        int result = 1;
        result = prime * result;
        result = prime * result + ttl;
        result = prime * result + name.toLowerCase().hashCode();
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
