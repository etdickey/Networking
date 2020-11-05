@REM *******INDEPENDENT TESTS*******
@REM start server in the background (manually for now)
@REM START /B startServer.bat > serverOutput.out

@cd ../../
@javac -d compiled/ sdns/app/udp/client/Client.java
@cd compiled/

REM ATTENTION: The following tests execute two at a time because of the timeout
REM ATTENTION: **Server must be running on localhost 5300**
REM ATTENTION: BEGINNING MALFORMED TESTS (RESULTING IN TIMEOUT)
CALL ../testscripts/clientTests/malformedTests.bat
REM ATTENTION: BEGINNING INVALID TESTS
CALL ../testscripts/clientTests/invalidTests.bat

@cd ../testscripts/clientTests
