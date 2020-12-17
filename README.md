# Networking Undergraduate networking class programs.

The major code is held in Prog0/src/sdns/*.

## Overview 
This project creates DNS protocol-handling code.  It uses a simpler version of DNS (*S*DNS), closer to the original protocol specifications for feasibility within a class.  It integrates over **1800 JUnit tests** with 2 clients (UDP and TCP) and 3 servers:
* UDP
* TCP Syncronous using thread pools
  * Blocking
* TCP Asynchronous
  * Nonblocking

All of the servers use my TCP client as their backend when accessing other public DNS servers.  This is the trace of execution:

client -> my [UDP, TCP] server -> my TCP client (handled by the "masterfile" class) -> public DNS server.

## Testing
The JUnit tests make use of abstract factories in order to reduce code duplication.  These implementations are particularly useful with common testable fields such as Domain Names and IP addresses.

The project also includes fuzz testing, load testing, and custom tests for UDP java clients.  The testing for the TCP java client was integrated into the rest of the tests as that is the backend used for all servers (as explained above).

## Note on connecting to my server (using Dig or another program):
The program does not implement EDNS, the ADFlag, and will only respond to queries with ANY class requested.  These were part of the program specifications to simplify the semester-long project.

For a detailed report on this, see (link is broken, go to file directly)

[Diging Hosted Server](Prog0/Dig'ing Hosted Server.pdf)

## Questions and Access to PDF SDNS protocol specifications
For access to the specifications or questions regarding the code, please contact me through github.

Keywords: java, TCP java, UDP java, sockets java, asynchronous java, synchronous java, junit, junit abstract factories, fuzzer, fuzzing, load testing java, DNS java
