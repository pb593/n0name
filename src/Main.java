import core.Client;
import exception.UserIDTakenException;

import java.util.Scanner;
import java.util.logging.Logger;

public class Main {

    public final static Logger logger = Logger.getAnonymousLogger();

    public static void main(String[] argv) {

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
