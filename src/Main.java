import java.util.Random;
import java.util.Scanner;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    public final static Logger logger = Logger.getAnonymousLogger();

    public static void main(String[] argv){

        //tune the logger
        logger.setUseParentHandlers(false); //disable default handlers
        logger.setLevel(Level.INFO); //set level for logger
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.INFO); //set level for handler
        logger.addHandler(handler);

        // get username and choose port
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your username: ");
        String user = scanner.nextLine();

        Client cl = new Client(user); //create a client on that port
        cl.run(); //run the client in the same thread

    }
}
