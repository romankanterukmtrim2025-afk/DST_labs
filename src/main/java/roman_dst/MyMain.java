package roman_dst;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class MyMain {
    static Socket socket;
    static DataInputStream in;
    static DataOutputStream out;

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 3000;

        try {
            socket = new Socket(host, port);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            System.out.println("Connected to " + host + ":" + port);
        } catch (Exception e) {
            System.out.println("Connection failed: " + e.getMessage());
            return;
        }

        Scanner scanner = new Scanner(System.in);

        final String[] lastUsers = {""};
        java.util.Timer timer = new java.util.Timer();
        timer.scheduleAtFixedRate(new java.util.TimerTask() {
            public void run() {
                try {
                    synchronized (socket) {
                        byte[] response = executeCommand(new byte[]{10});
                        if (response.length > 1) {
                            String[] users = (String[]) deserialize(response);
                            String current = java.util.Arrays.toString(users);
                            if (!current.equals(lastUsers[0])) {
                                System.out.println("[Update] Users online: " + current);
                                lastUsers[0] = current;
                            }
                        }
                    }
                } catch (Exception e) {}
            }
        }, 0, 1000);

        while (true) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+", 2);
            String command = parts[0];

            if (command.equals("exit")) {
                timer.cancel();
                socket.close();
                System.out.println("Disconnected.");
                break;

            } else if (command.equals("ping")) {
                byte[] response = executeCommand(new byte[]{1});
                System.out.println("Ping response: " + response[0]);

            } else if (command.equals("echo")) {
                String text = parts.length > 1 ? parts[1] : "";
                byte[] textBytes = text.getBytes();
                byte[] msg = new byte[textBytes.length + 1];
                msg[0] = 3;
                System.arraycopy(textBytes, 0, msg, 1, textBytes.length);
                byte[] response = executeCommand(msg);
                System.out.println("Echo: " + new String(response));

            } else if (command.equals("login")) {
                String[] p = parts[1].split("\\s+");
                byte[] payload = serializeStrings(new String[]{p[0], p[1]});
                byte[] msg = new byte[payload.length + 1];
                msg[0] = 5;
                System.arraycopy(payload, 0, msg, 1, payload.length);
                byte[] response = executeCommand(msg);
                if (response[0] == 6) System.out.println("Registered!");
                else if (response[0] == 7) System.out.println("Logged in!");
                else System.out.println("Error: " + response[0]);

            } else if (command.equals("list")) {
                byte[] response = executeCommand(new byte[]{10});
                if (response.length == 1) System.out.println("Error: " + response[0]);
                else {
                    String[] users = (String[]) deserialize(response);
                    System.out.println("Users: " + java.util.Arrays.toString(users));
                }

            } else if (command.equals("msg")) {
                String[] p = parts[1].split("\\s+", 2);
                byte[] payload = serializeStrings(new String[]{p[0], p[1]});
                byte[] msg = new byte[payload.length + 1];
                msg[0] = 15;
                System.arraycopy(payload, 0, msg, 1, payload.length);
                byte[] response = executeCommand(msg);
                if (response[0] == 16) System.out.println("Message sent!");
                else System.out.println("Error: " + response[0]);

            } else if (command.equals("file")) {
                String[] p = parts[1].split("\\s+", 2);
                String receiver = p[0];
                String filePath = p[1];

                java.io.File file = new java.io.File(filePath);
                if (!file.exists()) {
                    System.out.println("File not found!");
                    continue;
                }
                if (file.length() > 10 * 1024 * 1024) {
                    System.out.println("File too big! Max 10MB.");
                    continue;
                }

                byte[] fileBytes = java.nio.file.Files.readAllBytes(file.toPath());
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(new Object[]{receiver, file.getName(), fileBytes});
                oos.flush();
                byte[] payload = bos.toByteArray();

                byte[] msg = new byte[payload.length + 1];
                msg[0] = 20;
                System.arraycopy(payload, 0, msg, 1, payload.length);
                byte[] response = executeCommand(msg);
                if (response[0] == 21) System.out.println("File sent!");
                else System.out.println("Error: " + response[0]);

            } else {
                System.out.println("Unknown command.");
            }
        }
        scanner.close();
    }

    static byte[] executeCommand(byte[] command) throws Exception {
        synchronized (socket) {
            out.writeInt(command.length);
            out.write(command);
            out.flush();
            int responseSize = in.readInt();
            byte[] response = new byte[responseSize];
            in.readFully(response);
            return response;
        }
    }

    static byte[] serializeStrings(String[] strings) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(strings);
        oos.flush();
        return bos.toByteArray();
    }

    static Object deserialize(byte[] data) throws Exception {
        return new ObjectInputStream(new ByteArrayInputStream(data)).readObject();
    }
}