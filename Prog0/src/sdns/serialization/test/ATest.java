//Contains the ATest class (see comments below)
//Created: 9/18/20
package sdns.serialization.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sdns.serialization.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
/**
 * @author Ethan Dickey
 * @author Harrison Rogers
 */
class ATest {
    private final long A_TYPE_VALUE = 1L;

    /**
     * Trivial test (DONE)
     */
    @Test
    void getTypeValue() {
        A a;
        try {
            a = new A(".", 32, (Inet4Address)Inet4Address.getByName("1.1.1.1"));
            assertEquals(A_TYPE_VALUE, a.getTypeValue());
        } catch(Exception e){
            assert(false);
        }
    }

    /**
     * Address name setter and getter tests
     */
    @Nested
    class IPv4SetterGetter {
        //set ipv4 name tests ERROR
        @Test
        @DisplayName("Null ipv4")
        void setIPv4NullPtr() {
            A a;
            try {
                a = new A(".", 0, (Inet4Address)Inet4Address.getByName("0.0.0.0"));
                assertThrows(ValidationException.class, () -> a.setAddress(null));
            } catch(Exception e){
                assert(false);
            }
        }

        //set ipv4 tests VALID
        @ParameterizedTest(name = "IPv4 Name = {0}")
        @ValueSource(strings = {"0.0.0.0", "1.1.1.1", "255.255.255.255"})
        void setAndGetIPv4Valid(String name) {
            A a;
            try {
                Inet4Address n = (Inet4Address)Inet4Address.getByName(name);
                a = new A(".", 0, (Inet4Address)Inet4Address.getByName("0.0.0.0"));
                a.setAddress(n);
                assertEquals(n, a.getAddress());
            } catch(Exception e){
                assert(false);
            }
        }
    }

    /**
     * To string tests
     */
    @Test
    void testToString() {
        String expected = "A: name=foo.com. ttl=42 address=0.0.0.0";
        try {
            A test = new A("foo.com.", 42, (Inet4Address)Inet4Address.getByName("0.0.0.0"));
            String output = test.toString();

            assertEquals(expected, output);
        } catch (ValidationException | UnknownHostException e) {
            fail();
        }
    }

    /**
     * Constructor error tests
     */
    @Nested
    class AConstructorErrors {
        //ValidationException name tests
        @ParameterizedTest(name = "Name = {0}")
        @ValueSource(strings = {"", "asdf", "asdf.].", "asdf.Ƞ.", "asdf..", "www.baylor.edu/", "..", "asdf.asdf", "f0-9.c0m-.", "Ẵ.Ẓ.㛃.⭐.⭕.",
                "-a.f", "-.", "-",
                "a234567890123456789012345678901234567890123456789012345678901234.",//64
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a2345678901."//256
        })
        void constrNameValidationError(String name) {
            assertThrows(ValidationException.class, () -> new A(name, 0, (Inet4Address)Inet4Address.getByName("0.0.0.0")));
        }

        //Null name test
        @Test
        @DisplayName("Name null")
        void constrNameValidationErrorNull() {
            assertThrows(ValidationException.class, () -> new A(null, 0, (Inet4Address)Inet4Address.getByName("0.0.0.0")));
        }

        //Null IPv4
        @Test
        @DisplayName("IPv4 name null ptr")
        void constrIPv4NullPtr() {
            assertThrows(ValidationException.class, () -> new A(".", 0, null));
        }

        //ValidationException TTL tests
        @ParameterizedTest(name = "TTL = {0}")
        @ValueSource(ints = {-1, -2147483648})
        void constTTLValidationError(int ttl){
            assertThrows(ValidationException.class, () -> new A(".", ttl, (Inet4Address)Inet4Address.getByName("0.0.0.0")));
        }
    }

    /**
     * Valid constructor tests
     */
    @Nested
    class AConstructorValid {
        //Constructor name tests VALID
        @ParameterizedTest(name = "Name = {0}")
        @ValueSource(strings = {"a23456789012345678901234567890123456789012345678901234567890123.",
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a234567890.",//255
                "foo.", "foo.com.", "f0-9.c0m.", "google.com.", "www.baylor.edu.", "f0.c-0.", ".", "f-0.", "f-0a."
        })
        void constrNameValid(String name) {
            A a;
            try {
                a = new A(name, 0, (Inet4Address)Inet4Address.getByName("0.0.0.0"));
                assertEquals(name, a.getName());
            } catch(Exception e){
                assert(false);
            }
        }

        //Constructor canonical name tests VALID
        @ParameterizedTest(name = "IPv4 Name = {0}")
        @ValueSource(strings = {"0.0.0.0", "1.1.1.1", "255.255.255.255"})
        void constrCanNameValid(String name) {
            A a;
            try {
                Inet4Address n = (Inet4Address)Inet4Address.getByName(name);
                a = new A(".", 0, n);
                assertEquals(n, a.getAddress());
            } catch(Exception e){
                assert(false);
            }
        }

        //Constructor TTL tests
        @ParameterizedTest(name = "TTL = {0}")
        @ValueSource(ints = {0, 1, 2147483647})
        void constTTLValid(int ttl){
            A a;
            try {
                a = new A(".", ttl, (Inet4Address)Inet4Address.getByName("0.0.0.0"));
                assertEquals(ttl, a.getTTL());
            } catch(Exception e){
                assert(false);
            }
        }
    }


    //When data enters the domain system its original case should be preserved whenever possible
    //
    //A name is serialized as a continuous sequence of labels. A label is serialized as a one octet length field
    //  followed by that number of octets
    //The final label of a name is serialized as either a single octet with value 0 or two octets where the first octet
    //  starts with two set bits (remaining 14 bits are ignored)
    //Test each section
    //  input too long, too short, null, invalid characters, missing sections
    //  case insensitivity

    /**
     * Test encode
     */
    @Nested
    class ValidEncodeTests {
        ByteArrayOutputStream testStream = new ByteArrayOutputStream();

        //Basic encode test
        @Test @DisplayName("A RR valid encode basic")
        void basicEncodeTest() {
            byte[] comparisonArr = {3, 102, 111, 111, 3, 99, 111, 109, 0,
                    0, 1,
                    0, 1,
                    0, 0, 0, 42,
                    0, 4,
                    -64, -88, 0, 69};
            try {
                //A object instantiation here
                A testA = new A("foo.com.", 42, (Inet4Address)Inet4Address.getByName("192.168.0.69"));
                testA.encode(testStream);
                byte[] dumpArray = testStream.toByteArray();
                testStream.reset();
                assertArrayEquals(comparisonArr, dumpArray);
            } catch (ValidationException | IOException e) {
                fail();
            }
        }

        //Encode null
        @Test @DisplayName("A RR valid encode null test")
        void encodeNullOutStreamTest() {
            try {
                A testA = new A("foo.com.", 42, (Inet4Address)Inet4Address.getByName("192.168.0.42"));
                assertThrows(NullPointerException.class, () -> testA.encode(null));
            } catch (ValidationException | IOException e) {
                fail();
            }
        }

        //Encode empty name
        @Test @DisplayName("A RR valid encode empty name test")
        void encodeEmptyName() {
            byte[] comparisonArr = {0,
                    0, 1,
                    0, 1,
                    0, 0, 0, 42,
                    0, 4,
                    -64, -88, 0, 69};
            try {
                //A object instantiation here
                A testA = new A(".", 42, (Inet4Address)Inet4Address.getByName("192.168.0.69"));
                testA.encode(testStream);
                byte[] dumpArray = testStream.toByteArray();
                testStream.reset();

                assertArrayEquals(comparisonArr, dumpArray);
            } catch (ValidationException | IOException e) {
                fail();
            }
        }

        //Encode with numbers and hyphens
        @Test @DisplayName("A RR valid encode num and hyphens in name")
        void encodeTestNumbersAndHyphens() {
            byte[] comparisonArr = {3, 'f', '-', '9', 0,
                    0, 1,
                    0, 1,
                    0, 0, 0, 42,
                    0, 4,
                    -64,-88,0,69};
            try {
                A testA = new A("f-9.", 42,  (Inet4Address)Inet4Address.getByName("192.168.0.69"));
                testA.encode(testStream);
                byte[] dumpArray = testStream.toByteArray();
                testStream.reset();
                assertArrayEquals(dumpArray, comparisonArr);
            } catch (ValidationException | IOException e) {
                fail();
            }
        }
    }

    /**
     * Equals and hash code testing
     */
    @Nested
    class EqualsAndHashCode {
        //basic equal test
        @Test @DisplayName("Equals test")
        void equality1(){
            ResourceRecord u1, u2;

            try {
                u1 = new A(".", 0, (Inet4Address)Inet4Address.getByName("0.0.0.0"));
                u2 = new A(".", 0, (Inet4Address)Inet4Address.getByName("0.0.0.0"));

                assertTrue(u1.equals(u2));
                assertTrue(u2.equals(u1));
            } catch (ValidationException | UnknownHostException e) {
                assert(false);
            }
        }

        //not equal canonical name
        @Test @DisplayName("Equals tests not equals")
        void equality2(){
            ResourceRecord u1, u2;

            try {
                u1 = new A(".", 123, (Inet4Address)Inet4Address.getByName("0.0.0.0"));
                u2 = new A(".", 123, (Inet4Address)Inet4Address.getByName("1.0.0.0"));

                assertFalse(u1.equals(u2));
                assertFalse(u2.equals(u1));
            } catch (ValidationException | UnknownHostException e) {
                assert(false);
            }
        }

        //not equal id
        @Test @DisplayName("Equals tests not equals")
        void equality3(){
            ResourceRecord u1, u2;

            try {
                u1 = new A(".", 123, (Inet4Address)Inet4Address.getByName("0.0.0.0"));
                u2 = new A(".", 379, (Inet4Address)Inet4Address.getByName("0.0.0.0"));//this makes 0000 0000 0111 1011 into 0000 0001 0111 1011

                assertFalse(u1.equals(u2));
                assertFalse(u2.equals(u1));
            } catch (ValidationException | UnknownHostException e) {
                assert(false);
            }
        }

        //not equal nmae
        @Test @DisplayName("Equals tests not equals")
        void equality4(){
            ResourceRecord u1, u2;

            try {
                u1 = new A("good.com.", 123, (Inet4Address)Inet4Address.getByName("0.0.0.0"));
                u2 = new A("good.com.q.", 123, (Inet4Address)Inet4Address.getByName("0.0.0.0"));

                assertFalse(u1.equals(u2));
                assertFalse(u2.equals(u1));
            } catch (ValidationException | UnknownHostException e) {
                assert(false);
            }
        }

        //hashcode equals
        @Test @DisplayName("Hashcode basic")
        void hash1(){
            ResourceRecord u1, u2;

            try {
                u1 = new A("good.com.", 123, (Inet4Address)Inet4Address.getByName("0.0.0.0"));
                u2 = new A("good.com.", 123, (Inet4Address)Inet4Address.getByName("0.0.0.0"));

                assertEquals(u1.hashCode(), u2.hashCode());
            } catch (ValidationException | UnknownHostException e) {
                assert(false);
            }
        }

        //hashcode not equals
        @Test @DisplayName("Hashcode not equals")
        void hash2(){
            ResourceRecord u1, u2;

            try {
                u1 = new A("good.com.", 123, (Inet4Address)Inet4Address.getByName("0.0.0.0"));
                u2 = new A("good.com.", 124, (Inet4Address)Inet4Address.getByName("0.0.0.0"));

                assertNotEquals(u1.hashCode(), u2.hashCode());
            } catch (ValidationException | UnknownHostException e) {
                assert(false);
            }
        }

        //hashcode different types
        @Test @DisplayName("Hashcode not equals")
        void hash3(){
            ResourceRecord u1, u2;

            try {
                u1 = new A("good.com.", 123, (Inet4Address)Inet4Address.getByName("0.0.0.0"));
                u2 = new NS("good.com.", 123, "good.com.");

                assertNotEquals(u1.hashCode(), u2.hashCode());
            } catch (ValidationException | UnknownHostException e) {
                assert(false);
            }
        }
    }
}