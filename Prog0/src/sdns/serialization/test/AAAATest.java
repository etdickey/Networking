package sdns.serialization.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sdns.serialization.A;
import sdns.serialization.AAAA;
import sdns.serialization.ResourceRecord;
import sdns.serialization.ValidationException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ethan Dickey
 * @author Harrison Rogers
 */
class AAAATest {
    private final long AAAA_TYPE_VALUE = 28L;

    /**
     * Creates a default IPv6 ("0123:4567:89AB:CDEF:0123:4567:89AB:CDEF")
     * @return IPv6
     * @throws UnknownHostException if incorrect IPv6
     */
    private static Inet6Address getDefault0IPv6() throws UnknownHostException {
        return (Inet6Address)Inet6Address.getByName("0123:4567:89AB:CDEF:0123:4567:89AB:CDEF");
    }
    /**
     * Creates a default IPv6 ("0000:0000:0000:0000:0000:0000:0000:0000")
     * @return IPv6
     * @throws UnknownHostException if incorrect IPv6
     */
    private static Inet6Address getDefault1IPv6() throws UnknownHostException {
        return (Inet6Address)Inet6Address.getByName("0000:0000:0000:0000:0000:0000:0000:0000");
    }
    /**
     * Creates a default IPv6 ("FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF")
     * @return IPv6
     * @throws UnknownHostException if incorrect IPv6
     */
    private static Inet6Address getDefault2IPv6() throws UnknownHostException {
        return (Inet6Address)Inet6Address.getByName("FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF");
    }

    /**
     * Trivial test (DONE)
     */
    @Test
    void getTypeValue() {
        AAAA a;
        try {
            a = new AAAA(".", 32, getDefault0IPv6());
            assertEquals(AAAA_TYPE_VALUE, a.getTypeValue());
        } catch(Exception e){
            assert(false);
        }
    }

    /**
     * To string test (DONE)
     */
    @Test
    void testToString() {
        try {
            String expected = "AAAA: name=foo.com. ttl=42 address=" + getDefault0IPv6().getHostAddress();
            AAAA test = new AAAA("foo.com.", 42, getDefault0IPv6());
            String output = test.toString();

            assertEquals(expected, output);
        } catch (ValidationException | UnknownHostException e) {
            fail();
        }
    }


    /**
     * Constructor error tests (DONE)
     */
    @Nested
    class AAAAConstructorErrors {
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
            assertThrows(ValidationException.class, () -> new AAAA(name, 0, getDefault0IPv6()));
        }

        //Null name test
        @Test @DisplayName("Name null")
        void constrNameValidationErrorNull() {
            assertThrows(ValidationException.class, () -> new AAAA(null, 0, getDefault0IPv6()));
        }

        //Null IPv4
        @Test @DisplayName("IPv6 name null ptr")
        void constrIPv4NullPtr() {
            assertThrows(ValidationException.class, () -> new AAAA(".", 0, null));
        }

        //ValidationException TTL tests
        @ParameterizedTest(name = "TTL = {0}")
        @ValueSource(ints = {-1, -2147483648})
        void constTTLValidationError(int ttl){
            assertThrows(ValidationException.class, () -> new AAAA(".", ttl, getDefault0IPv6()));
        }
    }

    /**
     * Valid constructor tests
     */
    @Nested
    class AAAAConstructorValid {
        //Constructor name tests VALID
        @ParameterizedTest(name = "Name = {0}")
        @ValueSource(strings = {"a23456789012345678901234567890123456789012345678901234567890123.",
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a234567890.",//255
                "foo.", "foo.com.", "f0-9.c0m.", "google.com.", "www.baylor.edu.", "f0.c-0.", ".", "f-0.", "f-0a."
        })
        void constrNameValid(String name) {
            AAAA a;
            try {
                a = new AAAA(name, 0, getDefault1IPv6());
                assertEquals(name, a.getName());
            } catch(Exception e){
                assert(false);
            }
        }

        //Constructor canonical name tests VALID
        @ParameterizedTest(name = "IPv6 Name = {0}")
        @ValueSource(strings = {"0000:0000:0000:0000:0000:0000:0000:0000", "0123:4567:89AB:CDEF:0123:4567:89AB:CDEF",
                "FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF"})
        void constrCanNameValid(String name) {
            AAAA a;
            try {
                Inet6Address n = (Inet6Address)Inet6Address.getByName(name);
                a = new AAAA(".", 0, n);
                assertEquals(n, a.getAddress());
            } catch(Exception e){
                assert(false);
            }
        }

        //Constructor TTL tests
        @ParameterizedTest(name = "TTL = {0}")
        @ValueSource(ints = {0, 1, 2147483647})
        void constTTLValid(int ttl){
            AAAA a;
            try {
                a = new AAAA(".", ttl, getDefault2IPv6());
                assertEquals(ttl, a.getTTL());
            } catch(Exception e){
                assert(false);
            }
        }
    }



    /**
     * Test encode
     */
    @Nested
    class ValidEncodeTests {
        ByteArrayOutputStream testStream = new ByteArrayOutputStream();

        //Basic encode test
        @Test @DisplayName("A RR valid encode basic")
        void basicEncodeTest() {
            byte[] comparisonArrNoIP = {3, 102, 111, 111, 3, 99, 111, 109, 0,//foo.com.
                    0, (byte)AAAA_TYPE_VALUE,//type
                    0, 1,//0x0001
                    0, 0, 0, 42,//ttl
                    0, 16};//rdlength

            try {
                byte[] addr = getDefault0IPv6().getAddress();
                byte[] comparisonArr = new byte[comparisonArrNoIP.length + addr.length];
                System.arraycopy(comparisonArrNoIP, 0, comparisonArr, 0, comparisonArrNoIP.length);
                System.arraycopy(addr, 0, comparisonArr, comparisonArrNoIP.length, addr.length);

                System.out.println("Arr: " + Arrays.toString(comparisonArr));
                //A object instantiation here
                AAAA testA = new AAAA("foo.com.", 42, getDefault0IPv6());
                testA.encode(testStream);
                byte[] dumpArray = testStream.toByteArray();
                System.out.println("Arr: " + Arrays.toString(dumpArray));
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
                AAAA testA = new AAAA("foo.com.", 42, getDefault0IPv6());
                assertThrows(NullPointerException.class, () -> testA.encode(null));
            } catch (ValidationException | IOException e) {
                fail();
            }
        }

        //Encode empty name
        @Test @DisplayName("A RR valid encode empty name test")
        void encodeEmptyName() {
            byte[] comparisonArrNoIP = { 0,//.
                    0, (byte)AAAA_TYPE_VALUE,//type
                    0, 1,//0x0001
                    0, 0, 0, 42,//ttl
                    0, 16};//,//rdlength

            try {
                byte[] addr = getDefault0IPv6().getAddress();
                byte[] comparisonArr = new byte[comparisonArrNoIP.length + addr.length];
                System.arraycopy(comparisonArrNoIP, 0, comparisonArr, 0, comparisonArrNoIP.length);
                System.arraycopy(addr, 0, comparisonArr, comparisonArrNoIP.length, addr.length);

                //A object instantiation here
                AAAA testA = new AAAA(".", 42, getDefault0IPv6());
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
            byte[] comparisonArrNoIP = {3, 'f', '-', '9', 0,//foo.com.
                    0, (byte)AAAA_TYPE_VALUE,//type
                    0, 1,//0x0001
                    0, 0, 0, 42,//ttl
                    0, 16};//,//rdlength
            //-64, -88, 0, 69};
            try {
                byte[] addr = getDefault0IPv6().getAddress();
                byte[] comparisonArr = new byte[comparisonArrNoIP.length + addr.length];
                System.arraycopy(comparisonArrNoIP, 0, comparisonArr, 0, comparisonArrNoIP.length);
                System.arraycopy(addr, 0, comparisonArr, comparisonArrNoIP.length, addr.length);

                AAAA testA = new AAAA("f-9.", 42, getDefault0IPv6());
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
                u1 = new AAAA(".", 0, getDefault0IPv6());
                u2 = new AAAA(".", 0, getDefault0IPv6());

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
                u1 = new AAAA(".", 123, (Inet6Address)Inet6Address.getByName("0000:0000:0000:0000:0000:0000:0000:0000"));
                u2 = new AAAA(".", 123, (Inet6Address)Inet6Address.getByName("0001:0000:0000:0000:0000:0000:0000:0000"));

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
                u1 = new AAAA(".", 123, getDefault1IPv6());
                u2 = new AAAA(".", 379, getDefault1IPv6());//this makes 0000 0000 0111 1011 into 0000 0001 0111 1011

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
                u1 = new AAAA("good.com.", 123, getDefault1IPv6());
                u2 = new AAAA("good.com.q.", 123, getDefault1IPv6());

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
                u1 = new AAAA("good.com.", 123, getDefault0IPv6());
                u2 = new AAAA("good.com.", 123, getDefault0IPv6());

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
                u1 = new AAAA("good.com.", 123, getDefault0IPv6());
                u2 = new AAAA("good.com.", 124, getDefault0IPv6());

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
                u1 = new A("good.com.", 123, (Inet4Address) Inet4Address.getByName("0.0.0.0"));
                u2 = new AAAA("good.com.", 123, getDefault1IPv6());

                assertNotEquals(u1.hashCode(), u2.hashCode());
            } catch (ValidationException | UnknownHostException e) {
                assert(false);
            }
        }
    }
}