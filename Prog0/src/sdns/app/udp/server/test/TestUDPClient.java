package sdns.app.udp.server.test;

import sdns.app.udp.server.Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class TestUDPClient {
    private static final int NUM_TESTS = 5;

    private static byte[] getNextTest(int i) {
        byte[] toSend = null;
        switch(i){
            case 0: toSend = new byte[]{ 0, 1 };
                break;
            case 1: break;
            case 2: break;
            case 3: break;
            case 4: break;
        }
        return toSend;
    }

    public static void main(String[] args) throws IOException {
        if(args.length != 2){
            throw new IllegalArgumentException("Parameters: <IP> <Port>");
        }

        int servPort = Integer.parseInt(args[1]);
        InetAddress servAddr = InetAddress.getByName(args[0]);

        DatagramSocket socket = new DatagramSocket();
        int currTest = 0;
        while(currTest < NUM_TESTS){
            byte[] toSend = getNextTest(currTest++);
            if(toSend != null){
                DatagramPacket pack = new DatagramPacket(toSend, toSend.length, servAddr, servPort);
                socket.send(pack);
            } else {
                break;
            }
        }
    }
}
