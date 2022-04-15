import java.io.*;
import java.util.HashMap;

public class test {

    public static void main(String[] args) {
        testRouteRequest();
    }

    public static void testAcks() {
        try {
            Field myField = new Field(0, 25, 25);
            myField.createNewDevice(0, 0);
            myField.createNewDevice(5, 5);
            myField.createNewDevice(10, 10);
            myField.createNewDevice(15, 15);
            myField.createNewDevice(20, 20);
            myField.createNewDevice(24, 24);
            myField.startDevices();
            myField.printField();
            HashMap<Integer, Device> fieldMap = myField.getMap();
            EndDevice handy = (EndDevice) fieldMap.get(3001);
            Router router = (Router) fieldMap.get(3000);
            System.out.println(myField.getReachableRouter(router.getPort(), router.getXCoord(), router.getYCoord()));
            handy.sendMessage(3010, "Hi");
        } catch ( Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }
    
    public static void testRouteRequest() {
        try {
          Field myField = new Field(25, 25, 25);
          myField.startDevices();
          //myField.createNewDevice()
          try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
              myField.printField();
    
              String line = reader.readLine();
              String[] parts = line.split(" ");
              HashMap<Integer, Device> myDevices = myField.getMap();
              int port = Integer.parseInt(parts[1]);
              if(port % 2 == 0) {
                port++;
              }
              EndDevice handy = (EndDevice) myDevices.get(port);
              switch (parts[0].toUpperCase()) {
                case "MOVE":
                  // 23, 12, 0, 0 
                  // myField.moveDevice(handy.getXCoord(), handy.getYCoord());
                  myField.moveDevice(handy.getXCoord(), handy.getYCoord(), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                  break;
                case "SEND":
                  handy.sendMessage(Integer.parseInt(parts[2]), parts[3]);
                  System.out.println("geschafft!");
                  break;
                default:
                  break;
              }
    
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
    
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    
      public static void testFieldCreaetion() {
        try {
          Field myField = new Field(20, 10, 10);
          try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
              myField.printField();
    
              String line = reader.readLine();
              String[] parts = line.split(" ");
    
              myField.moveDevice(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                  Integer.parseInt(parts[3]));
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
}
