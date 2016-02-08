/**
 * Created by pb593 on 08/11/2015.
 *
 * This class will implement the basic network communication functionality required.
 *
 * Class will be used my multiple instance of other classes for communication, so
 * needs to be threadsafe.
 *
 */

import message.Message;
import org.json.simple.parser.ParseException;

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

    private final Integer SAF_REQUEST_RATE = 5; // period of checking store-n-forward for new messages


    private final Thread sktserver = new Thread () {
        @Override
        public void run() { //start listening on the port for incoming messages
            boolean ERROR = false;
            Main.logger.info(String.format("Communicator with id=%d has started on port=%d\n", id, port));
            while (!ERROR) {
                try {
                    Socket s = srvskt.accept();
                    BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    String jsonLine = br.readLine();
                    msgReceived(jsonLine); // pass to client in a thread-safe way
                }
                catch (IOException e) {
                    Main.logger.severe(String.format("Communicator with id = %d has broken down upon accepting a connection",
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
                    try {
                        Thread.sleep(SAF_REQUEST_RATE * 1000); // sleep for 5 seconds
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    for(String l: jsonLines) {
                        msgReceived(l); // pass to Client in thread-safe way
                    }
                }
            }
        }
    };

    private void msgReceived(String jsonString) {
        Message msg = null;
        try {
            msg = Message.fromJSON(jsonString);
        } catch (ParseException e) {
            Main.logger.severe("Could not parse one of the incoming messages");
        }

        // prepare thread to run
        final Message finalMsg = msg; // made final to pass into thread
        Thread handle = new Thread() {
            public void run() {
                client.msgReceived(finalMsg);
            }
        };
        handle.setDaemon(true);

        synchronized (client) {
            handle.start(); // pass it on to client in a separate thread
        }
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


    public synchronized boolean send(String userID, Message msg){
        InetSocketAddress dest = AddressBook.lookup(userID); // get address of the user
        if(dest == null) {// user not in address book
            Main.logger.warning(String.format("Communicator failed to send msg to user %s. User not in address book",
                        userID));
            return false;
        }
        try {
            if(dest.toString().equals("/0.0.0.0:0")) { // address is private -> use store-n-forward
                boolean ret = StoreAndForward.send(userID, msg.toJSON());
                return ret; // return true if ACKed
            }
            else {
                Socket skt = new Socket(dest.getAddress(), dest.getPort());
                PrintWriter writer = new PrintWriter(skt.getOutputStream(), true);
                String toSend = msg.toJSON();
                writer.println(toSend); // send the JSON representation of message
                return true;
            }
        } catch (IOException e) {
            Main.logger.warning(String.format("Error sending message to address %s:%d\n",
                    dest.getHostString() , dest.getPort()));
            return false;
        }

    }

    public Integer getPort() {
        return port; // reading and immutable value, so no need for syncronization
    }

}
