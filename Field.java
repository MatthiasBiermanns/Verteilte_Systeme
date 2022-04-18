import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;
import java.util.logging.*;

import Exceptions.InvalidInputException;
import Exceptions.PlacementException;
import Exceptions.DeviceNotFound;

class Field {
  final static int ID_LENGTH = 32;
  final static String STANDARD_PATH = System.getProperty("user.home") + "/Desktop/DSR_Logs/";
  static int nextPort = 3000;
  private int xLength, yLength;
  private Semaphore fieldSem = new Semaphore(1, true);
  private Device[][][] field;
  private HashMap<Integer, Device> map = new HashMap<Integer, Device>();
  private Logger logger;

  /**
     * Erzeugt ein neues Feld zum Testen des implementierten Routing-Verfahrens. Die 
     * automatisch erzeugten Router auf dem Feld, werden zufällig platziert.
     * 
     * @param      routerCnt Anzahl der Router auf dem Feld
     * @param      xLenth Breite des Feldes
     * @param      yLength Länge des Feldes
     * @throws     InvalidInputException - Wird geworfen, wenn kein Feld mit 
     *             diesen Eingabeparametern erzeugt werden kann
     * @return     Ein der Message entsprechendes DatagramPacket
     */
  public Field(int routerCnt, int xLength, int yLength) throws InvalidInputException {
    if (xLength <= 0 || yLength <= 0 || xLength * yLength < routerCnt) {
      throw new InvalidInputException();
    }

    this.xLength = xLength;
    this.yLength = yLength;
    field = new Device[xLength][yLength][2];
    this.logger = Logger.getLogger("Logger_field");
    this.setUpLogger();
    for (int i = 0; i < routerCnt; i++) {
      try {
        createNewDevice();
      } catch (PlacementException e) {
        e.printStackTrace();
        break;
      }
    }
    this.cleanDir();
  }

  public void setUpLogger() {
    try {
      this.logger.setLevel(Level.ALL);
        FileHandler handler = new FileHandler(
          Field.STANDARD_PATH + "log_field.txt"
        );
        SimpleFormatter formatter = new SimpleFormatter();
        this.logger.addHandler(handler);
        handler.setFormatter(formatter);
        logger.info("Startet Logging");
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }

  /**
     * Startet alle Router- und EndDevice-Threads innerhalb des Feldes
     */
  public void startDevices() {
    for (Entry<Integer, Device> e : this.map.entrySet()) {
      e.getValue().start();
    }
    logger.info("Devices startet successfully");
  }

  public void cleanDir() {
    File dir = new File(Field.STANDARD_PATH);
    for( File file: dir.listFiles()) {
      file.delete();
    }
  }

  /**
     * Erzeugt einen neuen Router und zugehöriges EndDevice an einer zufälligen Stelle im Feld.
     * Sollte an 500 zufälligen Stellen kein Router platziert werden könne, so wird eine Exception 
     * geworfen. Der Router und das EndDevice bekommen die 2 nächsten freien Ports.
     * 
     * @throws PlacementException - Wird geworfen, wenn nach 500 zufällig ausgewählten Stellen
     *         kein freier Platz für einen Router gefunden wurde.
     */
  public void createNewDevice() throws PlacementException {
    try {
      fieldSem.acquire();

      int xCoord, yCoord;
      int cnt = 0;
      do {
        xCoord = (int) (Math.random() * xLength);
        yCoord = (int) (Math.random() * yLength);
        cnt++;
      } while (field[xCoord][yCoord][0] != null && cnt < 500);

      if (cnt >= 500) {
        throw new PlacementException();
      }

      Router r = new Router(xCoord, yCoord, nextPort, this, nextPort + 1, this.fieldSem);
      EndDevice d = new EndDevice(xCoord, yCoord, this, nextPort + 1, nextPort);
      nextPort++;
      nextPort++;

      field[xCoord][yCoord][0] = r;
      field[xCoord][yCoord][1] = d;
      map.put(r.getPort(), r);
      map.put(d.getPort(), d);

      logger.info("Device created at x: " + xCoord + ", y: " + yCoord + " with Ports: " + r.getPort() + ", " + d.getPort() + "\n");
      fieldSem.release();
    } catch (InterruptedException e) {

    }

  }

  /**
     * Erzeugt einen neuen Router und zugehöriges EndDevice an der angegebenen Stelle im Feld.
     * Ist die Stelle bereits belegt, so wird eine Exception geworfen. Der Router und das 
     * EndDevice bekommen die 2 nächsten freien Ports.
     * 
     * @param  xCoord x-Koordinate des neuen Routers/EndDevice
     * @param  yCoord y-Koordinate des neuen Routers/EndDevice
     * @throws PlacementException - Wird geworfen, wenn die angegebene Stelle bereits belegt ist 
     *         oder sich die Stelle außerhalb des Feldes befindet.
     */
  public void createNewDevice(int xCoord, int yCoord) throws PlacementException {
    try {
      fieldSem.acquire();

      if (xCoord > this.xLength || yCoord > this.yLength || field[xCoord][yCoord][0] != null) {
        throw new PlacementException();
      }

      Router r = new Router(xCoord, yCoord, nextPort, this, nextPort + 1, this.fieldSem);
      EndDevice d = new EndDevice(xCoord, yCoord, this, nextPort + 1, nextPort);
      nextPort++;
      nextPort++;

      field[xCoord][yCoord][0] = r;
      field[xCoord][yCoord][1] = d;
      map.put(r.getPort(), r);
      map.put(d.getPort(), d);

      logger.info("Device created at x: " + xCoord + ", y: " + yCoord + " with Ports: " + r.getPort() + ", " + d.getPort() + "\n");
      fieldSem.release();
    } catch (InterruptedException e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  /**
     * Bewegt den Router und das EndDevice, welches an der gegebenen Stelle ist, an eine zufällige
     * neue Position im Feld.
     * 
     * @param  oldX Alte x-Koordinate, des zu bewegenden Routers/EndDevices
     * @param  oldY Alte x-Koordinate, des zu bewegenden Routers/EndDevices
     * @throws DeviceNotFound - Wird geworfen, wenn sich an der gegebenen Stelle kein Router/EndDEvice befindet
     * @throws PlacementException - Wird geworfen, wenn nach 500 zufällig ausgewählten Stellen
     *         kein freier Platz den Router/ das EndDevice gefunden wurde.
     */
  public void moveDevice(int oldX, int oldY) throws DeviceNotFound, PlacementException {
    try {
      fieldSem.acquire();

      if (field[oldX][oldY][0] == null) {
        throw new DeviceNotFound();
      }

      int newX, newY;
      int cnt = 0;
      do {
        newX = (int) (Math.random() * xLength);
        newY = (int) (Math.random() * yLength);
        cnt++;
      } while (field[newX][newY][0] != null && cnt < 500);

      if (cnt >= 500) {
        throw new PlacementException();
      }

      Router r = (Router) field[oldX][oldY][0];
      EndDevice d = (EndDevice) field[oldX][oldY][1];
      r.setXCoord(newX);
      r.setYCoord(newY);
      d.setXCoord(newX);
      d.setYCoord(newY);
      field[newX][newY][0] = r;
      field[newX][newY][1] = d;
      field[oldX][oldY][0] = null;
      field[oldX][oldY][1] = null;

      logger.info("Devices " + r.getPort() + ", " +d.getPort() + " moved from x: " + oldX + ", y: " + oldY + " to x: " + newX + ", y: " + newY +"\n");
      r.logNewPosition();
      fieldSem.release();
    } catch (InterruptedException e) {

    }
  }

  /**
     * Bewegt den Router und das EndDevice, welches an der gegebenen Stelle ist, an die angegebene Stelle im Feld,
     * falls möglich.
     * 
     * @param  oldX Alte x-Koordinate, des zu bewegenden Routers/EndDevices
     * @param  oldY Alte x-Koordinate, des zu bewegenden Routers/EndDevices
     * @param  newX X-Koordinate, an die der Router/ das EndDevice bewegt werden soll 
     * @param  newY y-Koordinate, an die der Router/ das EndDevice bewegt werden soll
     * @throws DeviceNotFound - Wird geworfen, wenn sich an der gegebenen Stelle kein Router/EndDEvice befindet
     * @throws PlacementException - Wird geworfen, wenn die neue angegebene Stelle bereits belegt ist, 
     *         oder die Stelle außerhalb des aufgespannten Feldes liegt
     */
  public void moveDevice(int oldX, int oldY, int newX, int newY) throws DeviceNotFound, PlacementException {
    try {
      fieldSem.acquire();

      if (field[oldX][oldY][0] == null) {
        throw new DeviceNotFound();
      }

      if (newX > this.xLength || newY > this.yLength || field[newX][newY][0] != null) {
        throw new PlacementException();
      }

      Router r = (Router) field[oldX][oldY][0];
      EndDevice d = (EndDevice) field[oldX][oldY][1];
      r.setXCoord(newX);
      r.setYCoord(newY);
      d.setXCoord(newX);
      d.setYCoord(newY);
      field[newX][newY][0] = r;
      field[newX][newY][1] = d;
      field[oldX][oldY][0] = null;
      field[oldX][oldY][1] = null;

      logger.info("Devices " + r.getPort() + ", " +d.getPort() + " moved from x: " + oldX + ", y: " + oldY + " to x: " + newX + ", y: " + newY +"\n");
      r.logNewPosition();
      fieldSem.release();
    } catch (InterruptedException e) {

    }
  }

  /**
     * Gibt eine Liste der Router in einem 10m (1 Array-Feld = 1m) Umkreis an, die mit einer Nachricht
     * direkt erreicht werden könnten.
     * 
     * @param  port Port des aufrufenden Routers, damit dieser in der Rückgabeliste nicht enthalten ist
     * @param  x X-Koordinate des aufrufenden Routers
     * @param  y Y-Koordinate des aufrufenden Routers
     */
  public LinkedList<Router> getReachableRouter(int port, int x, int y) {
    LinkedList<Router> reachableRouter = new LinkedList<Router>();

    for (Entry<Integer, Device> entry : this.map.entrySet()) {
      Device d = entry.getValue();
      if (d instanceof Router) {
        Router r = (Router) d;
        if (getDistance(x, y, r.getXCoord(), r.getYCoord()) <= 10 && r.getPort() != port) {
          reachableRouter.add(r);
        }
      }
    }
    return reachableRouter;
  }

  /**
     * Gibt zurück, ob ein bestimmter Router direkt von einer Position aus zu erreichen ist.
     * 
     * @param  routerPort Port des zu erreichenden Routers
     * @param  oldX Alte x-Koordinate, des zu bewegenden Routers/EndDevices
     * @param  oldY Alte x-Koordinate, des zu bewegenden Routers/EndDevices
     */
  public boolean isRouterInRange(int routerPort, int x, int y) {
    LinkedList<Router> reachable = this.getReachableRouter(-1, x, y);
    Iterator<Router> it = reachable.iterator();

    while (it.hasNext()) {
      if (it.next().getPort() == routerPort) {
        return true;
      }
    }
    return false;
  }

  /**
     * Gibt den abstand von 2 Positionen im Feld wieder.
     * 
     * @param  x1 x-Koordinate der ersten Position
     * @param  y1 y-Koordinate der ersten Position
     * @param  x2 x-Koordinate der zweiten Position
     * @param  y2 y-Koordinate der zweiten Position
     */
  public static double getDistance(int x1, int y1, int x2, int y2) {
    int xDist = (x1 - x2) >= 0 ? x1 - x2 : x2 - x1;
    int yDist = (y1 - y2) >= 0 ? y1 - y2 : y2 - y1;

    if (xDist == 0) {
      return yDist;
    }
    if (yDist == 0) {
      return xDist;
    }
    return Math.sqrt(xDist * xDist + yDist * yDist);
  }

  /**
     * Löscht den Router und das EndDevice, welches an der gegebenen Stelle zu finden ist.
     * 
     * @param  x X-Koordinate des zu löschenden Routers/EndDevices
     * @param  y y-Koordinate des zu löschenden Routers/EndDevices
     * @throws DeviceNotFound - Wird geworfen, wenn sich an der gegebenen Stelle kein Router/EndDEvice befindet
     */
  public void deleteDevice(int x, int y) throws DeviceNotFound {
    try {
      fieldSem.acquire();

      if (field[x][y] == null) {
        throw new DeviceNotFound();
      }

      Device[] device = field[x][y];
      map.remove(device[0].getPort());
      map.remove(device[1].getPort());
      field[x][y][0] = null;
      field[x][y][1] = null;

      fieldSem.release();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
     * Gibt eine 2-Dimensionale Darstellung samt allen Routern (in Form von deren Ports) auf der Konsole aus
     */
  public void printField() {
    for (int i = 0; i < field.length; i++) {
      for (int j = 0; j < field[i].length; j++) {
        if (field[i][j][0] != null) {
          System.out.print(field[i][j][0].getPort() + " - ");
        } else {
          System.out.print("___ - ");
        }
      }
      System.out.println("\n");
    }
  }

  public Device getDevice(int port) throws DeviceNotFound {
    if(this.map.containsKey(port)) {
      return this.map.get(port);
    }
    throw new DeviceNotFound("There is no Device with the given Port");
  }

  public HashMap<Integer, Device> getMap() {
    return this.map;
  }

  public Device[][][] getField() {
    return this.field;
  }
}