import java.io.*;

import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Client implements Runnable {

  Socket socket;

  public Client(Socket socket) {
    this.socket = socket;
  }

  @Override
  public void run() {
    InputStream inputStream = null;
    OutputStream outputStream = null;

    try {
      inputStream = socket.getInputStream();
      outputStream = socket.getOutputStream();
    } catch (IOException e) {
      e.printStackTrace();
    }

    PrintWriter writer = new PrintWriter(outputStream);
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

    try {
      String line = reader.readLine();
      System.out.println(line);
    } catch (IOException e) {
      e.printStackTrace();
    }

//      String line = readLine(reader);
//      System.out.println(line);

//        writer.println(line);
//        writer.flush();

//        String encodedName = Encode(line);
//        System.out.println(encodedName);
//        writer.println(encodedName);
//        writer.flush();

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


  private String readLine(BufferedReader reader) {
    String text = "";

    try {
      text = reader.readLine();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return text;
  }

}


