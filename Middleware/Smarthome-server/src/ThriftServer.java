import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;

import java.util.Random;
import java.util.Scanner;

public class ThriftServer {
    public static final Random rand = new Random();

    public static void main(String[] args) {
        System.out.println("Hello dear smarthomer. Choose port to run your server:");
        Scanner scanner = new Scanner(System.in);
        int port = scanner.nextInt();
        try {
            new Thread(() -> multiplex(port)).start();
        } catch (Exception ex) {
            SmarthomeHandler.cleanup();
            ex.printStackTrace();
        }
    }

    public static void multiplex(int port) {
        try {
            SmarthomeHandler smarthomeHandler = new SmarthomeHandler();
            smarthomeHandler.initialize();
            Smarthome.Processor<SmarthomeHandler> smarthomeProcessor = new Smarthome.Processor<>(smarthomeHandler);
            Cleaner.Processor<CleanerHandler> cleanerProcessor = new Cleaner.Processor<>(new CleanerHandler());
            Stove.Processor<StoveHandler> stoveProcessor = new Stove.Processor<>(new StoveHandler());
            Fridge.Processor<FridgeHandler> fridgeProcessor = new Fridge.Processor<>(new FridgeHandler());

            TServerTransport serverTransport = new TServerSocket(port);

            TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();

            TMultiplexedProcessor multiplex = new TMultiplexedProcessor();

            multiplex.registerProcessor("Smarthome", smarthomeProcessor);
            multiplex.registerProcessor("Stove", stoveProcessor);
            multiplex.registerProcessor("Cleaner", cleanerProcessor);
            multiplex.registerProcessor("Fridge", fridgeProcessor);

            TServer server = new TSimpleServer(new Args(serverTransport).protocolFactory(protocolFactory).processor(multiplex));

            System.out.println("Starting the multiplex server...");
            server.serve();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}