
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private List<Thread> clientThreads = new ArrayList<>();
    private List<Thread> heartBeatThreads = new ArrayList<>();
    private List<Client> clients = new ArrayList<>();
    private List<HeartBeat> heartBeats = new ArrayList<>();

    public static void main(String[] args) {
        new Server().launch();
    }

    private void launch() {
        try {
            int SERVER_PORT = 1337;
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server loopt");

            while (true) {
                //TODO: Start a message processing thread for each connecting client.
                Socket clientSocket = serverSocket.accept();
                System.out.println(clientSocket.getInetAddress() + " Has Connected");
                Thread client = new Thread(new Client(clientSocket, this));
                client.start();
                clientThreads.add(client);
                System.out.println(client.getClass());
                System.out.println("Connected Clients: " + clientThreads.size());

                //TODO: Start a ping thread for each connecting client.
                Thread heartBeatThread = new Thread(new HeartBeat(clientSocket));
                heartBeatThread.start();
                heartBeatThreads.add(heartBeatThread);
                System.out.println("HeartBeatThread created for: " + client.getName());

            }
        } catch (IOException e1) {
            System.out.println("Server niet beschikbaar");
        }
    }

    void disconnectClient(Client client) {
        //TODO: Client en bijbehorende HeartBeat threads correct afsluiten
        clients.remove(client);
        System.out.println(clients.size());
    }

    void bcstMessage(String message, Client client) throws IOException {
        for (Client c : clients) {
            if (!c.getSocket().equals(client.getSocket())) {
                PrintWriter writer = new PrintWriter(c.getSocket().getOutputStream());
                writer.println("BCST " + message);
                writer.flush();
            }
        }
    }
}

