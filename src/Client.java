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
    private PrintWriter writer;

    private boolean running = true;

    Client(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            server.addClient(this);
            writer = new PrintWriter(socket.getOutputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            print("HELO");

            String heloName = reader.readLine();
            String parts[] = heloName.split(" ");

            String name = parts[1];
            if (name.matches("^[a-zA-Z0-9_]+$")) {
                if (!server.isUserLoggedIn(name)) {
                    print("+OK " + Encode(heloName));
                    username = name;
                } else {
                    print("-ERR user already logged in");
                    socket.close();
                    return;
                }
            } else {
                print("-ERR username has an invalid format");
                socket.close();
                return;
            }

            while (running) {
                String message = reader.readLine();
                if (message.contains("QUIT")) {
                    print("+OK Goodbye");
                    socket.close();
                    return;
                } else if (message.contains("PONG")) {
                    heartBeat.stopTimer();
                } else if (message.contains("CLTLIST")){
                    server.getClientList(this);
                } else {
                    print("+OK " + Encode(message));
                    broadcastMessage(message);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            server.disconnectClient(this);
            System.out.println("client stops");
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

    private void broadcastMessage(String message) throws IOException {
        String spiltMessage[] = message.split(" ", 2);
        String broadcastMessage = username + ": " + spiltMessage[1];
        server.broadcastMessage(broadcastMessage, this);
    }

    private void print(String message) {
        writer.println(message);
        writer.flush();
    }

    void stop() {
        running = false;
    }

    //getters
    Socket getSocket() {
        return socket;
    }

    HeartBeat getHeartBeat() {
        return heartBeat;
    }

    String getUsername() {
        return username;
    }

    //setters
    void setHeartBeat(HeartBeat heartBeat) {
        this.heartBeat = heartBeat;
        heartBeat.setClient(this);
    }
}


