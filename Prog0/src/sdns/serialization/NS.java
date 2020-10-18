//Contains the NS class (see comments below)
//Created: 9/8/20
package sdns.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static sdns.serialization.IOUtils.*;
import static sdns.serialization.ValidationUtils.validateDomainName;

/**
 * Represents a NS and provides serialization/deserialization
 *   Credit: Dr. Donahoo of Baylor University for comments and API
 * @version 1.1
 */
public class NS extends ResourceRecord {
    //Name server
    private String nameServer;

    /**
     * Constructs NS using given values
     * @param name RR name
     * @param ttl RR TTL
     * @param nameServer name server
     * @throws ValidationException if validation fails (see specification), including null name or nameServer
     */
    public NS(String name, int ttl, String nameServer) throws ValidationException {
        super(name, ttl);
        //require non null, domain name validation happens in each individual method
        this.setNameServer(nameServer);
    }

    /**
     * Finishes parsing a NS ResourceRecord from the input stream
     * @param name RR name
     * @param in the input stream to finish parsing from
     * @throws ValidationException if validation fails (see specification), including null name
     * @throws IOException if IO error (such as premature EOF)
     */
    protected NS(String name, InputStream in) throws ValidationException, IOException {
        super(name, in);
        //RDLength
        int rdlen = readUnsignedShortBigEndian(in);
        //RData
        String rdata = readDomainName(in, rdlen);
        this.setNameServer(rdata);
    }

    /**
     * Get name server
     * @return name
     */
    public String getNameServer() { return this.nameServer; }

    /**
     * Set name server
     * @param nameServer new name server
     * @return this NS with new name server
     * @throws ValidationException if invalid name server, including null
     */
    public NS setNameServer(String nameServer) throws ValidationException {
        if(nameServer == null){
            throw new ValidationException("Name Server cannot be null", "null");
        }

        //require non null and validate domain name all in one!
        if(validateDomainName(nameServer)){
            this.nameServer = nameServer;
        } else {
            throw new ValidationException("Name Server did not pass domain name checks", nameServer);
        }

        return this;
    }

    /**
     * Return type value for NS
     * @return type value
     */
    @Override
    public int getTypeValue() { return NS_TYPE_VALUE; }

    /**
     * Returns a byte array of the rdata for this object.  For internal use only.
     * @return the serialized version of this objects rdata
     */
    @Override
    protected List<Byte> serializeRData(){
        List<Byte> rdataBytes = new ArrayList<>();
        try {
            serializeDomainName(this.getNameServer(), rdataBytes);
        } catch (ValidationException e) {
            //Do nothing
//            throw new ValidationException("WARN WARN WARN: \"NAMESERVER\" FIELD DOES NOT CONTAIN A VALID DOMAIN NAME", this.getName());
        }

        return rdataBytes;
    }

    /**
     * Returns a String representation
     * NS: name=<name> ttl=<ttl> nameserver=<nameserver>
     *   For example
     *     NS: name=foo.com. ttl=500 nameserver=ns.com
     * @return a String representation
     */
    @Override
    public String toString() { return "NS: name=" + this.getName() + " ttl=" + this.getTTL() + " nameserver=" + this.getNameServer(); }

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
        NS that = (NS) o;
        return nameServer.equals(that.nameServer);
    }

    /**
     * Hashes the object
     * @return the hashed value
     */
    @Override
    public int hashCode() {
        final int prime = 1019;
        int result = 1;
        result = prime * result + super.hashCode();
        result = prime * result + ((nameServer == null) ? 0 : nameServer.hashCode());
        return result;
    }
}
