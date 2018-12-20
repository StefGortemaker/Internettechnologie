import java.io.*;
import java.net.Socket;

public class HeartBeat implements Runnable {

    private Socket socket;
    private Client client;
    private PrintWriter writer;
    private Server server;

    HeartBeat(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            server.addHeatBeat(this);
            server.setclientHeartBeat(this);
            writer = new PrintWriter(socket.getOutputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (true) {
                Thread.sleep(10000);
                if (client != null) {
                    writer.println("PING");
                    writer.flush();

                    client.startTimer();
                }

                //TODO: Kijk of Pong respone krijgt binnen 3 seconden anders heartbeat & client thread afsluiten

            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    void disconnectClient() {
        writer.println("DSCN Pong timeout");
        writer.flush();
    }

    void setClient(Client client) {
        this.client = client;
    }

    Socket getSocket() {
        return socket;
    }
}
