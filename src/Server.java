
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private List<Client> clients = new ArrayList<>();
    private List<HeartBeat> heartBeats = new ArrayList<>();
    private List<Group> groups = new ArrayList<>();

    public static void main(String[] args) {
        new Server().launch();
    }

    private void launch() {
        try {
            int SERVER_PORT = 1337;
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server loopt");

            while (true) {
                // Start a message processing thread for each connecting client.
                Socket clientSocket = serverSocket.accept();
                System.out.println(clientSocket.getInetAddress() + " Has Connected");
                Client client = new Client(clientSocket, this);
                clients.add(client);
                Thread clientThread = new Thread(client);
                clientThread.start();

                // Start a ping thread for each connecting client.
                HeartBeat heartbeat = new HeartBeat(client, this);
                heartBeats.add(heartbeat);
                Thread heartBeatThread = new Thread(heartbeat);
                heartBeatThread.start();
                System.out.println("HeartBeatThread created for: " + clientThread.getName());

                System.out.println("Connected Clients: " + clients.size());
            }
        } catch (IOException e1) {
            System.out.println("Server niet beschikbaar");
        }
    }

    void addGroup(Group group) {
        groups.add(group);
    }

    void disconnectClient(Client client) {
        clients.remove(client);
        client.stop();
        heartBeats.remove(client.getHeartBeat());
        client.getHeartBeat().stop();
    }

    List<Client> getClients() {
        return clients;
    }

    List<Group> getGroups() {
        return groups;
    }
}

