package roman_dst;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import lpi.server.rmi.IServer;

public class MyMain {
    static String sessionId = null;
    static IServer proxy;
    static Timer timer;

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "lv.rst.uk.to";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 152;

        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            proxy = (IServer) registry.lookup(IServer.RMI_SERVER_NAME);
            System.out.println("Connected to RMI server!");
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
                    proxy.sendMessage(sessionId, new IServer.Message(p[0], p[1]));
                    System.out.println("Message sent!");

                } else if (command.equals("file")) {
                    if (sessionId == null) { System.out.println("Please login first!"); continue; }
                    String[] p = parts[1].split("\\s+", 2);
                    java.io.File file = new java.io.File(p[1]);
                    if (!file.exists()) { System.out.println("File not found!"); continue; }
                    proxy.sendFile(sessionId, new IServer.FileInfo(p[0], file));
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
        final String[] lastUsers = {""};
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    // Перевірка повідомлень
                    IServer.Message msg = proxy.receiveMessage(sessionId);
                    if (msg != null) {
                        System.out.println("\n[Message from " + msg.getSender() + "]: " + msg.getMessage());
                    }

                    // Перевірка файлів
                    IServer.FileInfo file = proxy.receiveFile(sessionId);
                    if (file != null) {
                        file.saveFileTo(new java.io.File("."));
                        System.out.println("\n[File received]: " + file.getFilename());
                    }

                    // Бонус: live список користувачів
                    String[] users = proxy.listUsers(sessionId);
                    String current = Arrays.toString(users);
                    if (!current.equals(lastUsers[0])) {
                        System.out.println("[Update] Users online: " + current);
                        lastUsers[0] = current;
                    }

                } catch (Exception e) {}
            }
        }, 0, 1000);
    }
}