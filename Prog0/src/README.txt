<h1>Clients</h1>
My TCP and UDP clients, through use of common functions and utilities reduce duplicate
protocol handling as much as possible.  The common utility classes and functions allow
for varied behavior while maintaining only one spot for the protocol handling.

<h1>Servers</h1>
My TCP-AOI, TCP and UDP servers, through use of common functions and an abstract
common parent class with dispatch, reduce duplicate protocol handling as
much as possible.

The parent class (utils/ServerProtocol.java) with dispatch allows polymorphic
behavior while still maintaining only one area that handles the protocols.