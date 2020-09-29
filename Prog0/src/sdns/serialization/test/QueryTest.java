//Contains the Query testing class (see comments below)
//Created: 9/18/20
package sdns.serialization.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sdns.serialization.*;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ethan Dickey
 * @author Harrison Rogers
 */
class QueryTest {
    /**
     * Query constructor invalid tests
     */
    @Nested
    class QueryConstructorInvalid {
        //ID test:: unsigned 16 bit integer
        @ParameterizedTest(name = "ID = {0}")
        @ValueSource(ints = {65536, -1, -2147418113})
        void constIdTestInvalid(int id){
            assertThrows(ValidationException.class, () -> new Query(id, "."));
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
            assertThrows(ValidationException.class, () -> new Query(0, query));
        }

        //Null query test
        @Test
        @DisplayName("Null query")
        void constQueryNullTest(){
            assertThrows(ValidationException.class, () -> new Query(0, null));
        }
    }

    /**
     * Query constructor valid tests
     */
    @Nested
    class QueryConstructorValid {
        //Test valid IDs (16 bit unsigned int)
        @ParameterizedTest(name = "ID = {0}")
        @ValueSource(ints = {0, 1, 65535, 65280, 32768, 34952, 34824})
        void constIdTestValid(int id){
            Message q;
            try {
                q = new Query(id, ".");
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
                q = new Query(0, query);
                assertEquals(query, q.getQuery());
            } catch (ValidationException e) {
                assert(false);
            }
        }
    }

    /**
     * Query toString basic test
     */
    @Test
    void testToString() {
        String expected = "Query: id=0 query=.";
        try {
            Query test = new Query(0, ".");
            String output = test.toString();

            assertEquals(expected, output);
        } catch (ValidationException e) {
            fail();
        }
    }

    /**
     * Query equals() basic tests
     */
    @Nested
    class Equals {
        //basic equal test
        @Test
        @DisplayName("Equals test")
        void equality1(){
            Query u1, u2;

            try {
                u1 = new Query(0, "foo.");
                u2 = new Query(0, "foo.");

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
            Query u1, u2;

            try {
                u1 = new Query(123, "good.com.");
                u2 = new Query(123, "good.com.");

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
            Query u1, u2;

            try {
                u1 = new Query(123, "good.com.");
                u2 = new Query(123, "good.com.q.");

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
            Query u1, u2;

            try {
                u1 = new Query(123, "good.com.");
                u2 = new Query(379, "good.com.");//this makes 0000 0000 0111 1011 into 0000 0001 0111 1011

                assertFalse(u1.equals(u2));
                assertFalse(u2.equals(u1));
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //not equal classes
        @Test
        @DisplayName("Equals tests not same class")
        void equality5(){
            Query u1;
            Response u2;

            try {
                u1 = new Query(123, "good.com.");
                u2 = new Response(123, "good.com.");//this makes 0000 0000 0111 1011 into 0000 0001 0111 1011

                assertFalse(u2.equals(u1));
                assertFalse(u1.equals(u2));
            } catch (ValidationException e) {
                assert(false);
            }
        }
    }

    /**
     * Query hashcode() basic tests
     */
    @Nested
    class HashCode {
        //basic HashCode test
        @Test
        @DisplayName("HashCode test")
        void hashCode1(){
            Query u1, u2;

            try {
                u1 = new Query(0, "foo.");
                u2 = new Query(0, "foo.");

                assertEquals(u1.hashCode(), u2.hashCode());
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //Different hashcode test
        @Test
        @DisplayName("HashCode test 2")
        void hashCode2(){
            Query u1, u2;

            try {
                u1 = new Query(123, "good.com.");
                u2 = new Query(123, "good.com.");

                assertEquals(u1.hashCode(), u2.hashCode());
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //not equal query hashcode
        @Test
        @DisplayName("HashCode tests not equal query")
        void hashCode3(){
            Query u1, u2;

            try {
                u1 = new Query(123, "good.com.");
                u2 = new Query(123, "good.com.q.");

                assertNotEquals(u1.hashCode(), u2.hashCode());
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //not equal id hashcode
        @Test
        @DisplayName("HashCode tests not equal id")
        void hashCode4(){
            Query u1, u2;

            try {
                u1 = new Query(123, "good.com.");
                u2 = new Query(379, "good.com.");//this makes 0000 0000 0111 1011 into 0000 0001 0111 1011

                assertNotEquals(u1.hashCode(), u2.hashCode());
            } catch (ValidationException e) {
                assert(false);
            }
        }
    }


    /**
     * Encode tests
     */
    @Nested
    class encodeTest {
        //Basic encode test
        @Test
        @DisplayName("Query valid encode basic")
        void basicEncodeTest() {
            byte[] queryExpect = {
                    0, 42,
                    1, 0,
                    0, 1,
                    0, 0,
                    0, 0,
                    0, 0,
                    3, 'f','o','o',3,'c','o','m',0,
                    0, -1,
                    0, 1
            };

            try {
                byte[] result;
                Query testQuery = new Query(42, "foo.com.");
                result = testQuery.encode();

                assertArrayEquals(queryExpect, result);
            } catch(Exception e) {
                fail("Exception Thrown", e);
            }
        }

        //Encoding with numbers and hyphens
        @Test
        @DisplayName("Query valid encode num and hyphens")
        void numAndHyphenQueryEncode() {
            byte[] queryExpect = {
                    0, 42,
                    1, 0,
                    0, 1,
                    0, 0,
                    0, 0,
                    0, 0,
                    3, 'f','-','9',3,'c','0','m',0,
                    0, -1,
                    0, 1
            };

            try {
                byte[] result;
                Query testQuery = new Query(42, "f-9.c0m.");
                result = testQuery.encode();
                assertArrayEquals(queryExpect, result);
            } catch(Exception e) {
                fail("Exception Thrown", e);
            }
        }
    }
}