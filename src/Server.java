
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {

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
                // Start a message processing thread for each connecting client.
                Socket clientSocket = serverSocket.accept();
                System.out.println(clientSocket.getInetAddress() + " Has Connected");
                Thread client = new Thread(new Client(clientSocket, this));
                client.start();
                System.out.println(client.getClass());

                // Start a ping thread for each connecting client.
                Thread heartBeatThread = new Thread(new HeartBeat(clientSocket, this));
                heartBeatThread.start();
                System.out.println("HeartBeatThread created for: " + client.getName());

                System.out.println("Connected Clients: " + clients.size());
            }
        } catch (IOException e1) {
            System.out.println("Server niet beschikbaar");
        }
    }

    void disconnectClient(Client client) {
        clients.remove(client);
        heartBeats.remove(client.getHeartBeat());
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

    void addClient(Client client) {
        clients.add(client);
    }

    void addHeatBeat(HeartBeat heartBeat){
        heartBeats.add(heartBeat);
    }

    void setclientHeartBeat(HeartBeat heartBeat){
        for (Client client : clients){
            if (client.getSocket().equals(heartBeat.getSocket())){
                client.setHeartBeat(heartBeat);
            }
        }
    }
}

