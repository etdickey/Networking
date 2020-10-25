//Contains the CAA class (see comments below)
//Created: 10/25/20
package sdns.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static sdns.serialization.IOUtils.*;
import static sdns.serialization.ValidationUtils.validateVisibleAscii;

/**
 * Represents a CAA and provides serialization/deserialization
 * @author Ethan Dickey
 *   Credit: Dr. Donahoo of Baylor University for comments and API
 * @version 1.0
 */
public class CAA extends ResourceRecord {
    //Width of the preference field
    private static final int HEADER_CAA_WIDTH_BYTES = 2;
    //The proper CAA header
    private static final byte[] PROPER_HEADER = {0, 5, 'i', 's', 's', 'u', 'e'};
    //String representation of the domain name of certificate issuer
    private String issuer;

    /**
     * Constructs CAA RR using given values
     *
     * @param name RR name
     * @param ttl  RR ttl
     * @param issuer issuer name
     * @throws ValidationException if invalid name or ttl
     */
    public CAA(String name, int ttl, String issuer) throws ValidationException {
        super(name, ttl);
        this.setIssuer(issuer);
    }

    /**
     * Finishes parsing a CAA ResourceRecord from the input stream
     *
     * @param name name to set
     * @param in   input stream to read from
     * @throws ValidationException if validation fails (see specification), including null name
     * @throws IOException         if IO error (such as premature EOF)
     */
    protected CAA(String name, InputStream in) throws ValidationException, IOException {
        super(name, in);

        //RDLength
        int rdlen = readUnsignedShortBigEndian(in);

        if(rdlen < HEADER_CAA_WIDTH_BYTES + 1){
            throw new ValidationException("ERROR: RDLen too small: " + rdlen, rdlen + "");
        }

        //RData
        //0x0, 0x5, "issue"
        byte tempByte;
        for(byte b : PROPER_HEADER){
            tempByte = readByte(in, "when reading " + b);
            if(tempByte != b){
                throw new ValidationException("ERROR: " + b + " byte not set correctly: " + tempByte, tempByte + "");
            }
        }

        //issue
        StringBuilder tempIssuer = new StringBuilder();
        for(int i=0; i<rdlen-HEADER_CAA_WIDTH_BYTES; i++){
            tempIssuer.append(readByte(in, "when reading issuer"));
        }
        this.setIssuer(tempIssuer.toString());
    }


    /**
     * Return type value for specific RR
     *
     * @return type value
     */
    @Override
    public int getTypeValue() {
        return CAA_TYPE_VALUE;
    }

    /**
     * Returns a byte array of the rdata for this object.  For internal use only.
     *
     * @return the serialized version of this objects rdata
     */
    @Override
    protected List<Byte> serializeRData() {
        List<Byte> bout = new ArrayList<>(Arrays.asList(toObject(PROPER_HEADER)));

        for(int i=0; i<issuer.length(); i++){
            bout.add((byte) issuer.charAt(i));
        }

        return bout;
    }

    /**
     * Get issuer name
     * @return name
     */
    public String getIssuer() { return this.issuer; }

    /**
     * Set issuer name
     * @param issuer new issuer name
     * @return this RR with new issuer name
     * @throws ValidationException if invalid issue name, including null
     */
    public CAA setIssuer(String issuer) throws ValidationException {
        if(issuer == null){
            throw new ValidationException("Issuer cannot be null", "null");
        }

        //Validate domain name
        if(validateVisibleAscii(issuer)){
            this.issuer = issuer;
        } else {
            throw new ValidationException("Issuer did not pass visible name checks: " + issuer, issuer);
        }

        return this;
    }

    /**
     * Returns a String representation
     *   CAA: name=<name> ttl=<ttl> issuer=<issuer>
     *
     * For example
     *   CAA: name=foo.com. ttl=500 issuer=pki.goog.
     * @return a String representation
     */
    @Override
    public String toString(){
        return "CAA: name=" + this.getName() + " ttl=" + this.getTTL() + " issuer=" + this.issuer;
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
        if (!super.equals(o)) return false;
        CAA caa = (CAA) o;
        return issuer.equalsIgnoreCase(caa.issuer);
    }

    /**
     * Hashes the object
     * @return the hashed value
     */
    @Override
    public int hashCode() {
        final int prime = 8887;
        int result = 1;
        result = prime * result + super.hashCode();
        result = prime * result + ((issuer == null) ? 0 : issuer.toLowerCase().hashCode());
        return result;
    }
}
