package roman_dst;

import java.io.*;
import java.time.Instant;
import java.util.*;
import javax.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import lpi.server.mq.FileInfo;

public class MyMain {
    static Connection connection;
    static Session session;
    static Session asyncSession;
    static String currentLogin;
    static Instant lastAction = Instant.now();

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "lv.rst.uk.to";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 61616;
        String brokerUrl = "tcp://" + host + ":" + port;

        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
            factory.setTrustedPackages(Arrays.asList("lpi.server.mq"));
            connection = factory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            asyncSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            System.out.println("Connected to ActiveMQ: " + brokerUrl);
        } catch (Exception e) {
            System.out.println("Connection failed: " + e.getMessage());
            return;
        }

        Scanner scanner = new Scanner(System.in);
        while (true) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+", 2);
            String command = parts[0];
            lastAction = Instant.now();

            try {
                if (command.equals("exit")) {
                    sendAndReceive("chat.exit", session.createMessage());
                    asyncSession.close();
                    session.close();
                    connection.close();
                    System.out.println("Goodbye!");
                    break;

                } else if (command.equals("ping")) {
                    sendAndReceive("chat.diag.ping", session.createMessage());
                    System.out.println("Pong!");

                } else if (command.equals("echo")) {
                    String text = parts.length > 1 ? parts[1] : "";
                    Message response = sendAndReceive("chat.diag.echo", session.createTextMessage(text));
                    if (response instanceof TextMessage)
                        System.out.println("Echo: " + ((TextMessage) response).getText());

                } else if (command.equals("login")) {
                    String[] p = parts[1].split("\\s+");
                    currentLogin = p[0];
                    MapMessage request = session.createMapMessage();
                    request.setString("login", p[0]);
                    request.setString("password", p[1]);
                    Message response = sendAndReceive("chat.login", request);
                    if (response instanceof MapMessage) {
                        MapMessage mapResp = (MapMessage) response;
                        if (mapResp.getBoolean("success")) {
                            System.out.println("Logged in!");
                            startMessageListener();
                            startFileListener();
                        } else {
                            System.out.println("Login failed: " + mapResp.getString("message"));
                            currentLogin = null;
                        }
                    }

                } else if (command.equals("list")) {
                    if (currentLogin == null) { System.out.println("Please login first!"); continue; }
                    Message response = sendAndReceive("chat.listUsers", session.createMessage());
                    if (response instanceof ObjectMessage) {
                        Object obj = ((ObjectMessage) response).getObject();
                        if (obj instanceof String[])
                            System.out.println("Users: " + Arrays.toString((String[]) obj));
                    }

                } else if (command.equals("msg")) {
                    if (currentLogin == null) { System.out.println("Please login first!"); continue; }
                    String[] p = parts[1].split("\\s+", 2);
                    MapMessage request = session.createMapMessage();
                    request.setString("receiver", p[0]);
                    request.setString("message", p[1]);
                    Message response = sendAndReceive("chat.sendMessage", request);
                    if (response instanceof MapMessage) {
                        MapMessage mapResp = (MapMessage) response;
                        if (mapResp.getBoolean("success")) System.out.println("Message sent!");
                        else System.out.println("Error: " + mapResp.getString("message"));
                    }

                } else if (command.equals("file")) {
                    if (currentLogin == null) { System.out.println("Please login first!"); continue; }
                    String[] p = parts[1].split("\\s+", 2);
                    File file = new File(p[1]);
                    if (!file.exists()) { System.out.println("File not found!"); continue; }
                    FileInfo fileInfo = new FileInfo(p[0], file);
                    fileInfo.setSender(currentLogin);
                    ObjectMessage request = session.createObjectMessage(fileInfo);
                    Message response = sendAndReceive("chat.sendFile", request);
                    if (response instanceof MapMessage) {
                        MapMessage mapResp = (MapMessage) response;
                        if (mapResp.getBoolean("success")) System.out.println("File sent!");
                        else System.out.println("Error: " + mapResp.getString("message"));
                    }

                } else {
                    System.out.println("Unknown command.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        scanner.close();
    }

    static Message sendAndReceive(String queueName, Message msg) throws Exception {
        Destination targetQueue = session.createQueue(queueName);
        Destination replyQueue = session.createTemporaryQueue();
        msg.setJMSReplyTo(replyQueue);
        MessageProducer producer = session.createProducer(targetQueue);
        MessageConsumer consumer = session.createConsumer(replyQueue);
        producer.send(msg);
        Message response = consumer.receive(2000);
        consumer.close();
        producer.close();
        return response;
    }

    static void startMessageListener() throws Exception {
        Destination queue = asyncSession.createQueue("chat.messages");
        MessageConsumer consumer = asyncSession.createConsumer(queue);
        consumer.setMessageListener(message -> {
            try {
                if (message instanceof MapMessage) {
                    MapMessage mapMsg = (MapMessage) message;
                    String sender = mapMsg.getString("sender");
                    String text = mapMsg.getString("message");
                    System.out.println("\n[Message from " + sender + "]: " + text);

                    // Бонус: автовідповідь якщо AFK > 5 хвилин
                    long idleMinutes = (Instant.now().getEpochSecond() - lastAction.getEpochSecond()) / 60;
                    if (idleMinutes >= 5) {
                        MapMessage reply = session.createMapMessage();
                        reply.setString("receiver", sender);
                        reply.setString("message", "Sorry, I'm AFK, will answer ASAP");
                        sendAndReceive("chat.sendMessage", reply);
                        System.out.println("[Auto-reply sent to " + sender + "]");
                    }
                }
            } catch (Exception e) {}
        });
    }

    static void startFileListener() throws Exception {
        Destination queue = asyncSession.createQueue("chat.files");
        MessageConsumer consumer = asyncSession.createConsumer(queue);
        consumer.setMessageListener(message -> {
            try {
                if (message instanceof ObjectMessage) {
                    Object obj = ((ObjectMessage) message).getObject();
                    if (obj instanceof FileInfo) {
                        FileInfo fileInfo = (FileInfo) obj;
                        fileInfo.saveFileTo(new File("."));
                        System.out.println("\n[File received]: " + fileInfo.getFilename());
                    }
                }
            } catch (Exception e) {}
        });
    }
}