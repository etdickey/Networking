//Contains the DomainNameTestFactory class (see comments below)
//Created: 10/18/20
package sdns.serialization.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sdns.serialization.ValidationException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ethan Dickey
 * @author Harrison Rogers
 * Makes an abstract class for testing anything with a domain name
 */
abstract class DomainNameTestFactory {
    /**
     * Valid domain name tests
     * @param dm domain name to test
     */
    @ParameterizedTest(name = "Valid DM = {0}")
    @ValueSource(strings = {"a23456789012345678901234567890123456789012345678901234567890123.",
            "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                    "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                    "a234567890.",//255
            "foo.", "foo.com.", "f0-9.c0m.", "google.com.", "www.baylor.edu.", "f0.c-0.", ".", "f-0.", "f-0a."
    })
    void testValidDomainNames(String dm){
        try {
            assertEquals(dm, this.setGetDomainName(dm));
        } catch (ValidationException e) {
            fail();
        }
    }

    /**
     * Invalid domain name tests
     * @param dm domain name to test
     */
    @ParameterizedTest(name = "Invalid DM = {0}")
    @ValueSource(strings = {"", "asdf", "asdf.].", "asdf.Ƞ.", "asdf..", "www.baylor.edu/", "..", "asdf.asdf", "f0-9.c0m-.", "Ẵ.Ẓ.㛃.⭐.⭕.",
            "-a.f", "-.", "-",
            "a234567890123456789012345678901234567890123456789012345678901234.",//64
            "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                    "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                    "a2345678901."//256
    })
    void testInvalidDomainNames(String dm){
        assertThrows(ValidationException.class, () -> this.setGetDomainName(dm));
    }

    /**
     * Null domain name test (with variable throwbale class)
     */
    @Test @DisplayName("Null")
    void testNullDomainName(){
        assertThrows(this.getNullThrowableType(), () -> this.setGetDomainName(null));
    }

    /**
     * Factory method for calling the appropriate function you want to test for domain name validity
     * @param dm domain name to test
     * @return the result of a getDM on the respective object
     * @throws ValidationException if invalid domain name
     */
    protected abstract String setGetDomainName(String dm) throws ValidationException;

    /**
     * Allows the concrete class to specify which exception it wants to be thrown when a
     *   null string is passed to the function
     * @return class to throw
     */
    protected abstract Class<? extends Throwable> getNullThrowableType();
}
