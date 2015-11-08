import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by pb593 on 08/11/2015.
 *
 * This class will implement the basic network communication functionality required.
 * Will work on
 *
 */
class Communicator extends Thread {

    private final ServerSocket srvskt;
    private final int port;
    private final int id;


    public Communicator(int port) throws IOException {
        this.port = port;
        srvskt = new ServerSocket(port);
        this.setDaemon(true); //communicator is a daemon thread
        id = this.hashCode();
    }

    @Override
    public void run() { //start listening on the port for incoming messages
        boolean ERROR = false;
        System.out.printf("Communicator with id=%d has started on port=%d\n", id, port);
        while(!ERROR){
            try {
                Socket s = srvskt.accept();
                InputStream is = s.getInputStream();
                byte[] data = new byte[5120]; //5kB
                int n = is.read(data, 0, data.length);
                System.out.printf("Communicator with id=%d received message \"%s\"\n", id, new String(data, 0, n));
            } catch (IOException e) {
                System.err.println("The Communicator shut down after accepting a connection.");
                ERROR = true;
            }
        }
    }

    public boolean send(String host, int port, String str){
        try {
            Socket skt = new Socket(host, port);
            OutputStream os = skt.getOutputStream();
            os.write(str.getBytes());
            System.out.printf("Communicator with id=%d sent message \"%s\" to %s:%d\n", id, str, host, port);
            return true;
        } catch (IOException e) {
            System.err.printf("Error sending message \"%s\" to address %s:%d\n", str, host, port);
            return false;
        }

    }
}
