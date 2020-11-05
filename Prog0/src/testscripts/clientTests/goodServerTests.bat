@REM *******INDEPENDENT TESTS*******
@REM start server in the background (manually for now)
@REM START /B startServer.bat > serverOutput.out

@cd ../../
@javac -d compiled/ sdns/app/udp/client/Client.java
@cd compiled/

REM TODO:: FINISH
@REM asdfasdfasdfasdf

REM ATTENTION: The following tests execute two at a time because of the timeout
@REM ATTENTION: BEGINNING MALFORMED TESTS (RESULTING IN TIMEOUT)
@REM CALL ../testscripts/clientTests/malformedTests.bat
@REM ATTENTION: BEGINNING INVALID TESTS
@REM CALL ../testscripts/clientTests/invalidTests.bat

REM ATTENTION: Beginning Donahoo server tests


@cd ../testscripts/clientTests
