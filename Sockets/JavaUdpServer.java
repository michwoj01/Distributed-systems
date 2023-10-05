import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class JavaUdpServer {

    public static void main(String args[]) {
        System.out.println("JAVA UDP SERVER");
        DatagramSocket socket = null;
        int portNumber = 9009;
        try {
            socket = new DatagramSocket(portNumber);
            byte[] receiveBuffer = new byte[1024];
            Arrays.fill(receiveBuffer, (byte) 0);
            DatagramPacket receivePacket = null;
            while (true) {
                receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                socket.receive(receivePacket);
                int nb = ByteBuffer.wrap(receivePacket.getData()).getInt();
                System.out.println(nb);
                nb +=1 ;
                receiveBuffer = ByteBuffer.allocate(4).putInt(nb).array();
                break;
            }
            DatagramPacket sendPacket = new DatagramPacket(receiveBuffer, receiveBuffer.length,
                    receivePacket.getAddress(),
                    receivePacket.getPort());
            socket.send(sendPacket);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}
