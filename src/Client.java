import java.io.*;

import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Client implements Runnable {

    private Socket socket;
    private Server server;
    private String username;
    private HeartBeat heartBeat;

    Client(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            server.addClient(this);
            PrintWriter writer = new PrintWriter(socket.getOutputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            writer.println("HELO");
            writer.flush();

            String heloName = reader.readLine();
            String parts[] = heloName.split(" ");

            String name = parts[1];
            if (name.matches("^[a-zA-Z0-9_]+$")) {
                if (!server.loggedIn(name)) {
                    String encodedName = Encode(heloName);
                    writer.println("+OK " + encodedName);
                    writer.flush();
                    username = name;
                } else {
                    writer.println("-ERR user already logged in");
                    writer.flush();
                    return;
                }
            } else {
                writer.println("-ERR username has an invalid format");
                writer.flush();
                return;
            }

            while (true) {
                String message = reader.readLine();
                if (message.contains("QUIT")) {
                    writer.println("+OK Goodbye");
                    writer.flush();
                    socket.close();
                    return;
                } else if (message.contains("PONG")) {
                    heartBeat.stopTimer();
                } else {
                    System.out.println(message);
                    String encodedMessage = Encode(message);
                    writer.println("+OK " + encodedMessage);
                    writer.flush();
                    String spiltMessage[] = message.split(" ", 2);
                    String bcstmessage = username + ": " + spiltMessage[1];
                    server.bcstMessage(bcstmessage, this);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            server.disconnectClient(this);
            System.out.println("client stopt");
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

    Socket getSocket() {
        return socket;
    }

    void setHeartBeat(HeartBeat heartBeat) {
        this.heartBeat = heartBeat;
        heartBeat.setClient(this);
    }

    HeartBeat getHeartBeat() {
        return heartBeat;
    }

    String getUsername() {
        return username;
    }
}


