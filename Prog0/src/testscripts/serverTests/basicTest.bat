@REM *******INDEPENDENT TEST*******
@REM The file of basic tests

@cd ../../
@javac -d compiled/ sdns/app/udp/server/Server.java
@cd compiled/

REM this shouldn't work, invalid port
CScript //B //T:05 java sdns/app/udp/server/Server 65536

REM this shouldn't work, invalid port
CScript //B //T:05 java sdns/app/udp/server/Server -1

REM this should work and be running
CScript //B //T:05 java sdns/app/udp/server/Server 1999 > serverOutput.out

@cd ../testscripts/serverTests
