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
                        createGroup(message);
                        break;
                    case "GRP_JOIN":
                        joinGroup(message);
                        break;
                    case "GRP_KICK":
                        kickUserFromGroup(message);
                        break;
                    case "GRP_LEAVE":
                        leaveGroup(message);
                        break;
                    case "GRP_LIST":
                        printGroupNames();
                        break;
                    case "GRP_SEND":
                        sendGroupMessage(message);
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

    private void createGroup(String message) {
        String[] splitString = message.split(" ", 2);
        String groupName = splitString[1];
        if (groupName.matches("^[a-zA-Z0-9_]+$")) { // check if groupName has correct format
            if (getGroupByGroupName(groupName) == null) { // check if group exists
                print("+OK " + groupName);
                Group group = new Group(groupName, this);
                server.addGroup(group);
            } else {
                print("-ERR groupname already exists");
            }
        } else {
            print("-ERR groupname has an invalid format");
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

    private void broadcastMessage(String message) {
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
            if (!isUserLoggedIn(name)) {
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

    private void directMessage(String message) {
        String[] splitMessage = message.split(" ", 3);
        ServerMessage directMessage = new ServerMessage(ServerMessage.MessageType.PM,
                username + " " + splitMessage[2]);
        String receivingUser = splitMessage[1];

        if (isUserLoggedIn(receivingUser)) {
            print("+OK " + Encode(message));
            for (Client c : server.getClients()) {
                if (c.getUsername().equals(receivingUser)) {
                    c.print(directMessage.toString());
                    return;
                }
            }
        } else print("-ERR User is not logged on");
    }

    private void joinGroup(String message) {
        String[] splitMessage = message.split(" ", 2);
        String groupName = splitMessage[1];

        Group group = getGroupByGroupName(groupName);
        if (group != null && !group.isUserInGroup(this)) {
            print("+OK " + Encode(message));
            group.addClient(this);
            //TODO group.sendMessage(); protocol
        } else if (group == null) print("–ERR group doesn't exist ");
        else print("-ERR already part of group: " + groupName);
    }

    private void leaveGroup(String message) {
        String[] splitMessage = message.split(" ", 2);
        String groupName = splitMessage[1];


        Group group = getGroupByGroupName(groupName);
        if (group != null && !group.isUserInGroup(this)) {
            print("+OK " + Encode(message));
            group.removeClient(this);
            //TODO group.sendMessage(); protocol
        } else if (group == null) print("–ERR group doesn't exist ");
        else print("-ERR not part of group: " + groupName);
    }


    private void kickUserFromGroup(String message) {
        String[] splitMessage = message.split(" ", 3);
        String groupName = splitMessage[1];
        String clientName = splitMessage[2];

        Group group = getGroupByGroupName(groupName);
        if (group != null && group.isLeader(username)) {
            Client client = getClientByUserName(clientName);
            if (client != null && group.isUserInGroup(client)) {
                print("+OK " + Encode(message));
                group.removeClient(client);
                //TODO client.print(); protocol
            } else print("-ERR user is not part of the group ");

        } else if (group == null) print("–ERR group doesn't exist ");
        else print("-ERR you must be the owner of the group to kick people ");
    }

    private void printGroupNames() {
        StringBuilder userNameList = new StringBuilder();
        userNameList.append("+OK ");
        for (Group group : server.getGroups()) {
            userNameList.append(group.getName()).append(", \n");
        }
        print(userNameList.toString());
    }

    private void printUsernameList() {
        StringBuilder userNameList = new StringBuilder();
        userNameList.append("+OK ");
        for (Client client : server.getClients()) {
            userNameList.append(client.getUsername()).append(", \n");
        }
        print(userNameList.toString());
    }

    private void sendGroupMessage(String message) {
        String[] splitMessage = message.split(" ", 3);
        String groupName = splitMessage[1];
        ServerMessage groupMessage = new ServerMessage(ServerMessage.MessageType.GRP_SEND, groupName + " " +
                username + " " + splitMessage[2]);

        Group group = getGroupByGroupName(groupName); // get group
        if (group != null && group.isUserInGroup(this)) { // check if user is in the group
            group.sendMessage(groupMessage.toString());
        } else if (group == null) print("-ERR group doesn't exist");
        else print("-ERR must be part of the group before you’re able to send messages in it");

    }

    private Client getClientByUserName(String userName) {
        for (Client client : server.getClients()) {
            if (client.getUsername().equals(userName)) {
                return client;
            }
        }
        return null;
    }

    private Group getGroupByGroupName(String groupName) {
        for (Group group : server.getGroups()) {
            if (group.getName().equals(groupName)) {
                return group;
            }
        }
        return null;
    }

    private boolean isUserLoggedIn(String userName) {
        for (Client client : server.getClients()) {
            if (userName.equals(client.getUsername())) return true;
        }
        return false;
    }

    void print(String message) {
        writer.println(message);
        writer.flush();
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


