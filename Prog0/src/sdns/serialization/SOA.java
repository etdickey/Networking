//Contains the SOA class (see comments below)
//Created: 11/4/20
package sdns.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static sdns.serialization.IOUtils.*;
import static sdns.serialization.ValidationUtils.validateDomainName;

/**
 * Represents an SOA RR (start of authority) and provides serialization/deserialization
 * @author Ethan Dickey
 *   Credit: Dr. Donahoo of Baylor University for comments and API
 * @version 1.0
 */
public class SOA extends ResourceRecord {
    private String mName, rName;
    private long serial, refresh, retry, expire, minimum;

    /**
     * Constructs SOA using given values
     * @param name RR name
     * @param ttl RR ttl
     * @param mName domain name of primary zone source
     * @param rName domain name of mailbox responsible person
     * @param serial version number
     * @param refresh time before zone refresh
     * @param retry wait time before retry refresh
     * @param expire max time zone authoritative
     * @param minimum minimum TTL for any exported RR
     * @throws ValidationException if validation fails (see specification)
     */
    public SOA(String name, int ttl, String mName, String rName,
               long serial, long refresh, long retry, long expire, long minimum) throws ValidationException {
        super(name, ttl);
        this.setMName(mName);
        this.setRName(rName);
        this.setSerial(serial);
        this.setRefresh(refresh);
        this.setRetry(retry);
        this.setExpire(expire);
        this.setMinimum(minimum);
    }


    /**
     * Finishes parsing a SOA ResourceRecord from the input stream
     * @param name the name of the RR
     * @param in the input stream to finish parsing from
     * @throws ValidationException if invalid data
     * @throws IOException if IO error (such as premature EOF)
     */
    protected SOA(String name, InputStream in) throws ValidationException, IOException {
        super(name, in);

        //RDLength
        int rdlen = readUnsignedShortBigEndian(in);

        //RData
        int totRDLen = 0;
        List<String> rdata;
        this.setMName((rdata = readDomainNameWithLength(in)).get(0));
        totRDLen += Integer.parseInt(rdata.get(1));
        this.setRName((rdata = readDomainNameWithLength(in)).get(0));
        totRDLen += Integer.parseInt(rdata.get(1));

        if(totRDLen + 20 != rdlen){
            throw new ValidationException("RDLen (" + rdlen + ") != rdata length (" + totRDLen + ")", rdlen + "");
        }

        System.out.println("This: " + this.toString());
        this.setSerial(readUnsignedIntBigEndian(in));
        this.setRefresh(readUnsignedIntBigEndian(in));
        System.out.println("This: " + this.toString());
        this.setRetry(readUnsignedIntBigEndian(in));
        this.setExpire(readUnsignedIntBigEndian(in));
        System.out.println("This: " + this.toString());
        this.setMinimum(readUnsignedIntBigEndian(in));
    }

    /**
     * Get mName
     * @return mName domain name
     */
    public String getMName(){ return this.mName; }

    /**
     * Get rName
     * @return rName domain name
     */
    public String getRName() { return this.rName; }

    /**
     * Get serial
     * @return serial value
     */
    public long getSerial() { return this.serial; }

    /**
     * Get refresh
     * @return refresh value
     */
    public long getRefresh() { return this.refresh; }

    /**
     * Get retry
     * @return retry value
     */
    public long getRetry() { return this.retry; }

    /**
     * Get expire
     * @return expire value
     */
    public long getExpire() { return this.expire; }

    /**
     * Get minimum
     * @return minimum value
     */
    public long getMinimum() { return this.minimum; }

    /**
     * Set mName domain name
     * @param mName new mName domain name
     * @return this RR with new mname
     * @throws ValidationException if invalid mName, including null
     */
    public SOA setMName(String mName) throws ValidationException {
        if(mName == null){
            throw new ValidationException("MName cannot be null", "null");
        }

        //validate domain name
        if(validateDomainName(mName)){
            this.mName = mName;
        } else {
            throw new ValidationException("MName did not pass domain name checks: " + mName, mName);
        }

        return this;
    }

    /**
     * Set rName domain name
     * @param rName new rName domain name
     * @return this RR with new rName
     * @throws ValidationException if invalid rName, including null
     */
    public SOA setRName(String rName) throws ValidationException {
        if(rName == null){
            throw new ValidationException("RName cannot be null", "null");
        }

        //validate domain name
        if(validateDomainName(rName)){
            this.rName = rName;
        } else {
            throw new ValidationException("RName did not pass domain name checks: " + rName, rName);
        }

        return this;
    }

    /**
     * Set serial
     * @param serial new serial
     * @return this RR with new serial
     * @throws ValidationException if invalid serial
     */
    public SOA setSerial(long serial) throws ValidationException {
        if(serial < 0 || serial > ValidationUtils.MAX_UNSIGNED_INT){
            throw new ValidationException("Serial number out of range.", serial + "");
        }
        this.serial = serial;
        return this;
    }

    /**
     * Set refresh
     * @param refresh new refresh
     * @return this RR with new refresh
     * @throws ValidationException if invalid refresh
     */
    public SOA setRefresh(long refresh) throws ValidationException {
        if(refresh < 0 || refresh > ValidationUtils.MAX_UNSIGNED_INT){
            throw new ValidationException("Refresh number out of range.", refresh + "");
        }
        this.refresh = refresh;
        return this;
    }

    /**
     * Set retry
     * @param retry new retry
     * @return this RR with new retry
     * @throws ValidationException if invallid retry
     */
    public SOA setRetry(long retry) throws ValidationException {
        if(retry < 0 || retry > ValidationUtils.MAX_UNSIGNED_INT){
            throw new ValidationException("Retry number out of range.", retry + "");
        }
        this.retry = retry;
        return this;
    }

    /**
     * Set expire
     * @param expire new expire
     * @return this RR with new expire
     * @throws ValidationException if invalid expire
     */
    public SOA setExpire(long expire) throws ValidationException {
        if(expire < 0 || expire > ValidationUtils.MAX_UNSIGNED_INT){
            throw new ValidationException("Expire number out of range.", expire + "");
        }
        this.expire = expire;
        return this;
    }

    /**
     * Set minimum
     * @param minimum new minimum
     * @return this RR with new minimum
     * @throws ValidationException if invalid minimum
     */
    public SOA setMinimum(long minimum) throws ValidationException {
        if(minimum < 0 || minimum > ValidationUtils.MAX_UNSIGNED_INT){
            throw new ValidationException("Minimum number out of range.", minimum + "");
        }
        this.minimum = minimum;
        return this;
    }


    /**
     * Returns a String representation
     *  Ex:
     * SOA: name=<name> ttl=<ttl> mname=<mname> rname=<rname> serial=<serial> refresh=<refresh>
     *     retry=<retry> expire=<expire> minimum=<minimum>
     *
     * SOA: name=foo.com. ttl=500 mname=ns1.com rname=dns.com serial=543 refresh=900 retry=900 expire=1800 minimum=60
     *
     * @return a string representation
     */
    @Override
    public String toString(){ return "SOA: name=" + this.getName() + " ttl=" + this.getTTL() + " mname=" + this.mName
        + " rname=" + this.rName + " serial=" + this.serial + " refresh=" + this.refresh + " retry=" + this.retry
            + " expire=" + this.expire  + " minimum=" + this.minimum; }

    /**
     * Return type value for specific RR
     * @return type value
     */
    @Override
    public int getTypeValue() { return SOA_TYPE_VALUE; }

    /**
     * Returns a byte array of the rdata for this object.  For internal use only.
     *
     * @return the serialized version of this objects rdata
     */
    @Override
    protected List<Byte> serializeRData() {
        List<Byte> rdataBytes = new ArrayList<>();

        //add all the domain names
        try {
            serializeDomainName(this.mName, rdataBytes);
            serializeDomainName(this.rName, rdataBytes);
        } catch (ValidationException e) {
            //ack!
        }

        //add all the longs
        rdataBytes.addAll(Arrays.asList(toObject(writeIntBigEndian((int) this.serial))));
        rdataBytes.addAll(Arrays.asList(toObject(writeIntBigEndian((int) this.refresh))));
        rdataBytes.addAll(Arrays.asList(toObject(writeIntBigEndian((int) this.retry))));
        rdataBytes.addAll(Arrays.asList(toObject(writeIntBigEndian((int) this.expire))));
        rdataBytes.addAll(Arrays.asList(toObject(writeIntBigEndian((int) this.minimum))));

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
        SOA that = (SOA) o;
        return mName.equalsIgnoreCase(that.mName) && rName.equalsIgnoreCase(that.rName)
                && serial == that.serial && refresh == that.refresh && retry == that.retry
                && expire == that.expire && minimum == that.minimum;
    }

    /**
     * Hashes the object
     * @return the hashed value
     */
    @Override
    public int hashCode() {
        final int prime = 1229;
        int result = 1;
        result = prime * result + super.hashCode();
        result = prime * result + ((mName == null) ? 0 : mName.toLowerCase().hashCode());
        result = prime * result + ((rName == null) ? 0 : rName.toLowerCase().hashCode());
        result = (int) (prime * result + serial);
        result = (int) (prime * result + refresh);
        result = (int) (prime * result + retry);
        result = (int) (prime * result + expire);
        result = (int) (prime * result + minimum);
        return result;
    }
}
