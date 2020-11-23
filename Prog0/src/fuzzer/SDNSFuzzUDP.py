# Designed for use with boofuzz v0.2.0
#
# A UDP SDNS fuzzer.
#
# Credit:
#     Modified the examples/mdns.py file in the FooBuzz repository to get this file.
# Author: Ethan Dickey
# Author: Harrison Rogers

from boofuzz import *
import sys

#parse arguments
try:
    SERVER = sys.argv[1]
    PORT = int(sys.argv[2])
except:
    print("Error: Usage: <server> <port>")
    exit(1)


def insert_questions(target, fuzz_data_logger, session, node, edge, *args, **kwargs):
    node.names["Questions"].value = 1 + node.names["queries"].current_reps

s_initialize("query")
s_word(0, name="TransactionID")#word = binary word = 2 bytes
s_word(0, name="Flags")
s_word(1, name="Questions", endian=">")
s_word(0, name="Answer", endian=">")
s_word(0, name="Authority", endian=">")
s_word(0, name="Additional", endian=">")

# ######## Queries ################
if s_block_start("query"):
    if s_block_start("name_chunk"):
        s_size("string", length=1)
        if s_block_start("string"):
            s_string(chr(6) + "google" + chr(3) + "com")
        s_block_end()
    s_block_end()
    s_repeat("name_chunk", min_reps=2, max_reps=4, step=1, fuzzable=True, name="aName")

    s_group("end", values=["\x00", "\xc0\xb0"])  # very limited pointer fuzzing
    s_word(0x00FF, name="Type", endian=">")
    s_word(0x0001, name="Class", endian=">")
s_block_end()
s_repeat("query", 0, 1000, 40, name="queries")

s_word(0)

#Establish connection
sess = Session(target=Target(connection=UDPSocketConnection(SERVER, PORT)))
sess.connect(s_get("query"), callback=insert_questions)

sess.fuzz()
