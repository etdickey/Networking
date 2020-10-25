//Contains the Unknown class (see comments below)
//Created: 9/8/20
package sdns.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static sdns.serialization.IOUtils.readUnsignedShortBigEndian;
import static sdns.serialization.IOUtils.readXBytes;

/**
 * Represents an unknown type and provide deserialization
 *   Credit: Dr. Donahoo of Baylor University for comments and API
 * @version 1.1
 */
public class Unknown extends ResourceRecord {
    //RR type
    private int type;

    /**
     * Finishes parsing an Unknown ResourceRecord from the input stream (including clearing the input stream
     *  to the end of the RR)
     * @param name RR name
     * @param type RR type
     * @param in input stream to finish parsing from
     * @throws ValidationException if invalid data
     * @throws IOException  if IO error (such as premature EOF)
     */
    protected Unknown(String name, int type, InputStream in) throws ValidationException, IOException {
        super(name, in);
        this.type = type;
        //RDLength
        int rdlen = readUnsignedShortBigEndian(in);
        readXBytes(in, rdlen);
    }

    /**
     * Prevent anyone from using the default constructor
     * @throws ValidationException if invalid name or ttl (never will happen)
     * @throws UnsupportedOperationException every time used (DON'T USE IT!)
     */
    private Unknown() throws ValidationException { super(".", 1); throw new UnsupportedOperationException("DO NOT CONSTRUCT THIS WAY"); }

    /**
     * Always throws UnsupportedOperationException
     * @param out serialization sink
     */
    @Override
    public void encode(OutputStream out) throws UnsupportedOperationException { throw new UnsupportedOperationException("Cannot encode Unknown type"); }

    /**
     * Returns a byte array of the rdata for this object.  For internal use only.
     * @return the serialized version of this objects rdata
     */
    @Override
    protected List<Byte> serializeRData() throws UnsupportedOperationException { throw new UnsupportedOperationException("Cannot encode Unknown type"); }

    /**
     * Return type value for specific RR
     * @return type value
     */
    @Override
    public int getTypeValue() { return this.type; }

    /**
     * Returns a String representation
     * Unknown: name=<name> ttl=<ttl>
     *   For example
     *     Unknown: name=foo.com. ttl=500
     * @return a String representation
     */
    @Override
    public String toString() { return "Unknown: name=" + this.getName() + " ttl=" + this.getTTL(); }

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
        Unknown that = (Unknown) o;
        return this.getTypeValue() == that.getTypeValue();
    }

    /**
     * Hashes the object
     * @return the hashed value
     */
    @Override
    public int hashCode() {
        final int prime = 1087;
        int result = 1;
        result = prime * result + super.hashCode();
        result = prime * result + this.type;
        return result;
    }
}
