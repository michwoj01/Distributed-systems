import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Admin {
    private static final String EXCHANGE_NAME = "mainExchange";
    private static final String ADMIN_EXCHANGE = "adminExchange";
    private static final String ADMIN_QUEUE = "adminQueue";

    public static void main(String[] argv) throws Exception {
        boolean repeat = true;
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(ADMIN_EXCHANGE, BuiltinExchangeType.TOPIC);

        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
        channel.queueDeclare(ADMIN_QUEUE, true, false, false, null);
        channel.queueBind(ADMIN_QUEUE, EXCHANGE_NAME, "passengers");
        channel.queueBind(ADMIN_QUEUE, EXCHANGE_NAME, "cargo");
        channel.queueBind(ADMIN_QUEUE, EXCHANGE_NAME, "satellites");

        channel.basicConsume(ADMIN_QUEUE, true, (consumerTag, delivery) -> {
            String adminMessage = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("Received message from agency: " + adminMessage);
        }, consumerTag -> {
        });

        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .correlationId("admin")
                .build();

        String choose = """
                 Choose type of admin notification:
                 1. Only agencies
                 2. Only carters
                 3. Both carters and agiencies
                """;
        System.out.print(choose);

        String routingKey;
        while (repeat) {

            routingKey = null;
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String requestType = br.readLine();
            switch (requestType) {
                case "1" -> routingKey = "agency";
                case "2" -> routingKey = "carter";
                case "3" -> routingKey = "agency.carter";
                case "4" -> repeat = false;
                default -> {
                    System.out.println("Bad input");
                    continue;
                }
            }
            channel.basicPublish(ADMIN_EXCHANGE, routingKey, properties, "Admin notification".getBytes());
        }
    }
}
