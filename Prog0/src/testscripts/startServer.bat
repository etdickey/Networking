cd ../
javac -d compiled/ sdns/app/udp/client/test/TestUDPEchoServer.java
cd compiled/

java sdns/app/udp/client/test/TestUDPEchoServer 5300

cd ../testscripts
