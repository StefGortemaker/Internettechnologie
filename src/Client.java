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
        BufferedReader reader;
        PrintWriter writer;

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line = in.readLine();
            System.out.println(line);

            String encodedName = Encode(line);
            System.out.println(encodedName);

//            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            String line = in.readLine();
//            System.out.println(line);

            OutputStreamWriter os = new OutputStreamWriter(socket.getOutputStream());
            PrintWriter writer1 = new PrintWriter(os);
            writer1.println(encodedName);
            writer1.flush();


        } catch (IOException e) {
            System.out.println("Could not connect to server!");
            e.printStackTrace();
            return;
        }
    }

    private String Encode(String line){

        try {
            byte[] bytes = line.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] newLine = md.digest(bytes);
            String line2 = new String(Base64.getEncoder().encode(newLine));
            return line2;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException ns){
            ns.printStackTrace();
        }

        return null;
    }
}


