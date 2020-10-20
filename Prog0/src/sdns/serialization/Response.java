//Contains the Response class (see comments below)
//Created: 9/18/20
package sdns.serialization;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static sdns.serialization.IOUtils.*;

/**
 * Represents a SDNS response and provides serialization/deserialization
 * @author Ethan Dickey
 *   Credit: Dr. Donahoo of Baylor University for comments and API
 * @version 1.1
 */
public class Response extends Message {
    // = new ArrayList<>() is folded into the constructor
    //Per Intellij's recommendation, these lists are final
    //A list of answer RRs
    private final List<ResourceRecord> answerList = new ArrayList<>();
    //A list of name server RRs
    private final List<ResourceRecord> nameServerList = new ArrayList<>();
    //A list of additional RRs
    private final List<ResourceRecord> additionalList = new ArrayList<>();
    //Message response code
    private RCode responseCode = RCode.NOERROR;

    /**
     * Constructs SDNS response using given values
     * @param id query id
     * @param query query domain name
     * @param rcode response code
     * @throws ValidationException if validation fails (see specification), including null query, rcode
     */
    public Response(int id, String query, RCode rcode) throws ValidationException {
        super(id, query);
        this.setRCode(rcode);
    }

    /**
     * Finishes parsing a Response Message from the input stream
     * @param id Message ID
     * @param in Input stream to finish parsing from
     * @throws ValidationException if validation fails (see specification)
     */
    protected Response(int id, InputStream in) throws ValidationException {
        super(id);

        try {
            //Finish parsing header from RA to ARCount
            byte temp = readByte(in, "when reading the header");
            //RA, ignore; Z, ignore (top 4 bytes)
            //rcode
            this.setRCode(RCode.getRCode(temp & 0x0F));

            //0x0001
            int trash = readUnsignedShortBigEndian(in);
            if (trash != 1) {
                throw new ValidationException("ERROR: 0x0001 not set correctly: " + trash, trash + "");
            }
            //ANCOUNT, NSCOUNT, ARCOUNT
            int ancount = readUnsignedShortBigEndian(in),//answer
                nscount = readUnsignedShortBigEndian(in),//name servers (authority)
                arcount = readUnsignedShortBigEndian(in);//additional records

            //decode question
            super.decodeQuestion(in);

            //Parse Answer field
            for(int i=0;i<ancount;i++){
                this.addAnswer(ResourceRecord.decode(in));
            }

            //Parse Authority field
            for(int i=0;i<nscount;i++){
                this.addNameServer(ResourceRecord.decode(in));
            }

            //Parse Additional field
            for(int i=0;i<arcount;i++){
                this.addAdditional(ResourceRecord.decode(in));
            }
        } catch (IOException e) {
            throw new ValidationException("ERROR: stream read error", e.getMessage());
        }
    }


    /*Begin Encode functionality*/
    /**
     * Finishes writing the encoded header based on which subtype it is
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
     * @param out the output stream to write to
     */
    @Override
    protected void writeHeader(ByteArrayOutputStream out) throws IOException {
        out.write(new byte[]{
                //Write from QR to Response code
                //1[000 0]001        0[000] [this.response & 0x0F]
                (byte) 0x81, (byte) (this.responseCode.getRCodeValue() & 0x0F),
                //Write 0x0001
                0, 1
        });

        //Write ANCount, NSCount, and ARCount DISCOUNTING number of unknowns
        out.write(writeShortBigEndian((short) this.answerList.stream()
                                                                .filter(e -> !(e instanceof Unknown))
                                                                .count()));
        out.write(writeShortBigEndian((short) this.nameServerList.stream()
                                                                .filter(e -> !(e instanceof Unknown))
                                                                .count()));
        out.write(writeShortBigEndian((short) this.additionalList.stream()
                                                                .filter(e -> !(e instanceof Unknown))
                                                                .count()));
    }

    /**
     * Writes the encode Answer, Authority, and Additional sections to the output array
     * @param out output stream to write to
     */
    public void writeData(ByteArrayOutputStream out) {
        try {
            encodeRRList(answerList, out);
            encodeRRList(nameServerList, out);
            encodeRRList(additionalList, out);
        } catch(IOException e){
            throw new RuntimeException("ERROR: internal encoding failure: ", e);
        }
    }
    /*End Encode functionality*/


    /**
     * Set new response code
     * @param rcode new response code
     * @return this Response with updated response code
     * @throws ValidationException if rcode is invalid
     */
    public Response setRCode(RCode rcode) throws ValidationException {
        if(rcode == null) {
            throw new ValidationException("ERROR: RCode null", "null");
        }
        this.responseCode = rcode;
        return this;
    }

    /**
     * Add new answer to answer list (duplicates ignored)
     * @param answer new answer to add to answer list
     * @return this Response with new answer
     * @throws ValidationException if answer is invalid (null)
     */
    public Response addAnswer(ResourceRecord answer) throws ValidationException {
        if(answer == null){ throw new ValidationException("ERROR: addAnswer parameter cannot be null", "null"); }
        if(!this.answerList.contains(answer)) {
            this.answerList.add((ResourceRecord) answer.clone());
        }
        return this;
    }

    /**
     * Add new name server to name server list (duplicates ignored)
     * @param nameServer new name server to add to name server list
     * @return this Response with new name server
     * @throws ValidationException if nameServer is invalid (null)
     */
    public Response addNameServer(ResourceRecord nameServer) throws ValidationException {
        if(nameServer == null){ throw new ValidationException("ERROR: addNameServer parameter cannot be null", "null"); }
        if(!this.nameServerList.contains(nameServer)) {
            this.nameServerList.add((ResourceRecord) nameServer.clone());
        }
        return this;
    }

    /**
     * Add new additional to additional list (duplicates ignored)
     * @param additional new additional to add to additional list
     * @return this Response with new additional
     * @throws ValidationException if additional is invalid (null)
     */
    public Response addAdditional(ResourceRecord additional) throws ValidationException {
        if(additional == null){ throw new ValidationException("ERROR: addAdditional parameter cannot be null", "null"); }
        if(!this.additionalList.contains(additional)) {
            this.additionalList.add((ResourceRecord) additional.clone());
        }
        return this;
    }

    /**
     * Get response code
     * @return response code
     */
    public RCode getRCode() { return this.responseCode; }

    /**
     * Get a list of RR answers
     * @return list of RRs
     */
    public List<ResourceRecord> getAnswerList() { return this.answerList; }

    /**
     * Get list of RR name servers
     * @return list of RRs
     */
    public List<ResourceRecord> getNameServerList() { return this.nameServerList; }

    /**
     * Get list of RR additionals
     * @return list of RRs
     */
    public List<ResourceRecord> getAdditionalList() { return this.additionalList; }

    /**
     * Returns a String representation
     * Response: id=<id> query=<query> answers=[<answer>,...,<answer>] nameservers=[<nameserver>,...,<nameserver>] additionals=[<additional>,...,<additional>]
     *
     * For example
     * Response: id=500 query=ns.com. answer=[a.com.,b.com.] nameservers=[ns1.com.] additionals=[]
     * @return a String representation
     */
    @Override
    public String toString(){ return "Response: id=" + this.getID() + " query=" + this.getQuery() +
            " answers=" + this.answerList.toString() +
            " nameservers=" + this.nameServerList.toString() +
            " additionals=" + this.additionalList.toString(); }

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
        Response response = (Response) o;
        return responseCode == response.responseCode &&
                Objects.equals(answerList, response.answerList) &&
                Objects.equals(nameServerList, response.nameServerList) &&
                Objects.equals(additionalList, response.additionalList);
    }

    /**
     * Hashes the object
     * @return the hashed value
     */
    @Override
    public int hashCode() {
        final int prime = 6619;
        int result = 1;
        result = prime * result + super.hashCode();
        result = prime * result + (answerList == null ? 0 : answerList.hashCode());
        result = prime * result + (nameServerList == null ? 0 : nameServerList.hashCode());
        result = prime * result + (additionalList == null ? 0 : additionalList.hashCode());
        result = prime * result + responseCode.getRCodeValue();
        return result;
    }
}
