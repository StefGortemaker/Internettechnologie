import java.util.Timer;
import java.util.TimerTask;

public class HeartBeat implements Runnable {

    private Client client;
    private Server server;
    private TimerTask timeout;

    private boolean running = true;

    HeartBeat(Client client, Server server) {
        this.server = server;
        this.client = client;
    }

    @Override
    public void run() {
        try {
            client.setHeartBeat(this);
            Timer timer = new Timer();
            timeout = new TimerTask() {
                @Override
                public void run() {
                    disconnectClient();
                }
            };
            while (running) {
                Thread.sleep(60000);
                if (client != null) {
                    client.print(ServerMessage.MessageType.PING.toString());
                    timer.purge();

                    timer.schedule(timeout, 3000);
                } else {
                    return;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Heartbeat Stopt");
        }
    }

    private void disconnectClient() {
        client.print(new ServerMessage(ServerMessage.MessageType.DSCN, "pong timeout").toString());
        server.disconnectClient(client);
        client = null;
    }

    void stop() {
        running = false;
    }

    void stopTimer() {
        timeout.cancel();
        timeout = new TimerTask() {
            @Override
            public void run() {
                disconnectClient();
            }
        };
    }
}
