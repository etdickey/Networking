//Contains the Resource Record testing class (see comments below)
//Created: 9/8/20
package sdns.serialization.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import sdns.serialization.*;
import sdns.serialization.test.factories.DomainNameTestFactory;
import sdns.serialization.test.factories.PreferenceTestFactory;
import sdns.serialization.test.factories.TTLTestFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

//Note: rerun all tests with different character encodings (UTF-32, UTF-16, etc) at runtime
/**
 * @author Ethan Dickey
 * @author Harrison Rogers
 */
class ResourceRecordTest {
    /**
     * Serializes the given domain name.  Does not check that name is a valid domain name.
     * @param name the domain name to validate
     * @param b the byte arraylist to append to
     */
    private static void serialize(String name, ArrayList<Byte> b) {
        if(name.length() == 1 && name.charAt(0) == '.'){
            b.add((byte)0);
            return;
        }

        if(name.length() == 0){
            return;
        }

        for(int i=0;i<name.length();i++){
            if((name.charAt(i) == '.' && i != name.length()-1) || i == 0){
                byte len = 0;
                //find how long next segment is
                for(int j=i+1;j<name.length();j++){
                    if(name.charAt(j) == '.'){
                        len = (byte)(j-i-1);
                        break;
                    }
                }
                if(i == 0 && name.length() != 1){
                    len++;
                }
                if(len == 0){//two periods in a row
                    b.add((byte)0);
                } else {// if(len > 0)
                    b.add(len);
                }

//                if(len == -1){//at the end without finding a period
//                    //just end the serialization
//                    break;
//                }

                if(i == 0){//insert the first letter
                    b.add((byte) name.charAt(0));
                }
            } else {
                if(i != name.length()-1){
                    b.add((byte) name.charAt(i));
                }
            }
        }

        //Add the final '0' to signal the end of the array
        b.add((byte)0);
    }

    /**
     * Concatenates two byte arrays
     * @param a first arr
     * @param b second arr
     * @return concatenated byte arrays
     */
    private static byte[] concatByteArrs(byte[] a, byte[] b){
        //ALTERNATIVE 1 (kept _intentionally_ for reference)
//        byte[] buff = new byte[a.length + b.length];
//        System.arraycopy(a, 0, buff, 0, a.length);
//        System.arraycopy(b, 0, buff, a.length, b.length);
//        return buff;

        //ALTERNATIVE 2 (more elegant and extendable)
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        try {
            outputStream.write( a );
            outputStream.write( b );
        } catch (IOException e) {
            fail();
        }
        return outputStream.toByteArray();
    }

    //Test invalid input stream
    //  Too short in any field
    //  Too long in any field
    //Name:
    //  Each label must start with a letter, end with a letter or digit, and have as interior characters only letters
    //    (A-Z and a-z), digits (0-9), and hypen (-).
    //  A name with a single, empty label (".") is acceptable
    //
    //Test each field
    //  input too long, too short, null, invalid characters
    //  case insensitivity
    /**
     * Tests invalid decodes (DONE)
     */
    @Nested
    class DecodeValidationErrorInvalid {
        /*
        Valid data:
        byte[] buff = { 5, -64, 111, 111, 102, 3,//"foo."
                        2, 0,
                        1, 0,
                        0, 0, 0, 0,
                        0, 6,
                        5, -64, 111, 111, 102, 3};//"foo."
         */
        //Bad length name short
        @Test @DisplayName("Bad length name long")
        void decodeValidationError1(){
            //foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            byte[] buff = { 3, 'f', 'o', 'o', 'o', -64, 5,
                            0, 2,
                            0, 1,
                            0, 0, 0, 0,
                            0, 6,
                            3, 'f', 'o', 'o', -64, 5};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(EOFException.class, () -> ResourceRecord.decode(b));
        }

        //Bad length name short
        @Test
        @DisplayName("Bad length name short")
        void decodeValidationError2(){
            //foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            byte[] buff = { 3, 'o', 'o', -64, 5,
                            0, 2,
                            0, 1,
                            0, 0, 0, 0,
                            0, 6,
                            3, 'f', 'o', 'o', -64, 5};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(ValidationException.class, () -> ResourceRecord.decode(b));
        }

        //Bad name incorrect ending
        @ParameterizedTest(name = "Bad name incorrect ending {0}")
        @ValueSource(bytes = {-128, 0, 1, -96, -84})
        void decodeValidationError(byte badEnding){
            //foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            byte[] buff = { 3, 'f', 'o', 'o', badEnding, 5,
                            0, 2,
                            0, 1,
                            0, 0, 0, 0,
                            0, 6,
                            3, 'f', 'o', 'o', -64, 5};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(ValidationException.class, () -> ResourceRecord.decode(b));
        }

        //Bad name incorrect ending (EOF)
        @ParameterizedTest(name = "Bad name incorrect ending (EOF) {0}")
        @ValueSource(bytes = {96, 12})
        void decodeValidationErrorEOF(byte badEnding){
            //foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            byte[] buff = { 3, 'f', 'o', 'o', badEnding, 5,
                            0, 2,
                            0, 1,
                            0, 0, 0, 0,
                            0, 6,
                            3, 'f', 'o', 'o', -64, 5};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(EOFException.class, () -> ResourceRecord.decode(b));
        }

        //Bad name incorrect ending 64
        @Test @DisplayName("Bad name incorrect ending 64")
        void decodeValidationErrorBadEnding(){
            //foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            byte[] buff = { 3, 'f', 'o', 'o', 64, 5,
                    0, 2,
                    0, 1,
                    0, 0, 0, 0,
                    0, 6,
                    3, 'f', 'o', 'o', -64, 5};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(EOFException.class, () -> ResourceRecord.decode(b));
        }

        //Bad 0x0001 in various formats
        @Test @DisplayName("Bad 0x0001")
        void decodeValidationError3(){
            //foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            byte[] buff = { 3, 'f', 'o', 'o', -64, 5,
                            0, 2,
                            0,
                            0, 0, 0, 0,
                            0, 6,
                            3, 'f', 'o', 'o', -64, 5};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(ValidationException.class, () -> ResourceRecord.decode(b));
        }
        @Test @DisplayName("Bad 0x0001")
        void decodeValidationError4(){
            //foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            byte[] buff = { 3, 'f', 'o', 'o', -64, 5,
                            0, 2,
                            0, 1, 1,
                            0, 0, 0, 0,
                            0, 6,
                            3, 'f', 'o', 'o', -64, 5};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(ValidationException.class, () -> ResourceRecord.decode(b));
        }
        @Test @DisplayName("Bad 0x0001")
        void decodeValidationError5(){
            //foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            byte[] buff = { 3, 'f', 'o', 'o', -64, 5,
                            0, 2,
                            0, 2,
                            0, 0, 0, 0,
                            0, 6,
                            3, 'f', 'o', 'o', -64, 5};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(ValidationException.class, () -> ResourceRecord.decode(b));
        }
        @Test @DisplayName("Bad 0x0001")
        void decodeValidationError6(){
            //foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            byte[] buff = { 3, 'f', 'o', 'o', -64, 5,
                            0, 2,
                            1, 1,
                            0, 0, 0, 0,
                            0, 6,
                            3, 'f', 'o', 'o', -64, 5};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(ValidationException.class, () -> ResourceRecord.decode(b));
        }

        //Null input stream
        @Test @DisplayName("Null input stream")
        void decodeNullInput(){
            assertThrows(NullPointerException.class, () -> ResourceRecord.decode(null));
        }

        //No RDLength
        @Test @DisplayName("No RDLength")
        void decodeValidationErrorNoRDLength(){
            //foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            byte[] buff = { 3, 'f', 'o', 'o', -64, 5,
                            0, 2,
                            0, 1,
                            0, 0, 0, 0,
                            3, 'f', 'o', 'o', -64, 5};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(EOFException.class, () -> ResourceRecord.decode(b));
        }

        //No Name
        @Test @DisplayName("No Name")
        void decodeValidationErrorNoName(){
            //foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            byte[] buff = {
                            0, 2,
                            0, 1,
                            0, 0, 0, 0,
                            0, 6,
                            3, 'f', 'o', 'o', -64, 5};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(ValidationException.class, () -> ResourceRecord.decode(b));
        }

        //Half Name
        @Test @DisplayName("Half Name")
        void decodeValidationErrorHalfName(){
            //foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            byte[] buff = { 3, 'f', 'o',
                            0, 2,
                            0, 1,
                            0, 0, 0, 0,
                            0, 6,
                            3, 'f', 'o', 'o', -64, 5};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(ValidationException.class, () -> ResourceRecord.decode(b));
        }

        //No type
        @Test @DisplayName("No type")
        void decodeValidationErrorNoType(){
            //foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            byte[] buff = { 3, 'f', 'o', 'o', -64, 5,
                            0, 1,
                            0, 0, 0, 0,
                            0, 6,
                            3, 'f', 'o', 'o', -64, 5};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(ValidationException.class, () -> ResourceRecord.decode(b));
        }

        //No 0x0001
        @Test @DisplayName("No 0x0001")
        void decodeValidationErrorNo0x0001(){
            //foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            byte[] buff = { 3, 'f', 'o', 'o', -64, 5,
                            0, 2,
                            0, 0, 0, 0,
                            0, 6,
                            3, 'f', 'o', 'o', -64, 5};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(ValidationException.class, () -> ResourceRecord.decode(b));
        }

        //No TTL
        @Test @DisplayName("No TTL")
        void decodeValidationErrorNoTTL(){
            //foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            byte[] buff = { 3, 'f', 'o', 'o', -64, 5,
                            0, 2,
                            0, 1,
                            0, 6,
                            3, 'f', 'o', 'o', -64, 5};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(EOFException.class, () -> ResourceRecord.decode(b));
        }

        //No RData
        @Test @DisplayName("No RData")
        void decodeValidationErrorNoRData(){
            //foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            byte[] buff = { 3, 'f', 'o', 'o', -64, 5,
                            0, 2,
                            0, 1,
                            0, 0, 0, 0,
                            0, 6};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(EOFException.class, () -> ResourceRecord.decode(b));
        }
    }

    /**
     * Tests decode validation errors
     */
    @Nested
    class DecodeValidationError { //"asdf.." is a valid test _if serialized correctly_
        /*
           foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
          Valid data:
                3, 'f', 'o', 'o', -64, 5,
                0, 2,
                0, 1, //0x0001
                0, 0, 0, 0,
                0, 6,
                3, 'f', 'o', 'o', -64, 5//"foo."
         */

        /**
         * Tests invalid domain names in the name field
         * @param name invalid name
         */
        @ParameterizedTest(name = "Invalid name = {0}")
        @ValueSource(strings = {"", "asdf.].", "asdf.Ƞ.", "asdf..", "www.baylor.edu/", "..", "asdf.asdf", "f0-9.c0m-.", "Ẵ.Ẓ.㛃.⭐.⭕.",
                "-a.f", "-.", "-",
                "a234567890123456789012345678901234567890123456789012345678901234.",//64
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a2345678901."//256
        })
        void decodeName(String name){
            ArrayList<Byte> b = new ArrayList<>();
            serialize(name, b);

            //insert rest of bytes here
            Collections.addAll(b,
                    (byte)0, (byte)2,
                    (byte)0, (byte)1,
                    (byte)0, (byte)0, (byte)0, (byte)0,
                    (byte)0, (byte)3,
                    (byte)1, (byte)'o', (byte)0);

            byte[] buff = new byte[b.size()];
            for(int i=0;i<b.size();i++){
                buff[i] = b.get(i);
            }

            ByteArrayInputStream bstream = new ByteArrayInputStream(buff);
            assertThrows(ValidationException.class, () -> ResourceRecord.decode(bstream));
        }

        /**
         * Tests for bad domain name with EOF exception
         * @param name bad domain name
         */
        @ParameterizedTest(name = "Invalid name (EOF) = {0}")
        @ValueSource(strings = {"asdf"})
        void decodeNameEOF(String name){
            ArrayList<Byte> b = new ArrayList<>();
            serialize(name, b);

            //insert rest of bytes here
            Collections.addAll(b,
                    (byte)0, (byte)2,
                    (byte)0, (byte)1,
                    (byte)0, (byte)0, (byte)0, (byte)0,
                    (byte)0, (byte)3,
                    (byte)1, (byte)'o', (byte)0);

            byte[] buff = new byte[b.size()];
            for(int i=0;i<b.size();i++){
                buff[i] = b.get(i);
            }

            ByteArrayInputStream bstream = new ByteArrayInputStream(buff);
            assertThrows(EOFException.class, () -> ResourceRecord.decode(bstream));
        }

        /**
         * Tests for invalid domain names in NS RData
         * @param name invalid name to try
         */
        @ParameterizedTest(name = "Invalid rdata = {0}")
        @ValueSource(strings = {"", "asdf", "asdf.].", "asdf.Ƞ.", "asdf..", "www.baylor.edu/", "..", "asdf.asdf", "f0-9.c0m-.", "Ẵ.Ẓ.㛃.⭐.⭕.",
                "-a.f", "-.", "-",
                "a234567890123456789012345678901234567890123456789012345678901234.",//64
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a2345678901."//256
        })
        void decodeNSRDataInvalid(String name){
            ArrayList<Byte> b = new ArrayList<>();
            ArrayList<Byte> a = new ArrayList<>();

            //insert rest of bytes here
            Collections.addAll(a, (byte)1, (byte)'o', (byte)0,
                    (byte)0, (byte)2,
                    (byte)0, (byte)1,
                    (byte)0, (byte)0, (byte)0, (byte)0);

            serialize(name, b);

            a.add((byte)0);
            a.add((byte)b.size());
            a.addAll(b);


            byte[] buff = new byte[a.size()];
            for(int i=0;i<a.size();i++){
                buff[i] = a.get(i);
            }

            ByteArrayInputStream bstream = new ByteArrayInputStream(buff);
            try{
                ResourceRecord.decode(bstream);
            } catch(ValidationException | EOFException e){
                assert(true);
            } catch(IOException e){
                assert(false);
            }
        }

        /**
         * Tests for invalid domain names in CName RData
         * @param name invalid name to try
         */
        @ParameterizedTest(name = "Invalid rdata = {0}")
        @ValueSource(strings = {"", "asdf", "asdf.].", "asdf.Ƞ.", "asdf..", "www.baylor.edu/", "..", "asdf.asdf", "f0-9.c0m-.", "Ẵ.Ẓ.㛃.⭐.⭕.",
                "-a.f", "-.", "-",
                "a234567890123456789012345678901234567890123456789012345678901234.",//64
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a2345678901."//256
        })
        void decodeCNameRDataInvalid(String name){
            ArrayList<Byte> b = new ArrayList<>();
            ArrayList<Byte> a = new ArrayList<>();

            //insert rest of bytes here
            Collections.addAll(a, (byte)1, (byte)'o', (byte)0,
                    (byte)0, (byte)5,
                    (byte)0, (byte)1,
                    (byte)0, (byte)0, (byte)0, (byte)0);

            serialize(name, b);

            a.add((byte)0);
            a.add((byte)b.size());
            a.addAll(b);


            byte[] buff = new byte[a.size()];
            for(int i=0;i<a.size();i++){
                buff[i] = a.get(i);
            }

            ByteArrayInputStream bstream = new ByteArrayInputStream(buff);
            try{
                ResourceRecord.decode(bstream);
            } catch(ValidationException | EOFException e){
                assert(true);
            } catch(IOException e){
                assert(false);
            }
        }

        //The next few tests test invalid IPv4 sizes in RDLength
        @Test @DisplayName("Invalid IPv4 size 3")
        void decodeARDataInvalid0(){
            byte[] buff = { 0,
                    0, 1,
                    0, 1,
                    0, 0, 0, 0,
                    0, 3,
                    0, 0, 0};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(ValidationException.class, () -> ResourceRecord.decode(b));
        }
        @Test @DisplayName("Invalid IPv4 size 2")
        void decodeARDataInvalid1(){
            byte[] buff = { 0,
                    0, 1,
                    0, 1,
                    0, 0, 0, 0,
                    0, 2,
                    0, 0};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(ValidationException.class, () -> ResourceRecord.decode(b));
        }
        @Test @DisplayName("Invalid IPv4 size 1")
        void decodeARDataInvalid2(){
            byte[] buff = { 0,
                    0, 1,
                    0, 1,
                    0, 0, 0, 0,
                    0, 1,
                    0};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(ValidationException.class, () -> ResourceRecord.decode(b));
        }
        @Test @DisplayName("Invalid IPv4 size 5")
        void decodeARDataInvalid3(){
            byte[] buff = { 0,
                    0, 1,
                    0, 1,
                    0, 0, 0, 0,
                    0, 5,
                    0, 0, 0, 0, 0};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(ValidationException.class, () -> ResourceRecord.decode(b));
        }

        //The next few tests test invalid IPv6 sizes in RDLength
        @Test @DisplayName("Invalid IPv6 size 15")
        void decodeAAAARDataInvalid0(){
            byte[] buff = { 0,
                    0, 28,
                    0, 1,
                    0, 0, 0, 0,
                    0, 15,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(ValidationException.class, () -> ResourceRecord.decode(b));
        }
        @Test @DisplayName("Invalid IPv6 size 17")
        void decodeAAAARDataInvalid1(){
            byte[] buff = { 0,
                    0, 28,
                    0, 1,
                    0, 0, 0, 0,
                    0, 17,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(ValidationException.class, () -> ResourceRecord.decode(b));
        }
        @Test @DisplayName("Invalid IPv6 size 4")
        void decodeAAAARDataInvalid2(){
            byte[] buff = { 0,
                    0, 28,
                    0, 1,
                    0, 0, 0, 0,
                    0, 4,
                    0, 0, 0, 0};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(ValidationException.class, () -> ResourceRecord.decode(b));
        }

        /**
         * Tests preferences in the MX object type
         */
        @Nested
        class DecodeMXInvalidPreference extends PreferenceTestFactory{
            @Override
            protected boolean getTestInvalid() {
                return false;
            }

            /**
             * Factory method for calling the appropriate function you want to test for preference validity
             *
             * @param pref preference to test
             * @return the result of a getPreference on the respective object
             * @throws ValidationException if invalid object
             */
            @Override
            protected int setGetPreference(int pref) throws ValidationException {
                byte[] buff = { 0,//name
                        0, 15,//type
                        0, 1,//0x0001
                        0, 0, 0, 0,//ttl
                        0, 3,//RDLen
                        (byte)(pref >> 8), (byte)pref, //preference
                        0//exchange
                };
                ByteArrayInputStream b = new ByteArrayInputStream(buff);
                ResourceRecord rr = null;
                try {
                    rr = ResourceRecord.decode(b);
                } catch (IOException e) {
                    fail();//oops
                }

                assert(rr instanceof MX);
                return ((MX)rr).getPreference();
            }
        }

        /**
         * Tests for invalid domain names in MX RData Exchange
         * @param name invalid name to try
         */
        @ParameterizedTest(name = "Invalid rdata = {0}")
        @ValueSource(strings = {"", "asdf", "asdf.].", "asdf.Ƞ.", "asdf..", "www.baylor.edu/", "..", "asdf.asdf", "f0-9.c0m-.", "Ẵ.Ẓ.㛃.⭐.⭕.",
                "-a.f", "-.", "-",
                "a234567890123456789012345678901234567890123456789012345678901234.",//64
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a2345678901."//256
        })
        void decodeMXExchangeInvalid(String name){
            ArrayList<Byte> b = new ArrayList<>();
            ArrayList<Byte> a = new ArrayList<>();

            //insert rest of bytes here
            Collections.addAll(a, (byte)1, (byte)'o', (byte)0,
                    (byte)0, (byte)15,
                    (byte)0, (byte)1,
                    (byte)0, (byte)0, (byte)0, (byte)0);

            serialize(name, b);

            Collections.addAll(b, (byte)0, (byte)205);//add preference
            a.add((byte)0);
            a.add((byte)b.size());
            a.addAll(b);


            byte[] buff = new byte[a.size()];
            for(int i=0;i<a.size();i++){
                buff[i] = a.get(i);
            }

            ByteArrayInputStream bstream = new ByteArrayInputStream(buff);
            try{
                ResourceRecord.decode(bstream);
            } catch(ValidationException | EOFException e){
                assert(true);
            } catch(IOException e){
                assert(false);
            }
        }

        //todo: test invalid CAA fields

        /**
         * Tests invalid label fields in the name domain name field
         * @param n bad label
         */
        @ParameterizedTest(name = "Bad label field: {0}")
        @ValueSource(bytes = {5, 3, -3, 0})
        void decodeValidationErrorLabel(byte n){
            //foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            byte[] buff = { n, 'f', 'o', 'o', 'o', -64, 5,
                    0, 2,
                    0, 1,
                    0, 0, 0, 0,
                    0, 6,
                    3, 'f', 'o', 'o', -64, 5};

            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            try{
                ResourceRecord.decode(b);
            } catch(ValidationException | EOFException e){
                assert(true);
            } catch(IOException e){
                assert(false);
            }
        }

        /**
         * Tests invalid RDLength (too short)
         * @param n bad rdlength
         */
        @ParameterizedTest(name = "Bad RDLength field: {0}")
        @ValueSource(bytes = {3, 0})
        void decodeValidationErrorRDLength(byte n){
            //foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            byte[] buff = { 4, 'f', 'o', 'o', 'o', -64, 5,
                    0, 2,
                    0, 1,
                    0, 0, 0, 0,
                    0, n,
                    3, 'f', 'o', 'o', -64, 5};

            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(ValidationException.class, () -> ResourceRecord.decode(b));
        }

        /**
         * Tests bad RDLength fields
         * @param n bad rdlength
         */
        @ParameterizedTest(name = "Bad RDLength field (EOF): {0}")
        @ValueSource(bytes = {7, 127, -3, -128})
        void decodeValidationErrorRDLengthEOF(byte n){
            //foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            byte[] buff = { 4, 'f', 'o', 'o', 'o', -64, 5,
                    0, 2,
                    0, 1,
                    0, 0, 0, 0,
                    0, n,
                    3, 'f', 'o', 'o', -64, 5};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            assertThrows(EOFException.class, () -> ResourceRecord.decode(b));
        }
    }

    /**
     * Tests valid decodes
     */
    @Nested
    class DecodeValid {
        /**
         * Decode valid names tests
         * @param name valid name
         */
        @ParameterizedTest(name = "Valid name = {0}")
        @ValueSource(strings = {"a23456789012345678901234567890123456789012345678901234567890123.",//63
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a234567890.",//255
                "foo.", "foo.com.", "f0-9.c0m.", "google.com.", "www.baylor.edu.", "f0.c-0.", ".", "f-0.", "f-0a."})
        void decodeName(String name){
            /*
            "foo." = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            Valid data:
            3, 'f', 'o', 'o', -64, 5,
                                0, 2,
                                0, 1, //0x0001
                                0, 0, 0, 0,
                                0, 6,
                                3, 'f', 'o', 'o', -64, 5//"foo."
             */
            ArrayList<Byte> b = new ArrayList<>();
            serialize(name, b);

            //insert rest of bytes here
            Collections.addAll(b,
                    (byte)0, (byte)2,
                    (byte)0, (byte)1,
                    (byte)0, (byte)0, (byte)0, (byte)0,
                    (byte)0, (byte)3,
                    (byte)1, (byte)'o', (byte)0);

            byte[] buff = new byte[b.size()];
            for(int i=0;i<b.size();i++){
                buff[i] = b.get(i);
            }

            ByteArrayInputStream bstream = new ByteArrayInputStream(buff);
            try {
                ResourceRecord temp = ResourceRecord.decode(bstream);
                assert temp != null;
                assertEquals(temp.getName(), name);
            } catch (ValidationException | IOException e) {
                assert(false);
            }
        }

        /**
         * Simple test with the compressed standard
         */
        @Test @DisplayName("Test compressed name")
        void decodeName1(){
            byte[] buff = { -64, 5,
                    0, 2,
                    0, 1, //0x0001
                    0, 0, 0, 0,
                    0, 6,
                    3, 'f', 'o', 'o', -64, 5};
            try {
                ResourceRecord temp = ResourceRecord.decode(new ByteArrayInputStream(buff));
                assert temp != null;
                assertEquals(temp.getName(), ".");
            } catch (ValidationException | IOException e) {
                assert(false);
            }
        }

        /**
         * Test valid domain names in NS RData
         * @param name valid name
         */
        @ParameterizedTest(name = "Valid name = {0}")
        @ValueSource(strings = {"a23456789012345678901234567890123456789012345678901234567890123.",//63
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a234567890.",//255
                "foo.", "foo.com.", "f0-9.c0m.", "google.com.", "www.baylor.edu.", "f0.c-0.", ".", "f-0.", "f-0a."})
        void decodeRDataNS(String name){
            /*
            foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            Valid data:
            3, 'f', 'o', 'o', -64, 5,
                0, 2,
                0, 1, //0x0001
                0, 0, 0, 0,
                0, 6,
                3, 'f', 'o', 'o', -64, 5//"foo."
             */
            ArrayList<Byte> b = new ArrayList<>();
            ArrayList<Byte> a = new ArrayList<>();

            //insert rest of bytes here
            Collections.addAll(a, (byte)1, (byte)'o', (byte)0,
                    (byte)0, (byte)2,
                    (byte)0, (byte)1,
                    (byte)0, (byte)0, (byte)0, (byte)0);

            serialize(name, b);

            a.add((byte)(b.size() >> 8));
            a.add((byte)b.size());
            a.addAll(b);

            byte[] buff = new byte[a.size()];
            for(int i=0;i<a.size();i++){
                buff[i] = a.get(i);
            }

            ByteArrayInputStream bstream = new ByteArrayInputStream(buff);
            try {
                ResourceRecord temp = ResourceRecord.decode(bstream);
                assert(temp != null);
                assertEquals(((NS)temp).getNameServer(), name);
            } catch (ValidationException | IOException e) {
                assert(false);
            }
        }

        /**
         * Test valid domain names in CName RData
         * @param name valid name
         */
        @ParameterizedTest(name = "Valid name = {0}")
        @ValueSource(strings = {"a23456789012345678901234567890123456789012345678901234567890123.",//63
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a234567890.",//255
                "foo.", "foo.com.", "f0-9.c0m.", "google.com.", "www.baylor.edu.", "f0.c-0.", ".", "f-0.", "f-0a."})
        void decodeRDataCName(String name){
            /*
            foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            Valid data:
            3, 'f', 'o', 'o', -64, 5,
                0, 2,
                0, 1, //0x0001
                0, 0, 0, 0,
                0, 6,
                3, 'f', 'o', 'o', -64, 5//"foo."
             */
            ArrayList<Byte> b = new ArrayList<>();
            ArrayList<Byte> a = new ArrayList<>();

            //insert rest of bytes here
            Collections.addAll(a, (byte)1, (byte)'o', (byte)0,
                    (byte)0, (byte)5,
                    (byte)0, (byte)1,
                    (byte)0, (byte)0, (byte)0, (byte)0);

            serialize(name, b);

            a.add((byte)(b.size() >> 8));
            a.add((byte)b.size());
            a.addAll(b);


            byte[] buff = new byte[a.size()];
            for(int i=0;i<a.size();i++){
                buff[i] = a.get(i);
            }

            ByteArrayInputStream bstream = new ByteArrayInputStream(buff);
            try {
                ResourceRecord temp = ResourceRecord.decode(bstream);
                assert(temp != null);
                assertEquals(((CName)temp).getCanonicalName(), name);
            } catch (ValidationException | IOException e) {
                assert(false);
            }
        }

        //The following tests valid IPv4 with compression of name
        @Test @DisplayName("Valid values without EOS and with compression")
        void decodeRDataACompressed(){
            byte[] buff = { -43, 3,
                    0, 1,
                    0, 1,
                    0, 0, 1, 65,
                    0, 4,
                    6, 7, 8, 9 };
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            ResourceRecord temp;
            try {
                temp = ResourceRecord.decode(b);
                assert(temp != null);
                assertEquals(((A)temp).getAddress(), Inet4Address.getByName("6.7.8.9"));
            } catch (ValidationException | IOException e) {
                assert(false);
            }
        }

        //The following tests valid IPv6 with compression of name
        @Test @DisplayName("Valid values without EOS and with compression")
        void decodeRDataAAAACompressed(){
            byte[] buff = { -43, 3,
                    0, 28,
                    0, 1,
                    0, 0, 1, 65,
                    0, 16,
                    6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            ResourceRecord temp;
            try {
                temp = ResourceRecord.decode(b);
                assert(temp != null);
                assertEquals(((AAAA)temp).getAddress(), Inet6Address.getByName("607:809:A0B:C0D:E0F:1011:1213:1415"));
            } catch (ValidationException | IOException e) {
                assert(false);
            }
        }

        /**
         * Tests valid IPv4s
         * @param ipstr valid IPv4
         */
        @ParameterizedTest(name = "IPv4 = {0}")
        @ValueSource(strings = {"0.0.0.0", "1.0.255.0", "255.255.255.255", "255.0.255.137"})
        void decodeRDataA(String ipstr){
            byte[] comparisonArrNoIP = {3, 102, 111, 111, 3, 99, 111, 109, 0,//foo.com.
                    0, 1,//type
                    0, 1,//0x0001
                    0, 0, 0, 42,//ttl
                    0, 4};//rdlength

            try {
                //Set up helper
                Inet4Address ip = (Inet4Address)Inet4Address.getByName(ipstr);
                byte[] buff = concatByteArrs(comparisonArrNoIP, ip.getAddress());

                //Decode
                ByteArrayInputStream b = new ByteArrayInputStream(buff);
                ResourceRecord temp = ResourceRecord.decode(b);

                //Validate
                assert(temp != null);
                assertEquals(ipstr, ((A)temp).getAddress().getHostAddress());
            } catch (IOException | ValidationException e) {
                fail();
            }
        }

        /**
         * Tests valid IPv6s
         * @param ipstr valid IPv6
         */
        @ParameterizedTest(name = "IPv6 = {0}")
        @ValueSource(strings = {"0:0:0:0:0:0:0:0", "123:4567:89AB:CDEF:123:4567:89AB:CDEF",
                                "FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF"})
        void decodeRDataAAAA(String ipstr){
            byte[] comparisonArrNoIP = {3, 102, 111, 111, 3, 99, 111, 109, 0,//foo.com.
                    0, 28,//type
                    0, 1,//0x0001
                    0, 0, 0, 42,//ttl
                    0, 16};//rdlength

            try {
                //Set up helper
                Inet6Address ip = (Inet6Address)Inet6Address.getByName(ipstr);
                byte[] buff = concatByteArrs(comparisonArrNoIP, ip.getAddress());

                //Decode
                ByteArrayInputStream b = new ByteArrayInputStream(buff);
                ResourceRecord temp = ResourceRecord.decode(b);

                //Validate
                assert(temp != null);
                assertEquals(ipstr.toLowerCase(), ((AAAA)temp).getAddress().getHostAddress().toLowerCase());
            } catch (IOException | ValidationException e) {
                fail();
            }
        }

        /**
         * Test valid domain names in MX RData Exchange
         * @param name valid name
         */
        @ParameterizedTest(name = "Valid name = {0}")
        @ValueSource(strings = {"a23456789012345678901234567890123456789012345678901234567890123.",//63
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a234567890.",//255
                "foo.", "foo.com.", "f0-9.c0m.", "google.com.", "www.baylor.edu.", "f0.c-0.", ".", "f-0.", "f-0a."})
        void decodeRDataMXExchange(String name){
            /*
            //foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            Valid data:
             3, 'f', 'o', 'o', -64, 5,
                0, 2,
                0, 1, //0x0001
                0, 0, 0, 0,
                0, 6,
                3, 'f', 'o', 'o', -64, 5//"foo."
             */
            ArrayList<Byte> b = new ArrayList<>();
            ArrayList<Byte> a = new ArrayList<>();

            //insert rest of bytes here
            Collections.addAll(a, (byte)1, (byte)'o', (byte)0,//name
                    (byte)0, (byte)15,//type
                    (byte)0, (byte)1,//0x0001
                    (byte)0, (byte)0, (byte)0, (byte)0);//ttl

            serialize(name, b);
            b.add(0, (byte)205);//add preference
            b.add(0, (byte)0);
            a.add((byte)(b.size() >> 8));//rdlen
            a.add((byte)b.size());//rdata
            a.addAll(b);


            byte[] buff = new byte[a.size()];
            for(int i=0;i<a.size();i++){
                buff[i] = a.get(i);
            }

            ByteArrayInputStream bstream = new ByteArrayInputStream(buff);
            try {
                ResourceRecord temp = ResourceRecord.decode(bstream);
                assert(temp != null);
                assertEquals(((MX)temp).getExchange(), name);
            } catch (ValidationException | IOException e) {
                assert(false);
            }
        }

        //todo:: decode preference and check for proper decoding

        //todo:: CAA fields valid testing

        //The following 4 tests test for the correct type value
        @Test @DisplayName("Test type 1")
        void decodeA(){
            byte[] buff = { 3, 'f', 'o', 'o', -64, 5,
                    0, 1,
                    0, 1, //0x0001
                    0, 0, 0, 0,
                    0, 4,
                    0, 0, 0, 0};
            try {
                ResourceRecord temp = ResourceRecord.decode(new ByteArrayInputStream(buff));
                assert(temp != null);
                assertEquals(A.class, temp.getClass());
            } catch (ValidationException | IOException e) {
                assert(false);
            }
        }
        @Test @DisplayName("Test type 2")
        void decodeNS(){
            byte[] buff = { 3, 'f', 'o', 'o', -64, 5,
                            0, 2,
                            0, 1, //0x0001
                            0, 0, 0, 0,
                            0, 6,
                            3, 'f', 'o', 'o', -64, 5};
            try {
                ResourceRecord temp = ResourceRecord.decode(new ByteArrayInputStream(buff));
                assert temp != null;
                assertEquals(temp.getClass(), NS.class);
            } catch (ValidationException | IOException e) {
                assert(false);
            }
        }
        @Test @DisplayName("Test type 5")
        void decodeCName(){
            byte[] buff = { 3, 'f', 'o', 'o', -64, 5,
                    0, 5,
                    0, 1, //0x0001
                    0, 0, 0, 0,
                    0, 6,
                    3, 'f', 'o', 'o', -64, 5};
            try {
                ResourceRecord temp = ResourceRecord.decode(new ByteArrayInputStream(buff));
                assert(temp != null);
                assertEquals(temp.getClass(), CName.class);
            } catch (ValidationException | IOException e) {
                assert(false);
            }
        }
        @Test @DisplayName("Test type 15")
        void decodeMX(){
            byte[] buff = { 3, 'f', 'o', 'o', -64, 5,
                    0, 15,
                    0, 1, //0x0001
                    0, 0, 0, 0,
                    0, 8,//rdlen
                    1, 1,//preference
                    3, 'f', 'o', 'o', -64, 5};//exchange
            try {
                ResourceRecord temp = ResourceRecord.decode(new ByteArrayInputStream(buff));
                assert(temp != null);
                assertEquals(MX.class, temp.getClass());
            } catch (ValidationException | IOException e) {
                assert(false);
            }
        }
        @Test @DisplayName("Test type 28")
        void decodeAAAA(){
            byte[] buff = { 3, 'f', 'o', 'o', -64, 5,
                    0, 28,
                    0, 1, //0x0001
                    0, 0, 0, 0,
                    0, 16,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            try {
                ResourceRecord temp = ResourceRecord.decode(new ByteArrayInputStream(buff));
                assert(temp != null);
                assertEquals(AAAA.class, temp.getClass());
            } catch (ValidationException | IOException e) {
                assert(false);
            }
        }
        @Test @DisplayName("Test type 257")
        void decodeCAA(){
            byte[] buff = { 3, 'f', 'o', 'o', -64, 5,
                    1, 1,
                    0, 1, //0x0001
                    0, 0, 0, 0,
                    0, 23,//rdlen
                    0, 5, 'i', 's', 's', 'u', 'e',
                    '0', 'a', 'c', '%', 'f', 'M', '>', '<', '+', '@', '\\', '\"', ')', '-', '_', '|'};
            try {
                ResourceRecord temp = ResourceRecord.decode(new ByteArrayInputStream(buff));
                assert(temp != null);
                assertEquals(CAA.class, temp.getClass());
            } catch (ValidationException | IOException e) {
                assert(false);
            }
        }

        /**
         * Test decoding TTL
         * @param ttl ttl
         */
        @ParameterizedTest(name = "Valid ttl = {0}")
        @ValueSource(ints = {0, 1, 2147483647, 321})
        void decodeTTL(int ttl){
            byte[] buff = { 3, 'f', 'o', 'o', -64, 5,
                    0, 2,
                    0, 1, //0x0001
                    (byte)((ttl >> 24) & 0xff), (byte)((ttl >> 16) & 0xff), (byte)((ttl >> 8) & 0xff), (byte)((ttl) & 0xff),
                    0, 6,
                    3, 'f', 'o', 'o', -64, 5};

            try {
                ResourceRecord temp = ResourceRecord.decode(new ByteArrayInputStream(buff));
                assert(temp != null);
                assertEquals(temp.getTTL(), ttl);
            } catch (ValidationException | IOException e) {
                assert(false);
            }
        }

        //Test Unknown name
        @Test @DisplayName("Test Unknown name")
        void decodeUnknown1(){
            byte[] buff = { 0,
                    0, 8,
                    0, 1, //0x0001
                    0, 0, 1, 65,
                    0, 8,
                    0, 0, 0, 0, 0, 0, 0, 0};
            try {
                ResourceRecord temp = ResourceRecord.decode(new ByteArrayInputStream(buff));
                assert temp != null;
                assertEquals(".", temp.getName());
                assertEquals(321, temp.getTTL());
                assertEquals(8, temp.getTypeValue());
            } catch (ValidationException | IOException e) {
                assert(false);
            }
        }

        //Test Unknown double, premature EoS
        @Test @DisplayName("Test Unknown double, premature EoS")
        void decodeUnknown2(){
            byte[] buff = { 0,
                    0, -1,
                    0, 1, //0x0001
                    0, 0, 1, 65,
                    0, 8,
                    0, 0, 0, 0, 0, 0, 0, 1,
                    0,
                    0, 8,
                    0, 1,
                    0, 0, 1, 65,
                    0, 8,
                    0, 0, 0, 0, 0, 0, 0};

            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            try{
                ResourceRecord temp = ResourceRecord.decode(b);
                assert temp != null;
                assertEquals(".", temp.getName());
                assertEquals(321, temp.getTTL());
                assertEquals(255, temp.getTypeValue());
            } catch (ValidationException | IOException e) {
                fail();
            }

            assertThrows(EOFException.class, () -> ResourceRecord.decode(b));

        }

        //Test 2 unknowns in a row
        @Test @DisplayName("Test Unknown valid double values")
        void decodeUnknown3(){
            byte[] buff = {0,
                    0, 8,
                    0, 1, //0x0001
                    0, 0, 1, 65,
                    0, 8,
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0,
                    0, 8,
                    0, 1, //0x0001
                    0, 0, 1, 65,
                    0, 8,
                    0, 0, 0, 0, 0, 0, 0, 0};

            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            try {
                ResourceRecord temp = ResourceRecord.decode(b);
                assert temp != null;
                assertEquals(".", temp.getName());
                assertEquals(321, temp.getTTL());
                assertEquals(8, temp.getTypeValue());

                ResourceRecord temp2 = ResourceRecord.decode(b);
                assert temp2 != null;
                assertEquals(".", temp.getName());
                assertEquals(321, temp.getTTL());
                assertEquals(8, temp.getTypeValue());
            } catch (ValidationException | IOException e) {
                assert(false);
            }
        }
    }

    /**
     * Name setter and getter tests (DONE)
     */
    @Nested
    class NameSetterGetter extends DomainNameTestFactory {
        /**
         * Factory method for calling the appropriate function you want to test for domain name validity
         *
         * @param dm domain name to test
         * @return the result of a getDM on the respective object
         * @throws ValidationException if invalid domain name
         */
        @Override
        protected String setGetDomainName(String dm) throws ValidationException {
            ResourceRecord cn = null;
            try {//Shouldn't fail here
                cn = new CName(".", 0, ".");
            } catch(ValidationException e){
                fail();
            }
            cn.setName(dm);
            return cn.getName();
        }

        /**
         * Allows the concrete class to specify which exception it wants to be thrown when a
         * null string is passed to the function
         *
         * @return class to throw
         */
        @Override
        protected Class<? extends Throwable> getNullThrowableType() {
            return ValidationException.class;
        }
    }

    /**
     * TTL Setter and getter (DONE)
     */
    @Nested
    class TTLSetterGetter extends TTLTestFactory {
        /**
         * Factory method for calling the appropriate function you want to test for TTL validity
         *
         * @param ttl ttl to test
         * @return the result of a getTTL on the respective object
         * @throws ValidationException if invalid object
         */
        @Override
        protected int setGetTTL(int ttl) throws ValidationException {
            ResourceRecord cn = null;
            try {
                cn = new CName(".", 0, ".");
            } catch(ValidationException e){
                fail();
            }
            cn.setTTL(ttl);
            return cn.getTTL();
        }
    }
}