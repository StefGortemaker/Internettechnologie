
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private List<Thread> clients = new ArrayList<>();
    private List<Thread> heartBeats = new ArrayList<>();
    private List<Socket> clientSockets = new ArrayList<>();

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
                clients.add(client);
                clientSockets.add(clientSocket);
                System.out.println(client.getClass());
                System.out.println("Connected Clients: " + clients.size());

                // TODO: Start a ping thread for each connecting client.
                Thread heartBeatThread = new Thread(new HeartBeat(clientSocket));
                heartBeatThread.start();
                heartBeats.add(heartBeatThread);
                System.out.println("HeartBeatThread created for: " + client.getName());

            }
        } catch (IOException e1) {
            System.out.println("Server niet beschikbaar");
        }
    }

    void disconnectClient(Client client) {
        clientSockets.remove(client.getSocket());
        System.out.println(clientSockets.size());
    }

    void bcstMessage(String message) throws IOException {
        for (Socket clientSocket: clientSockets){
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
            writer.println("BCST " + message);
            writer.flush();
        }
    }

}

