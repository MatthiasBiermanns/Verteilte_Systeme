import java.util.logging.*;

import java.io.*;
import java.util.HashMap;
import java.util.Random;

public class test {
  private final static String STANDARD_PATH = System.getProperty("user.home") + "/Desktop/DSR_Logs/";

  public static void main(String[] args) {

    /*
    try{
      Field myField = new Field(0,25,25);
      sendMessage(myField);
    }catch(Exception e){
      e.printStackTrace();
    }
    */

    /*
    try{
      Field myField = new Field(5, 10, 10);
      sendMessageParallel(myField);
    }catch(Exception e){
      e.printStackTrace();
    }
    */


    /*try{
      Field myField = new Field(100,100,100);
      diffrentNumberOfRouters(myField);
    }catch(Exception e){
      e.printStackTrace();
    }
    */
    

    int count = testNintyPercentVermascht();
    try{
      Thread.sleep(1000);
    }catch(Exception e){
      e.printStackTrace();
    }
    System.out.println("Anzahl Router für 90% vollvermaschtes Netz: " + count);
    
    
    
   /* try {
      Field myField = new Field(250, 100, 100);
      sendMessageWhileMovingRouter(myField);
    } catch (InvalidInputException e) {
      e.printStackTrace();
    } 
    */

    //testNetzVermaschung();
    
    
      
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
      myField.startRouter();
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
      myField.startRouter();
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



  // Eine Hallo Nachricht versenden von 3001 zu 3011

  public static void sendMessage(Field myField) {
    try{
    myField.createNewDevice(0,0);
    myField.createNewDevice(5,5);
    myField.createNewDevice(10, 10);
    myField.createNewDevice(15, 15);
    myField.createNewDevice(20, 20);
    myField.createNewDevice(24, 24);
    myField.startRouter(); 
    myField.printField();
    HashMap<Integer, Device> fieldMap = myField.getMap();
    EndDevice handy = (EndDevice) fieldMap.get(3001);
    handy.sendMessage(3011, "Hallo");
    }catch (Exception e){
      e.printStackTrace();
    }
  }

  // Zwei Hallo Nachrichten versenden von zwei verschiedenen Geräten an das selbe Gerät

  public static void sendMessageParallel(Field myField) {
    try {
      HashMap<Integer, Device> fieldMap = myField.getMap();
      myField.startRouter();
      myField.printField();
      EndDevice handy1 = (EndDevice) fieldMap.get(3001);
      handy1.sendMessage(3005, "Hallo1");
      EndDevice handy2 = (EndDevice) fieldMap.get(3003);
      handy2.sendMessage(3005, "Hallo2");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // Es wird eine Nachricht gesendet und eine Antwort 

  public static void changePath(Field myField){
    try{
      myField.createNewDevice(0,0);
      myField.createNewDevice(2,2);
      myField.createNewDevice(5,5);
      myField.createNewDevice(8,8);
      myField.createNewDevice(9,9);
      HashMap<Integer, Device> fieldMap = myField.getMap();
      myField.startRouter();
      EndDevice handy1 = (EndDevice) fieldMap.get(3001);
      handy1.sendMessage(3011, "Message");
      // Station an dieser Stelle gelöscht, muss einen neuen Pfad nehmen
      myField.deleteDevice(8,8);
      EndDevice handy2 = (EndDevice) fieldMap.get(3011);
      handy2.sendMessage(3001, "Answer");
    }catch (Exception e) {
      e.printStackTrace();
    }
  }
  

  // Testen von unterschiedliche räumliche Verteilungen und verschiedene Anzahl der Router 

  public static void diffrentNumberOfRouters(Field myField) {
    try {
      HashMap<Integer, Device> fieldMap = myField.getMap();
      myField.startRouter();
      EndDevice handy = (EndDevice) fieldMap.get(3001);
      handy.sendMessage(3003, "Hallo");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // Testen ab welcher Knotenzahl das Netz vermascht ist

  public static int testNintyPercentVermascht() {
    int counter;
    int routerCnt = 216;
    do{
      counter = 0;
      for(int i = 0; i < 10; i++){
        try{
          Field myField = new Field(routerCnt, 100, 100);
          if(myField.isNetzVermascht()){
            //Für grobe Abschätzung erstmal größere Schritte
            counter+= 10;
          }
          System.gc();
          //Thread.sleep(1000);
        }catch(Exception e){
          e.printStackTrace();
        }
      }
      routerCnt++;
    }while(counter < 9);
    return routerCnt-1;
  }

  public static void testNetzVermaschung() {
    try {
      Field myField = new Field(0, 20, 20);
      myField.createNewDevice(1, 1);
      myField.createNewDevice(4, 4);
      myField.createNewDevice(5, 5);
      myField.createNewDevice(12, 19);
      // myField.createNewDevice(19, 0);
      myField.createNewDevice(17, 13);
      myField.createNewDevice(7, 0);
      myField.createNewDevice(19, 11);
      myField.createNewDevice(19, 13);
      myField.createNewDevice(11, 14);
      myField.printField();
      System.out.println(myField.isNetzVermascht());
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
      myField.startRouter();
      new Gui(myField, "Mein Fenster");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  

  // -------------------------------------------
  // ----------------Test B---------------------
  // -------------------------------------------


  //Bewegt alle 1-5 Sekunden einen zufällig gewählten Router an eine andere Stelle
  public static void moveRandomRandomRouter(Field myField){
    Random r = new Random();
    try {
      myField.startRouter();

      //Anzahl an Routern auf aktuellem Feld holen um zu wissen welche Router bewegt werden können
      HashMap<Integer, Device> myDevices = myField.getMap();
      int devicesOnField = myDevices.size() / 2;

      while (true) {
        //Zeit zwischen 1 und 5 Sekunden wählen
        int randomTime = r.nextInt(5) * 1000;

        //Zufällige Enddevice ports ermitteln
        int randomDevice = r.nextInt(devicesOnField) * 2;
        randomDevice = 3000 + randomDevice +1;

        try {
          Thread.sleep(randomTime);

          //Enddevice holen und verschieben
          EndDevice deviceToMove = (EndDevice) myField.getDevice(randomDevice);
          myField.moveDevice(deviceToMove.getXCoord(), deviceToMove.getYCoord());

        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
  

  // Beweget alle 2 Sekunden zufääligen Router und sendet von einem zufälligen Router eine 
  //Nachricht an einen anderen zufällig gewählten Router. Wird 20x wiederholt
  public static void sendMessageWhileMovingRouter(Field myField) {
    Random r = new Random();
    try {
      myField.startRouter();
      HashMap<Integer, Device> myDevices = myField.getMap();
      int devicesOnField = myDevices.size() / 2;

      int count = 0;
      while (count < 20) {
        Thread.sleep(2000);

        //Alle zufääligen Enddevice ports bestimmen
        int randomStartDevice = r.nextInt(devicesOnField) * 2;
        randomStartDevice = 3000 + randomStartDevice + 1;

        int randomDestDevice = r.nextInt(devicesOnField) * 2;
        randomDestDevice = 3000 + randomDestDevice + 1;

        int randomDevice = r.nextInt(devicesOnField) * 2;
        randomDevice = 3000 + randomDevice + 1;

        try {   
          // Enddevice holen und bewegen
          EndDevice deviceToMove = (EndDevice) myField.getDevice(randomDevice);
          myField.moveDevice(deviceToMove.getXCoord(), deviceToMove.getYCoord());
          
          // Sender holen und Nachricht "Hallo" mit Sender und Empfänger Port an Empfänger senden
          EndDevice sendDevice = (EndDevice) myField.getDevice(randomStartDevice);
          sendDevice.sendMessage(randomDestDevice, "Hallo from " + randomStartDevice + " to " + randomDestDevice);
  
        } catch (Exception e) {
          e.printStackTrace();
        }
        count++;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
