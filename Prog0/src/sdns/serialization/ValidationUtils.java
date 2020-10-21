//Contains the ValidationUtils class (see comments below)
//Created: 9/18/20
package sdns.serialization;

import java.util.Objects;

/**
 * Contains a set of utility functions for validating certain common types in the sdns.serialization package
 *  Also contains a few common constants to the sdns.serialization package
 * @author Ethan Dickey
 * @version 1.0
 */
public class ValidationUtils {
    //Max domain name length
    public static final int DOMAIN_NAME_MAX_LEN = 255;
    //Max label length
    public static final int DOMAIN_NAME_LABEL_MAX_LEN = 63;
    //Max unsigned short
    public static final int MAX_UNSIGNED_SHORT = 65535;
    //Top bit is set for QR
    public static final byte QR_BIT_SET = (byte) 0x80;

    /**
     * Checks if character is in the set of characters [a-zA-Z]
     * @param c char to check
     * @return if character is in [a-zA-Z]
     */
    static boolean isAZaz(char c){
        //I use the -1/+1 syntax because <= makes the assembly check 2 conditions whereas incrementing/decrementing allows
        //  for a faster check (because the +-1 is calculated at compile time)
        return ('A'-1 < c && c < 'Z'+1) || ('a'-1 < c && c < 'z'+1);
    }

    /**
     * Checks if character is in the set of characters [a-zA-Z0-9]
     * @param c char to check
     * @return if character is in [a-zA-Z0-9]
     */
    static boolean isAZaz09(char c){
        //I use the -1/+1 syntax because <= makes the assembly check 2 conditions whereas incrementing/decrementing allows
        //  for a faster check (because the +-1 is calculated at compile time)
        return ('0'-1 < c && c < '9'+1) || ('A'-1 < c && c < 'Z'+1) || ('a'-1 < c && c < 'z'+1);
    }

    /**
     * Checks if character is in the set of characters [a-zA-Z0-9-]
     * @param c char to check
     * @return if character is in [a-zA-Z0-9-]
     */
    static boolean isAZaz09Dash(char c){
        //I use the -1/+1 syntax because <= makes the assembly check 2 conditions whereas incrementing/decrementing allows
        //  for a faster check (because the +-1 is calculated at compile time)
        return ('0'-1 < c && c < '9'+1) || ('A'-1 < c && c < 'Z'+1) || ('a'-1 < c && c < 'z'+1) || (c == '-');
    }

    /**
     * Validates a domain with the following constraints
     * @param domainName the domain name to validate
     * @return whether or not the domain name is valid based on the Specifications
     */
    public static boolean validateDomainName(String domainName){
        Objects.requireNonNull(domainName, "Domain names cannot be null");

        //Name: -- this applies to all domain name fields
        //  Each label must start with a letter, end with a letter or digit, and have as interior characters only letters
        //    (A-Z and a-z), digits (0-9), and hypen (-).
        //  A name with a single, empty label (".") is acceptable

        //Base checks
        //A name may not be longer than 255 characters, inclusive of dots
        if("".equals(domainName) || domainName.length() < 1 || domainName.length() > DOMAIN_NAME_MAX_LEN){
            return false;
        }

        //  A name with a single, empty label (".") is acceptable
        if(".".equals(domainName)){
            return true;
        }

        //Check that the first character isn't an empty label so that when we split, it doesn't get rid of
        // that bad test case
        //Also check that the last character is a ., which gets lost when splitting
        if(domainName.charAt(0) == '.' || domainName.charAt(domainName.length()-1) != '.'){
            return false;
        }
        //Remove the last dot after validating that it's there because it causes split to add one extra field
        domainName = domainName.substring(0, domainName.length()-1);

        //Parse into labels
        String[] labels = domainName.split("\\.", -1);

        if(labels.length < 1){ return false; }

        //Validate each label
        for(String l : labels){
            //A label may not be longer than 63 characters
            if(l.length() < 1 || l.length() > DOMAIN_NAME_LABEL_MAX_LEN){
                return false;
            }

            //  Each label must start with a letter
            if(!isAZaz(l.charAt(0))){
                return false;
            }

            //  Each label must end with a letter or digit
            if(!isAZaz09(l.charAt(l.length()-1))){
                return false;
            }

            //  Each label must have as interior characters only letters (A-Z and a-z), digits (0-9), and hypen (-)
            for(char c : l.toCharArray()){
                if(!isAZaz09Dash(c)){
                    return false;
                }
            }
        }

        return true;
    }
}
