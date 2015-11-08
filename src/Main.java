import java.io.IOException;

public class Main {
   public static void main(String[] argv){

       try {
           Communicator c1 = new Communicator(50000);
           c1.start();

           Communicator c2 = new Communicator(50001);
           c2.start();
           c2.send("localhost", 50000, new Message("Hello, C1!"));

           Thread.sleep(3000); //wait for msg to arrive
       }
       catch (IOException e) {
           e.printStackTrace();
       }
       catch (InterruptedException e) {
           e.printStackTrace();
       }


   }
}
