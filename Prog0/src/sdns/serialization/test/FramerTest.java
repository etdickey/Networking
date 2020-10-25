//Contains the FramerTest class (see comments below)
//Created: 10/25/20
package sdns.serialization.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import sdns.serialization.*;


import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ethan Dickey
 * @author Harrison Rogers
 */
public class FramerTest {
    /**
     * Test frame message
     */
    @Nested
    class TestFrameMessage {
        /**
         * Null check
         */
        @Test @DisplayName("Null error")
        void testNull(){
            assertThrows(NullPointerException.class, () -> Framer.frameMsg(null));
        }

        /**
         * Valid tests
         */
        @Nested
        class TestValidMessages {
            /**
             * Frames a byte array and validates that it was performed correctly
             * @param buff byte array to frame
             */
            void frameMessageAndValidate(byte[] buff){
                //Frame it
                byte[] framed = null;
                try {
                    framed = Framer.frameMsg(buff);
                } catch (ValidationException e) {
                    fail();
                }

                int lengthAdded = (framed[0] << 8) + (framed[1]);

                //Check length added was correct
                assertEquals(buff.length, lengthAdded);

                //Check the rest of the byte array was preserved
                assertArrayEquals(buff, Arrays.copyOfRange(framed, 2, framed.length));
            }

            /**
             * Basic test
             */
            @Test @DisplayName("Basic encoding test")
            void basicTest(){
                //Get encoded arr
                byte[] buff = { 1, 2, 3, 4, 5, 99, 'a', '!', '/', '\n', '\"'};
                frameMessageAndValidate(buff);
            }

            /**
             * Complex test with a response message
             */
            @Test @DisplayName("Complex Response test")
            void testComplexResponse(){
                Response r = null;
                try {
                    r = new Response(0, ".", RCode.NOERROR);
                    CName cn = new CName(".", 5, "good.com.");
                    CName cn2 = new CName(".", 6, "good.com.");
                    NS ns = new NS(".", 123, "good.server.");
                    A a = new A(".", 123, (Inet4Address)Inet4Address.getByName("0.0.0.0"));
                    r.addAnswer(cn);
                    r.addAnswer(ns);
                    r.addAnswer(cn2);
                    r.addAnswer(a);
                } catch (ValidationException | UnknownHostException e) {
                    fail();
                }

                //Get encoded arr
                byte[] buff = r.encode();
                frameMessageAndValidate(buff);
            }
        }

        /**
         * Invalid tests
         */
        @Nested
        class TestInvalidMessages {
            /**
             * Too long
             */
            @ParameterizedTest(name = "Test too long of a message: {0}")
            @ValueSource(ints = {65536, 70000, 1000000})
            void testTooLongMessage(){
                byte[] veryLongArray = new byte[65536];
                assertThrows(ValidationException.class, () -> Framer.frameMsg(veryLongArray));
            }
        }
    }

    /**
     * Test nextMsg
     */
    @Nested
    class TestNextMsg {
        /**
         * Null check
         */
        @Test @DisplayName("Null error")
        void testNull(){
            assertThrows(NullPointerException.class, () -> Framer.nextMsg(null));
        }

        /**
         * Generates a random byte array for testing (it's a detectable pattern because tests should be consistent)
         * @param size size of array to make
         * @return random byte array
         */
        private byte[] generateRandomByteArray(int size){
            byte[] message;
            switch(size){
                case 18: message = new byte[]{3, 'f', 'o', 'o', 0,
                        0, 5,//type
                        0, 1,//0x0001
                        0, 0,//ttl
                        0, 5,//rdlen
                        3, 'f', 'o', 'o', 0
                    };
                    assertEquals(18, message.length);
                    break;
                default:
                    message = new byte[size];
                    for(int i=0;i<size;i++){
                        message[i] = (byte) (255 - (i%256));//any valid byte from 255 -> 0
                    }
            }
            return message;
        }

        /**
         * Puts a 2-byte, unsigned, big-endian int size in front of the array
         * @param message message to prepend a size to
         * @param size    size to put (DOESN'T HAVE TO BE THE ACTUAL SIZE OF THE MESSAGE)
         * @return a byte[] with 2 bytes of size prepended to message
         */
        private byte[] putHeader(byte[] message, int size){
            //Put length header in message
            byte[] buff = new byte[message.length + 2];
            buff[0] = (byte) (size >> 8);
            buff[1] = (byte) (size);
            if(size > 0){
                System.arraycopy(message, 0, buff, 2, message.length);
            }
            return buff;
        }

        /**
         * Valid tests
         */
        @Nested
        class TestValidMessages {
            /**
             * Test lots of types of values
             *  (18 is a sentinel value to the random byte generator to generate a CName RR)
             * @param size size of the array to generate/test
             */
            @ParameterizedTest(name = "Size: {0}")
            @ValueSource(ints = {0, 1, 10, 18, 20, 100, 255, 256, 65535})
            void testTooShort(int size){
                //make byte array of size actual
                byte[] message = generateRandomByteArray(size);

                //Put message in buffer
                byte[] buff = putHeader(message, size);

                //Send buffer to Framer
                byte[] results = null;
                try {
                    results = Framer.nextMsg(new ByteArrayInputStream(buff));
                } catch (IOException e) {
                    fail();
                }

                //Validate
                assertArrayEquals(message, results);
            }
        }

        /**
         * Invalid tests
         */
        @Nested
        class TestInvalidMessages {
            /**
             * Test length at beginning being longer than actual array length
             * @param given value to prepend to array (has to be bigger than actual)
             * @param actual the actual size of the array to generate (has to be smaller than given)
             */
            @ParameterizedTest(name = "Too short:: given: {0}, actual: {1}")
            @CsvSource({"10,9", "1,0", "256,255", "100,0", "-1,32"})
            void testTooShort(int given, int actual){
                //make byte array of size actual
                byte[] message = generateRandomByteArray(actual);

                //Put message in buffer
                byte[] buff = putHeader(message, given);

                //Send buffer to Framer
                assertThrows(EOFException.class, () -> Framer.nextMsg(new ByteArrayInputStream(buff)));
            }
        }
    }
}
