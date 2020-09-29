//Contains the A class (see comments below)
//Created: 9/18/20
package sdns.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;

import static sdns.serialization.IOUtils.readIPv4;
import static sdns.serialization.IOUtils.readUnsignedShortBigEndian;

/**
 * Represents an A RR (IPv4 address) and provides serialization/deserialization
 * @author Ethan Dickey
 *   Credit: Dr. Donahoo of Baylor University for comments and API
 * @version 1.0
 */
public class A extends ResourceRecord {
    Inet4Address address;

    /**
     * Constructs A RR using given values
     * @param name RR name
     * @param ttl RR TTL
     * @param address IPv4 address
     * @throws ValidationException if validation fails (see specification), including null name or address
     */
    public A(String name, int ttl, Inet4Address address) throws ValidationException {
        super(name, ttl);
        this.setAddress(address);
    }

    /**
     * Finishes parsing an A ResourceRecord from the input stream
     * @param name RR name
     * @param in the input stream to finish parsing from
     * @throws ValidationException if validation fails (see specification), including null name
     * @throws IOException if IO error (such as premature EOF)
     */
    protected A(String name, InputStream in) throws ValidationException, IOException {
        super(name, in);
        //RDLength
        int rdlen = readUnsignedShortBigEndian(in);
        if(rdlen != 4){ throw new ValidationException("ERROR: RDLen != 4 when deserializing type A", rdlen + ""); }
        //deserialze ipv4
        this.address = readIPv4(in);
    }

    /**
     * Set address
     * @param address new address
     * @return this RR with new address
     * @throws ValidationException if address is invalid (null)
     */
    public A setAddress(Inet4Address address) throws ValidationException {
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
    public Inet4Address getAddress() { return this.address; }

    /**
     * Returns a String representation
     * A: name=<name> ttl=<ttl> address=<address>
     *
     * For example
     * A: name=foo.com. ttl=500 address=1.2.3.4
     * @return a string representation
     */
    @Override
    public String toString(){ return "A: name=" + this.getName() + " ttl=" + this.getTTL() + " address=" + this.address.getHostAddress(); }

    /**
     * Return type value for specific RR
     *
     * @return type value
     */
    @Override
    public long getTypeValue() { return A_TYPE_VALUE; }

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
        A that = (A) o;
        return address.equals(that.address);
    }

    /**
     * Hashes the object
     * @return the hashed value
     */
    @Override
    public int hashCode() {
        final int prime = 2749;
        int result = 1;
        result = prime * result + super.hashCode();
        result = prime * result + ((address == null) ? 0 : address.hashCode());
        return result;
    }
}
