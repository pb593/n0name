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


        System.out.print("Please pick a username:\n> ");
        Scanner scanner = new Scanner(System.in);
        String userID = scanner.nextLine().trim();

        Client cl = new Client(userID);
        cl.run(); //run the client in the same thread

    }
}
