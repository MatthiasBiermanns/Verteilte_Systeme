import Exceptions.DeviceNotFound;
import Exceptions.InvalidInputException;
import Exceptions.PlacementException;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;
import java.util.logging.*;

class Field {

  static final int ID_LENGTH = 32;
  static final String STANDARD_PATH =
    System.getProperty("user.home") + "/Desktop/DSR_Logs/";
  private int nextPort = 3000;
  private int xLength, yLength;
  private Semaphore fieldSem = new Semaphore(1, true);
  private Device[][][] field;
  private HashMap<Integer, Device> map = new HashMap<Integer, Device>();
  private Logger logger;
  private FileHandler handler;

  /**
   * Erzeugt ein neues Feld zum Testen des implementierten Routing-Verfahrens.
   * Des Weiteren wird der Log-Folder geleert, eine Log-File erstellt und der
   * Logger
   * configuriert.
   *
   * Die automatisch erzeugten Router auf dem Feld, werden zufällig platziert.
   *
   * @param routerCnt Anzahl der Router auf dem Feld
   * @param xLenth    Breite des Feldes
   * @param yLength   Länge des Feldes
   * @throws InvalidInputException - Wird geworfen, wenn kein Feld mit
   *                               diesen Eingabeparametern erzeugt werden kann
   * @return Ein der Message entsprechendes DatagramPacket
   */
  public Field(int routerCnt, int xLength, int yLength)
    throws InvalidInputException {
    if (xLength <= 0 || yLength <= 0 || xLength * yLength < routerCnt) {
      throw new InvalidInputException();
    }

    this.xLength = xLength;
    this.yLength = yLength;
    field = new Device[xLength][yLength][2];
    this.logger = Logger.getLogger("Logger_field");
    try {
      this.handler = new FileHandler(Field.STANDARD_PATH + "log_field.txt");
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
    this.cleanDir();
    this.setUpLogger();
    for (int i = 0; i < routerCnt; i++) {
      try {
        createNewDevice();
      } catch (PlacementException e) {
        e.printStackTrace();
        break;
      }
    }
  }

  /**
   * Wird bei Löschung des Objektes aufgerufen. Schließt hier ordentlich den
   * Output-Stream in die Log-Datei und schreibt davor noch alle
   * nichtgeschriebenen
   * Logs.
   */
  @Override
  public void finalize() {
    this.handler.flush();
    this.logger.removeHandler(this.handler);
    this.handler.close();
  }

  /**
   * Konfiguriert den Logger. Setzt hier die Log-Möglichkeiten auf ALL (erlaubt
   * verschiedene
   * Log-Einträge; nicht weiter relevant) und ordnet dem Logger die in Konstruktor
   * erstellt Log-Datei bzw. den FileHandler zu.
   */
  private void setUpLogger() {
    this.logger.setLevel(Level.ALL);
    SimpleFormatter formatter = new SimpleFormatter();
    this.logger.addHandler(handler);
    handler.setFormatter(formatter);
    logger.info("Startet Logging\n");
  }

  /**
   * Startet alle Router-Threads innerhalb des Feldes.
   */
  public void startRouter() {
    for (Entry<Integer, Device> e : this.map.entrySet()) {
      if (e.getValue() instanceof Router) {
        e.getValue().start();
      }
    }
    logger.info("Router startet successfully");
  }

  /**
   * Löscht alle Log-Files im Log-Folder.
   */
  private void cleanDir() {
    File dir = new File(Field.STANDARD_PATH);
    for (File file : dir.listFiles()) {
      file.delete();
    }
  }

  /**
   * Erzeugt einen neuen Router und zugehöriges EndDevice an einer zufälligen
   * Stelle im Feld. Sollte an 500 zufälligen Stellen kein Router platziert
   * werden könne, so wird eine Exception geworfen. Der Router und das EndDevice
   * bekommen die 2 nächsten freien Ports. Operation wird unter gegenseitigem
   * Ausschluss ausgeführt, damit keine Abfragen über den Feldstatus vom Router
   * (Aus eigenem Thread) auf das Feld gemacht werden.
   *
   * @throws PlacementException - Wird geworfen, wenn nach 500 zufällig
   *                            ausgewählten Stellen
   *                            kein freier Platz für einen Router gefunden wurde.
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

      Router r = new Router(
        xCoord,
        yCoord,
        nextPort,
        this,
        nextPort + 1,
        this.fieldSem
      );
      EndDevice d = new EndDevice(xCoord, yCoord, this, nextPort + 1, nextPort);
      nextPort++;
      nextPort++;

      field[xCoord][yCoord][0] = r;
      field[xCoord][yCoord][1] = d;
      map.put(r.getPort(), r);
      map.put(d.getPort(), d);

      logger.info(
        "Device created at x: " +
        xCoord +
        ", y: " +
        yCoord +
        " with Ports: " +
        r.getPort() +
        ", " +
        d.getPort() +
        "\n"
      );
      fieldSem.release();
    } catch (InterruptedException e) {}
  }

  /**
   * Erzeugt einen neuen Router und zugehöriges EndDevice an der angegebenen
   * Stelle im Feld. Ist die Stelle bereits belegt oder außerhalb des Feldes,
   * so wird eine Exception geworfen. Der Router und das EndDevice bekommen
   * die 2 nächsten freien Ports. Operation wird unter gegenseitigem Ausschluss
   * ausgeführt, damit keine Abfragen über den Feldstatus vom Router (Aus
   * eigenem Thread) auf das Feld gemacht werden.
   *
   * @param xCoord x-Koordinate des neuen Routers/EndDevice
   * @param yCoord y-Koordinate des neuen Routers/EndDevice
   * @throws PlacementException - Wird geworfen, wenn die angegebene Stelle
   *                            bereits belegt ist
   *                            oder sich die Stelle außerhalb des Feldes
   *                            befindet.
   */
  public void createNewDevice(int xCoord, int yCoord)
    throws PlacementException {
    try {
      fieldSem.acquire();

      if (
        xCoord > this.xLength ||
        yCoord > this.yLength ||
        field[xCoord][yCoord][0] != null
      ) {
        throw new PlacementException();
      }

      Router r = new Router(
        xCoord,
        yCoord,
        nextPort,
        this,
        nextPort + 1,
        this.fieldSem
      );
      EndDevice d = new EndDevice(xCoord, yCoord, this, nextPort + 1, nextPort);
      nextPort++;
      nextPort++;

      field[xCoord][yCoord][0] = r;
      field[xCoord][yCoord][1] = d;
      map.put(r.getPort(), r);
      map.put(d.getPort(), d);

      logger.info(
        "Device created at x: " +
        xCoord +
        ", y: " +
        yCoord +
        " with Ports: " +
        r.getPort() +
        ", " +
        d.getPort() +
        "\n"
      );
      fieldSem.release();
    } catch (InterruptedException e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Bewegt den Router und das EndDevice, welches an der gegebenen Stelle ist, an
   * eine zufällige neue Position im Feld. Sollte nach 500 Versuchen keine freie
   * Stelle für den Router und das EndDevice gefunden werden, wird eine Exception
   * geworfen. Operation wird unter gegenseitigem Ausschluss ausgeführt, damit
   * keine
   * Abfragen über den Feldstatus vom Router (Aus eigenem Thread) auf das Feld
   * gemacht werden.
   *
   * @param oldX Alte x-Koordinate, des zu bewegenden Routers/EndDevices
   * @param oldY Alte x-Koordinate, des zu bewegenden Routers/EndDevices
   * @throws DeviceNotFound     - Wird geworfen, wenn sich an der gegebenen Stelle
   *                            kein Router/EndDEvice befindet
   * @throws PlacementException - Wird geworfen, wenn nach 500 zufällig
   *                            ausgewählten Stellen
   *                            kein freier Platz den Router/ das EndDevice
   *                            gefunden wurde.
   */
  public void moveDevice(int oldX, int oldY)
    throws DeviceNotFound, PlacementException {
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

      logger.info(
        "Devices " +
        r.getPort() +
        ", " +
        d.getPort() +
        " moved from x: " +
        oldX +
        ", y: " +
        oldY +
        " to x: " +
        newX +
        ", y: " +
        newY +
        "\n"
      );
      sendUpdateToGui(r.getPort(), oldX, oldY);
      r.logNewPosition();
      fieldSem.release();
    } catch (InterruptedException e) {}
  }

  /**
   * Bewegt den Router und das EndDevice, welches an der gegebenen Stelle ist, an
   * die angegebene Stelle im Feld, falls möglich. Operation wird unter
   * gegenseitigem
   * Ausschluss ausgeführt, damit keine Abfragen über den Feldstatus vom Router
   * (Aus eigenem Thread) auf das Feld gemacht werden.
   *
   * @param oldX Alte x-Koordinate, des zu bewegenden Routers/EndDevices
   * @param oldY Alte x-Koordinate, des zu bewegenden Routers/EndDevices
   * @param newX X-Koordinate, an die der Router/ das EndDevice bewegt werden soll
   * @param newY y-Koordinate, an die der Router/ das EndDevice bewegt werden soll
   * @throws DeviceNotFound     - Wird geworfen, wenn sich an der gegebenen Stelle
   *                            kein Router/EndDEvice befindet
   * @throws PlacementException - Wird geworfen, wenn die neue angegebene Stelle
   *                            bereits belegt ist,
   *                            oder die Stelle außerhalb des aufgespannten Feldes
   *                            liegt
   */
  public void moveDevice(int oldX, int oldY, int newX, int newY)
    throws DeviceNotFound, PlacementException {
    try {
      fieldSem.acquire();

      if (field[oldX][oldY][0] == null) {
        throw new DeviceNotFound();
      }

      if (
        newX > this.xLength ||
        newY > this.yLength ||
        field[newX][newY][0] != null
      ) {
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

      logger.info(
        "Devices " +
        r.getPort() +
        ", " +
        d.getPort() +
        " moved from x: " +
        oldX +
        ", y: " +
        oldY +
        " to x: " +
        newX +
        ", y: " +
        newY +
        "\n"
      );
      sendUpdateToGui(r.getPort(), oldX, oldY);
      r.logNewPosition();
      fieldSem.release();
    } catch (InterruptedException e) {}
  }

  /**
   * Gibt eine Liste der Router in einem 10m (1 Array-Feld = 1m) Umkreis an, die
   * mit einer Nachricht direkt erreicht werden könnten. Aufruf dieser Methode
   * sollte unter gegenseitigem Ausschluss über fieldSem ausgeführt werden
   * (Im Router), um keine ungültigen Abfragen zu machen, während sich das Feld
   * verändert.
   *
   * @param port Port des aufrufenden Routers, damit dieser in der Rückgabeliste
   *             nicht enthalten ist
   * @param x    X-Koordinate des aufrufenden Routers
   * @param y    Y-Koordinate des aufrufenden Routers
   */
  public LinkedList<Router> getReachableRouter(int port, int x, int y) {
    LinkedList<Router> reachableRouter = new LinkedList<Router>();

    for (Entry<Integer, Device> entry : this.map.entrySet()) {
      Device d = entry.getValue();
      if (d instanceof Router) {
        Router r = (Router) d;
        if (
          getDistance(x, y, r.getXCoord(), r.getYCoord()) <= 10 &&
          r.getPort() != port
        ) {
          reachableRouter.add(r);
        }
      }
    }
    return reachableRouter;
  }

  /**
   * Gibt zurück, ob alle Router auf dem Feld miteinander vollständig
   * verbunden sind, also von jedem Router, jeder Router erreicht
   * werden kann. Operation wird unter gegenseitigem Ausschluss
   * ausgeführt, für den Fall, dass die Methode während einer laufenden
   * Simulation aufgerufen wird. Ist eigentlich aber nicht für laufende
   * Simulationen geeignet/gedacht.
   */
  public boolean isNetzVermascht() {
    try {
      fieldSem.acquire();
      // Ein Router, der im Netz enthalten ist --> Startrouter für Breitensuche
      Router r1 = this.getFirstRouter();
      if (r1 != null) {
        // Ausgangslage: Alle möglichen Router sind enthalten
        LinkedList<Integer> allRouterPorts = getAllPorts();
        LinkedList<Router> queue = new LinkedList<>();
        queue.add(r1);

        // Entfernen, da startrouter sonst 2x berücksichtigt werden würde
        allRouterPorts.remove((Integer) r1.getPort());

        // Netzgröße des Netzes in dem sich der Startrouter befindet
        // zwischengespeichert, damit diese im Ausschluss ausgeführt werden
        int netSize = getPartNetSize(allRouterPorts, queue);
        int mapSize = this.map.size();
        fieldSem.release();

        // mapSize / 2, da für jeden Router auch EndDevices in der map enthalten sind
        return netSize == mapSize / 2;
      }
      fieldSem.release();

      // Netz ist leer, demnach ist es auch nicht voll verbunden
      return false;
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    // Return falls Exception auftritt
    // --> eigentliches Ergebnis konnte nicht ermittelt werden
    // --> Annahme: Netz ist nicht verbunden
    return false;
  }

  /**
   * Gibt den Router mit dem niedrigsten Port wieder, der sich aktuell im
   * Feld befindet. Meistens Router 3000, sofern dieser nicht gelöscht wurde.
   * Gibt null zurück, falls sich kein Router im Feld befindet. Kein
   * gegenseitiger Ausschluss, da die methode nur durch isNetzVermascht
   * ausgeführt wird, und dies bereits unter Ausschluss passiert. Folglich
   * gibt es so keinen Deadlock.
   *
   * @return Router den Router mit dem niedrigsten Port im Feld
   */
  private Router getFirstRouter() {
    if (this.map.size() > 0) {
      // 3000, da dies der niedrigste mögliche Port ist
      int cnt = 3000;
      while (true) {
        if (this.map.containsKey(cnt)) {
          fieldSem.release();

          // ist sicher ein Router, da immer um 2 hochgezählt wird
          return (Router) this.map.get(cnt);
        }
        // +2, damit die EndDevices übersprungen werden
        cnt = cnt + 2;
      }
    }
    return null;
  }

  /**
   * Gibt eine LinkedList aller Ports wieder, die sich im Feld befinden.
   * Kein gegenseitiger Ausschluss, da die methode nur durch isNetzVermascht
   * ausgeführt wird, und dies bereits unter Ausschluss passiert. Folglich
   * gibt es so keinen Deadlock.
   *
   * @return LinkedList<Integer> eine Liste der Ports aller Router, die sich im Feld befinden
   */
  private LinkedList<Integer> getAllPorts() {
    LinkedList<Integer> allRouterPorts = new LinkedList<>();
    for (Entry<Integer, Device> e : this.map.entrySet()) {
      if (e.getValue() instanceof Router) {
        allRouterPorts.add(e.getKey());
      }
    }
    return allRouterPorts;
  }

  /**
   * Rekursive Methode, die eine Breitensuche auf dem Netz simuliert.
   * Gibt die Größe des Netzes zurück, welches durch die Router in queue
   * erreicht werden kann. Kein gegenseitiger Ausschluss, da die methode
   * nur durch isNetzVermascht ausgeführt wird, und dies bereits unter
   * Ausschluss passiert. Folglich gibt es so keinen Deadlock.
   *
   * @param ports liste der Ports, welche sich im Teilnetz befinden und
   *              noch nicht durch die Suche berücksichtigt wurden
   * @param queue Liste der Router, die noch von der Suche durchlaufen
   *              werden müssen
   * @return größe des Netzes, welches über den anfangs gegebenen Router
   *         erreicht werden kann
   */
  private int getPartNetSize(
    LinkedList<Integer> ports,
    LinkedList<Router> queue
  ) {
    if (!queue.isEmpty()) {
      int netCnt = 1;
      Router currRouter = queue.remove();
      LinkedList<Router> reachable =
        this.getReachableRouter(
            currRouter.getPort(),
            currRouter.getXCoord(),
            currRouter.getYCoord()
          );
      for (Router router : reachable) {
        if (ports.contains(router.getPort())) {
          ports.remove((Integer) router.getPort());
          queue.add(router);
        }
      }
      return netCnt + this.getPartNetSize(ports, queue);
    }
    return 0;
  }

  /**
   * Gibt zurück, ob ein bestimmter Router direkt von einer Position aus zu
   * erreichen ist. Aufruf der Methode sollte unter gegenseitigem Ausschluss
   * über fieldSem stattfinden, damit keine Veränderungen währenddessen
   * durchgeführt werden können.
   *
   * @param routerPort Port des zu erreichenden Routers
   * @param oldX       Alte x-Koordinate, des zu bewegenden Routers/EndDevices
   * @param oldY       Alte x-Koordinate, des zu bewegenden Routers/EndDevices
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
   * @param x1 x-Koordinate der ersten Position
   * @param y1 y-Koordinate der ersten Position
   * @param x2 x-Koordinate der zweiten Position
   * @param y2 y-Koordinate der zweiten Position
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
   * Löscht den Router und das EndDevice, welches an der gegebenen Stelle zu
   * finden ist. Operation wird unter gegenseitigem Ausschluss ausgeführt,
   * damit keine Abfragen über den Feldstatus vom Router (Aus eigenem Thread)
   * auf das Feld gemacht werden.
   *
   * @param x X-Koordinate des zu löschenden Routers/EndDevices
   * @param y y-Koordinate des zu löschenden Routers/EndDevices
   * @throws DeviceNotFound - Wird geworfen, wenn sich an der gegebenen Stelle
   *                        kein Router/EndDEvice befindet
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
   * Gibt eine 2-Dimensionale Darstellung samt allen Routern (in Form von deren
   * Ports) auf der Konsole aus. Erfolgt under gegenseitigem Ausschluss, damit
   * das Bild einem bestimmten Zustand entspricht.
   */
  public void printField() {
    try {
      fieldSem.acquire();
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
      fieldSem.release();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Gibt das Device-Objekt (Router oder EndDevice) mit dem gegebenen Port wieder.
   *
   * @param port Port des Device
   * @return Device Router oder EndDevice mit gem gegebenen Port
   * @throws DeviceNotFound Wenn kein Device mit dem gegebenen Port auf dem Feld
   *                        existiert
   */
  public Device getDevice(int port) throws DeviceNotFound {
    if (this.map.containsKey(port)) {
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

  /**
   * Sends GuiUpdateMessage to GuiServer to update Router-Position.
   * @param port int Port of Router
   * @param oldX int xCoord before moving
   * @param oldY int yCoord before moving
   */
  private void sendUpdateToGui(int port, int oldX, int oldY) {
    try (DatagramSocket socket = new DatagramSocket()) {
      byte[] data = new GuiUpdateMessage(
        port,
        0, // destPort default, cause not needed
        oldX,
        oldY,
        Command.Unknown,
        new LinkedList<>()
      )
        .toString()
        .getBytes();

      socket.send(
        new DatagramPacket(
          data,
          data.length,
          InetAddress.getByName("localhost"),
          4998 // Port of Gui-Server
        )
      );
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
