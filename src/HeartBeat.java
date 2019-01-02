import java.io.*;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class HeartBeat implements Runnable {

    private Socket socket;
    private Client client;
    private PrintWriter writer;
    private Server server;
    private Timer timer;

    HeartBeat(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        this.timer = new Timer();
    }

    @Override
    public void run() {
        try {
            server.addHeatBeat(this);
            server.setclientHeartBeat(this);
            writer = new PrintWriter(socket.getOutputStream());
            while (true) {
                Thread.sleep(60000);
                if (client != null) {
                    writer.println("PING");
                    writer.flush();

                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            disconnectClient();
                        }
                    }, 3000);
                } else {
                    socket.close();
                    return;
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Heartbeat Stopt");
        }
    }

    private void disconnectClient() {
        writer.println("DSCN Pong timeout");
        writer.flush();
        server.disconnectClient(client);
        client = null;
    }

    void setClient(Client client) {
        this.client = client;
    }

    Socket getSocket() {
        return socket;
    }

    void stopTimer(){
        timer.cancel();
        timer = new Timer();
    }
}
