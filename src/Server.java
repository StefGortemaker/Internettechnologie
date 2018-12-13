
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class Server {

  private List<Thread> clients = new ArrayList<Thread>();

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
        Socket newClient = serverSocket.accept();
        System.out.println(newClient.getInetAddress() + " Has Connected");
        Thread client = new Thread(new Client(newClient));
        client.start();
        clients.add(client);
        System.out.println("Connected Clients: " + clients.size());

        OutputStreamWriter os = new OutputStreamWriter(newClient.getOutputStream());
        PrintWriter writer = new PrintWriter(os);
        writer.println("HELO");
        writer.flush();

        BufferedReader in = new BufferedReader(new InputStreamReader(newClient.getInputStream()));
        String line = in.readLine();
        System.out.println(line);

        String encodedLine = Encode(line);
        client.setName(encodedLine);

        writer.println("+OK "+encodedLine);
        writer.flush();

//        checkUsername(line);

        // TODO: Start a ping thread for each connecting client.

      }
    } catch (IOException e1) {
      System.out.println("Server niet beschikbaar");
    }
  }

  private boolean checkUsername(String username) {
    boolean available = false;

    for (Thread client : clients) {
      if (client.getName().equals(username)) {
        available = false;
      } else {
        available = true;
      }
    }

    return available;
  }

  private String Encode(String line) {

    try {
      byte[] bytes = line.getBytes("UTF-8");
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] newLine = md.digest(bytes);
      String line2 = new String(Base64.getEncoder().encode(newLine));
      return line2;
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    } catch (NoSuchAlgorithmException ns) {
      ns.printStackTrace();
    }

    return null;
  }

}

