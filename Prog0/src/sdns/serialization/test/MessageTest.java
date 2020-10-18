//Contains the MessageTest class (see comments below)
//Created: 9/18/20
package sdns.serialization.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import sdns.serialization.*;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;
/**
 * @author Ethan Dickey
 * @author Harrison Rogers
 */
class MessageTest {
    @Nested
    class DecodeHeaderMalformed {
        @Test
        @DisplayName("Basic header test error short id")
        void headerDecodeMal1(){
            byte[] buff = { 0, //id
                    0, 0, //0 0000 [ignored bit]x4 000 0000
                    0, 1, //0x0001
                    0, 0, //ANCount
                    0, 0, //NSCount
                    0, 0, //ARCount
                    3, 'f', 'o', 'o', -64, 5,//query
                    0, -1,//0x00FF
                    0, 1  //0x0001
            };
            assertThrows(ValidationException.class, () -> Message.decode(buff));
        }
        @Test
        @DisplayName("Basic header test error short flags")
        void headerDecodeMal2(){
            byte[] buff = { 0, 0,//id
                    0,  //0 0000 [ignored bit]x4 000 0000
                    0, 1, //0x0001
                    0, 0, //ANCount
                    0, 0, //NSCount
                    0, 0, //ARCount
                    3, 'f', 'o', 'o', -64, 5,//query
                    0, -1,//0x00FF
                    0, 1  //0x0001
            };
            assertThrows(ValidationException.class, () -> Message.decode(buff));
        }
        @Test
        @DisplayName("Basic header test error bad 0x0001")
        void headerDecodeMal3(){
            byte[] buff = { 0, 0,//id
                    0, 0, //0 0000 [ignored bit]x4 000 0000
                    0, //0x0001
                    0, 0, //ANCount
                    0, 0, //NSCount
                    0, 0, //ARCount
                    3, 'f', 'o', 'o', -64, 5,//query
                    0, -1,//0x00FF
                    0, 1  //0x0001
            };
            assertThrows(ValidationException.class, () -> Message.decode(buff));
        }
        @Test
        @DisplayName("Basic header test error bad 0x0001")
        void headerDecodeMal4(){
            byte[] buff = { 0, 0,//id
                    0, 0, //0 0000 [ignored bit]x4 000 0000
                    1, //0x0001
                    0, 0, //ANCount
                    0, 0, //NSCount
                    0, 0, //ARCount
                    3, 'f', 'o', 'o', -64, 5,//query
                    0, -1,//0x00FF
                    0, 1  //0x0001
            };
            assertThrows(ValidationException.class, () -> Message.decode(buff));
        }
        @Test
        @DisplayName("Basic header test error short ancount")
        void headerDecodeMal5(){
            byte[] buff = { 0, 0,//id
                    0, 0, //0 0000 [ignored bit]x4 000 0000
                    0, 1, //0x0001
                    0, //ANCount
                    0, 0, //NSCount
                    0, 0, //ARCount
                    3, 'f', 'o', 'o', -64, 5,//query
                    0, -1,//0x00FF
                    0, 1  //0x0001
            };
            assertThrows(ValidationException.class, () -> Message.decode(buff));
        }
        @Test
        @DisplayName("Basic header test error short nscount")
        void headerDecodeMal6(){
            byte[] buff = { 0, 0,//id
                    0, 0, //0 0000 [ignored bit]x4 000 0000
                    0, 1, //0x0001
                    0, 0, //ANCount
                    0,  //NSCount
                    0, 0, //ARCount
                    3, 'f', 'o', 'o', -64, 5,//query
                    0, -1,//0x00FF
                    0, 1  //0x0001
            };
            assertThrows(ValidationException.class, () -> Message.decode(buff));
        }
        @Test
        @DisplayName("Basic header test error short arcount")
        void headerDecodeMal7(){
            byte[] buff = { 0, 0,//id
                    0, 0, //0 0000 [ignored bit]x4 000 0000
                    0, 1, //0x0001
                    0, 0, //ANCount
                    0, 0, //NSCount
                    0, //ARCount
                    3, 'f', 'o', 'o', -64, 5,//query
                    0, -1,//0x00FF
                    0, 1  //0x0001
            };
            assertThrows(ValidationException.class, () -> Message.decode(buff));
        }

        @Test
        @DisplayName("Null test")
        void inputStreamNull(){
            assertThrows(NullPointerException.class, () -> Message.decode(null));
        }
    }

    @Nested
    class DecodeQueryMalformed {
        @Test @DisplayName("Basic header test error short query name")
        void queryDecodeMal1(){
            byte[] buff = { 0, 0,//id
                    0, 0, //0 0000 [ignored bit]x4 000 0000
                    0, 1, //0x0001
                    0, 0, //ANCount
                    0, 0, //NSCount
                    0, 0, //ARCount
                    3, 'f', 'o', -64, 5,//query
                    0, -1,//0x00FF
                    0, 1  //0x0001
            };
            assertThrows(ValidationException.class, () -> Message.decode(buff));
        }
        @Test @DisplayName("Basic header test error short query name ending")
        void queryDecodeMal2(){
            byte[] buff = { 0, 0,//id
                    0, 0, //0 0000 [ignored bit]x4 000 0000
                    0, 1, //0x0001
                    0, 0, //ANCount
                    0, 0, //NSCount
                    0, 0, //ARCount
                    3, 'f', 'o', 'o', -64,//query
                    0, -1,//0x00FF
                    0, 1  //0x0001
            };
            assertThrows(ValidationException.class, () -> Message.decode(buff));
        }
        @Test @DisplayName("Basic header test error short query name beginning")
        void queryDecodeMal3(){
            byte[] buff = { 0, 0,//id
                    0, 0, //0 0000 [ignored bit]x4 000 0000
                    0, 1, //0x0001
                    0, 0, //ANCount
                    0, 0, //NSCount
                    0, 0, //ARCount
                    'f', 'o', 'o', -64, 5,//query
                    0, -1,//0x00FF
                    0, 1  //0x0001
            };
            assertThrows(ValidationException.class, () -> Message.decode(buff));
        }
        @Test @DisplayName("Basic header test error short query 0x00FF")
        void queryDecodeMal4(){
            byte[] buff = { 0, 0,//id
                    0, 0, //0 0000 [ignored bit]x4 000 0000
                    0, 1, //0x0001
                    0, 0, //ANCount
                    0, 0, //NSCount
                    0, 0, //ARCount
                    3, 'f', 'o', 'o', -64, 5,//query
                    0,//0x00FF
                    0, 1  //0x0001
            };
            assertThrows(ValidationException.class, () -> Message.decode(buff));
        }
        @Test @DisplayName("Basic header test error short query 0x00FF")
        void queryDecodeMal5(){
            byte[] buff = { 0, 0,//id
                    0, 0, //0 0000 [ignored bit]x4 000 0000
                    0, 1, //0x0001
                    0, 0, //ANCount
                    0, 0, //NSCount
                    0, 0, //ARCount
                    3, 'f', 'o', 'o', -64, 5,//query
                    -1,//0x00FF
                    0, 1  //0x0001
            };
            assertThrows(ValidationException.class, () -> Message.decode(buff));
        }
        @Test @DisplayName("Basic header test error short query 0x0001")
        void queryDecodeMal6(){
            byte[] buff = { 0, 0,//id
                    0, 0, //0 0000 [ignored bit]x4 000 0000
                    0, 1, //0x0001
                    0, 0, //ANCount
                    0, 0, //NSCount
                    0, 0, //ARCount
                    3, 'f', 'o', 'o', -64, 5,//query
                    0, -1,//0x00FF
                    0  //0x0001
            };
            assertThrows(ValidationException.class, () -> Message.decode(buff));
        }
        @Test @DisplayName("Basic header test error short query 0x0001")
        void queryDecodeMal7(){
            byte[] buff = { 0, 0,//id
                    0, 0, //0 0000 [ignored bit]x4 000 0000
                    0, 1, //0x0001
                    0, 0, //ANCount
                    0, 0, //NSCount
                    0, 0, //ARCount
                    3, 'f', 'o', 'o', -64, 5,//query
                    0, -1,//0x00FF
                    1  //0x0001
            };
            assertThrows(ValidationException.class, () -> Message.decode(buff));
        }
    }

    @Nested
    class DecodeValidationException {
        //Basic query test
        @Test @DisplayName("Basic query test invalid 0x0001")
        void queryDecodeInvalid1(){
            byte[] buff = { 0, 0,//id
                    0, 0, //0 0000 [ignored bit]x4 000 0000
                    0, 2, //0x0001
                    0, 0, //ANCount
                    0, 0, //NSCount
                    0, 0, //ARCount
                    3, 'f', 'o', 'o', -64, 5,//query
                    0, -1,//0x00FF
                    0, 1  //0x0001
            };
            assertThrows(ValidationException.class, () -> Message.decode(buff));
        }

        //Invalid 0x0001
        @Test @DisplayName("Basic query test invalid 0x0001")
        void queryDecodeInvalid2(){
            byte[] buff = { 0, 0,//id
                    0, 0, //0 0000 [ignored bit]x4 000 0000
                    1, 1, //0x0001
                    0, 0, //ANCount
                    0, 0, //NSCount
                    0, 0, //ARCount
                    3, 'f', 'o', 'o', -64, 5,//query
                    0, -1,//0x00FF
                    0, 1  //0x0001
            };
            assertThrows(ValidationException.class, () -> Message.decode(buff));
        }

        //Invalid ANCount
        @Test @DisplayName("Basic query test invalid ANCount")
        void queryDecodeInvalid3(){
            byte[] buff = { 0, 0,//id
                    0, 0, //0 0000 [ignored bit]x4 000 0000
                    0, 1, //0x0001
                    0, 1, //ANCount
                    0, 0, //NSCount
                    0, 0, //ARCount
                    3, 'f', 'o', 'o', -64, 5,//query
                    0, -1,//0x00FF
                    0, 1  //0x0001
            };
            assertThrows(ValidationException.class, () -> Message.decode(buff));
        }

        //Invalid NSCount
        @Test @DisplayName("Basic query test invalid NSCount")
        void queryDecodeInvalid4(){
            byte[] buff = { 0, 0,//id
                    0, 0, //0 0000 [ignored bit]x4 000 0000
                    0, 1, //0x0001
                    0, 0, //ANCount
                    0, 1, //NSCount
                    0, 0, //ARCount
                    3, 'f', 'o', 'o', -64, 5,//query
                    0, -1,//0x00FF
                    0, 1  //0x0001
            };
            assertThrows(ValidationException.class, () -> Message.decode(buff));
        }

        //Invalid ARCount
        @Test @DisplayName("Basic query test invalid ARCount")
        void queryDecodeInvalid5(){
            byte[] buff = { 0, 0,//id
                    0, 0, //0 0000 [ignored bit]x4 000 0000
                    0, 1, //0x0001
                    0, 0, //ANCount
                    0, 0, //NSCount
                    0, 1, //ARCount
                    3, 'f', 'o', 'o', -64, 5,//query
                    0, -1,//0x00FF
                    0, 1  //0x0001
            };
            assertThrows(ValidationException.class, () -> Message.decode(buff));
        }

        //Basic query test invalid header byte 1
        @ParameterizedTest(name = "Basic query test invalid header byte 1")
        @ValueSource(bytes = {64, 32, 16, 8})
        void queryDecodeInvalid6(byte in){
            byte[] buff = { 0, 0,//id
                    in, 0, //0 0001 [ignored bit]x4 000 0000
                    0, 1, //0x0001
                    0, 0, //ANCount
                    0, 0, //NSCount
                    0, 1, //ARCount
                    3, 'f', 'o', 'o', -64, 5,//query
                    0, -1,//0x00FF
                    0, 1  //0x0001
            };
            assertThrows(ValidationException.class, () -> Message.decode(buff));
        }

        //Basic query test invalid header byte 2
        @ParameterizedTest(name = "Basic query test invalid header byte 2")
        @ValueSource(bytes = {64, 32, 16, 8, 4, 2, 1})
        void queryDecodeInvalid7(byte in){
            byte[] buff = { 0, 0,//id
                    0, in, //0 0000 [ignored bit]x4 000 0000
                    0, 1, //0x0001
                    0, 0, //ANCount
                    0, 0, //NSCount
                    0, 1, //ARCount
                    3, 'f', 'o', 'o', -64, 5,//query
                    0, -1,//0x00FF
                    0, 1  //0x0001
            };
            assertThrows(ValidationException.class, () -> Message.decode(buff));
        }
    }

    @Nested
    class DecodeValid {
        //Basic test
        @Test @DisplayName("Basic query test")
        void basicQueryDecode(){
            byte[] buff = { 0, 0,//id
                    0, 0, //0 0000 [ignored bit]x7 0000
                    0, 1, //0x0001
                    0, 0, //ANCount
                    0, 0, //NSCount
                    0, 0, //ARCount
                    3, 'f', 'o', 'o', -64, 5,//query
                    0, -1,//0x00FF
                    0, 1  //0x0001
            };
            try {
                Message temp = Message.decode(buff);
                assert temp != null;
                assertAll(() -> assertEquals("foo.", temp.getQuery()),
                        () -> assertEquals(0, temp.getID())
                        );
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //Testing ignoring certain header fields
        @Test @DisplayName("Basic query test (ignore header fields)")
        void basicQueryDecode2(){
            byte[] buff = { 0, 0,//id
                    7, -16, //0 0000 [ignored bit]x7 0000
                    0, 1, //0x0001
                    0, 0, //ANCount
                    0, 0, //NSCount
                    0, 0, //ARCount
                    3, 'f', 'o', 'o', -64, 5,//query
                    0, -1,//0x00FF
                    0, 1  //0x0001
            };
            try {
                Message temp = Message.decode(buff);
                assert temp != null;
                assertAll(() -> assertEquals("foo.", temp.getQuery()),
                        () -> assertEquals(0, temp.getID())
                        );
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //Comprehensive response test
        @Test @DisplayName("Basic response test")
        void basicResponseDecode(){
            byte[] buff = { 0, 0,//id
                    -128, 0, //1 0000 [ignored bit]x7 0000
                    0, 1, //0x0001
                    0, 3, //ANCount
                    0, 2, //NSCount
                    0, 3, //ARCount
                    //query
                    3, 'f', 'o', 'o', -64, 5,
                    0, -1,//0x00FF
                    0, 1,  //0x0001
                    //answer
                    3, 'f', 'o', 'o', -64, 5,//CName
                    0, 5,
                    0, 1, //0x0001
                    0, 0, 0, 0,
                    0, 6,
                    3, 'f', 'o', 'o', -64, 5,

                    0,//A
                    0, 1,
                    0, 1,
                    0, 0, 0, 0,
                    0, 4,
                    -1, 0, -1, -119,

                    -64, 5,//NS
                    0, 2,
                    0, 1, //0x0001
                    0, 0, 0, 0,
                    0, 6,
                    3, 'f', 'o', 'o', -64, 5,

                    //authority
                    -64, 5,//NS
                    0, 2,
                    0, 1, //0x0001
                    0, 0, 0, 0,
                    0, 6,
                    3, 'f', 'o', 'o', -64, 5,

                    3, 'f', 'o', 'o', -64, 5,//NS
                    0, 2,
                    0, 1, //0x0001
                    0, 0, 0, 0,
                    0, 1,
                    0,

                    //additional
                    0,//A
                    0, 1,
                    0, 1,
                    0, 0, 0, 0,
                    0, 4,
                    0, 0, 0, 0,

                    3, 'f', 'o', 'o', -64, 5,//CName
                    0, 5,
                    0, 1, //0x0001
                    0, 0, 0, 0,
                    0, 6,
                    3, 'f', 'o', 'o', -64, 5,

                    -64, 5,//NS
                    0, 2,
                    0, 1, //0x0001
                    0, 0, 0, 0,
                    0, 6,
                    3, 'f', 'o', 'o', -64, 5
            };
            try {
                Message temp = Message.decode(buff);
                assert temp != null;

                NS ns1 = new NS(".", 0, "foo.");
                NS ns2 = new NS("foo.", 0, ".");
                CName cn1 = new CName("foo.", 0, "foo.");
                A a1 = new A(".", 0, (Inet4Address)Inet4Address.getByName("255.0.255.137"));
                A a2 = new A(".", 0, (Inet4Address)Inet4Address.getByName("0.0.0.0"));

                assertAll(() -> assertEquals(Response.class, temp.getClass()),
                        () -> assertEquals("foo.", temp.getQuery()),
                        () -> assertEquals(0, temp.getID()),
                        () -> assertEquals(ns1, ((Response)temp).getNameServerList().get(0)),
                        () -> assertEquals(ns2, ((Response)temp).getNameServerList().get(1)),
                        () -> assertEquals(ns1, ((Response)temp).getAnswerList().get(2)),
                        () -> assertEquals(ns1, ((Response)temp).getAdditionalList().get(2)),

                        () -> assertEquals(cn1, ((Response)temp).getAnswerList().get(0)),
                        () -> assertEquals(cn1, ((Response)temp).getAdditionalList().get(1)),

                        () -> assertEquals(a1, ((Response)temp).getAnswerList().get(1)),
                        () -> assertEquals(a2, ((Response)temp).getAdditionalList().get(0))
                        );
            } catch (ValidationException | UnknownHostException e) {
                assert(false);
            }
        }

        //Basic response test ignoring certain header fields
        @Test @DisplayName("Basic response test (ignore header fields)")
        void basicResponseDecode2(){
            byte[] buff = { 0, 0,//id
                    -121, -16, //1 0000 [ignored bit]x7 0000
                    0, 1, //0x0001
                    0, 3, //ANCount
                    0, 0, //NSCount
                    0, 0, //ARCount
                    //query
                    3, 'f', 'o', 'o', -64, 5,
                    0, -1,//0x00FF
                    0, 1,  //0x0001
                    //answer
                    3, 'f', 'o', 'o', -64, 5,//CName
                    0, 5,
                    0, 1, //0x0001
                    0, 0, 0, 0,
                    0, 6,
                    3, 'f', 'o', 'o', -64, 5,

                    0,//A
                    0, 1,
                    0, 1,
                    0, 0, 0, 0,
                    0, 4,
                    -1, 0, -1, -119,

                    -64, 5,//NS
                    0, 2,
                    0, 1, //0x0001
                    0, 0, 0, 0,
                    0, 6,
                    3, 'f', 'o', 'o', -64, 5
            };
            try {
                Message temp = Message.decode(buff);
                assert temp != null;

                NS ns1 = new NS(".", 0, "foo.");
                CName cn1 = new CName("foo.", 0, "foo.");
                A a1 = new A(".", 0, (Inet4Address)Inet4Address.getByName("255.0.255.137"));

                assertAll(() -> assertEquals(Response.class, temp.getClass()),
                        () -> assertEquals("foo.", temp.getQuery()),
                        () -> assertEquals(0, temp.getID()),
                        () -> assertEquals(ns1, ((Response)temp).getAnswerList().get(2)),
                        () -> assertEquals(cn1, ((Response)temp).getAnswerList().get(0)),
                        () -> assertEquals(a1, ((Response)temp).getAnswerList().get(1)));
            } catch (ValidationException | UnknownHostException e) {
                assert(false);
            }
        }
    }

    //test getter and setter for id
    @Nested
    class GetSetID {
        //ID test:: unsigned 16 bit integer
        @ParameterizedTest(name = "ID = {0}")
        @ValueSource(ints = {65536, -1, -2147418113})
        void setIdInvalid(int id){
            Message q;
            try {
                q = new Response(0, ".");
                assertThrows(ValidationException.class, () -> q.setID(id));
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //Test valid IDs (16 bit unsigned int)
        @ParameterizedTest(name = "ID = {0}")
        @ValueSource(ints = {0, 1, 65535, 65280, 32768, 34952, 34824})
        void setIdValid(int id){
            Message q;
            try {
                q = new Response(0, ".");
                q.setID(id);
                assertEquals(id, q.getID());
            } catch (ValidationException e) {
                assert(false);
            }
        }
    }

    //test getter and setter for query
    @Nested
    class GetSetQuery {
        //Yay another domain name test
        @ParameterizedTest(name = "Query = {0}")
        @ValueSource(strings = {"", "asdf", "asdf.].", "asdf.Ƞ.", "asdf..", "www.baylor.edu/", "..", "asdf.asdf", "f0-9.c0m-.", "Ẵ.Ẓ.㛃.⭐.⭕.",
                "-a.f", "-.", "-",
                "a234567890123456789012345678901234567890123456789012345678901234.",//64
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a2345678901."//256
        })
        void setQueryInvalid(String query){
            Message q;
            try {
                q = new Response(0, ".");
                assertThrows(ValidationException.class, () -> q.setQuery(query));
            } catch (ValidationException e) {
                assert(false);
            }
        }

        //null test
        @Test
        @DisplayName("Null query")
        void setQueryNull(){
            Message q;
            try {
                q = new Response(0, ".");
                assertThrows(ValidationException.class, () -> q.setQuery(null));
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
        void setQueryValid(String query){
            Message q;
            try {
                q = new Response(0, ".");
                q.setQuery(query);
                assertEquals(query, q.getQuery());
            } catch (ValidationException e) {
                assert(false);
            }
        }
    }
}