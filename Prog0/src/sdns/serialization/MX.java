//Contains the MX class (see comments below)
//Created: 10/11/20
package sdns.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static sdns.serialization.IOUtils.*;
import static sdns.serialization.ValidationUtils.validateDomainName;

/**
 * Represents a MX and provides serialization/deserialization
 * @author Ethan Dickey
 *   Credit: Dr. Donahoo of Baylor University for comments and API
 * @version 1.0
 */
public class MX extends ResourceRecord {
    //Width of the preference field
    private static final int PREFERENCE_WIDTH_BYTES = 2;
    //A domain name which specifies a host willing to act as a mail exchange for the owner name
    private String exchange;
    //Specifies the preference given to this RR among others at the same owner. Lower values preferred
    private int preference;

    /**
     * Constructs MX using given values
     * @param name RR name
     * @param ttl RR TTL
     * @param exchange Domain name of mail exchange
     * @param preference Preference for mail exchange
     * @throws ValidationException if validation fails (see specs), including null name or exchange
     */
    public MX(String name, int ttl, String exchange, int preference) throws ValidationException {
        super(name, ttl);
        this.setExchange(exchange);
        this.setPreference(preference);
    }

    /**
     * Finishes parsing an MX ResourceRecord from the input stream
     *
     * @param name RR name
     * @param in   input stream to read from
     * @throws ValidationException if validation fails (see specification), including null name
     * @throws IOException         if IO error (such as premature EOF)
     */
    protected MX(String name, InputStream in) throws ValidationException, IOException {
        super(name, in);

        //RDLength
        int rdlen = readUnsignedShortBigEndian(in);

        if(rdlen < PREFERENCE_WIDTH_BYTES + 1){
            throw new ValidationException("ERROR: RDLen too small: " + rdlen, rdlen + "");
        }

        //RData
        //Preference
        int pref = readUnsignedShortBigEndian(in);
        this.setPreference(pref);

        //Exchange
        String rdata = readDomainName(in, rdlen-PREFERENCE_WIDTH_BYTES);
        this.setExchange(rdata);
    }


    /**
     * Get exchange
     * @return exchange domain name
     */
    public String getExchange() { return this.exchange; }

    /**
     * Get preference
     * @return preference
     */
    public int getPreference() { return this.preference; }

    /**
     * Set exchange domain name
     * @param exchange new exchange domain name
     * @return this RR with new exchange
     * @throws ValidationException if invalid exchange, including null
     */
    public MX setExchange(String exchange) throws ValidationException {
        if(exchange == null){
            throw new ValidationException("Exchange cannot be null", "null");
        }

        //require non null and validate domain name all in one!
        if(validateDomainName(exchange)){
            this.exchange = exchange;
        } else {
            throw new ValidationException("Exchange did not pass domain name checks: " + exchange, exchange);
        }

        return this;
    }

    /**
     * Set preference
     * @param preference exchange preference
     * @return this RR with new preference
     * @throws ValidationException if invalid preference
     */
    public MX setPreference(int preference) throws ValidationException {
        if(preference < 0 || preference > ValidationUtils.MAX_UNSIGNED_SHORT){
            throw new ValidationException("Preference: invalid unsigned 16-bit int: " + preference, preference + "");
        }
        this.preference = preference;
        return this;
    }


    /**
     * Return type value for specific RR
     *
     * @return type value
     */
    @Override
    public int getTypeValue() { return MX_TYPE_VALUE; }

    /**
     * Returns a byte array of the rdata for this object.  For internal use only.
     *
     * @return the serialized version of this objects rdata
     */
    @Override
    protected List<Byte> serializeRData() {
        List<Byte> rdataBytes = new ArrayList<>(Arrays.asList(
                toObject(writeShortBigEndian((short) this.preference))
        ));
        try {
            serializeDomainName(this.exchange, rdataBytes);
        } catch (ValidationException e) {
            //ack!
        }
        return rdataBytes;
    }

    /**
     * Returns a String representation
     * MX: name=<name> ttl=<ttl> exchange=<exchange> preference=<preference>
     *
     * For example
     * MX: name=foo.com. ttl=500 exchange=ex.com. preference=4
     * @return a string representation
     */
    @Override
    public String toString() {
        return "MX: name=" + this.getName() + " ttl=" + this.getTTL() +
            " exchange=" + this.exchange + " preference=" + this.preference;
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
        MX mx = (MX) o;
        return preference == mx.preference && exchange.equalsIgnoreCase(mx.exchange);
    }

    /**
     * Hashes the object
     * @return the hashed value
     */
    @Override
    public int hashCode() {
        final int prime = 1039;
        int result = 1;
        result = prime * result + super.hashCode();
        result = prime * result + ((exchange == null) ? 0 : exchange.toLowerCase().hashCode());
        result = prime * result + preference;
        return result;
    }
}
