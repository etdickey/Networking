//Contains the CNameTest class (see comments below)
//Created: 9/8/20
package sdns.serialization.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sdns.serialization.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
//Note: rerun all tests with different character encodings (UTF-32, UTF-16, etc) at runtime
/**
 * @author Ethan Dickey
 * @author Harrison Rogers
 */
class CNameTest {
    private final long CN_TYPE_VALUE = 5L;

    /**
     * test get type value (trivial) (DONE)
     */
    @Test
    void getTypeValue() {
        CName cn;
        try {
            cn = new CName(".", 32, ".");
            assertEquals(CN_TYPE_VALUE, cn.getTypeValue());
        } catch(Exception e){
            assert(false);
        }
    }

    /**
     * Test to string
     */
    @Test
    void testToString() {
        String expected = "CName: name=foo.com. ttl=42 canonicalname=thisIsABoringName.";
        try {
            CName test = new CName("foo.com.", 42, "thisIsABoringName.");
            String output = test.toString();

            assertEquals(expected, output);
        } catch (ValidationException e) {
            fail();
        }
    }

    //Canonical name setter and getter tests (DONE)
    //Name: -- these tests apply to all domain name field tests
    //  Each label must start with a letter, end with a letter or digit, and have as interior characters only letters
    //    (A-Z and a-z), digits (0-9), and hypen (-).
    //  A name with a single, empty label (".") is acceptable
    @Nested
    class CanonicalNameSetterGetter {
        //set canonical name tests ERROR
        @ParameterizedTest(name = "Canonical name = {0}")
        @ValueSource(strings = {"", "asdf", "asdf.].", "asdf.Ƞ.", "asdf..", "www.baylor.edu/", "..", "asdf.asdf", "f0-9.c0m-.", "Ẵ.Ẓ.㛃.⭐.⭕.",
                "-a.f", "-.", "-",
                "a234567890123456789012345678901234567890123456789012345678901234.",//64
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a2345678901."//256
        })
        void setCanNameValidationException(String name) {
            CName cn;
            try {
                cn = new CName(".", 0, ".");
                assertThrows(ValidationException.class, () -> cn.setCanonicalName(name));
            } catch(Exception e){
                assert(false);
            }
        }
        @Test
        void setCanNameNullPtr() {
            CName cn;
            try {
                cn = new CName(".", 0, ".");
                assertThrows(ValidationException.class, () -> cn.setCanonicalName(null));
            } catch(Exception e){
                assert(false);
            }
        }

        //set canonical name tests VALID
        @ParameterizedTest(name = "Canonical name = {0}")
        @ValueSource(strings = {"a23456789012345678901234567890123456789012345678901234567890123.",//63
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a234567890.",//255
                "foo.", "foo.com.", "f0-9.c0m.", "google.com.", "www.baylor.edu.", "f0.c-0.", ".", "f-0.", "f-0a."
        })
        void setAndGetCanNameValid(String name) {
            CName cn;
            try {
                cn = new CName(".", 0, ".");
                cn.setCanonicalName(name);
                assertEquals(name, cn.getCanonicalName());
            } catch(Exception e){
                assert(false);
            }
        }
    }

    //Constructor error tests (DONE)
    //Name: -- these tests apply to all domain name field tests
    //  Each label must start with a letter, end with a letter or digit, and have as interior characters only letters
    //    (A-Z and a-z), digits (0-9), and hypen (-).
    //  A name with a single, empty label (".") is acceptable
    @Nested
    class CNameConstructorErrorTests {
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
            assertThrows(ValidationException.class, () -> new CName(name, 0, "."));
        }
        @Test
        @DisplayName("Canonical name ptr")
        void constrNameValidationErrorNull() {
            assertThrows(ValidationException.class, () -> new CName(null, 0, "."));
        }

        //ValidationException canonical name tests
        @ParameterizedTest(name = "Canonical Name = {0}")
        @ValueSource(strings = {"", "asdf", "asdf.].", "asdf.Ƞ.", "asdf..", "www.baylor.edu/", "..", "asdf.asdf", "f0-9.c0m-.", "Ẵ.Ẓ.㛃.⭐.⭕.",
                "-a.f", "-.", "-",
                "a234567890123456789012345678901234567890123456789012345678901234.",//64
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a2345678901."//256
        })
        void constrCanNameValidationError(String name) {
            assertThrows(ValidationException.class, () -> new CName(".", 0, name));
        }
        @Test
        @DisplayName("Canonical name null ptr")
        void constrCanNameNullPtr() {
            assertThrows(ValidationException.class, () -> new CName(".", 0, null));
        }

        //ValidationException TTL tests
        @ParameterizedTest(name = "TTL = {0}")
        @ValueSource(ints = {-1, -2147483648})
        void constTTLValidationError(int ttl){
            assertThrows(ValidationException.class, () -> new CName(".", ttl, "."));
        }
    }

    /**
     * Constructor valid tests (DONE)
     */
    @Nested
    class CNameConstructorValidTests {
        //Constructor name tests VALID
        @ParameterizedTest(name = "Name = {0}")
        @ValueSource(strings = {"a23456789012345678901234567890123456789012345678901234567890123.",
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a234567890.",//255
                "foo.", "foo.com.", "f0-9.c0m.", "google.com.", "www.baylor.edu.", "f0.c-0.", ".", "f-0.", "f-0a."
        })
        void constrNameValid(String name) {
            CName cn;
            try {
                cn = new CName(name, 0, ".");
                assertEquals(name, cn.getName());
            } catch(Exception e){
                assert(false);
            }
        }

        //Constructor canonical name tests VALID
        @ParameterizedTest(name = "Canonical Name = {0}")
        @ValueSource(strings = {"a23456789012345678901234567890123456789012345678901234567890123.",
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a234567890.",//255
                "foo.", "foo.com.", "f0-9.c0m.", "google.com.", "www.baylor.edu.", "f0.c-0.", ".", "f-0.", "f-0a."
        })
        void constrCanNameValid(String name) {
            CName cn;
            try {
                cn = new CName(".", 0, name);
                assertEquals(name, cn.getCanonicalName());
            } catch(Exception e){
                assert(false);
            }
        }

        //Constructor TTL tests
        @ParameterizedTest(name = "TTL = {0}")
        @ValueSource(ints = {0, 1, 2147483647})
        void constTTLValid(int ttl){
            CName cn;
            //NS
            try {
                cn = new CName(".", ttl, ".");
                assertEquals(ttl, cn.getTTL());
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
    @Nested
    class EncodeTests {
        ByteArrayOutputStream testStream = new ByteArrayOutputStream();

        @Test
        void basicEncodeTest() {
            byte[] comparisonArr = {3, 102, 111, 111, 3, 99, 111, 109, 0,
                    0, 5,
                    0, 1,
                    0, 0, 0, 42,
                    0, 15,
                    13, 'c','a','n','o','n','i','c','a','l','N','a','m','e',0};
            int byteArrayBufferSize;
            try {
                CName testCName = new CName("foo.com.", 42, "canonicalName.");
                testCName.encode(testStream);
                byte[] dumpArray = testStream.toByteArray();
                testStream.reset();
                assertArrayEquals(comparisonArr, dumpArray);
            } catch (ValidationException | IOException e) {
                fail();
            }
        }

        @Test
        void encodeNullOutStreamTest() {
            try {
                CName testCName = new CName("foo.com.", 42, "canonicalName.");
                assertThrows(NullPointerException.class, () -> testCName.encode(null));
            } catch (ValidationException e) {
                e.printStackTrace();
            }
        }

        @Test
        void encodeTestNumbersAndHyphens() {
            byte[] comparisonArr = {3, 'f', '-', '9', 0,
                    0, 5,
                    0, 1,
                    0, 0, 0, 42,
                    0, 15,
                    13, 'c','a','n','o','n','i','c','a','l','N','a','m','e',0};
            int byteArrayBufferSize;
            try {
                CName testCName = new CName("f-9.", 42, "canonicalName.");
                testCName.encode(testStream);
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
                u1 = new CName(".", 0, "foo.");
                u2 = new CName(".", 0, "foo.");

                assertTrue(u1.equals(u2));
                assertTrue(u2.equals(u1));
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //not equal canonical name
        @Test @DisplayName("Equals tests not equals")
        void equality2(){
            ResourceRecord u1, u2;

            try {
                u1 = new CName(".", 123, "good.com.");
                u2 = new CName(".", 123, "good.com.q.");

                assertFalse(u1.equals(u2));
                assertFalse(u2.equals(u1));
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //not equal id
        @Test @DisplayName("Equals tests not equals")
        void equality3(){
            ResourceRecord u1, u2;

            try {
                u1 = new CName(".", 123, "good.com.");
                u2 = new CName(".", 379, "good.com.");//this makes 0000 0000 0111 1011 into 0000 0001 0111 1011

                assertFalse(u1.equals(u2));
                assertFalse(u2.equals(u1));
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //not equal nmae
        @Test @DisplayName("Equals tests not equals")
        void equality4(){
            ResourceRecord u1, u2;

            try {
                u1 = new CName("good.com.", 123, "good.com.");
                u2 = new CName("good.com.q.", 123, "good.com.");

                assertFalse(u1.equals(u2));
                assertFalse(u2.equals(u1));
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //hashcode equals
        @Test @DisplayName("Hashcode basic")
        void hash1(){
            ResourceRecord u1, u2;

            try {
                u1 = new CName("good.com.", 123, "good.com.");
                u2 = new CName("good.com.", 123, "good.com.");

                assertEquals(u1.hashCode(), u2.hashCode());
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //hashcode not equals
        @Test @DisplayName("Hashcode not equals")
        void hash2(){
            ResourceRecord u1, u2;

            try {
                u1 = new CName("good.com.", 123, "good.com.");
                u2 = new CName("good.com.", 124, "good.com.");

                assertNotEquals(u1.hashCode(), u2.hashCode());
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //hashcode different types
        @Test @DisplayName("Hashcode not equals")
        void hash3(){
            ResourceRecord u1, u2;

            try {
                u1 = new CName("good.com.", 123, "good.com.");
                u2 = new NS("good.com.", 123, "good.com.");

                assertNotEquals(u1.hashCode(), u2.hashCode());
            } catch (ValidationException e) {
                assert(false);
            }
        }
    }
}

