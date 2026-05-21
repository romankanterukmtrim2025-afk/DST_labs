package roman_dst;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import javax.ws.rs.client.*;
import javax.ws.rs.core.*;
import javax.xml.bind.annotation.*;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

public class MyMain {
    static Client client;
    static String baseUrl;
    static String currentLogin;
    static String currentPassword;
    static Timer timer;
    static String[] lastUsers = {};

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class UserInfo {
        public String login;
        public String password;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class WrappedList {
        public List<String> items;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class MessageInfo {
        public String sender;
        public String message;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class FileData {
        public String sender;
        public String filename;
        public String content;
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "lv.rst.uk.to";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 154;
        baseUrl = "http://" + host + ":" + port + "/chat/server";

        client = ClientBuilder.newClient();
        System.out.println("REST client ready. Server: " + baseUrl);

        Scanner scanner = new Scanner(System.in);
        while (true) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+", 2);
            String command = parts[0];

            try {
                if (command.equals("exit")) {
                    if (timer != null) timer.cancel();
                    if (client != null) client.close();
                    System.out.println("Goodbye!");
                    break;

                } else if (command.equals("ping")) {
                    String response = client.target(baseUrl + "/ping")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get(String.class);
                    System.out.println("Ping: " + response);

                } else if (command.equals("echo")) {
                    String text = parts.length > 1 ? parts[1] : "";
                    String response = client.target(baseUrl + "/echo")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .post(Entity.text(text), String.class);
                    System.out.println("Echo: " + response);

                } else if (command.equals("login")) {
                    String[] p = parts[1].split("\\s+");
                    currentLogin = p[0];
                    currentPassword = p[1];

                    UserInfo userInfo = new UserInfo();
                    userInfo.login = currentLogin;
                    userInfo.password = currentPassword;

                    Response response = client.target(baseUrl + "/user")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .put(Entity.entity(userInfo, MediaType.APPLICATION_JSON_TYPE));

                    if (response.getStatus() == 201) System.out.println("Registered!");
                    else if (response.getStatus() == 202) System.out.println("Logged in!");
                    else { System.out.println("Error: " + response.getStatus()); continue; }

                    client.register(HttpAuthenticationFeature.basic(currentLogin, currentPassword));
                    startTimer();

                } else if (command.equals("list")) {
                    if (currentLogin == null) { System.out.println("Please login first!"); continue; }
                    WrappedList users = client.target(baseUrl + "/users")
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get(WrappedList.class);
                    System.out.println("Users: " + users.items);

                } else if (command.equals("msg")) {
                    if (currentLogin == null) { System.out.println("Please login first!"); continue; }
                    String[] p = parts[1].split("\\s+", 2);
                    Response response = client.target(baseUrl + "/" + p[0] + "/messages")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .post(Entity.entity("\"" + p[1] + "\"", MediaType.APPLICATION_JSON_TYPE));
                    if (response.getStatus() == 201) System.out.println("Message sent!");
                    else System.out.println("Error: " + response.getStatus());

                } else if (command.equals("file")) {
                    if (currentLogin == null) { System.out.println("Please login first!"); continue; }
                    String[] p = parts[1].split("\\s+", 2);
                    File file = new File(p[1]);
                    if (!file.exists()) { System.out.println("File not found!"); continue; }

                    String encoded = Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));
                    FileData fileData = new FileData();
                    fileData.sender = currentLogin;
                    fileData.filename = file.getName();
                    fileData.content = encoded;

                    Response response = client.target(baseUrl + "/" + p[0] + "/files")
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .post(Entity.entity(fileData, MediaType.APPLICATION_JSON_TYPE));
                    if (response.getStatus() == 201) System.out.println("File sent!");
                    else System.out.println("Error: " + response.getStatus());

                } else {
                    System.out.println("Unknown command.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        scanner.close();
    }

    static void startTimer() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    // Отримання повідомлень
                    Response msgResp = client.target(baseUrl + "/" + currentLogin + "/messages")
                        .request(MediaType.APPLICATION_JSON_TYPE).get();
                    if (msgResp.getStatus() == 200) {
                        WrappedList ids = msgResp.readEntity(WrappedList.class);
                        for (String id : ids.items) {
                            MessageInfo msg = client.target(baseUrl + "/" + currentLogin + "/messages/" + id)
                                .request(MediaType.APPLICATION_JSON_TYPE).get(MessageInfo.class);
                            System.out.println("\n[Message from " + msg.sender + "]: " + msg.message);
                            client.target(baseUrl + "/" + currentLogin + "/messages/" + id)
                                .request().delete();
                        }
                    }

                    // Отримання файлів
                    Response fileResp = client.target(baseUrl + "/" + currentLogin + "/files")
                        .request(MediaType.APPLICATION_JSON_TYPE).get();
                    if (fileResp.getStatus() == 200) {
                        WrappedList ids = fileResp.readEntity(WrappedList.class);
                        for (String id : ids.items) {
                            FileData fd = client.target(baseUrl + "/" + currentLogin + "/files/" + id)
                                .request(MediaType.APPLICATION_JSON_TYPE).get(FileData.class);
                            byte[] content = Base64.getDecoder().decode(fd.content);
                            Files.write(new File(fd.filename).toPath(), content);
                            System.out.println("\n[File received]: " + fd.filename);
                            client.target(baseUrl + "/" + currentLogin + "/files/" + id)
                                .request().delete();
                        }
                    }

                    // Бонус: live список користувачів
                    WrappedList users = client.target(baseUrl + "/users")
                        .request(MediaType.APPLICATION_JSON_TYPE).get(WrappedList.class);
                    String[] currentUsers = users.items.toArray(new String[0]);
                    if (!Arrays.toString(currentUsers).equals(Arrays.toString(lastUsers))) {
                        for (String u : currentUsers) {
                            if (!Arrays.asList(lastUsers).contains(u))
                                System.out.println("[Update] User joined: " + u);
                        }
                        for (String u : lastUsers) {
                            if (!Arrays.asList(currentUsers).contains(u))
                                System.out.println("[Update] User left: " + u);
                        }
                        lastUsers = currentUsers;
                    }

                } catch (Exception e) {}
            }
        }, 0, 1000);
    }
}