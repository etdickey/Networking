//Contains the CName class (see comments below)
//Created: 9/8/20
package sdns.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static sdns.serialization.IOUtils.*;
import static sdns.serialization.ValidationUtils.validateDomainName;

/**
 * Represents a CName and provides serialization/deserialization
 *   Credit: Dr. Donahoo of Baylor University for comments and API
 * @version 1.1
 */
public class CName extends ResourceRecord {
    //Canonical name
    private String canonicalName;

    /**
     * Constructs CName using given values
     * @param name RR name
     * @param ttl RR TTL
     * @param canonicalName Canonical name
     * @throws ValidationException if validation fails (see specification), including null name or canonical name
     */
    public CName(String name, int ttl, String canonicalName) throws ValidationException {
        super(name, ttl);
        //require non null, domain name validation happens in each individual method
        this.setCanonicalName(canonicalName);
    }

    /**
     * Finishes parsing a CName ResourceRecord from the input stream
     * @param name the name of the RR
     * @param in the input stream to finish parsing from
     * @throws ValidationException if invalid data
     * @throws IOException if IO error (such as premature EOF)
     */
    protected CName(String name, InputStream in) throws ValidationException, IOException {
        super(name, in);
        //RDLength
        int rdlen = readUnsignedShortBigEndian(in);
        //RData
        String rdata = readDomainName(in, rdlen);
        this.setCanonicalName(rdata);
    }

    /**
     * Get canonical name
     * @return name
     */
    public String getCanonicalName() { return this.canonicalName; }

    /**
     * Set canonical name
     * @param canonicalName new canonical name
     * @return this RR with new canonical name
     * @throws ValidationException if invalid canonical name, including null
     */
    public CName setCanonicalName(String canonicalName) throws ValidationException {
        if(canonicalName == null){
            throw new ValidationException("Canonical Name cannot be null", "null");
        }

        //require non null and validate domain name all in one!
        if(validateDomainName(canonicalName)){
            this.canonicalName = canonicalName;
        } else {
            throw new ValidationException("Canonical Name did not pass domain name checks: " + canonicalName, canonicalName);
        }

        return this;
    }

    /**
     * Returns a String representation
     * CName: name=<name> ttl=<ttl> canonicalname=<canonicalname>
     *   For example
     *     CName: name=foo.com. ttl=500 canonicalname=ns.com
     *
     * @return String representation
     */
    @Override
    public String toString() { return "CName: name=" + this.getName() + " ttl=" + this.getTTL() + " canonicalname=" + this.getCanonicalName(); }

    /**
     * Return type value for specific RR
     * @return type value
     */
    @Override
    public int getTypeValue() { return CN_TYPE_VALUE; }

    /**
     * Returns a byte array of the rdata for this object.  For internal use only.
     * @return the serialized version of this objects rdata
     */
    @Override
    protected List<Byte> serializeRData(){
        List<Byte> rdataBytes = new ArrayList<>();
        try {
            serializeDomainName(this.getCanonicalName(), rdataBytes);
        } catch (ValidationException e) {
            //Do nothing
//            throw new ValidationException("WARN WARN WARN: \"CANONICALNAME\" FIELD DOES NOT CONTAIN A VALID DOMAIN NAME", this.getName());
        }

        return rdataBytes;
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
        CName that = (CName) o;
        return canonicalName.equalsIgnoreCase(that.canonicalName);
    }

    /**
     * Hashes the object
     * @return the hashed value
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + super.hashCode();
        result = prime * result + ((canonicalName == null) ? 0 : canonicalName.toLowerCase().hashCode());
        return result;
    }
}
