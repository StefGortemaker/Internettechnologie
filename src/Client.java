import java.io.*;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * The Client class handles all incoming messages from clients that are connected to the server
 */

public class Client implements Runnable {

    private Socket socket;
    private Server server;
    private String username;
    private HeartBeat heartBeat;
    private PrintWriter writer;

    private Encyptor encyptor;

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
            encyptor = new Encyptor();

            print(ServerMessage.MessageType.HELO.toString());
            checkUserName(reader.readLine());

            while (running) {
                String encryptedMessage = reader.readLine();
                String decryptedMessage = encyptor.decrypt(encryptedMessage);
                String[] splitMessage = decryptedMessage.split(" ");
                switch (splitMessage[0]) {
                    case "BCST":
                        print("+OK " + Encode(decryptedMessage));
                        broadcastMessage(decryptedMessage);
                        break;
                    case "CLTLIST":
                        printUsernameList();
                        break;
                    case "GRP_CREATE":
                        createGroup(decryptedMessage);
                        break;
                    case "GRP_JOIN":
                        joinGroup(decryptedMessage);
                        break;
                    case "GRP_KICK":
                        kickUserFromGroup(decryptedMessage);
                        break;
                    case "GRP_LEAVE":
                        leaveGroup(decryptedMessage);
                        break;
                    case "GRP_LIST":
                        printGroupNames();
                        break;
                    case "GRP_SEND":
                        sendGroupMessage(decryptedMessage);
                        break;
                    case "PM":
                        directMessage(decryptedMessage);
                        break;
                    case "PONG":
                        heartBeat.stopTimer();
                        break;
                    case "REQ_FILE":
                        requestFileTransfer(decryptedMessage);
                        break;
                    case "ACCEPT_FILE":
                        acceptFileTransfer(decryptedMessage);
                        break;
                    case "DENY_FILE":
                        denyFileTransfer(decryptedMessage);
                        break;
                    case "TRANSFER_FILE":
                        fileTransfer(decryptedMessage);
                        break;
                    case "FILE_RECEIVED":
                        fileReceived(decryptedMessage);
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

    /**
     * The acceptFileTransfer method sends a message to accept the file transfer.
     *
     * @param message A message that contains the command, the client's user name that send the file transfer request
     *                and the file name.
     */
    private void acceptFileTransfer(String message) {
        String[] splitString = message.split(" ", 3);
        Client client = getClientByUserName(splitString[1]);
        if (client != null) {
            ServerMessage acceptFileMessage = new ServerMessage(ServerMessage.MessageType.ACCEPT_FILE, username
                    + " " + splitString[2]);
            client.print(acceptFileMessage.toString());
        } else print("-ERR User is not logged on");
    }

    /**
     * The broadcastMessage method will broadcast a message to all user connected to the server.
     *
     * @param message A message that contains the command, the client's username and the message the client sends.
     */
    private void broadcastMessage(String message) {
        String[] spiltMessage = message.split(" ", 2);
        ServerMessage broadcastMessage = new ServerMessage(ServerMessage.MessageType.BCST,
                username + " " + spiltMessage[1]);

        for (Client c : server.getClients()) {
            if (!c.getSocket().equals(socket)) c.print(broadcastMessage.toString());
        }
    }

    /**
     * The checkuserName method checks whether a username is already taken. It does so by checking whether the username
     * has the correct format, if not the user gets an error message. If the username has the correct format it is
     * checked if the username isn't already in use, if the username is already in use the client gets an error message.
     * Else the client is given the username.
     *
     * @param heloName A message that contains the command helo and the username this client chose.
     */
    private void checkUserName(String heloName) throws IOException {
        String decryptedHeloName = encyptor.decrypt(heloName);
        String[] parts = decryptedHeloName.split(" ");
        String name = parts[1];

        if (name.matches("^[a-zA-Z0-9_]+$")) { // check if username has correct format
            if (!isUserLoggedIn(name)) { // check if user already exists
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

    /**
     * The createGroup method allows clients to create a group. It does so by checking whether the group name has the
     * correct format, if not the user gets an error message. If the group name has the correct format it is checked if
     * the group doesn't already exists, if the group already exists the client gets an error message. Else the group
     * is created and this client is added and becomes the leader of the group.
     *
     * @param message A message that contains the command and the group name.
     */
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

    /**
     * The denyFileTransfer method sends a message to deny the file transfer.
     *
     * @param message A message that contains the command, the client's user name that send the file transfer request
     *                and the file name.
     */
    private void denyFileTransfer(String message) {
        String[] splitString = message.split(" ", 3);
        Client client = getClientByUserName(splitString[1]);
        if (client != null) {
            ServerMessage acceptFileMessage = new ServerMessage(ServerMessage.MessageType.DENY_FILE, username
                    + " " + splitString[2]);
            client.print(acceptFileMessage.toString());
        } else print("-ERR User is not logged on");
    }

    /**
     * The directMessage method sends a message to one particular user. It does so by checking whether the user is
     * connected to the server. If the receiving user is connected this client will receive an +OK message and the
     * receiving client will receive the message. Else if the receiving client isn't connected this client will get an
     * error message containing that the receiving user isn't logged on.
     *
     * @param message A message that contains the command, the receiving client's username and the message send.
     */
    private void directMessage(String message) {
        String[] splitMessage = message.split(" ", 3);
        ServerMessage directMessage = new ServerMessage(ServerMessage.MessageType.PM,
                username + " " + splitMessage[2]);
        String receivingUser = splitMessage[1];
        Client client = getClientByUserName(receivingUser);
        if (client != null) {
            print("+OK " + Encode(message));
            client.print(directMessage.toString());
        } else print("-ERR User is not logged on");
    }


    /**
     * The encode method encodes lines with MD5 and encodes the encoded line with Base64.
     *
     * @param line The line that needs to be encoded
     * @return The encoded line
     */
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

    /**
     * The fileTransfer method will send an incoming file from this client, to another client. It does so by first
     * creating a DataInputStream from this client, to read the file, and a DataOutputStream for the socket of the
     * receiving user, to write the file. The it will send the filename and the size of the file. After it will read the
     * file from the inputStream whilst writing it to the OutputStream. At last it will flush the outputStream to send
     * the file to the receiving user.
     *
     * @param message A message that contains the command, the client's user name will receive the file and the file
     *                name.
     * @throws IOException Throws an exception when something went wrong whilst reading or sending the file over the
     *                     socket.
     */
    private void fileTransfer(String message) throws IOException {
        String[] splitMessage = message.split(" ", 3);
        Client client = getClientByUserName(splitMessage[1]);
        if (client != null) {
            ServerMessage serverMessage = new ServerMessage(ServerMessage.MessageType.TRANSFER_FILE, username
                    + " " + splitMessage[2]);
            client.print(serverMessage.toString());

            int bytesRead;

            DataInputStream clientData = new DataInputStream(socket.getInputStream());

            DataOutputStream outputStream = new DataOutputStream(client.getSocket().getOutputStream());
            outputStream.writeUTF(clientData.readUTF()); // send file name
            long size = clientData.readLong(); // get size of file
            outputStream.writeLong(size); // send size of file
            byte[] buffer = new byte[(int) size];
            // send bytes over socket
            while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                size -= bytesRead;
            }
            outputStream.flush();

        } else print("-ERR User is not logged on");
    }

    /**
     * The fileReceived method sends a confirmation that the file was successfully received
     *
     * @param message A message that contains the command, the username of the client that send the file and the
     *                filename
     */
    private void fileReceived(String message) {
        String[] splitMessage = message.split(" ", 3);
        ServerMessage serverMessage = new ServerMessage(ServerMessage.MessageType.FILE_RECEIVED, username + " "
                + splitMessage[2]);
        Client client = getClientByUserName(splitMessage[1]);
        if (client != null) {
            client.print(serverMessage.toString());
        }
    }

    /**
     * The joinGroup method allows client to join a specified group. This method Firstly checks whether the specified
     * group exists and then checks if the client is not part of that group. If the group exists and the client is not
     * part of the group the client will be added to the group. If the group doesn't exists the client gets a message
     * containing the err that the group doesn't exists. Else if the client is part of the group the client gets an
     * error message containing that the client is part of the group.
     *
     * @param message A message that contains the command and the group name the client wants to join
     */
    private void joinGroup(String message) {
        String[] splitMessage = message.split(" ", 2);
        String groupName = splitMessage[1];
        ServerMessage joinMessage = new ServerMessage(ServerMessage.MessageType.GRP_JOIN,
                username + " " + groupName);

        Group group = getGroupByGroupName(groupName);
        if (group != null && !group.isUserInGroup(this)) {
            print("+OK " + Encode(message));
            group.addClient(this);

            group.sendMessage(joinMessage.toString());

        } else if (group == null) print("–ERR group doesn't exist ");
        else print("-ERR already part of group: " + groupName);
    }

    /**
     * The leaveGroup method allows clients to leave a specified group. This method Firstly checks whether the specified
     * group exists and then checks if the client is part of that group. If the group exists and the client is part of
     * the group the client will be removed from the group. If the group doesn't exists the client gets a message
     * containing the err that the group doesn't exists. Else if the client is not part of the group the client gets an
     * error message containing that the client is not part of the group.
     *
     * @param message A message that contains the command and the group name the client wants to leave
     */
    private void leaveGroup(String message) {
        String[] splitMessage = message.split(" ", 2);
        String groupName = splitMessage[1];
        ServerMessage leaveMessage = new ServerMessage(ServerMessage.MessageType.GRP_LEAVE,
                groupName + " " + username);

        Group group = getGroupByGroupName(groupName);
        if (group != null && !group.isUserInGroup(this)) {
            print("+OK " + Encode(message));
            group.removeClient(this);

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
                groupName + " " + splitMessage[2]);

        Group group = getGroupByGroupName(groupName); // get group
        if (group != null && group.isLeader(username)) { // check if group exists and if this client is the group leader
            Client client = getClientByUserName(clientName); // get client that needs to be removed from group
            if (client != null && group.isUserInGroup(client)) { // check if client exists and is in the group
                print("+OK " + Encode(message)); // print OK message
                group.removeClient(client); // remove client from group

                client.print(clientKickMessage.toString());
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
     * The requestFileTransfer method will simply send a file transfer request to the client this client wants to send
     * a file to. It will checks if the receiving client exists, if the receiving client doesn't exist this client will
     * get an error message containing that the receiving client isn't logged on.
     *
     * @param mesage A message that contains the command, receiving client's name and the name file that is going to be
     *               transferred.
     */
    private void requestFileTransfer(String mesage) {
        String[] splitMessage = mesage.split(" ", 3);
        Client client = getClientByUserName(splitMessage[1]);
        ServerMessage serverMessage = new ServerMessage(ServerMessage.MessageType.REQ_FILE,
                username + " " + splitMessage[2]);
        if (client != null) {
            client.print(serverMessage.toString());
        } else print("-ERR User is not logged on");
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
        writer.println(encyptor.encrypt(message));
        writer.flush();
    }

    //stop this client from running
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


