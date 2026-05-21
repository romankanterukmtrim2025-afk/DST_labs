package roman_dst;

import java.io.*;
import java.net.*;

public class TestServer {
    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(3000);
        System.out.println("Server started on port 3000...");

        while (true) {
            Socket client = serverSocket.accept();
            System.out.println("Client connected!");
            new Thread(() -> handleClient(client)).start();
        }
    }

    static void handleClient(Socket client) {
        try {
            DataInputStream in = new DataInputStream(client.getInputStream());
            DataOutputStream out = new DataOutputStream(client.getOutputStream());

            while (true) {
                int size = in.readInt();
                byte[] msg = new byte[size];
                in.readFully(msg);
                byte commandId = msg[0];

                System.out.println("Received command id: " + commandId);

                if (commandId == 1) { // ping
                    out.writeInt(1);
                    out.write(new byte[]{2});
                    out.flush();

                } else if (commandId == 3) { // echo
                    byte[] text = new byte[msg.length - 1];
                    System.arraycopy(msg, 1, text, 0, text.length);
                    out.writeInt(text.length);
                    out.write(text);
                    out.flush();

                } else if (commandId == 5) { // login
                    out.writeInt(1);
                    out.write(new byte[]{7});
                    out.flush();

                } else if (commandId == 10) { // list
                    byte[] payload = serializeStrings(new String[]{"Roman", "TestUser"});
                    out.writeInt(payload.length);
                    out.write(payload);
                    out.flush();

                } else if (commandId == 15) { // msg
                    out.writeInt(1);
                    out.write(new byte[]{16});
                    out.flush();

                } else if (commandId == 20) { // file
                    out.writeInt(1);
                    out.write(new byte[]{21});
                    out.flush();

                } else {
                    out.writeInt(1);
                    out.write(new byte[]{103});
                    out.flush();
                }
            }
        } catch (Exception e) {
            System.out.println("Client disconnected.");
        }
    }

    static byte[] serializeStrings(String[] strings) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(strings);
        oos.flush();
        return bos.toByteArray();
    }
}