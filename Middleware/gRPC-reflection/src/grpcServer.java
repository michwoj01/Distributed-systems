import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


public class grpcServer {
    private static final Logger logger = Logger.getLogger(grpcServer.class.getName());

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = 50051;
        String address = "127.0.0.5";
        SocketAddress socket = null;
        try {
            socket = new InetSocketAddress(InetAddress.getByName(address), port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(404);
        }

        Server server = NettyServerBuilder.forAddress(socket).executor(Executors.newFixedThreadPool(16))
                .addService(new ServiceImpl())
                .addService(ProtoReflectionService.newInstance())
                .build()
                .start();

        logger.info("Server started, listening on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("Shutting down gRPC server since JVM is shutting down");
            server.shutdown();
            System.err.println("Server successfully shut down");
        }));
        server.awaitTermination();
    }

}
