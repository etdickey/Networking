//Contains the NS testing class (see comments below)
//Created: 9/8/20
package sdns.serialization.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sdns.serialization.NS;
import sdns.serialization.ValidationException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
//Note: rerun all tests with different character encodings (UTF-32, UTF-16, etc) at runtime
/**
 * @author Ethan Dickey
 * @author Harrison Rogers
 */
class NSTest {
    private final long NS_TYPE_VALUE = 2L;

    /**
     * Trivial test (DONE)
     */
    @Test
    void getTypeValue() {
        NS ns;
        try {
            ns = new NS(".", 32, ".");
            assertEquals(NS_TYPE_VALUE, ns.getTypeValue());
        } catch(Exception e){
            assert(false);
        }
    }

    /**
     * Test tostring
     */
    @Test
    void testToString() {
        String expected = "NS: name=foo.com. ttl=42 nameserver=thisIsABoringName.";
        try {
            NS test = new NS("foo.com.", 42, "thisIsABoringName.");
            String output = test.toString();

            assertEquals(expected, output);
        } catch (ValidationException e) {
            fail();
        }
    }

    /**
     * Equals tests
     */
    @Nested
    class Equals {
        //Equals test
        @Test @DisplayName("Donahoo failure")
        void testNotEqual(){
            NS a, b;

            try {
                a = new NS("good.com.q.", 123, "good.com.");
                b = new NS("good.com.", 123, "good.com.");
                assertNotEquals(a, b);
            } catch (ValidationException e) {
                assert(false);
            }
        }
    }

    /**
     * Name server setter and getter tests (DONE)
     *     //Name: -- these tests apply to all domain name field tests
     *     Each label must start with a letter, end with a letter or digit, and have as interior characters only letters
     *       (A-Z and a-z), digits (0-9), and hypen (-).
     *     A name with a single, empty label (".") is acceptable
     */
    @Nested
    class NameServerSetterGetter {
        //set name server tests ERROR
        @ParameterizedTest(name = "Name Server = {0}")
        @ValueSource(strings = {"", "asdf", "asdf.].", "asdf.Ƞ.", "asdf..", "www.baylor.edu/", "..", "asdf.asdf", "f0-9.c0m-.", "Ẵ.Ẓ.㛃.⭐.⭕.",
                "-a.f", "-.", "-",
                "a234567890123456789012345678901234567890123456789012345678901234.",//64
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a2345678901."//256
        })
        void setNameServerValidationException(String name) {
            NS ns;
            try {
                ns = new NS(".", 0, ".");
                assertThrows(ValidationException.class, () -> ns.setNameServer(name));
            } catch(Exception e){
                assert(false);
            }
        }
        @Test
        @DisplayName("Set name server null ptr")
        void setNameServerNullPtr() {
            NS ns;
            try {
                ns = new NS(".", 0, ".");
                assertThrows(ValidationException.class, () -> ns.setNameServer(null));
            } catch(Exception e){
                assert(false);
            }
        }

        //set name server tests VALID
        @ParameterizedTest(name = "Name Server = {0}")
        @ValueSource(strings = {"a23456789012345678901234567890123456789012345678901234567890123.",//63
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a234567890.",//255
                "foo.", "foo.com.", "f0-9.c0m.", "google.com.", "www.baylor.edu.", "f0.c-0.", ".", "f-0.", "f-0a."
        })
        void setAndGetNameServerValid(String name) {
            NS ns;
            try {
                ns = new NS(".", 0, ".");
                ns.setNameServer(name);
                assertEquals(name, ns.getNameServer());
            } catch(Exception e){
                assert(false);
            }
        }
    }

    /**
     * Constructor error tests (DONE)
     *     //Name: -- these tests apply to all domain name field tests
     *     Each label must start with a letter, end with a letter or digit, and have as interior characters only letters
     *       (A-Z and a-z), digits (0-9), and hypen (-).
     *     A name with a single, empty label (".") is acceptable
     */
    @Nested
    class NSConstructorErrorTests {
        //ValidationException name tests ERROR
        @ParameterizedTest(name = "Name = {0}")
        @ValueSource(strings = {"", "asdf", "asdf.].", "asdf.Ƞ.", "asdf..", "www.baylor.edu/", "..", "asdf.asdf", "f0-9.c0m-.", "Ẵ.Ẓ.㛃.⭐.⭕.",
                "-a.f", "-.", "-",
                "a234567890123456789012345678901234567890123456789012345678901234.",//64
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a2345678901."//256
        })
        void constrNameValidationError(String name) {
            assertThrows(ValidationException.class, () -> new NS(name, 0, "."));
        }

        @Test
        @DisplayName("Name null ptr")
        void constrNameValidationErrorNull() {
            assertThrows(ValidationException.class, () -> new NS(null, 0, "."));
        }

        //ValidationException name server tests ERROR
        @ParameterizedTest(name = "Name Server = {0}")
        @ValueSource(strings = {"", "asdf", "asdf.].", "asdf.Ƞ.", "asdf..", "www.baylor.edu/", "..", "asdf.asdf", "f0-9.c0m-.", "Ẵ.Ẓ.㛃.⭐.⭕.",
                "-a.f", "-.", "-",
                "a234567890123456789012345678901234567890123456789012345678901234.",//64
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a2345678901."//256
        })
        void constrNameServerValidationError(String name) {
            assertThrows(ValidationException.class, () -> new NS(".", 0, name));
        }

        @Test
        @DisplayName("Name Server null ptr")
        void constrNameServerNullPtr() {
            assertThrows(ValidationException.class, () -> new NS(".", 0, null));
        }

        //ValidationException TTL tests
        @ParameterizedTest(name = "TTL = {0}")
        @ValueSource(ints = {-1, -2147483648})
        void constTTLValidationError(int ttl) {
            assertThrows(ValidationException.class, () -> new NS(".", ttl, "."));
        }
    }

    /**
     * Constructor valid tests (DONE)
     */
    @Nested
    class NSConstructorValidTests {
        //Constructor name tests VALID
        @ParameterizedTest(name = "Name = {0}")
        @ValueSource(strings = {"a23456789012345678901234567890123456789012345678901234567890123.",
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a234567890.",//255
                "foo.", "foo.com.", "f0-9.c0m.", "google.com.", "www.baylor.edu.", "f0.c-0.", ".", "f-0.", "f-0a."
        })
        void constrNameValid(String name) {
            NS ns;
            try {
                ns = new NS(name, 0, ".");
                assertEquals(name, ns.getName());
            } catch(Exception e){
                assert(false);
            }
        }

        //Constructor name server tests VALID
        @ParameterizedTest(name = "Name Server = {0}")
        @ValueSource(strings = {"a23456789012345678901234567890123456789012345678901234567890123.",
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a234567890.",//255
                "foo.", "foo.com.", "f0-9.c0m.", "google.com.", "www.baylor.edu.", "f0.c-0.", ".", "f-0.", "f-0a."
        })
        void constrNameServerValid(String name) {
            NS ns;
            try {
                ns = new NS(".", 0, name);
                assertEquals(name, ns.getNameServer());
            } catch(Exception e){
                assert(false);
            }
        }

        //Constructor TTL tests
        @ParameterizedTest(name = "TTL = {0}")
        @ValueSource(ints = {0, 1, 2147483647})
        void constTTLValid(int ttl){
            NS ns;
            //NS
            try {
                ns = new NS(".", ttl, ".");
                assertEquals(ttl, ns.getTTL());
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
    //  input too long, too short, null, invalid characters
    //  case insensitivity
    /**
     * Test encode
     */
    @Nested
    class EncodeTests {
        ByteArrayOutputStream testStream = new ByteArrayOutputStream();

        @Test
        void basicEncodeTest() {
            byte[] comparisonArr = {3, 102, 111, 111, 3, 99, 111, 109, 0,
                    0, 2,
                    0, 1,
                    0, 0, 0, 42,
                    0, 9,
                    7, 'N','S','D','N','a','m','e',0};
            int byteArrayBufferSize;
            try {
                NS testNS = new NS("foo.com.", 42, "NSDName.");
                testNS.encode(testStream);
                byte[] dumpArray = testStream.toByteArray();
                testStream.reset();
                assertArrayEquals(dumpArray, comparisonArr);
            } catch (ValidationException | IOException e) {
                fail();
            }
        }

        @Test
        void encodeNullOutStreamTest() {
            try {
                NS testNS = new NS("foo.com.", 42, "NSDName.");
                assertThrows(NullPointerException.class, () -> testNS.encode(null));
            } catch (ValidationException e) {
                e.printStackTrace();
            }
        }

        @Test
        void encodeTestNumbersAndHyphens() {
            byte[] comparisonArr = {3, 'f', '-', '9', 0,
                    0, 2,
                    0, 1,
                    0, 0, 0, 42,
                    0, 9,
                    7, 'N','S','D','N','a','m','e',0};
            int byteArrayBufferSize;
            try {
                NS testNS = new NS("f-9.", 42, "NSDName.");
                testNS.encode(testStream);
                byte[] dumpArray = testStream.toByteArray();
                testStream.reset();
                assertArrayEquals(dumpArray, comparisonArr);
            } catch (ValidationException | IOException e) {
                fail();
            }
        }
    }
}