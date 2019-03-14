import java.io.*;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

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
            writer = new PrintWriter(socket.getOutputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            print(ServerMessage.MessageType.HELO.toString());

            checkUserName(reader.readLine());

            while (running) {
                String message = reader.readLine();
                String[] splitMessage = message.split(" ");
                switch (splitMessage[0]) {
                    case "BCST":
                        print("+OK " + Encode(message));
                        broadcastMessage(message);
                        break;
                    case "CLTLIST":
                        printUsernameList();
                        break;
                    case "GRP_CREATE":
                        break;
                    case "GRP_JOIN":
                        break;
                    case "GRP_KICK":
                        break;
                    case "GRP_LEAVE":
                        break;
                    case "GRP_LIST":
                        break;
                    case "GRP_SEND":
                        break;
                    case "PM":
                        directMessage(message);
                        break;
                    case "PONG":
                        heartBeat.stopTimer();
                        break;
                    case "QUIT":
                        print("+OK Goodbye");
                        socket.close();
                        return;
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
            byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] newLine = md.digest(bytes);
            return new String(Base64.getEncoder().encode(newLine));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void broadcastMessage(String message) throws IOException {
        String[] spiltMessage = message.split(" ", 2);
        ServerMessage broadcastMessage = new ServerMessage(ServerMessage.MessageType.BCST,
                username + " " + spiltMessage[1]);
        for (Client c : server.getClients()) {
            if (!c.getSocket().equals(socket)) c.print(broadcastMessage.toString());
        }
    }

    private void checkUserName(String heloName) throws IOException {
        String[] parts = heloName.split(" ");
        String name = parts[1];

        if (name.matches("^[a-zA-Z0-9_]+$")) {
            if (!server.isUserLoggedIn(name)) {
                print("+OK " + Encode(heloName));
                username = name;
            } else {
                print("-ERR user already logged in");
                socket.close();
            }
        } else {
            print("-ERR username has an invalid format");
            socket.close();
        }
    }

    private void directMessage(String message) throws IOException {
        String[] splitMessage = message.split(" ", 3);
        ServerMessage directMessage = new ServerMessage(ServerMessage.MessageType.PM,
                username + " " + splitMessage[2]);
        String receivingUser = splitMessage[1];

        if (server.isUserLoggedIn(receivingUser)) {
            print("+OK " + Encode(message));
            for (Client c : server.getClients()) {
                if (c.getUsername().equals(receivingUser)){
                    c.print(directMessage.toString());
                    return;
                }
            }
        } else {
            print("-ERR User is not logged on");
        }
    }

    void print(String message) {
        writer.println(message);
        writer.flush();
    }

    private void printUsernameList() {
        List<String> usernameList = new ArrayList<>();
        for (Client client : server.getClients()) {
            usernameList.add(client.getUsername());
        }
    }

    void stop() {
        running = false;
    }

    //getters
    private Socket getSocket() {
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
    }
}


