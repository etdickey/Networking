//Contains the A class (see comments below)
//Created: 9/18/20
package sdns.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet6Address;
import java.util.ArrayList;
import java.util.List;

import static sdns.serialization.IOUtils.readIPv6;
import static sdns.serialization.IOUtils.readUnsignedShortBigEndian;

/**
 * Represents an AAAA RR (IPv6 address) and provides serialization/deserialization
 * @author Ethan Dickey
 *   Credit: Dr. Donahoo of Baylor University for some comments
 * @version 1.0
 */
public class AAAA extends ResourceRecord {
    Inet6Address address;

    /**
     * Sets up this RR with name and ttl
     *
     * @param name RR name
     * @param ttl  RR ttl
     * @param address IPv6 address
     * @throws ValidationException if validation fails (see specification), including null name or address
     */
    public AAAA(String name, int ttl, Inet6Address address) throws ValidationException {
        super(name, ttl);
        this.setAddress(address);
    }

    /**
     * Finishes parsing an AAAA ResourceRecord from the input stream
     *
     * @param name RR name
     * @param in   input stream to read from
     * @throws ValidationException if validation fails (see specification), including null name
     * @throws IOException         if IO error (such as premature EOF)
     */
    protected AAAA(String name, InputStream in) throws ValidationException, IOException {
        super(name, in);
        //RDLength
        int rdlen = readUnsignedShortBigEndian(in);
        if(rdlen != 16){ throw new ValidationException("ERROR: RDLen != 16 when deserializing type AAAA", rdlen + ""); }
        //deserialze ipv6
        this.address = readIPv6(in);
    }

    /**
     * Set address
     * @param address new address
     * @return this RR with new address
     * @throws ValidationException if address is invalid (null)
     */
    public AAAA setAddress(Inet6Address address) throws ValidationException {
        if(address == null){
            throw new ValidationException("ERROR: Invalid address: null", "null");
        }
        this.address = address;
        return this;
    }

    /**
     * Get address
     * @return address of A RR
     */
    public Inet6Address getAddress() { return this.address; }

    /**
     * Returns a String representation
     * A: name=<name> ttl=<ttl> address=<address>
     *
     * For example
     * A: name=foo.com. ttl=500 address=1.2.3.4
     * @return a string representation
     */
    @Override
    public String toString(){ return "AAAA: name=" + this.getName() + " ttl=" + this.getTTL() + " address=" + this.address.getHostAddress(); }

    /**
     * Return type value for specific RR
     *
     * @return type value
     */
    @Override
    public long getTypeValue() { return AAAA_TYPE_VALUE; }

    /**
     * Returns a byte array of the rdata for this object.  For internal use only.
     *
     * @return the serialized version of this objects rdata
     */
    @Override
    protected List<Byte> serializeRData() {
        List<Byte> ret = new ArrayList<>();
        for(byte b : this.address.getAddress()){
            ret.add(b);
        }
        return ret;
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
        AAAA that = (AAAA) o;
        return address.equals(that.address);
    }

    /**
     * Hashes the object
     * @return the hashed value
     */
    @Override
    public int hashCode() {
        final int prime = 6577;
        int result = 1;
        result = prime * result + super.hashCode();
        result = prime * result + ((address == null) ? 0 : address.hashCode());
        return result;
    }
}
