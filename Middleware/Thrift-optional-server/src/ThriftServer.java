import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;

public class ThriftServer {

    public static void main(String[] args) {
        try {
            new Thread(ThriftServer::multiplex).start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void multiplex() {
        try {
            CalculatorHandler calculatorHandler = new CalculatorHandler();
            Calculator.Processor<CalculatorHandler> handlerProcessor = new Calculator.Processor<>(calculatorHandler);

            TServerTransport serverTransport = new TServerSocket(9090);

            TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();

            TMultiplexedProcessor multiplex = new TMultiplexedProcessor();

            multiplex.registerProcessor("Calculator", handlerProcessor);

            TServer server = new TSimpleServer(new Args(serverTransport).protocolFactory(protocolFactory).processor(multiplex));

            System.out.println("Starting the multiplex server...");
            server.serve();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}