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

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

//Note: rerun all tests with different character encodings (UTF-32, UTF-16, etc) at runtime
/**
 * @author Ethan Dickey
 * @author Harrison Rogers
 */
class ResourceRecordTest {
    //Case insensitive string comparison

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
     * Tests invalid decodes
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
        @Test
        @DisplayName("Bad length name long")
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

        @Test
        @DisplayName("Bad name incorrect ending 64")
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

        @Test
        @DisplayName("Bad 0x0001")
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
        @Test
        @DisplayName("Bad 0x0001")
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
        @Test
        @DisplayName("Bad 0x0001")
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
        @Test
        @DisplayName("Bad 0x0001")
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

        @Test
        @DisplayName("Null input stream")
        void decodeNullInput(){
            assertThrows(NullPointerException.class, () -> ResourceRecord.decode(null));
        }

        @Test
        @DisplayName("No RDLength")
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

        @Test
        @DisplayName("No Name")
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

        @Test
        @DisplayName("Half Name")
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

        @Test
        @DisplayName("No type")
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

        @Test
        @DisplayName("No 0x0001")
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

        @Test
        @DisplayName("No TTL")
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

        @Test
        @DisplayName("No RData")
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
    class DecodeValidationError { //These tests need to be rethought.  "asdf.." is a valid test _if serialized correctly_
        /*
            //foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
        Valid data:
        byte[] buff = { 3, 'f', 'o', 'o', -64, 5,
                            0, 2,
                            0, 1, //0x0001
                            0, 0, 0, 0,
                            0, 6,
                            3, 'f', 'o', 'o', -64, 5};//"foo."
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



        /*
        //foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
        Valid data:
        byte[] buff = { 3, 'f', 'o', 'o', -64, 5,
                            0, 2,
                            0, 1, //0x0001
                            0, 0, 0, 0,
                            0, 6,
                            3, 'f', 'o', 'o', -64, 5};//"foo."
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

        @Test
        @DisplayName("Invalid IPv4 size 3")
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
        @Test
        @DisplayName("Invalid IPv4 size 2")
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
        @Test
        @DisplayName("Invalid IPv4 size 1")
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
        @Test
        @DisplayName("Invalid IPv4 size 5")
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
            ResourceRecord temp = null;
            try {
                temp = ResourceRecord.decode(bstream);
                assert temp != null;
                assertEquals(temp.getName(), name);
            } catch (ValidationException | IOException e) {
                assert(false);
            }
        }

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

        @ParameterizedTest(name = "Valid name = {0}")
        @ValueSource(strings = {"a23456789012345678901234567890123456789012345678901234567890123.",//63
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a234567890.",//255
                "foo.", "foo.com.", "f0-9.c0m.", "google.com.", "www.baylor.edu.", "f0.c-0.", ".", "f-0.", "f-0a."})
        void decodeRDataNS(String name){
            /*
            //foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            Valid data:
            byte[] buff = { 3, 'f', 'o', 'o', -64, 5,
                                0, 2,
                                0, 1, //0x0001
                                0, 0, 0, 0,
                                0, 6,
                                3, 'f', 'o', 'o', -64, 5};//"foo."
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
            ResourceRecord temp = null;
            try {
                temp = ResourceRecord.decode(bstream);
                assert(temp != null);
                assertEquals(((NS)temp).getNameServer(), name);
            } catch (ValidationException | IOException e) {
                assert(false);
                e.printStackTrace();
            }
        }

        @ParameterizedTest(name = "Valid name = {0}")
        @ValueSource(strings = {"a23456789012345678901234567890123456789012345678901234567890123.",//63
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a234567890.",//255
                "foo.", "foo.com.", "f0-9.c0m.", "google.com.", "www.baylor.edu.", "f0.c-0.", ".", "f-0.", "f-0a."})
        void decodeRDataCName(String name){
            /*
            //foo. = 3 102, 111, 111, 192, 5 //-64 signed = 192 unsigned
            Valid data:
            byte[] buff = { 3, 'f', 'o', 'o', -64, 5,
                                0, 2,
                                0, 1, //0x0001
                                0, 0, 0, 0,
                                0, 6,
                                3, 'f', 'o', 'o', -64, 5};//"foo."
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
            ResourceRecord temp = null;
            try {
                temp = ResourceRecord.decode(bstream);
                assert(temp != null);
                assertEquals(((CName)temp).getCanonicalName(), name);
            } catch (ValidationException | IOException e) {
                assert(false);
                e.printStackTrace();
            }
        }


        @Test @DisplayName("Valid IPv4 0.0.0.0")
        void decodeRDataA0(){
            byte[] buff = { 0,
                    0, 1,
                    0, 1,
                    0, 0, 0, 0,
                    0, 4,
                    0, 0, 0, 0};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            ResourceRecord temp;
            try {
                temp = ResourceRecord.decode(b);
                assert(temp != null);
                assertEquals(((A)temp).getAddress(), (Inet4Address)Inet4Address.getByName("0.0.0.0"));
            } catch (ValidationException | IOException e) {
                assert(false);
            }
        }
        @Test @DisplayName("Valid IPv4 1.0.255.0")
        void decodeRDataA1(){
            byte[] buff = { 0,
                    0, 1,
                    0, 1,
                    0, 0, 0, 0,
                    0, 4,
                    1, 0, -1, 0};//-1 == 255 unsigned
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            ResourceRecord temp;
            try {
                temp = ResourceRecord.decode(b);
                assert(temp != null);
                assertEquals(((A)temp).getAddress(), Inet4Address.getByName("1.0.255.0"));
            } catch (ValidationException | IOException e) {
                assert(false);
            }
        }
        @Test @DisplayName("Valid IPv4 255.255.255.255")
        void decodeRDataA2(){
            byte[] buff = { 0,
                    0, 1,
                    0, 1,
                    0, 0, 0, 0,
                    0, 4,
                    -1, -1, -1, -1};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            ResourceRecord temp;
            try {
                temp = ResourceRecord.decode(b);
                assert(temp != null);
                assertEquals(((A)temp).getAddress(), (Inet4Address)Inet4Address.getByName("255.255.255.255"));
            } catch (ValidationException | IOException e) {
                assert(false);
            }
        }
        @Test @DisplayName("Valid IPv4 255.0.255.137")
        void decodeRDataA3(){
            byte[] buff = { 0,
                    0, 1,
                    0, 1,
                    0, 0, 0, 0,
                    0, 4,
                    -1, 0, -1, -119};
            ByteArrayInputStream b = new ByteArrayInputStream(buff);
            ResourceRecord temp;
            try {
                temp = ResourceRecord.decode(b);
                assert(temp != null);
                assertEquals(((A)temp).getAddress(), (Inet4Address)Inet4Address.getByName("255.0.255.137"));
            } catch (ValidationException | IOException e) {
                assert(false);
            }
        }
        @Test @DisplayName("Valid values without EOS and with compression")
        void decodeRDataA4(){
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
                assertEquals(((A)temp).getAddress(), (Inet4Address)Inet4Address.getByName("6.7.8.9"));
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
                e.printStackTrace();
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
                e.printStackTrace();
            }
        }

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
                e.printStackTrace();
            }
        }

        @ParameterizedTest(name = "Valid ttl = {0}")
        @ValueSource(ints = {0, 1, 2147483647, 321})
        void decodeTTL(int ttl){
            byte[] buff = { 3, 'f', 'o', 'o', -64, 5,
                    0, 2,
                    0, 1, //0x0001
                    (byte)((ttl >> 24) & 0xff), (byte)((ttl >> 16) & 0xff), (byte)((ttl >> 8) & 0xff), (byte)((ttl >> 0) & 0xff),
                    0, 6,
                    3, 'f', 'o', 'o', -64, 5};

            try {
                ResourceRecord temp = ResourceRecord.decode(new ByteArrayInputStream(buff));
                assert(temp != null);
                assertEquals(temp.getTTL(), ttl);
            } catch (ValidationException | IOException e) {
                assert(false);
                e.printStackTrace();
            }
        }

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

        @Test @DisplayName("Test Unknown double, premature EoS")
        void decodeUnknown2(){
            byte[] buff = { 0,
                    0, 8,
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
                assertEquals(8, temp.getTypeValue());
            } catch (ValidationException | IOException e) {
                assert (false);
            }

            assertThrows(EOFException.class, () -> ResourceRecord.decode(b));

        }

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
                assert temp != null;
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
     * Name: -- these tests apply to all domain name field tests
     *     Each label must start with a letter, end with a letter or digit, and have as interior characters only letters
     *       (A-Z and a-z), digits (0-9), and hypen (-).
     *     A name with a single, empty label (".") is acceptable
     */
    @Nested
    class NameSetterGetter {
        //set canonical name tests ERROR
        @ParameterizedTest(name = "Name error = {0}")
        @ValueSource(strings = {"", "asdf", "asdf.].", "asdf.Ƞ.", "asdf..", "www.baylor.edu/", "..", "asdf.asdf", "f0-9.c0m-.", "Ẵ.Ẓ.㛃.⭐.⭕.",
                "-a.f", "-.", "-",
                "a234567890123456789012345678901234567890123456789012345678901234.",
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a2345678901."//256
        })
        void setNameValidationException(String name) {
            ResourceRecord cn, ns;
            //CName
            try {
                cn = new CName(".", 0, ".");
                assertThrows(ValidationException.class, () -> cn.setName(name));
            } catch(Exception e){
                assert(false);
            }
            //NS
            try {
                ns = new NS(".", 0, ".");
                assertThrows(ValidationException.class, () -> ns.setName(name));
            } catch(Exception e){
                assert(false);
            }
        }
        @Test
        void setNameNullPtr() {
            ResourceRecord cn, ns;
            //CName
            try {
                cn = new CName(".", 0, ".");
                assertThrows(ValidationException.class, () -> cn.setName(null));
            } catch(Exception e){
                assert(false);
            }
            //NS
            try {
                ns = new NS(".", 0, ".");
                assertThrows(ValidationException.class, () -> ns.setName(null));
            } catch(Exception e){
                assert(false);
            }
        }

        //set canonical name tests VALID
        @ParameterizedTest(name = "Name valid = {0}")
        @ValueSource(strings = {"a23456789012345678901234567890123456789012345678901234567890123.",
                "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//122
                        "a23456789012345678901234567890123456789012345678901234567890.a23456789012345678901234567890123456789012345678901234567890." +//244
                        "a234567890.",//255
                "foo.", "foo.com.", "f0-9.c0m.", "google.com.", "www.baylor.edu.", "f0.c-0.", ".", "f-0.", "f-0a."
        })
        void setAndGetNameValid(String name) {
            ResourceRecord cn, ns;
            //CName
            try {
                cn = new CName(".", 0, ".");
                cn.setName(name);
                assertEquals(name, cn.getName());
            } catch(Exception e){
                assert(false);
            }
            //NS
            try {
                ns = new NS(".", 0, ".");
                ns.setName(name);
                assertEquals(name, ns.getName());
            } catch(Exception e){
                assert(false);
            }
        }
    }

    /**
     * TTL Setter and getter (DONE)
     */
    @Nested
    class TTLSetterGetter {
        //ValidationException TTL tests
        @ParameterizedTest(name = "TTL error = {0}")
        @ValueSource(ints = {-1, -2147483648})
        void constTTLValidationError(int ttl){
            ResourceRecord cn, ns;
            //CName
            try {
                cn = new CName(".", 0, ".");
                assertThrows(ValidationException.class, () -> cn.setTTL(ttl));
            } catch(Exception e){
                assert(false);
            }
            //NS
            try {
                ns = new NS(".", 0, ".");
                assertThrows(ValidationException.class, () -> ns.setTTL(ttl));
            } catch(Exception e){
                assert(false);
            }
        }

        //set AND get ttl tests VALID
        @ParameterizedTest(name = "TTL valid = {0}")
        @ValueSource(ints = {0, 1, 2147483647})
        void setAndGetTTLValid(int ttl) {
            ResourceRecord cn, ns;
            //CName
            try {
                cn = new CName(".", 0, ".");
                cn.setTTL(ttl);
                assertEquals(ttl, cn.getTTL());
            } catch(Exception e){
                assert(false);
            }
            //NS
            try {
                ns = new NS(".", 0, ".");
                ns.setTTL(ttl);
                assertEquals(ttl, ns.getTTL());
            } catch(Exception e){
                assert(false);
            }
        }
    }
}