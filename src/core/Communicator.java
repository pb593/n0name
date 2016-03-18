package core; /**
 * Created by pb593 on 08/11/2015.
 *
 * This class will implement the basic network communication functionality required.
 *
 * Class will be used my multiple instance of other classes for communication, so
 * needs to be threadsafe.
 *
 */

import scaffolding.AddressBook;
import scaffolding.StoreAndForward;
import scaffolding.Utils;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

class Communicator extends Thread {

    private final Client client; //callbacks on received messages go here
    private final ServerSocket srvskt;
    private final int port;
    private final int id;


    private final Thread sktserver = new Thread () { // P2P receiver, binds on a port and listens
        @Override
        public void run() { //start listening on the port for incoming messages
            boolean ERROR = false;
            System.out.print(String.format("Communicator with id=%d has started on port=%d\n", id, port));
            while (!ERROR) {
                try {
                    Socket s = srvskt.accept();
                    BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    String jsonLine = br.readLine();
                    msgReceived(jsonLine); // pass to client in a thread-safe way
                }
                catch (IOException e) {
                    System.err.print(String.format("Communicator with id = %d has broken down upon accepting a connection",
                            id));
                    ERROR = true;
                }
            }
        }
    };

    private final Thread safclient = new Thread() { // store-n-forward client, pulls new messages
        @Override
        public void run() {
            while(true) {
                List<String> jsonLines = StoreAndForward.retrieve(client.getUserID());
                if(jsonLines == null) {
                    Utils.sleep(StoreAndForward.SAF_REFRESH_RATE);
                }
                else {
                    for(String l: jsonLines) {
                        msgReceived(l); // pass to Client in thread-safe way
                    }
                }
            }
        }
    };

    synchronized private void msgReceived(String datagram) {
        // synchronized – only one message is being handled at a time
        client.msgReceived(datagram); // pass the string to Client

    }

    public Communicator(Client client, int port) throws IOException {
        this.client = client;
        this.port = port;
        srvskt = new ServerSocket(port);
        this.setDaemon(true); //communicator is a daemon thread
        id = this.hashCode();
    }

    @Override
    public void run() {
        safclient.start(); // spawn off store-n-forward client in a new thread
        sktserver.run(); // run the socket (P2P) server in this thread
    }


    public synchronized boolean send(String userID, String urlSafeString){
        InetSocketAddress dest = AddressBook.lookup(userID); // get address of the user
        if(dest == null) {// user not in address book
            System.err.print(String.format("Communicator failed to send msg to user %s. User not in address book\n",
                        userID));
            return false;
        }
        try {
            if(dest.toString().equals("/0.0.0.0:0")) { // address is private -> use store-n-forward
                boolean ret = StoreAndForward.send(userID, urlSafeString);
                return ret; // return true if ACKed
            }
            else {
                Socket skt = new Socket(dest.getAddress(), dest.getPort());
                PrintWriter writer = new PrintWriter(skt.getOutputStream(), true);
                writer.println(urlSafeString);
                return true;
            }
        } catch (IOException e) {
            System.err.print(String.format("Error sending message to address %s:%d\n",
                    dest.getHostString() , dest.getPort()));
            return false;
        }

    }

    public Integer getPort() {
        return port; // reading and immutable value, so no need for synchronization
    }

}
