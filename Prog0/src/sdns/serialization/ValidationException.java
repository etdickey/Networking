//Contains the ValidationException class (see comments below)
//Created: 9/8/20
package sdns.serialization;

import java.io.Serializable;

/**
 * Exception for handling validation problems
 *   Credit: Dr. Donahoo of Baylor University for comments and API
 * @version 1.0
 */
public class ValidationException extends Exception implements Serializable {
    String badToken;

    /**
     * Equivalent to ValidationException(message, null, badToken)
     * @param message exception message
     * @param badToken string causing exception (null if no such string)
     */
    public ValidationException(String message, String badToken){
        super(message);
        this.badToken = badToken;
    }

    /**
     * Constructs validation exception
     * @param message exception message
     * @param cause exception cause
     * @param badToken string causing exception (null if no such string)
     */
    public ValidationException(String message, Throwable cause, String badToken){
        super(message, cause);
        this.badToken = badToken;
    }

    /**
     * Returns bad token
     * @return bad token
     */
    public String getBadToken(){ return this.badToken; }
}
