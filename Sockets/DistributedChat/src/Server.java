import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private final ServerSocket serverSocket;
    private final DatagramSocket datagramSocket;
    private final Map<Integer, InetAddress> addresses = new HashMap<>();
    private final byte[] recBuffer = new byte[1024];

    public Server(ServerSocket serverSocket, DatagramSocket datagramSocket) {
        this.serverSocket = serverSocket;
        this.datagramSocket = datagramSocket;
    }

    public static void main(String[] args) throws IOException {
        System.out.println("SERVER: Starting work");
        DatagramSocket datagramSocket = new DatagramSocket(8888);
        ServerSocket serverSocket = new ServerSocket(8888);
        Server server = new Server(serverSocket, datagramSocket);
        new Thread(server::listenForTCPClients).start();
        server.broadcastUDPMessages();
    }

    public void listenForTCPClients() {
        try {
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected");
                TCPHandler TCPHandler = new TCPHandler(clientSocket);
                Thread thread = new Thread(TCPHandler);
                thread.start();
            }
        } catch (IOException e) {
            closeTCPSession();
        }
    }

    public void broadcastUDPMessages() {
        while (!datagramSocket.isClosed()) {
            try {
                Arrays.fill(recBuffer, (byte) 0);
                DatagramPacket receivePacket = new DatagramPacket(recBuffer, recBuffer.length);
                datagramSocket.receive(receivePacket);
                addresses.put(receivePacket.getPort(), receivePacket.getAddress());
                if (receivePacket.getData()[0] != 'W') {
                    for (Map.Entry<Integer, InetAddress> entry : addresses.entrySet()) {
                        if (receivePacket.getPort() != entry.getKey()) {
                            DatagramPacket sendPacket = new DatagramPacket(recBuffer, recBuffer.length, entry.getValue(), entry.getKey());
                            datagramSocket.send(sendPacket);
                        }
                    }
                }
            } catch (IOException e) {
                closeUDPSession();
            }
        }
    }

    public void closeTCPSession() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeUDPSession() {
        if (datagramSocket != null) {
            datagramSocket.close();
        }
    }

    private static class TCPHandler implements Runnable {
        private static final List<TCPHandler> handlers = new ArrayList<>();
        private final Socket clientSocket;
        private BufferedReader in;
        private BufferedWriter out;
        private String clientName;

        public TCPHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                this.out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                this.clientName = in.readLine();
                handlers.add(this);
                broadcastMessage("SERVER: " + clientName + " has entered the chat");
            } catch (IOException e) {
                closeConnection(clientSocket, in, out);
            }
        }

        @Override
        public void run() {
            String msg;
            while (clientSocket.isConnected()) {
                try {
                    msg = in.readLine();
                    broadcastMessage(msg);
                } catch (IOException e) {
                    closeConnection(clientSocket, in, out);
                    break;
                }
            }
        }

        public void broadcastMessage(String message) {
            for (TCPHandler handler : handlers) {
                try {
                    if (!handler.clientName.equals(clientName)) {
                        handler.out.write(message);
                        handler.out.newLine();
                        handler.out.flush();
                    }
                } catch (IOException e) {
                    closeConnection(clientSocket, in, out);
                }
            }
        }

        public void closeConnection(Socket clientSocket, BufferedReader in, BufferedWriter out) {
            handlers.remove(this);
            broadcastMessage("Client " + clientName + " left the chat");
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
