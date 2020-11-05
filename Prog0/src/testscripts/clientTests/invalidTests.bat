@REM *******CHILD TESTS*******
@REM the file for invalid tests

REM 0:Basic query test invalid 0x0001 (VALIDATION ERROR)
REM 1:Basic query test invalid 0x0001 (VALIDATION ERROR)
java sdns/app/udp/client/Client localhost 5300 www.google.com.

REM 2:Basic query test invalid ANCount (VALIDATION ERROR)
REM 3:Basic query test invalid NSCount (VALIDATION ERROR)
java sdns/app/udp/client/Client localhost 5300 www.google.com.

REM 4:Basic query test invalid ARCount (VALIDATION ERROR)
REM 5:Basic query test invalid header byte 1 {16} (VALIDATION ERROR)
java sdns/app/udp/client/Client localhost 5300 www.google.com.

REM 6:Basic query test invalid header byte 1 {8} (VALIDATION ERROR)
REM 7:Basic query test invalid header byte 1 {64} (VALIDATION ERROR)
java sdns/app/udp/client/Client localhost 5300 www.google.com.

REM 8:Basic query test invalid header byte 1 {32} (VALIDATION ERROR)
REM 9:Basic query test invalid header byte 2 {4} (VALIDATION ERROR)
java sdns/app/udp/client/Client localhost 5300 www.google.com.

REM 10:Basic query test invalid header byte 2 {2} (VALIDATION ERROR)
REM 11:Basic query test invalid header byte 2 {1} (VALIDATION ERROR)
java sdns/app/udp/client/Client localhost 5300 www.google.com.

REM 12:Basic query test invalid header byte 2 {64} (VALIDATION ERROR)
REM 13:Basic query test invalid header byte 2 {32} (VALIDATION ERROR)
java sdns/app/udp/client/Client localhost 5300 www.google.com.

REM 14:Basic query test invalid header byte 2 {16} (VALIDATION ERROR)
REM 15:Basic query test invalid header byte 2 {8} (VALIDATION ERROR)
java sdns/app/udp/client/Client localhost 5300 www.google.com.
