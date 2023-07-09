import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Carter {
    private static final String EXCHANGE_NAME = "mainExchange";
    private static final String ADMIN_EXCHANGE = "adminExchange";
    private static final String PASSENGERS_QUEUE = "passengersQueue";
    private static final String CARGO_QUEUE = "cargoQueue";
    private static final String SATELLITES_QUEUE = "satellitesQueue";
    private static Channel channel = null;

    private static void processRequest(String requestType, String routing_key) throws IOException {
        DeliverCallback requestCallback = (consumerTag, delivery) -> {
            AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(delivery.getProperties().getCorrelationId())
                    .build();
            String acknowledgement = "";
            try {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
//                Thread.sleep(5000);
                acknowledgement = routing_key + " request number " + message + " has been processed successfully";
                System.out.println(acknowledgement);
            } catch (RuntimeException e) {
                acknowledgement = "Error while parsing " + routing_key + " message: " + e;
                System.out.println(acknowledgement);
            } finally {
                channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, acknowledgement.getBytes(StandardCharsets.UTF_8));
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };

        channel.queueDeclare(requestType, true, false, false, null);
        channel.queueBind(requestType, EXCHANGE_NAME, routing_key);
        channel.basicConsume(requestType, false, requestCallback, (consumerTag) -> {
        });
    }

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        channel = connection.createChannel();
        channel.basicQos(1);
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
        channel.exchangeDeclare(ADMIN_EXCHANGE, BuiltinExchangeType.TOPIC);

        String callbackQueue = channel.queueDeclare().getQueue();
        channel.queueBind(callbackQueue, ADMIN_EXCHANGE, "#.carter");
        channel.basicConsume(callbackQueue, true, (consumerTag, delivery) -> {
            String adminMessage = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("Received message from admin: " + adminMessage);
        }, consumerTag -> {
        });

        String choose = """
                 Choose type of carrier:
                 1. Passengers transit + cargo transit
                 2. Cargo transit + satellites to orbit
                 3. Satellite to orbit + passengers transit
                """;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.print(choose);
        String requestType = br.readLine();

        switch (requestType) {
            case "1" -> {
                processRequest(PASSENGERS_QUEUE, "passengers");
                processRequest(CARGO_QUEUE, "cargo");
            }
            case "2" -> {
                processRequest(SATELLITES_QUEUE, "satellites");
                processRequest(CARGO_QUEUE, "cargo");
            }
            case "3" -> {
                processRequest(PASSENGERS_QUEUE, "passengers");
                processRequest(SATELLITES_QUEUE, "satellites");
            }
            default -> System.out.println("Bad input");
        }
    }
}
