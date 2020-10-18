//Contains the ResponseTest class (see comments below)
//Created: 9/18/20
package sdns.serialization.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sdns.serialization.*;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
/**
 * @author Ethan Dickey
 * @author Harrison Rogers
 */
class ResponseTest {
    /**
     * Response toString basic test
     */
    @Test
    void testToString() {
        String expected = "Response: id=500 query=ns.com. " +
                "answers=[CName: name=a.com. ttl=0 canonicalname=., A: name=b.com. ttl=0 address=0.0.0.0] " +
                "nameservers=[NS: name=ns.com. ttl=0 nameserver=ns1.com.] additionals=[]";
        try {
            Response test = new Response(500, "ns.com.");
            test.addAnswer(new CName("a.com.", 0, "."));
            test.addAnswer(new A("b.com.", 0, (Inet4Address)Inet4Address.getByName("0.0.0.0")));
            test.addNameServer(new NS("ns.com.", 0, "ns1.com."));

            String output = test.toString();

            assertEquals(expected, output);
        } catch (ValidationException | UnknownHostException e) {
            fail();
        }
    }

    /**
     * Test adder error conditions
     */
    @Nested
    class ResponseAddToListErrors {
        //Null add additional error
        @Test
        @DisplayName("Test add additional null")
        void testNullAddAdditional(){
            Response r;
            try {
                r = new Response(0, ".");
                assertThrows(ValidationException.class, () -> r.addAdditional(null));
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //Null add answer error
        @Test
        @DisplayName("Test add answer null")
        void testNullAddAnswerList(){
            Response r;
            try {
                r = new Response(0, ".");
                assertThrows(ValidationException.class, () -> r.addAnswer(null));
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //Null add name server error
        @Test
        @DisplayName("Test add name server null")
        void testNullAddNameServer(){
            Response r;
            try {
                r = new Response(0, ".");
                assertThrows(ValidationException.class, () -> r.addNameServer(null));
            } catch (ValidationException e) {
                assert(false);
            }
        }
    }

    /**
     * Test adders and getters valid
     */
    @Nested
    class ResponseAdderGetterValid {
        //Test addAdditional
        @Test
        @DisplayName("Test addAdditional")
        void addAdditionalValid(){
            Response r;
            try {
                r = new Response(0, ".");
                CName cn = new CName(".", 123, "good.com.");
                CName cn2 = new CName(".", 123, "good.com.q.");
                NS ns = new NS(".", 123, "good.server.");
                A a = new A(".", 123, (Inet4Address)Inet4Address.getByName("0.0.0.0"));
                r.addAdditional(cn);
                r.addAdditional(ns);
                r.addAdditional(cn2);
                r.addAdditional(a);

                List<ResourceRecord> all = r.getAdditionalList();
                assertNotNull(all);
                assertAll("Records",
                        () -> assertEquals(cn, all.get(0)),
                        () -> assertEquals(ns, all.get(1)),
                        () -> assertEquals(cn2, all.get(2)),
                        () -> assertEquals(a, all.get(3))
                        );
            } catch (ValidationException | UnknownHostException e) {
                assert(false);
            }
        }
        //Test addAdditional ignore duplicates
        @Test
        @DisplayName("Test addAdditional ignore duplicates")
        void addAdditionalDuplicateValid(){
            Response r;
            try {
                r = new Response(0, ".");
                CName cn1 = new CName(".", 123, "good.com.");
                CName cn2 = new CName(".", 123, "good.com.");
                CName cn3 = new CName(".", 123, "good.com.q.");
                CName cn4 = new CName(".", 123, "good.com.q.");
                NS ns1 = new NS(".", 123, "good.server.");
                NS ns2 = new NS(".", 123, "good.server.");
                A a1 = new A(".", 123, (Inet4Address)Inet4Address.getByName("0.0.0.0"));
                A a2 = new A(".", 123, (Inet4Address)Inet4Address.getByName("0.0.0.0"));
                r.addAdditional(cn1);
                r.addAdditional(cn2);
                r.addAdditional(cn3);
                r.addAdditional(cn4);
                r.addAdditional(ns1);
                r.addAdditional(ns2);
                r.addAdditional(a1);
                r.addAdditional(a2);

                List<ResourceRecord> all = r.getAdditionalList();
                assertNotNull(all);
                assertAll("Records",
                        () -> assertEquals(cn1, all.get(0)),
                        () -> assertEquals(cn3, all.get(1)),
                        () -> assertEquals(ns1, all.get(2)),
                        () -> assertEquals(a1, all.get(3)),
                        () -> assertEquals(4, all.size())
                );
            } catch (ValidationException | UnknownHostException e) {
                assert(false);
            }
        }

        //Test addAnswer
        @Test
        @DisplayName("Test addAnswer")
        void addAnswerValid(){
            Response r;
            try {
                r = new Response(0, ".");
                CName cn = new CName(".", 123, "good.com.");
                CName cn2 = new CName(".", 123, "good.com.q.");
                NS ns = new NS(".", 123, "good.server.");
                A a = new A(".", 123, (Inet4Address)Inet4Address.getByName("0.0.0.0"));
                r.addAnswer(cn);
                r.addAnswer(ns);
                r.addAnswer(cn2);
                r.addAnswer(a);

                List<ResourceRecord> all = r.getAnswerList();
                assertNotNull(all);
                assertAll("Records",
                        () -> assertEquals(cn, all.get(0)),
                        () -> assertEquals(ns, all.get(1)),
                        () -> assertEquals(cn2, all.get(2)),
                        () -> assertEquals(a, all.get(3))
                        );
            } catch (ValidationException | UnknownHostException e) {
                assert(false);
            }
        }

        //Test addAnswer ignore duplicates
        @Test
        @DisplayName("Test addAnswer ignore duplicates")
        void addAnswerDuplicateValid(){
            Response r;
            try {
                r = new Response(0, ".");
                CName cn1 = new CName(".", 123, "good.com.");
                CName cn2 = new CName(".", 123, "good.com.");
                CName cn3 = new CName(".", 123, "good.com.q.");
                CName cn4 = new CName(".", 123, "good.com.q.");
                NS ns1 = new NS(".", 123, "good.server.");
                NS ns2 = new NS(".", 123, "good.server.");
                A a1 = new A(".", 123, (Inet4Address)Inet4Address.getByName("0.0.0.0"));
                A a2 = new A(".", 123, (Inet4Address)Inet4Address.getByName("0.0.0.0"));
                r.addAnswer(cn1);
                r.addAnswer(cn2);
                r.addAnswer(cn3);
                r.addAnswer(cn4);
                r.addAnswer(ns1);
                r.addAnswer(ns2);
                r.addAnswer(a1);
                r.addAnswer(a2);

                List<ResourceRecord> all = r.getAnswerList();
                assertNotNull(all);
                assertAll("Records",
                        () -> assertEquals(cn1, all.get(0)),
                        () -> assertEquals(cn3, all.get(1)),
                        () -> assertEquals(ns1, all.get(2)),
                        () -> assertEquals(a1, all.get(3)),
                        () -> assertEquals(4, all.size())
                        );
            } catch (ValidationException | UnknownHostException e) {
                assert(false);
            }
        }

        //Test addNameServer
        @Test
        @DisplayName("Test addNameServer")
        void addNameServerValid(){
            Response r;
            try {
                r = new Response(0, ".");
                NS ns1 = new NS(".", 123, "good.server.");
                NS ns2 = new NS(".", 123, "good.server.a1.");
                r.addNameServer(ns1);
                r.addNameServer(ns2);

                List<ResourceRecord> all = r.getNameServerList();
                assertNotNull(all);
                assertAll("Records",
                        () -> assertEquals(ns1, all.get(0)),
                        () -> assertEquals(ns2, all.get(1))
                        );
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //Test addNameServer ignore duplicates
        @Test
        @DisplayName("Test addNameServer ignore duplicates")
        void addNameServerDuplicateValid(){
            Response r;
            try {
                r = new Response(0, ".");
                NS ns1 = new NS(".", 123, "good.server.");
                NS ns2 = new NS(".", 123, "good.server.");
                NS ns3 = new NS(".", 356, "good.server.a1.");
                NS ns4 = new NS(".", 356, "good.server.a1.");
                r.addNameServer(ns1);
                r.addNameServer(ns2);
                r.addNameServer(ns3);
                r.addNameServer(ns4);

                List<ResourceRecord> all = r.getNameServerList();
                assertNotNull(all);
                assertAll("Records",
                        () -> assertEquals(ns1, all.get(0)),
                        () -> assertEquals(ns3, all.get(1)),
                        () -> assertEquals(2, all.size())
                        );
            } catch (ValidationException e) {
                assert(false);
            }
        }
    }

    /**
     * Test get set response code
     */
    @Nested
    class ResponseGetSetResponseCode extends RCodeSetGetAbstractTest {
        /**
         * Factory method for calling the appropriate function you want to test for rcode validity
         *
         * @param rcode rcode to call with
         * @return the result of a getRCode on the respective object
         * @throws ValidationException if invalid rcode
         */
        @Override
        protected RCode callSetGetRCode(int rcode) throws ValidationException {
            Response r = null;
            try {
                r = new Response(0, ".");
            } catch (ValidationException e) {
                assert(false);
            }

            r.setResponseCode(rcode);
            return r.getResponseCode();
        }
    }

    /**
     * Response constructor invalid tests (DONE)
     */
    @Nested
    class ResponseConstructorInvalid {
        //ID test:: unsigned 16 bit integer
        @ParameterizedTest(name = "ID = {0}")
        @ValueSource(ints = {65536, -1, -2147418113})
        void constIdTestInvalid(int id){
            assertThrows(ValidationException.class, () -> new Response(id, "."));
        }

        //Yay another domain name test
        @ParameterizedTest(name = "Query = {0}")
        @ValueSource(strings = {"", "asdf", "asdf.].", "asdf.Ƞ.", "asdf..", "www.baylor.edu/", "..", "asdf.asdf", "f0-9.c0m-.", "Ẵ.Ẓ.㛃.⭐.⭕.",
                "-a.f", "-.", "-",
                "a234567890123456789012345678901234567890123456789012345678901234.",//64
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a2345678901."//256
        })
        void constQueryTestInvalid(String query){
            assertThrows(ValidationException.class, () -> new Response(0, query));
        }

        //Null query test
        @Test
        @DisplayName("Null query")
        void constQueryNullTest(){
            assertThrows(ValidationException.class, () -> new Response(0, null));
        }
    }

    /**
     * Response constructor valid tests (DONE)
     */
    @Nested
    class ResponseConstructorValid {
        //Test valid IDs (16 bit unsigned int)
        @ParameterizedTest(name = "ID = {0}")
        @ValueSource(ints = {0, 1, 65535, 65280, 32768, 34952, 34824})
        void constIdTestValid(int id){
            Message q;
            try {
                q = new Response(id, ".");
                assertEquals(id, q.getID());
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //Valid queries (domain name tests)
        @ParameterizedTest(name = "Query = {0}")
        @ValueSource(strings = {"a23456789012345678901234567890123456789012345678901234567890123.",
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a234567890.",//255
                "foo.", "foo.com.", "f0-9.c0m.", "google.com.", "www.baylor.edu.", "f0.c-0.", ".", "f-0.", "f-0a."
        })
        void constQueryTestValue(String query){
            Message q;
            try {
                q = new Response(0, query);
                assertEquals(query, q.getQuery());
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //Test that all fields are initialized
        @Test
        @DisplayName("Testing other fields are initialized")
        void constInitializationTest(){
            Response r;
            try {
                r = new Response(0, ".");
                assertNotNull(r);
                assertAll("other fields", () -> assertEquals(0, r.getAdditionalList().size()),
                        () -> assertEquals(0, r.getAnswerList().size()),
                        () -> assertEquals(0, r.getNameServerList().size()),
                        () -> assertEquals(RCode.NOERROR, r.getResponseCode())
                        );
            } catch (ValidationException | NullPointerException e) {
                assert(false);
            }
        }
    }


    /**
     * Response equals() basic tests
     */
    @Nested
    static
    class Equals {
        //basic equal test
        @Test
        @DisplayName("Equals test")
        void equality1(){
            Response u1, u2;

            try {
                u1 = new Response(0, "foo.");
                u2 = new Response(0, "foo.");

                assertTrue(u1.equals(u2));
                assertTrue(u2.equals(u1));
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //not not equal based on presumed testing from professor
        @Test
        @DisplayName("Equals tests not not equals")
        void equality2(){
            Response u1, u2;

            try {
                u1 = new Response(123, "good.com.");
                u2 = new Response(123, "good.com.");

                assertFalse(!u1.equals(u2));
                assertFalse(!u2.equals(u1));
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //not equal query
        @Test
        @DisplayName("Equals tests not equals")
        void equality3(){
            Response u1, u2;

            try {
                u1 = new Response(123, "good.com.");
                u2 = new Response(123, "good.com.q.");

                assertFalse(u1.equals(u2));
                assertFalse(u2.equals(u1));
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //not equal id
        @Test
        @DisplayName("Equals tests not equals")
        void equality4(){
            Response u1, u2;

            try {
                u1 = new Response(123, "good.com.");
                u2 = new Response(379, "good.com.");//this makes 0000 0000 0111 1011 into 0000 0001 0111 1011

                assertFalse(u1.equals(u2));
                assertFalse(u2.equals(u1));
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //not equal classes
        //done in Query

        //complex equal test
        @Test
        @DisplayName("Equals test")
        void equality5(){
            Response u1, u2;

            try {
                u1 = new Response(0, "foo.");
                u2 = new Response(0, "foo.");

                CName cn = new CName(".", 123, "good.com.");
                CName cn2 = new CName(".", 123, "good.com.q.");
                NS ns = new NS(".", 123, "good.server.");
                A a = new A(".", 123, (Inet4Address)Inet4Address.getByName("0.0.0.0"));
                NS ns1 = new NS(".", 123, "good.server.");
                NS ns2 = new NS(".", 123, "good.server.a1.");
                complexEqualsHelper(u1, cn, cn2, ns, a, ns1, ns2);

                complexEqualsHelper(u2, cn, cn2, ns, a, ns1, ns2);

                assertTrue(u1.equals(u2));
                assertTrue(u2.equals(u1));
            } catch (ValidationException | UnknownHostException e) {
                assert(false);
            }
        }

        //helper setter for complex objects
        static void complexEqualsHelper(Response u1, CName cn, CName cn2, NS ns, A a, NS ns1, NS ns2) throws ValidationException {
            u1.addNameServer(ns1);
            u1.addNameServer(ns2);
            u1.addAdditional(cn);
            u1.addAdditional(ns);
            u1.addAdditional(cn2);
            u1.addAdditional(a);
            u1.addAnswer(cn);
            u1.addAnswer(ns);
            u1.addAnswer(cn2);
            u1.addAnswer(a);
        }
    }

    /**
     * Response hashcode() basic tests
     */
    @Nested
    class HashCode {
        //basic HashCode test
        @Test
        @DisplayName("HashCode test")
        void hashCode1(){
            Response u1, u2;

            try {
                u1 = new Response(0, "foo.");
                u2 = new Response(0, "foo.");

                assertEquals(u1.hashCode(), u2.hashCode());
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //Different hashcode test
        @Test
        @DisplayName("HashCode test 2")
        void hashCode2(){
            Response u1, u2;

            try {
                u1 = new Response(123, "good.com.");
                u2 = new Response(123, "good.com.");

                assertEquals(u1.hashCode(), u2.hashCode());
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //not equal query hashcode
        @Test
        @DisplayName("HashCode tests not equal query")
        void hashCode3(){
            Response u1, u2;

            try {
                u1 = new Response(123, "good.com.");
                u2 = new Response(123, "good.com.q.");

                assertNotEquals(u1.hashCode(), u2.hashCode());
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //not equal id hashcode
        @Test
        @DisplayName("HashCode tests not equal id")
        void hashCode4(){
            Response u1, u2;

            try {
                u1 = new Response(123, "good.com.");
                u2 = new Response(379, "good.com.");//this makes 0000 0000 0111 1011 into 0000 0001 0111 1011

                assertNotEquals(u1.hashCode(), u2.hashCode());
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //not equal id hashcode
        @Test
        @DisplayName("HashCode tests complex equals")
        void hashCode5(){
            Response u1, u2;

            try {
                u1 = new Response(123, "good.com.");
                u2 = new Response(123, "good.com.");

                CName cn = new CName(".", 123, "good.com.");
                CName cn2 = new CName(".", 123, "good.com.q.");
                NS ns = new NS(".", 123, "good.server.");
                A a = new A(".", 123, (Inet4Address)Inet4Address.getByName("0.0.0.0"));
                NS ns1 = new NS(".", 123, "good.server.");
                NS ns2 = new NS(".", 123, "good.server.a1.");
                complexEqualsHelper(u1, cn, cn2, ns, a, ns1, ns2);

                complexEqualsHelper(u2, cn, cn2, ns, a, ns1, ns2);

                assertEquals(u1.hashCode(), u2.hashCode());
            } catch (ValidationException | UnknownHostException e) {
                assert(false);
            }
        }

        //helper setter functions
        private void complexEqualsHelper(Response u1, CName cn, CName cn2, NS ns, A a, NS ns1, NS ns2) throws ValidationException {
            Equals.complexEqualsHelper(u1, cn, cn2, ns, a, ns1, ns2);
        }
    }

    /**
     * Encode tests
     */
    @Nested
    class encodeTest {
        //Basic response encode test
        @Test
        @DisplayName("Response valid encode basic")
        void basicEncodeTest() {
            byte[] responseExpect = {
                    0, 42,
                    -127, 0,
                    0, 1,
                    0, 2,
                    0, 2,
                    0, 2,
                    3,'w','w','w', 3,'f','o','o',3,'c','o','m',0,
                    0, -1,
                    0, 1,
                    3, 'w', 'w', 'w',3,'f','o','o',3,'c','o','m', 0,
                    0, 5,
                    0, 1,
                    0, 0, 0, -56,
                    0, 9,
                    3,'f','o','o',3,'c','o','m',0,
                    3,'w','w','w',3,'f','o','o',3,'c','o','m',0,
                    0, 1,
                    0, 1,
                    0,0,0,-56,
                    0,4,
                    20,69,42,21,
                    3,'f','o','o',3,'c','o','m',0,
                    0,2,
                    0,1,
                    0,0,0,-56,
                    0, 12,
                    2,'n','s',3,'f','o','o',3,'c','o','m',0,
                    3,'f','o','o',3,'c','o','m',0,
                    0,2,
                    0,1,
                    0,0,0,-56,
                    0,13,
                    3,'n','s','2',3,'f','o','o',3,'c','o','m',0,
                    2,'n','s',3,'f','o','o',3,'c','o','m',0,
                    0,1,
                    0,1,
                    0,0,0,-56,
                    0,4,
                    100,4,5,6,
                    3,'n','s','2',3,'f','o','o',3,'c','o','m',0,
                    0,1,
                    0,1,
                    0,0,0,-56,
                    0,4,
                    100,4,5,7
            };

            try {
                byte[] result;
                Response testResponse = new Response(42, "www.foo.com.");
                testResponse.addAnswer(new CName("www.foo.com.", 200, "foo.com."));
                testResponse.addAnswer(new A("www.foo.com.",200,(Inet4Address)Inet4Address.getByName("20.69.42.21")));
                testResponse.addNameServer(new NS("foo.com.", 200, "ns.foo.com."));
                testResponse.addNameServer(new NS("foo.com.", 200, "ns2.foo.com."));
                testResponse.addAdditional(new A("ns.foo.com.", 200, (Inet4Address)Inet4Address.getByName("100.4.5.6")));
                testResponse.addAdditional(new A("ns2.foo.com.", 200, (Inet4Address)Inet4Address.getByName("100.4.5.7")));
                result = testResponse.encode();
                assertArrayEquals(responseExpect, result);
            } catch(Exception e) {
                fail("Exception Thrown", e);
            }
        }
    }
}