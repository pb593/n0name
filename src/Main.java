import java.net.InetSocketAddress;
import java.util.HashMap;
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


        /*
        // parse out debugging config
        try (BufferedReader br = new BufferedReader(new FileReader("src/testconfig.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("\\s+");
                String userID = tokens[0];
                String hostport = tokens[1];

                // error handling ?
                String host = hostport.split(":")[0];
                Integer port = Integer.parseInt(hostport.split(":")[1]);

                addressBook.put(userID, new InetSocketAddress(host, port));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // choose username and port
        List<String> keyList = new ArrayList<>(addressBook.keySet());
        for(int i = 0; i < keyList.size(); i++) {
            System.out.printf("%d. %s - %s\n", i, keyList.get(i), addressBook.get(keyList.get(i)));
        }
        Scanner scanner = new Scanner(System.in);
        System.out.print("Choose your number: ");
        Integer opt = Integer.parseInt(scanner.nextLine());

        String userID = keyList.get(opt);
        InetSocketAddress hostport = addressBook.get(userID);
        */
        System.out.print("Please pick a username:\n> ");
        Scanner scanner = new Scanner(System.in);
        String userID = scanner.nextLine().trim();

        Client cl = new Client(userID);
        cl.run(); //run the client in the same thread

    }
}
