//Contains the Unknown testing class (see comments below)
//Created: 9/8/20
package sdns.serialization.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sdns.serialization.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

//Note: rerun all tests with different character encodings (UTF-32, UTF-16, etc) at runtime
/**
 * @author Ethan Dickey
 * @author Harrison Rogers
 */
class UnknownTest {
    private final long CN_TYPE_VALUE = 5L, NS_TYPE_VALUE = 2L, A_TYPE_VALUE = 1L;

    /**
     * Verifies a RR
     * @param rr RR to verify
     * @param name Correct name
     * @param ttl correct ttl
     * @param type correct type
     */
    private static void verifyRR(ResourceRecord rr, String name, int ttl, int type){
        Objects.requireNonNull(rr);
        assertEquals(name, rr.getName());
        assertEquals(ttl, rr.getTTL());
        assertEquals(type, rr.getTypeValue());
    }

    /**
     * Helper construct
     * @return Unknown with ttl = 321, name = ".", type = 8
     */
    private static Unknown constructUnknownDot321(){
        byte[] buff = { 0,
                0, 8,
                0, 1, //0x0001
                0, 0, 1, 65,
                0, 8,
                0, 0, 0, 0, 0, 0, 0, 0};
        Unknown temp = constructUnknown(buff);
        verifyRR(temp, ".", 321, 8);
        return temp;
    }

    /**
     * Helper construct
     * @return Unknown with ttl = 123, name = "good.com.", type = 2056
     */
    private static Unknown constructUnknownGood123(){
        byte[] buff = { 4, 'g', 'o', 'o', 'd', 3, 'c', 'o', 'm', -64, 5,
                8, 8,
                0, 1, //0x0001
                0, 0, 0, 123,
                0, 8,
                0, 0, 0, 0, 0, 0, 0, 0};
        Unknown temp = constructUnknown(buff);
        verifyRR(temp, "good.com.", 123, 2056);
        return temp;
    }

    /**
     * Decode an Unknown type to use from an array buffer
     * @param buff byte buffer
     * @return decoded Unknown object
     */
    private static Unknown constructUnknown(byte[] buff){
        ResourceRecord temp = null;
        try {
            temp = ResourceRecord.decode(new ByteArrayInputStream(buff));
        } catch (ValidationException | IOException e) {
            assert(false);
        }
        assertEquals(Unknown.class, temp.getClass());
        return (Unknown)temp;
    }

    /**
     * Test that it always throws an exception no matter what (varied input) (DONE)
     */
    @Test @DisplayName("Default encode fail")
    void test0() {
        Unknown ut = constructUnknownDot321();
        assertThrows(UnsupportedOperationException.class, () -> ut.encode(System.out));
    }

    /**
     * Test that it doesn't equal either of the two known values (DONE)
     */
    @Test @DisplayName("Assert that unknown type isn't any of the other types")
    void getTypeValue() {
        Unknown un = constructUnknownDot321();
        try {
            assertNotEquals(CN_TYPE_VALUE, un.getTypeValue());
            assertNotEquals(NS_TYPE_VALUE, un.getTypeValue());
            assertNotEquals(A_TYPE_VALUE, un.getTypeValue());
        } catch(Exception e){
            assert(false);
        }
    }

    /**
     * Equals and hashcode tests
     */
    @Nested
    class EqualsAndHashCode {
        @Test @DisplayName("Equals test basic")
        void equality1(){
            ResourceRecord u1, u2;

            u1 = constructUnknownDot321();
            u2 = constructUnknownDot321();

            assertEquals(u1, u2);
        }

        @Test @DisplayName("Equals tests not not equals")
        void equality2(){
            ResourceRecord u1, u2;

            u1 = constructUnknownGood123();
            u2 = constructUnknownGood123();

            assertFalse(!u1.equals(u2));
        }

        @Test @DisplayName("Hashcode not equals")
        void hash1(){
            ResourceRecord u1, u2, u3, u4;

            try {
                u1 = constructUnknownGood123();
                u2 = new NS("good.com.", 123, ".");
                u3 = new CName("good.com.", 123, ".");
                u4 = new A("good.com.", 123, (Inet4Address)Inet4Address.getByName("1.1.1.1"));

                assertAll("Hashcode not equal to any other type",
                        () -> assertNotEquals(u1.hashCode(), u2.hashCode()),
                        () -> assertNotEquals(u1.hashCode(), u3.hashCode()),
                        () -> assertNotEquals(u1.hashCode(), u4.hashCode())
                );
            } catch (ValidationException | UnknownHostException e) {
                assert(false);
            }
        }
    }

    /**
     * To string test
     */
    @Test
    void testToString() {
        String expected = "Unknown: name=foo.com. ttl=500";
        try {
            byte[] buff = { 3, 'f', 'o', 'o', 3, 'c', 'o', 'm', 0,
                    0, 116,
                    0, 1, //0x0001
                    0, 0, 1, -12,//500
                    0, 8,
                    0, 0, 0, 0, 0, 0, 0, 0};
            ResourceRecord test = ResourceRecord.decode(new ByteArrayInputStream(buff));
            verifyRR(test, "foo.com.", 500, 116);
            String output = test.toString();

            assertEquals(expected, output);
        } catch (ValidationException | IOException e) {
            fail();
        }
    }
}