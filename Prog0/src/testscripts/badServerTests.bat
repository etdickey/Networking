@REM start server in the background (manually for now)
@REM START /B startServer.bat > serverOutput.out

@cd ../
@javac -d compiled/ sdns/app/udp/client/Client.java
@cd compiled/

REM ATTENTION: The following tests execute two at a time because of the timeout
REM ATTENTION: BEGINNING MALFORMED TESTS (RESULTING IN TIMEOUT)
CALL ../testscripts/malformedTests.bat
REM ATTENTION: BEGINNING INVALID TESTS
CALL ../testscripts/invalidTests.bat

@cd ../testscripts
