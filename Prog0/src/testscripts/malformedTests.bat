@REM the file for malformed tests

REM this should timeout (TIMEOUT)
java sdns/app/udp/client/Client localhost 5300 www.google.com.

REM 0:this should throw some sort of validation exception (or too short) (ERROR)
REM 1:Basic header test error short query name (ERROR)
java sdns/app/udp/client/Client localhost 5300 www.google.com.

REM 2:Basic header test error short query name beginning (ERROR)
REM 3:Basic header test error short query 0x00FF (ERROR)
java sdns/app/udp/client/Client localhost 5300 www.google.com.

REM 4:Basic header test error short query name ending (ERROR)
REM 5:Basic header test error short query 0x00FF (ERROR)
java sdns/app/udp/client/Client localhost 5300 www.google.com.

REM 6:Basic header test error short query 0x0001 (ERROR)
REM 7:Basic header test error short query 0x0001 (ERROR)
java sdns/app/udp/client/Client localhost 5300 www.google.com.
