import java.io.*;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
        ServerMessage joinMessage = new ServerMessage(ServerMessage.MessageType.GRP_JOIN,
            username + " " + groupName);

        Group group = getGroupByGroupName(groupName);
        if (group != null && !group.isUserInGroup(this)) {
            print("+OK " + Encode(message));
            group.addClient(this);

            //TODO group.sendMessage(); protocol
            group.sendMessage(joinMessage.toString());

        } else if (group == null) print("–ERR group doesn't exist ");
        else print("-ERR already part of group: " + groupName);
    }

    private void leaveGroup(String message) {
        String[] splitMessage = message.split(" ", 2);
        String groupName = splitMessage[1];
        ServerMessage leaveMessage = new ServerMessage(ServerMessage.MessageType.GRP_LEAVE,
            groupName + " " + username);

        Group group = getGroupByGroupName(groupName);
        if (group != null && !group.isUserInGroup(this)) {
            print("+OK " + Encode(message));
            group.removeClient(this);

            //TODO group.sendMessage(); protocol
            group.sendMessage(leaveMessage.toString());

        } else if (group == null) print("–ERR group doesn't exist ");
        else print("-ERR not part of group: " + groupName);
    }

    /**
     * The kickUserFromGroup method removes a user from a group. It does so by firstly retrieving the group name and
     * the name of the client that needs to be removed from the group. Then the group is looked up with use of the
     * getGroupByGroupName method. When the group is retrieved it checks if the group isn't null and this client is
     * the group leader. Else if the group is null this client will get an ERR message containing the message "group
     * doesn't exists", or when the group isn't null but this client isn't the leader this client will get an ERR
     * message containing: "you must be the owner of the group to kick people". If the group isn't null and this client
     * is the group leader the client that needs to be removed will be retrieved and checked if that client is actually
     * in the group. If the client isn't null and is in the group this client will get an OK message and the client will
     * be removed from the group. Else if client is null or the user isn't in the group this client will get an ERR
     * message containing the message: "user is not part of the group".
     *
     * @param message A message that contains the command, group name and the name of the client that has to be removed
     */
    private void kickUserFromGroup(String message) {
        String[] splitMessage = message.split(" ", 3);
        String groupName = splitMessage[1];
        String clientName = splitMessage[2];
        ServerMessage clientKickMessage = new ServerMessage(ServerMessage.MessageType.GRP_KICK,
            groupName);
        ServerMessage groupKickMessage = new ServerMessage(ServerMessage.MessageType.GRP_KICK,
            groupName + " " + username);

        Group group = getGroupByGroupName(groupName); // get group
        if (group != null && group.isLeader(username)) { // check if group exists and if this client is the group leader
            Client client = getClientByUserName(clientName); // get client that needs to be removed from group
            if (client != null && group.isUserInGroup(client)) { // check if client exists and is in the group
                print("+OK " + Encode(message)); // print OK message
                group.removeClient(client); // remove client from group

                //TODO client.print(); protocol
                client.sendGroupMessage(clientKickMessage.toString());
                group.sendMessage(groupKickMessage.toString());
            } else print("-ERR user is not part of the group ");

        } else if (group == null) print("–ERR group doesn't exist ");
        else print("-ERR you must be the owner of the group to kick people ");
    }

    /**
     * The printGroupNames simply prints all the names of the groups that are created on the server to the client who
     * requested a list of group names
     */
    private void printGroupNames() {
        StringBuilder userNameList = new StringBuilder();
        userNameList.append("+OK ");
        for (Group group : server.getGroups()) {
            userNameList.append(group.getName()).append(", \n");
        }
        print(userNameList.toString());
    }

    /**
     * The printUsernameList method simply prints all the names of the clients connected to the server to the client who
     * requested a list of usernames.
     */
    private void printUsernameList() {
        StringBuilder userNameList = new StringBuilder();
        userNameList.append("+OK ");
        for (Client client : server.getClients()) {
            userNameList.append(client.getUsername()).append(", \n");
        }
        print(userNameList.toString());
    }

    /**
     * The sendGroupMessage method will send a message to all the users in a certain group. It does so by firstly
     * splitting the message into three parts, the command, the name of the group and the message the client send to the
     * group. Then a ServerMessage will created which will contains the MessageType GRP_SEND, the group name and the
     * message the user wants to send to the group. After the group is looked up by the getGroupByGroupName method there
     * is a check to see if the group exists and the user, that wants to send the message to the group, is actually part
     * of that group.If the group doesn't exist the user will get an error saying the group doesn't exists. Else if the
     * group exists but the user isn't part of the group the user will get an error saying he isn't part of the group.
     * If the group exists and the user is part of the group the message will be send to all clients in that group.
     *
     * @param message A message that contains all elements of the message the user send to the server which should be
     *                converted like "GRP_SEND groupName message"
     */
    private void sendGroupMessage(String message) {
        String[] splitMessage = message.split(" ", 3);
        String groupName = splitMessage[1];
        ServerMessage groupMessage = new ServerMessage(ServerMessage.MessageType.GRP_SEND, groupName + " " +
                username + " " + splitMessage[2]);

        Group group = getGroupByGroupName(groupName); // get group
        if (group != null && group.isUserInGroup(this)) { // check if group exists & user is in the group
            group.sendMessage(groupMessage.toString()); // send message to all clients in group
        } else if (group == null) print("-ERR group doesn't exist");
        else print("-ERR must be part of the group before you’re able to send messages in it");

    }

    /**
     * The getClientByUserName method searches for a client with a certain username within the server.
     *
     * @param userName The username of the client that needs to be looked up
     * @return Returns the client with the same username, else returns null
     */
    private Client getClientByUserName(String userName) {
        for (Client client : server.getClients()) {
            if (client.getUsername().equals(userName)) {
                return client;
            }
        }
        return null;
    }

    /**
     * The getGroupByGroupName method searches for a group with a certain groupName within the server.
     *
     * @param groupName the name of the group that needs to be looked up
     * @return Returns the group with the same groupName, else returns null
     */
    private Group getGroupByGroupName(String groupName) {
        for (Group group : server.getGroups()) {
            if (group.getName().equals(groupName)) {
                return group;
            }
        }
        return null;
    }

    /**
     * The isUserLoggedIn method checks if a user with a certain username exists within the client list
     *
     * @param userName The userName that needs to be checked
     * @return Returns true when there is a user with a certain username within the client list, else returns false
     */
    private boolean isUserLoggedIn(String userName) {
        for (Client client : server.getClients()) {
            if (userName.equals(client.getUsername())) return true;
        }
        return false;
    }

    /**
     * The print method prints the message, the method receives, to the client
     *
     * @param message The message that needs to be printed to the client
     */
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


