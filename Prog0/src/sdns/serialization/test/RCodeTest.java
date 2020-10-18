//Contains the RCodeTest class (see comments below)
//Created: 10/18/20
package sdns.serialization.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sdns.serialization.RCode;
import sdns.serialization.ValidationException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ethan Dickey
 * @author Harrison Rogers
 */
public class RCodeTest {
    /**
     * Test getRCode
     */
    @Nested
    class RCodeGetRCodeSetGet extends RCodeSetGetAbstractTest {
        /**
         * Factory method for calling the appropriate function you want to test for rcode validity
         *
         * The valid tests are redundant for this, but the abstract functionality is still useful for invalid tests.
         * @param rcode rcode to call with
         * @return the result of a getRCode on the respective object
         * @throws ValidationException if invalid rcode
         */
        @Override
        protected RCode callSetGetRCode(int rcode) throws ValidationException {
            return RCode.getRCode(rcode);
        }
    }

    //Test getRCodeValue is set correctly
    @Test @DisplayName("Test getRCodeValue")
    void testGetRCodeValue(){
        assertAll("Each rcode value is correct",
                () -> assertEquals(0, RCode.NOERROR.getRCodeValue()),
                () -> assertEquals(1, RCode.FORMATERROR.getRCodeValue()),
                () -> assertEquals(2, RCode.SERVERFAILURE.getRCodeValue()),
                () -> assertEquals(3, RCode.NAMEERROR.getRCodeValue()),
                () -> assertEquals(4, RCode.NOTIMPLEMENTED.getRCodeValue()),
                () -> assertEquals(5, RCode.REFUSED.getRCodeValue()));
    }

    //Test getRCodeMessage contains some semblance of a message relating to the name (according to the specifications)
    @Test @DisplayName("Test getRCodeMessage returns an appropriate message")
    void testGetRCodeMessage(){
        assertAll("Each rcode value is correct",
                () -> assertTrue(RCode.NOERROR.getRCodeMessage().toLowerCase().contains("no error")),
                () -> assertTrue(RCode.FORMATERROR.getRCodeMessage().toLowerCase().contains("format error")),
                () -> assertTrue(RCode.SERVERFAILURE.getRCodeMessage().toLowerCase().contains("server failure")),
                () -> assertTrue(RCode.NAMEERROR.getRCodeMessage().toLowerCase().contains("name error")),
                () -> assertTrue(RCode.NOTIMPLEMENTED.getRCodeMessage().toLowerCase().contains("not implemented")),
                () -> assertTrue(RCode.REFUSED.getRCodeMessage().toLowerCase().contains("refused")));
    }
}
