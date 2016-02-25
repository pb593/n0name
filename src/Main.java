import core.Client;
import core.Cryptographer;
import exception.UserIDTakenException;
import message.Message;
import message.TextMessage;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Logger;

public class Main {

    public final static Logger logger = Logger.getAnonymousLogger();

    public static void main(String[] argv) {

        /*
        Cryptographer crypto = new Cryptographer();
        Random r = new Random();
        for(int i = 0; i < 100; i++) {
            int J = r.nextInt(1000);
            StringBuffer sb = new StringBuffer();
            for(int j =0; j < J; j++) {
                sb.append(UUID.randomUUID().toString());
            }
            TextMessage txtMsg = new TextMessage(sb.toString(),
                                                UUID.randomUUID().toString(), UUID.randomUUID().toString());
            String encrypted = crypto.encryptMsg(txtMsg);
            Message m = crypto.decryptMsg(encrypted);
            System.out.printf("%s\n", txtMsg.equals(m));
        }
        */

        Client cl = null;
        while(true) {
            System.out.print("Please pick a username:\n> ");
            Scanner scanner = new Scanner(System.in);
            String userID = scanner.nextLine().trim();

            try {
                cl = new Client(userID);
                break; // exit loop if Client successfully constructed
            } catch (UserIDTakenException e) {
                System.out.println("Username already in use. Pick another one.");
            }
        }
        cl.run(); // run the client in the same thread
    }
}
