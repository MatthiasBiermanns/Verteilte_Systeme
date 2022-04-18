import java.util.logging.*;
import java.io.*;
import java.util.HashMap;

public class test {
    private final static String STANDARD_PATH = System.getProperty("user.home") + "/Desktop/DSR_Logs/";
    public static void main(String[] args) {
      testFieldCreaetion();
      //try {
      //      Field myField = new Field(30, 25, 25);
      //      testRouteRequest(myField);
      //  } catch ( Exception e) {
      //      e.printStackTrace();
      //  }
    }

    public static void testLogging() {
        Logger logger = Logger.getLogger("log_" + 100);
        logger.setLevel(Level.ALL);
        try {
            FileHandler handler = new FileHandler(STANDARD_PATH + "testlogging.txt");
            logger.addHandler(handler);
            SimpleFormatter sf = new SimpleFormatter();
            handler.setFormatter(sf);

            logger.warning("Warning");
            logger.info("Info");
            logger.severe("Severe");
            logger.fine("fine");
        } catch (IOException e) {
            e.printStackTrace();
        }
        
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
            handy.sendMessage(3010, "Hi");
        } catch ( Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }
    
    // Needs to get field form outside, so Gui works on the same Field.
    public static void testRouteRequest(Field myField) {
        try {
          // Field myField = new Field(25, 25, 25);
          myField.startDevices();
          try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
              Thread.sleep(1000);
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
