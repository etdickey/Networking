//Contains the RCode enum (see comments below)
//Created: 10/17/20
package sdns.serialization;

/**
 * Allowable response codes with associated numeric values and response messages.  Each RCOde (e.g. FORMATERROR)
 *   has an associated value (integer) and message (string).  See specificaiton for details
 * @author Ethan Dickey
 *   Credit: Dr. Donahoo of Baylor University for comments and API
 * @version 1.0
 */
public enum RCode {
    /**
     * Indicates no error
     */
    NOERROR(0, "No error condition"),
    /**
     * Indicates that the name server was unable to interpret the query
     */
    FORMATERROR(1, "The name server was unable to interpret the query"),
    /**
     * Indicates that the name server was unable to process this query due to a problem with the name server
     */
    SERVERFAILURE(2, "The name server was unable to process this query " +
            "due to a problem with the name server"),
    /**
     * Indicates that the domain name referenced in the query does not exist
     */
    NAMEERROR(3, "The domain name referenced in the query does not exist"),
    /**
     * Indicates that the name server does not support the requested kind of query
     */
    NOTIMPLEMENTED(4, "The name server does not support the requested kind of query"),
    /**
     * Indicates that the name server refuses to perform the specified operation
     */
    REFUSED(5, "The name server refuses to perform the specified operation");

    /** Value for rcode */
    private int val;
    /** Error message associated with the rcode */
    private String errMessage;

    /**
     * Constructs and rcode with a value and error message
     * @param val value for the enum
     * @param errMessage associated error message
     */
    RCode(int val, String errMessage){
        this.val = val;
        this.errMessage = errMessage;
    }

    /**
     * Get the rcode associated with the given rcode value
     * @param rcodeValue rcode value
     * @return RCode assocaited with given value
     * @throws ValidationException if rcode value is out of range
     */
    public static RCode getRCode(int rcodeValue) throws ValidationException {
        for(RCode r : RCode.values()){
            if(r.val == rcodeValue){
                return r;//compiler will optimize appropriately
            }
        }
        throw new ValidationException("Invalid RCode: " + rcodeValue, rcodeValue + "");
    }

    /**
     * Get the rcode value
     * @return the value associated with the rcode
     */
    public int getRCodeValue() {
        return this.val;
    }

    /**
     * Get the rcode message
     * @return the message associated with the rcode
     */
    public String getRCodeMessage(){
        return this.errMessage;
    }
}
