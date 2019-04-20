import java.util.Timer;
import java.util.TimerTask;

/**
 * The HeartBeat class sends a PING message to the client every 60 seconds. If it doesn't get a PONG message in response
 * it will disconnect the corresponding client and stop itself.
 */

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
            while (running) {
                Thread.sleep(60000);
                if (client != null) {
                    client.print(ServerMessage.MessageType.PING.toString());
                    timeout = new TimerTask() {
                        @Override
                        public void run() {
                            disconnectClient();
                        }
                    };
                    timer.schedule(timeout, 3000);
                    timer.purge();
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
        System.out.println("stop timer");
    }
}
