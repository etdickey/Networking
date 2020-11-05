@REM *******INDEPENDENT TEST*******
@REM The file of basic tests

@cd ../../
@javac -d compiled/ sdns/app/udp/client/Client.java
@cd compiled/

REM this shouldn't work, invalid server (no response)
java sdns/app/udp/client/Client 129.62.148.1 53 www.google.com.

REM this shouldn't work, invalid server (no response)
java sdns/app/udp/client/Client dfw06s48-in-f100.1e100.net 53 www.google.com.

REM this should work for www.google.com. (with a verbose output),
REM    but www.amazon.com. has an invalid canonical domain name
REM    response from google's DNS (8.8.8.8)
java sdns/app/udp/client/Client 8.8.8.8 53 www.google.com. www.amazon.com.

REM this should work for www.google.com. (but has an empty list of everything),
REM   but www.amazon.com. has an invalid canonical domain name response
REM   from google's DNS (8.8.8.8)
java sdns/app/udp/client/Client 8.8.8.8 53 google.com. amazon.com.


REM this should work for both clients with many responses
java sdns/app/udp/client/Client c.root-servers.net. 53 www.google.com. www.amazon.com.

@cd ../testscripts/clientTests
