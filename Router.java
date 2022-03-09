public class Router extends Thread {
  private int xCoord, yCoord;
  private String id;
  private RoutingTable routingTable;

  public Router(String id, int xCoord, int yCoord) {
    this.xCoord = xCoord;
    this.yCoord = yCoord;
    this.id = id;
    checkRoutingTable();
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

  public void checkRoutingTable() {

  }

  public String getRouterId() {
    return this.id;
  }

  public int getXCoord() {
    return this.xCoord;
  }

  public int getYCoord() {
    return this.yCoord;
  }
}
