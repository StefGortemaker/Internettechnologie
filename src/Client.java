import java.io.*;

import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Client implements Runnable {

    private Socket socket;

    Client(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        BufferedReader reader;
        PrintWriter writer ;
        OutputStreamWriter os;

        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            os = new OutputStreamWriter(socket.getOutputStream());
            writer = new PrintWriter(os);

            String line = reader.readLine();
            System.out.println(line);

            String encodedName = Encode(line);
            System.out.println(encodedName);

            writer.println(encodedName);
            writer.flush();

        } catch (IOException e) {
            System.out.println("Could not connect to server!");
            e.printStackTrace();
        }
    }

    private String Encode(String line){
        try {
            byte[] bytes = line.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] newLine = md.digest(bytes);
            return new String(Base64.getEncoder().encode(newLine));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
