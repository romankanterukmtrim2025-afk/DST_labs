package roman_dst;

import java.util.Arrays;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import lpi.server.soap.IServer;
import lpi.server.soap.IServer.FileInfo;
import lpi.server.soap.IServer.Message;

public class MyMain {
    static String sessionId = null;
    static IServer proxy;
    static Timer timer;
    static String[] lastUsers = {};

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "lv.rst.uk.to";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 153;

        try {
            // Підключення до SOAP сервера
            javax.xml.ws.Service service = javax.xml.ws.Service.create(
                new java.net.URL("http://" + host + ":" + port + "/chat?wsdl"),
                new javax.xml.namespace.QName("http://soap.server.lpi/", "ChatServer")
            );
            proxy = service.getPort(IServer.class);
            System.out.println("Connected to SOAP server!");
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

            try {
                if (command.equals("exit")) {
                    if (sessionId != null) proxy.exit(sessionId);
                    if (timer != null) timer.cancel();
                    System.out.println("Goodbye!");
                    break;

                } else if (command.equals("ping")) {
                    proxy.ping();
                    System.out.println("Pong!");

                } else if (command.equals("echo")) {
                    String text = parts.length > 1 ? parts[1] : "";
                    String response = proxy.echo(text);
                    System.out.println("Echo: " + response);

                } else if (command.equals("login")) {
                    String[] p = parts[1].split("\\s+");
                    sessionId = proxy.login(p[0], p[1]);
                    System.out.println("Logged in! Session: " + sessionId);
                    startTimer();

                } else if (command.equals("list")) {
                    if (sessionId == null) { System.out.println("Please login first!"); continue; }
                    String[] users = proxy.listUsers(sessionId);
                    System.out.println("Users: " + Arrays.toString(users));

                } else if (command.equals("msg")) {
                    if (sessionId == null) { System.out.println("Please login first!"); continue; }
                    String[] p = parts[1].split("\\s+", 2);
                    Message msg = new Message(p[0], p[1]);
                    proxy.sendMessage(sessionId, msg);
                    System.out.println("Message sent!");

                } else if (command.equals("file")) {
                    if (sessionId == null) { System.out.println("Please login first!"); continue; }
                    String[] p = parts[1].split("\\s+", 2);
                    java.io.File file = new java.io.File(p[1]);
                    if (!file.exists()) { System.out.println("File not found!"); continue; }
                    FileInfo fileInfo = new FileInfo(p[0], file);
                    proxy.sendFile(sessionId, fileInfo);
                    System.out.println("File sent!");

                } else {
                    System.out.println("Unknown command.");
                }

            } catch (IServer.LoginException e) {
                System.out.println("Login error: " + e.getMessage());
            } catch (IServer.ArgumentException e) {
                System.out.println("Argument error: " + e.getMessage());
            } catch (IServer.ServerException e) {
                System.out.println("Server error: " + e.getMessage());
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
                    // Перевірка повідомлень
                    Message msg = proxy.receiveMessage(sessionId);
                    if (msg != null) {
                        System.out.println("\n[Message from " + msg.getSender() + "]: " + msg.getMessage());
                    }

                    // Перевірка файлів
                    FileInfo file = proxy.receiveFile(sessionId);
                    if (file != null) {
                        file.saveFileTo(new java.io.File("."));
                        System.out.println("\n[File received]: " + file.getFilename());
                    }

                    // Бонус 1: live список користувачів
                    String[] currentUsers = proxy.listUsers(sessionId);
                    String current = Arrays.toString(currentUsers);
                    if (!current.equals(Arrays.toString(lastUsers))) {
                        // Бонус 2: привітання новому користувачу
                        for (String user : currentUsers) {
                            boolean isNew = true;
                            for (String old : lastUsers) {
                                if (old.equals(user)) { isNew = false; break; }
                            }
                            if (isNew) {
                                System.out.println("[Update] User joined: " + user);
                                proxy.sendMessage(sessionId, new Message(user, "Hello there, " + user + "!"));
                            }
                        }
                        for (String old : lastUsers) {
                            boolean left = true;
                            for (String user : currentUsers) {
                                if (user.equals(old)) { left = false; break; }
                            }
                            if (left) System.out.println("[Update] User left: " + old);
                        }
                        lastUsers = currentUsers;
                    }

                } catch (Exception e) {}
            }
        }, 0, 1000);
    }
}