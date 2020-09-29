//Contains the Query class (see comments below)
//Created: 9/18/20
package sdns.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import static sdns.serialization.IOUtils.*;

/**
 * Represents a SDNS query and provides serialization/deserialization
 * @author Ethan Dickey
 *   Credit: Dr. Donahoo of Baylor University for comments and API
 * @version 1.0
 */
public class Query extends Message {
    /**
     * Constructs SDNS query using given values
     * @param id query id
     * @param query query domain name
     * @throws ValidationException if validation fails (see specification), including null query
     */
    public Query(int id, String query) throws ValidationException { super(id, query); }

    /**
     * Finishes parsing a Query Message from the input stream
     *
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
     *
     * @param id Message ID
     * @param in Input stream to finish parsing from
     * @throws ValidationException if validation fails (see specification)
     */
    protected Query(int id, InputStream in) throws ValidationException {
        super(id);

        try {
            byte temp = readByte(in, "when reading the header");
            //RA, ignore; Z, 0; RCode, 0
            if((temp & 0x70) != 0 || (temp & 0x0F) != 0){
                throw new ValidationException("ERROR: Z or RCode not set correctly, byte: " + temp, temp + "");
            }
            //0x0001
            int trash = readUnsignedShortBigEndian(in);
            if(trash != 1){
                throw new ValidationException("ERROR: 0x0001 not set correctly: " + trash, trash + "");
            }
            //ANCOUNT, NSCOUNT, ARCOUNT
            final String[] args = {"ANCOUNT", "NSCOUNT", "ARCOUNT"};
            for(int i=0; i<3; ++i){
                trash = readUnsignedShortBigEndian(in);
                if(trash != 0){
                    throw new ValidationException("ERROR: " + args[i] + " not set correctly: " + trash, trash + "");
                }
            }

            //decode question
            super.decodeQuestion(in);
        } catch (IOException e) {
            throw new ValidationException("ERROR: stream read error", e.getMessage());
        }
    }

    /**
     * Finishes writing the encoded header based on which subtype it is
     *
     * @param out the output array to write to
     */
    @Override
    protected void writeHeader(List<Byte> out) {
        //Write from QR to Response code
        //0[000 0]001      0[000] [0000]
        out.add((byte) 1); out.add((byte) 0);

        //Write 0x0001
        out.add((byte) 0); out.add((byte) 1);

        //Write ANCount, NSCount, and ARCount
        Collections.addAll(out, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0);
    }

    /**
     * Returns a String representation
     * Query: id=<id> query=<query>
     *
     * For example
     * Query: id=500 query=ns.com.
     * @return a String representation
     */
    @Override
    public String toString() { return "Query: id=" + this.getID() + " query=" + this.getQuery(); }

    /**
     * Checks for equality
     * @param o the object to compare to
     * @return whether or not the objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return super.equals(o);
    }

    /**
     * Hashes the object
     * @return the hashed value
     */
    @Override
    public int hashCode() {
        final int prime = 5749;
        int result = 1;
        result = prime * result + super.hashCode();
        return result;
    }
}
