import java.util.HashMap;
import java.util.Random;

public class test {
  public static void main(String[] args) {

    int count = testNintyPercentVermascht();
    try{
      Thread.sleep(1000);
    }catch(Exception e){
      e.printStackTrace();
    }
    System.out.println("Anzahl Router für 90% vollvermaschtes Netz: " + count);    
      
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
