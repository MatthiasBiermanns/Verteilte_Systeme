import java.util.logging.*;

import Exceptions.InvalidInputException;

import java.io.*;
import java.util.HashMap;
import java.util.Random;

public class test {
  private final static String STANDARD_PATH = System.getProperty("user.home") + "/Desktop/DSR_Logs/";

  public static void main(String[] args) {
    // testFieldCreaetion();
    // try {
    // Field myField = new Field(30, 25, 25);
    // testRouteRequest(myField);
    // } catch ( Exception e) {
    // e.printStackTrace();
    // }
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
    } catch (Exception e) {
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
          if (port % 2 == 0) {
            port++;
          }
          EndDevice handy = (EndDevice) myDevices.get(port);
          switch (parts[0].toUpperCase()) {
            case "MOVE":
              myField.moveDevice(handy.getXCoord(), handy.getYCoord(), Integer.parseInt(parts[2]),
                  Integer.parseInt(parts[3]));
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

  // -------------------------------------------
  // ----------------Test A---------------------
  // -------------------------------------------

  public static void diffrentNumberOfRouters() {
    /*
     * Random r = new Random();
     * int randomNumber = r.nextInt(50);
     * System.out.println(randomNumber);
     */
    Field myField;
    try {
      myField = new Field(500, 100, 100);
      myField.startDevices();
    } catch (InvalidInputException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  public static void sendRandomMessage() {
    try {
      Field myField = new Field(0, 25, 25);
      myField.createNewDevice(0, 0);
      myField.createNewDevice(5, 5);
      myField.createNewDevice(10, 10);
      myField.createNewDevice(15, 15);
      myField.createNewDevice(20, 20);
      myField.createNewDevice(24, 24);
      myField.startDevices();
      HashMap<Integer, Device> fieldMap = myField.getMap();
      EndDevice handy = (EndDevice) fieldMap.get(3001);
      handy.sendMessage(3010, "Hallöchen");

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public static void zweiRouterAufEinemFeld() {

    try {
      Field myField = new Field(0, 100, 100);
      myField.createNewDevice(0, 0);
      myField.createNewDevice(0, 0);
      myField.createNewDevice(5, 5);
      myField.createNewDevice(10, 10);
      myField.startDevices();
      new Gui(myField, "Mein Fenster");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void sendMessage() {
    try {
      Field myField = new Field(5, 10, 10);
      myField.startDevices();
      myField.printField();
      EndDevice handy = (EndDevice) myField.getDevice(3001);
      handy.sendMessage(3005, "Hallo");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void sendMessageBack() {
    try {
      Field myField = new Field(10, 10, 10);
      myField.startDevices();
      myField.printField();
      EndDevice handy = (EndDevice) myField.getDevice(3001);
      handy.sendMessage(3003, "Hallo");
      EndDevice handy2 = (EndDevice) myField.getDevice(3003);
      handy2.sendMessage(3001, "Hallo zurück");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void sendMessageParallel() {
    try {
      Field myField = new Field(5, 10, 10);
      myField.startDevices();
      myField.printField();

      EndDevice handy1 = (EndDevice) myField.getDevice(3001);
      handy1.sendMessage(3005, "Hallo1");
      // handy.sendMessage(3003, "Hi");

      EndDevice handy2 = (EndDevice) myField.getDevice(3003);
      handy2.sendMessage(3005, "Hallo2");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // -------------------------------------------
  // ----------------Test B---------------------
  // -------------------------------------------


  public static void moveRandomRandomRouter(Field myField){
    Random r = new Random();
    try {
      myField.startDevices();

      while (true) {
        int randomTime = r.nextInt(5) * 1000;

        int randomDevice = r.nextInt(25) * 2;
        randomDevice = 3000 + randomDevice +1;

        try {
          Thread.sleep(randomTime);

          HashMap<Integer, Device> myDevices = myField.getMap();
          EndDevice deviceToMove = (EndDevice) myDevices.get(randomDevice);
          myField.moveDevice(deviceToMove.getXCoord(), deviceToMove.getYCoord());

        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public static void sendMessageWhileMovingRouter(Field myField) {
    Random r = new Random();
    try {
      myField.startDevices();

      while (true) {
        int randomStartDevice = r.nextInt(25) * 2;
        randomStartDevice = 3000 + randomStartDevice + 1;

        int randomDestDevice = r.nextInt(25) * 2;
        randomDestDevice = 3000 + randomDestDevice + 1;

        int randomDevice = r.nextInt(25) * 2;
        randomDevice = 3000 + randomDevice + 1;

        try {
          Thread.sleep(2000);

          HashMap<Integer, Device> myDevices = myField.getMap();
          EndDevice deviceToMove = (EndDevice) myDevices.get(randomDevice);
          myField.moveDevice(deviceToMove.getXCoord(), deviceToMove.getYCoord());

          EndDevice sendDevice = (EndDevice) myDevices.get(randomStartDevice);
          sendDevice.sendMessage(randomDestDevice, "Hallo");
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    } catch (Exception e) {
      // TODO: handle exception
    }
  }
}
