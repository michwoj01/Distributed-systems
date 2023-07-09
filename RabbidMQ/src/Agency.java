import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.UUID;

public class Agency {
    private static final String EXCHANGE_NAME = "mainExchange";
    private static final String ADMIN_EXCHANGE = "adminExchange";

    public static void main(String[] argv) throws Exception {
        String name = argv[0];
        boolean repeat = true;
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
        channel.exchangeDeclare(ADMIN_EXCHANGE, BuiltinExchangeType.TOPIC);

        final String corrId = UUID.randomUUID().toString();
        String callbackQueue = channel.queueDeclare().getQueue();
        channel.queueBind(callbackQueue, ADMIN_EXCHANGE, "agency.#");
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .correlationId(corrId)
                .replyTo(callbackQueue)
                .build();

        channel.basicConsume(callbackQueue, true, (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                String responseMessage = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println("Received response message: " + responseMessage);
            } else if (delivery.getProperties().getCorrelationId().equals("admin")) {
                String adminMessage = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println("Received message from admin: " + adminMessage);
            }
        }, consumerTag -> {
        });

        Random random = new Random(2173);
        String choose = """
                Choose type of the request:
                1. Passengers transit
                2. Cargo transit
                3. Satellite to orbit
                4. Exit (I'm bored)
                """;
        System.out.print(choose);
        String routingKey;
        int requestNumber;
        while (repeat) {

            routingKey = null;
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String requestType = br.readLine();
            switch (requestType) {
                case "1" -> routingKey = "passengers";
                case "2" -> routingKey = "cargo";
                case "3" -> routingKey = "satellites";
                case "4" -> repeat = false;
                default -> {
                    System.out.println("Bad input");
                    continue;
                }
            }
            requestNumber = random.nextInt();
            String message = name + " " + requestNumber;
            channel.basicPublish(EXCHANGE_NAME, routingKey, properties, message.getBytes());
            System.out.println("Sent: " + requestNumber);
        }
    }
}
