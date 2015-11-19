public class Main {
   public static void main(String[] argv){

       Client c1 = new Client("Pavel", 50000);
       Client c2 = new Client("Edgar", 50001);
       Client c3 = new Client("Cata", 50002);

       c1.start();
       c2.start();
       c3.start();

       try {
           Thread.sleep(1000); //sleep for 1s to make sure everyone is running
       } catch (InterruptedException e) {
           e.printStackTrace();
       }



   }
}
