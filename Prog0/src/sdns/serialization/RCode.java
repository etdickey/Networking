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
    NOERROR,
    /**
     * Indicates that the name server was unable to interpret the query
     */
    FORMATERROR,
    /**
     * Indicates that the name server was unable to process this query due to a problem with the name server
     */
    SERVERFAILURE,
    /**
     * Indicates that the domain name referenced in the query does not exist
     */
    NAMEERROR,
    /**
     * Indicates that the name server does not support the requested kind of query
     */
    NOTIMPLEMENTED,
    /**
     * Indicates that the name server refuses to perform the specified operation
     */
    REFUSED;

    /**
     * Get the rcode associated with the given rcode value
     * @param rcodeValue rcode value
     * @return RCode assocaited with given value
     * @throws ValidationException if rcode value is out of range
     */
    public static RCode getRCode(int rcodeValue) throws ValidationException {
        switch(rcodeValue){
            case 0: return NOERROR;
            case 1: return FORMATERROR;
            case 2: return SERVERFAILURE;
            case 3: return NAMEERROR;
            case 4: return NOTIMPLEMENTED;
            case 5: return REFUSED;
            default: throw new ValidationException("Invalid RCode: " + rcodeValue, rcodeValue + "");
        }
    }

    /**
     * Get the rcode value
     * @return the value associated with the rcode
     */
    public int getRCodeValue() {
        int toRet;
        switch(this){
            case NOERROR: toRet = 0; break;
            case FORMATERROR: toRet = 1; break;
            case SERVERFAILURE: toRet = 2; break;
            case NAMEERROR: toRet = 3; break;
            case NOTIMPLEMENTED: toRet = 4; break;
            case REFUSED: toRet = 5; break;
            default: throw new IllegalStateException("Unexpected value: " + this);
        }
        return toRet;
    }

    /**
     * Get the rcode message
     * @return the message associated with the rcode
     */
    public String getRCodeMessage(){
        String toRet;
        switch(this){
            case NOERROR: toRet = "No error condition"; break;
            case FORMATERROR: toRet = "The name server was unable to interpret the query"; break;
            case SERVERFAILURE: toRet = "The name server was unable to process this query " +
                    "due to a problem with the name server"; break;
            case NAMEERROR: toRet = "The domain name referenced in the query does not exist"; break;
            case NOTIMPLEMENTED: toRet = "The name server does not support the requested kind of query"; break;
            case REFUSED: toRet = "The name server refuses to perform the specified operation"; break;
            default: throw new IllegalStateException("Unexpected value: " + this);
        }
        return toRet;
    }
}
