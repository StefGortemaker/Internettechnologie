import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class HeartBeat implements Runnable {

    private Socket socket;

    HeartBeat(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            PrintWriter writer = new PrintWriter(socket.getOutputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (true) {
                Thread.sleep(60000);
                writer.println("PING");
                writer.flush();

                //TODO: Kijk of Pong respone krijgt binnen 3 seconden anders heartbeat & client thread afsluiten
                //writer.println("DSCN Pong timeout");
                //writer.flush();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
