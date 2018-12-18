import java.io.*;

import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Client implements Runnable {

    private Socket socket;

    Client(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            PrintWriter writer = new PrintWriter(socket.getOutputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            writer.println("HELO");
            writer.flush();

            String name = reader.readLine();
            System.out.println(name);

            String encodedName = Encode(name);

            writer.println("+OK " + encodedName);
            System.out.println("server: +OK " + encodedName);

            writer.flush();

            while (true) {
                String message = reader.readLine();
                if (message.contains("QUIT")) {
                    writer.println("+OK Goodbye");
                    writer.flush();
                    socket.close();
                    return;
                } else if (message.contains("PONG")) {
                    writer.println("PING");
                } else {
                    System.out.println(message);
                    message = Encode(message);
                    writer.println("+OK " + message);
                    writer.flush();
                }
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private String Encode(String line) {

        try {
            byte[] bytes = line.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] newLine = md.digest(bytes);
            return new String(Base64.getEncoder().encode(newLine));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

}


