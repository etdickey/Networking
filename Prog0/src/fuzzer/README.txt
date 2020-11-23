Authors: Ethan Dickey and Harrison Rogers

=======INSTALL========
1. install boofuzz on your machine with "pip install boofuzz"
2. run with
    python3 SDNSFuzzUDP.py <server> <port>
    python3 SDNSFuzzTCP.py <server> <port>


=======TESTING========
This is different from the tests they were based on in their content.  The Authority
  section was removed and the header and query values were changed to match
  SDNS protocol

2844 tests
- Each field of the SDNS header is tested with many different values.
- The domain name in the Question is tested with a full suite of test values
    which vary in length and content.  All are based on the default google.com.
Note: BooFuzz randomly seg faults.  This is a known issue and can be fixed by
  rerunning the script.  This is not a problem with the script as it happens
  with the example scripts as well.
