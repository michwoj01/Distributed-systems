import java.io.*;
import java.net.*;
import java.util.Arrays;

public class Client {
    private static final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    private static final byte[] unicastBuffer = ("""
                 _____ \s
               .'     `.
              /         \\
             |           |\s
             '.  +^^^+  .'
               `. \\./ .'
                 |_|_| \s
                 (___)   \s
            jgs  (___)
                 `---'""").getBytes();
    private static final byte[] multicastBuffer = ("""
                ,-`-.
              ,' .___;
             /_ ,'@@,-.\s
             (C`____ -'
              \\ `--' ;
               ``:`:'
              .-`  '--.
             ( /7   \\7 )
              \\\\i--._|/
             (,-,)   |
             ,--:_.  /
            (  ..__,/
             `-' ;  ``.
             dwb `---`""").getBytes();
    private static InetAddress group;
    private final Socket socket;
    private final byte[] normalBuffer = new byte[1024];
    private final DatagramSocket datagramSocket;
    private final MulticastSocket multicastSocket;
    private final String hostname;
    private BufferedReader in;
    private BufferedWriter out;

    public Client(Socket socket, DatagramSocket datagramSocket, MulticastSocket multicastSocket, String hostname) {
        this.socket = socket;
        this.datagramSocket = datagramSocket;
        this.multicastSocket = multicastSocket;
        this.hostname = hostname;
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            closeTCPConnection(socket, in, out);
            closeUDPConnection(datagramSocket);
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Client starting. Put his name below:");
        String hostName = reader.readLine();
        Socket socket = new Socket("localhost", 8888);
        DatagramSocket datagramSocket = new DatagramSocket();
        MulticastSocket multicastSocket = new MulticastSocket(9999);
        group = InetAddress.getByName("230.0.0.0");
        multicastSocket.joinGroup(group);
        Client client = new Client(socket, datagramSocket, multicastSocket, hostName);
        new Thread(client::listenForTCPMessage).start();
        new Thread(client::listenForUDPMessage).start();
        new Thread(client::listenForUDPMulticast).start();
        client.sendTCPMessage();
    }

    public void sendTCPMessage() {
        try {
            out.write(hostname);
            out.newLine();
            out.flush();
            DatagramPacket welcomePacket = new DatagramPacket("Welcome".getBytes(), 7, InetAddress.getLocalHost(), 8888);
            datagramSocket.send(welcomePacket);
            while (socket.isConnected() || !datagramSocket.isClosed()) {
                String msgToSend = reader.readLine();
                if (msgToSend.startsWith("U")) {
                    DatagramPacket sendPacket = new DatagramPacket(unicastBuffer, unicastBuffer.length, InetAddress.getLocalHost(), 8888);
                    try {
                        datagramSocket.send(sendPacket);
                    } catch (IOException e) {
                        closeUDPConnection(datagramSocket);
                    }
                } else if (msgToSend.startsWith("M")) {
                    DatagramPacket sendPacket = new DatagramPacket(multicastBuffer, multicastBuffer.length, group, 9999);
                    try {
                        multicastSocket.send(sendPacket);
                    } catch (IOException e) {
                        closeUDPConnection(multicastSocket);
                    }
                } else {
                    try {
                        out.write(hostname + ": " + msgToSend);
                        out.newLine();
                        out.flush();
                    } catch (IOException e) {
                        closeTCPConnection(socket, in, out);
                    }
                }
            }
        } catch (IOException e) {
            closeTCPConnection(socket, in, out);
            closeUDPConnection(datagramSocket);
        }
    }

    public void listenForUDPMessage() {
        while (!datagramSocket.isClosed()) {
            try {
                Arrays.fill(normalBuffer, (byte) 0);
                DatagramPacket receivePacket = new DatagramPacket(normalBuffer, normalBuffer.length);
                datagramSocket.receive(receivePacket);
                System.out.println(new String(Arrays.copyOfRange(receivePacket.getData(), 0, unicastBuffer.length)));
            } catch (IOException e) {
                closeUDPConnection(datagramSocket);
            }
        }
    }

    public void listenForUDPMulticast() {
        while (!multicastSocket.isClosed()) {
            try {
                Arrays.fill(normalBuffer, (byte) 0);
                DatagramPacket receivePacket = new DatagramPacket(normalBuffer, normalBuffer.length);
                multicastSocket.receive(receivePacket);
                System.out.println(new String(Arrays.copyOfRange(receivePacket.getData(), 0, multicastBuffer.length)));
            } catch (IOException e) {
                closeUDPConnection(multicastSocket);
            }
        }
    }

    public void listenForTCPMessage() {
        String msgFromChat;
        while (socket.isConnected()) {
            try {
                msgFromChat = in.readLine();
                System.out.println(msgFromChat);
            } catch (IOException e) {
                closeTCPConnection(socket, in, out);
            }
        }
    }

    public void closeTCPConnection(Socket clientSocket, BufferedReader in, BufferedWriter out) {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeUDPConnection(DatagramSocket datagramSocket) {
        if (datagramSocket != null) {
            datagramSocket.close();
        }
    }
}
