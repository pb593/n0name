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

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

class Communicator extends Thread {

    private final Client client; //callbacks on received messages go here
    private final ServerSocket srvskt;
    private final int port;
    private final int id;



    public Communicator(Client client, int port) throws IOException {
        this.client = client;
        this.port = port;
        srvskt = new ServerSocket(port);
        this.setDaemon(true); //communicator is a daemon thread
        id = this.hashCode();
    }

    @Override
    public void run() { //start listening on the port for incoming messages
        boolean ERROR = false;
        Main.logger.info(String.format("Communicator with id=%d has started on port=%d\n", id, port));
        while(!ERROR){
            try {
                Socket s = srvskt.accept();
                ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
                Message msg = (Message) ois.readObject();
                // spawn off a new thread to do a callback and handle the message
                Thread handle = new Thread() {
                    @Override
                    public void run() {
                        client.msgReceived(msg);
                    }
                };
                handle.setDaemon(true);
                handle.start(); //start the handler thread
            } catch (IOException e) {
                Main.logger.severe(String.format("Communicator with id = %d has broken down upon accepting a connection",
                                                                                                            this.id));
                ERROR = true;
            } catch (ClassNotFoundException e) {
                Main.logger.warning(String.format("New message of unknown type received by Communicator with id=%d\n",
                                                                                                            this.id));
            }
        }
    }

    public synchronized boolean send(InetSocketAddress dest, Message msg){
        try {
            Socket skt = new Socket(dest.getAddress(), dest.getPort());
            ObjectOutputStream oos = new ObjectOutputStream(skt.getOutputStream());
            oos.writeObject(msg);
            Main.logger.config(String.format("Communicator with id=%d sent message to %s:%d\n",
                                                                    this.id, dest.getHostString() , dest.getPort()));
            return true;
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
