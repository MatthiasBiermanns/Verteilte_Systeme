public class Router extends Thread {
  int xCoord, yCoord;
  String id;
  RoutingTable routingTable;

  public Router(String id, int xCoord, int yCoord) {

  }

  public void run() {
    while (true) {
      // workflow
      try {
        Thread.sleep(1000);
        System.out.println(id + ": Waiting for packages");
      } catch (InterruptedException e) {

      }
    }
  }

  public void send() {

  }

  public void flood(String msg) {
    // flooding all routers in 10m range with given message
  }
}
