package MANet_Abgabe.src;

import Exceptions.InvalidMessage;
import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;
import java.util.logging.*;

public class Router extends Device {

  private HashMap<Integer, RoutingEntry> paths;
  private byte[] buffer;
  private int myDevicePort;
  private Semaphore sem;
  private HashMap<String, Message> waiting;
  private HashMap<String, ackTimer> timer;
  private Logger logger;
  private FileHandler handler;
  private HashMap<String, Long> knownIds;

  /**
   * Erzeugt einen neuen komplett unbelasteten Router. Legt zusätzlich eine 
   * Log-Datei für den Router an und Konfiguriert den Logger.
   *
   * @param xCoord       x-Koordinate in field
   * @param yCoord       y-Koordinate in field
   * @param port         UDP-Port über den der Router zu erreichen ist
   * @param field        das feld in dem der Router sich befindet
   * @param myDevicePort Port des zugewiesenen Devices (Typischerweise: eigener
   *                     Port + 1)
   * @param sem          Semaphore für Interaktionen mit dem Field (muss gleiche
   *                     wie bei Field-Objekt sein)
   *
   * @return ein neues Router-Objekt
   */
  public Router(
      int xCoord,
      int yCoord,
      int port,
      Field field,
      int myDevicePort,
      Semaphore sem) {
    super(xCoord, yCoord, field, port);
    this.myDevicePort = myDevicePort;
    this.sem = sem;
    this.paths = new HashMap<>();
    this.waiting = new HashMap<>();
    this.knownIds = new HashMap<>();
    this.timer = new HashMap<>();
    this.buffer = new byte[65507];
    this.logger = Logger.getLogger("Logger_" + this.port);
    try {
      this.handler = new FileHandler(
          Field.STANDARD_PATH + "log_" + this.port + ".txt");
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
    this.setUpLogger();
  }

  /**
   * Wird bei Löschung des Objektes aufgerufen. Schließt hier ordentlich den
   * Output-Stream in die Log-Datei und schreibt davor noch alle 
   * nichtgeschriebenen Logs.
   */
  @Override
  public void finalize() {
    this.handler.flush();
    this.logger.removeHandler(this.handler);
    this.handler.close();
  }

  /**
   * Diese Methode wird bei start des Router-Threads ausgeführt und wartet bis zur
   * terminierung dessen auf ankommende Packete des UDP-Protokolls for den this.port.
   * Anschließend werden abhängig von der einkommenden Nachricht weitere UDP-Packete 
   * versendet und es wird auf weitere einkommende Packete gewartet.
   */
  public void run() {
    try (DatagramSocket socket = new DatagramSocket(this.port)) {
      while (true) {
        DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
        try {
          socket.receive(dp);
          try {
            String messageString = new String(dp.getData(), 0, dp.getLength());
            Message message = new Message(messageString);
            DatagramPacket[] toSend = evaluateMessage(message);

            // Builds necessary data for Guiserver.
            byte[] data = new GuiUpdateMessage(
                this.port,
                message.getDestPort(),
                this.xCoord,
                this.yCoord,
                message.getCommand(),
                message.getPath())
                .toString()
                .getBytes();
            // sends data to Gui Server for visual updates
            socket.send(
                new DatagramPacket(
                    data,
                    data.length,
                    InetAddress.getByName("localhost"),
                    4998));

            for (int i = 0; i < toSend.length; i++) {
              socket.send(toSend[i]);
            }
          } catch (InvalidMessage e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Wird von Router-Thread automatisch durch die run() Methode aufgerufen
   *
   * Interpretiert den Inhalt einer Message anhand des Commands und dem Ziel- bzw.
   * Ausgangsrouter (Port). In Abhängigkeit des inhaltes wird die Nachrict 
   * verarbeitet oder weitere Methoden aufgerufen. Die eingehende Nachricht 
   * und der Status des Routers nach Verarbeitung wird geloggt.
   *
   * @param msg Die Nachricht, die zu interpretieren ist
   * @return Ein Array der zu versendenden DatagramPackets
   */
  private DatagramPacket[] evaluateMessage(Message msg) {
    DatagramPacket[] toSend = new DatagramPacket[0];

    // Verhindert das Loggen von RouteRequest, die vom Protokoll scnhließend 
    // sowieso ignoriert werden
    if (!this.knownIds.containsKey(msg.getMessageId()) ||
        msg.getCommand() != Command.RouteRequest) {
      this.logger.info(msg.toBeautyString());
    }
    try {
      switch (msg.getCommand()) {
        case Send:
          toSend = this.processSend(msg);
          break;
        case RouteRequest:
          // Test ob die RouteRequest das zugehörige EndDevice
          if (msg.getDestPort() == this.myDevicePort &&
              !knownIds.containsKey(msg.getMessageId())) {
            knownIds.put(msg.getMessageId(), Instant.now().getEpochSecond());
            
            // Setzt die nötigen Attribute der Message für die ausgehende RouteReply  
            msg.setCommand(Command.RouteReply);
            msg.addToPath(this.port);
            msg.setDestPort(msg.getSourcePort());
            msg.setSourcePort(this.port);
            toSend = new DatagramPacket[1];
            toSend[0] = this.createDatagramPacket(msg, this.getPreviousPort(msg));
          } else {
            toSend = this.processRouteRequest(msg);
          }
          break;
        case RouteReply:
          if (msg.getDestPort() == this.port) {
            // deckt ab, dass der entdeckte Pfad in Cache eingearbeitet wird
            toSend = this.processRouteReply(msg);
          } else {
            // schickt die RouteReply den Pfad entlang zum Ausgangsrouter
            sem.acquire();
            if (field.isRouterInRange(
                this.getPreviousPort(msg),
                this.xCoord,
                this.yCoord)) {
              toSend = new DatagramPacket[1];
              // Previous Router, da der entdeckte RoutingPfad rückwärts durchlaufen wird
              toSend[0] = this.createDatagramPacket(msg, this.getPreviousPort(msg));
            }
            sem.release();
          }
          break;
        case Forward:
          // Prüfen, ob Nachricht an das EndDevice gerichtet ist, oder weitergeleitet werden muss
          if (msg.getDestPort() == this.myDevicePort) {
            // Leitet die Nachricht an das EndDevice weiter
            toSend = new DatagramPacket[1];
            Message ack = new Message(
                msg.getMessageId(),
                Command.Ack,
                this.port,
                this.getPreviousPort(msg),
                msg.getPath(),
                "");
            toSend[0] = this.createDatagramPacket(ack, ack.getDestPort());
          } else {
            toSend = this.processForward(msg);
          }
          break;
        case RouteError:
          // Prüft, ob Error an diesen Router gerichtet ist
          if (msg.getDestPort() == this.port) {
            // Verarbeiten des eingegangenen Errors
            toSend = this.processRouteError(msg);
          } else {
            // Weiterleiten des Errors
            // previousRouter, da der ehemals geplante Pfad rückwärts durchlaufen wird
            int prevRouter = getPreviousPort(msg);
            sem.acquire();
            if (field.isRouterInRange(prevRouter, this.xCoord, this.yCoord)) {
              toSend = new DatagramPacket[1];
              toSend[0] = createDatagramPacket(msg, prevRouter);
            }
            sem.release();
          }
          break;
        case Ack:
          // Terminieren des Timer-Threads, da Nachricht erfolgreich weitergeleitet wurd
          ackTimer myAckTimer = timer.get(msg.getMessageId());
          timer.remove(msg.getMessageId());
          try {
            myAckTimer.stop();
          } catch (NullPointerException e) {
            System.out.println("Exception in " + this.port);
            System.out.println(e.getMessage());
            e.printStackTrace();
          }
          break;
        case AckNeeded:
          toSend = new DatagramPacket[1];
          // Test ob Message, die verloren gegegangen ist, von diesem Router ausging
          if (msg.getSourcePort() == this.myDevicePort) {
            // Ging von hier aus --> Nachricht neu anfragen bei EndDevice
            Message retryMessage = new Message(
                getUUID(),
                Command.Retry,
                this.port,
                this.myDevicePort,
                new LinkedList<>(),
                msg.getMessageId());
            toSend[0] = createDatagramPacket(retryMessage, myDevicePort);
          } else {
            // Nachricht ging nicht von hier aus --> RouteError an den Ursprungsrouter
            toSend[0] = createRouteError(msg);
          }
          break;
        default:
          System.out.println(msg.getCommand());
          System.out.println("Error: Command unknown");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    // verhindert logging von RReqs, die vom Router ignoriert werden
    if (!this.knownIds.containsKey(msg.getMessageId()) ||
        msg.getCommand() != Command.RouteRequest) {
      this.logStatus();
    }
    return toSend;
  }

  /**
   * Startet eine Routine (groomPaths) zum aufräumen aktuell gecacheter
   * Routingpfade und
   * überprüft ob für die zu versendende Message bereits ein Routingpfad zur
   * Verfügung steht.
   * --> Falls Ja: Rückgabe eines DatagramPackets entsprechend der zu versendenden
   * Message
   * --> Falls Nein: Zurückstellen der Message in this.waiting und einleiten der
   * Route-Discovery Routine
   *
   * @param msg Die Nachricht, die versendet werden soll
   * @throws UnknownHostException - Ausnahmefall: Nur falls localhost nicht
   *                              bekannt sein sollte
   * @return Ein Array der zu versendenden DatagramPackets
   */
  private DatagramPacket[] processSend(Message msg) throws UnknownHostException {
    DatagramPacket[] packet;

    // Aufräumen des Caches --> Alte Pfade werden gelöscht
    this.groomPaths();

    //Abfrage, ob ein Pfad bekannt ist
    if (paths.containsKey(msg.getDestPort())) {
      //Ein Pfad ist bekannt --> Nachricht vorbereiten und verschicken
      packet = new DatagramPacket[1];

      msg.setCommand(Command.Forward);
      RoutingEntry entry = paths.get(msg.getDestPort());
      entry.updateUsage();
      msg.setPath(entry.getPath());
      msg.setSourcePort(port);

      packet[0] = createDatagramPacket(msg, getNextPort(msg));

      //Timer setzen, falls Nachricht verloren geht
      ackTimer myAckTimer = new ackTimer(msg, this.port);
      timer.put(msg.getMessageId(), myAckTimer);
    } else {
      // kein Pfad bekannt --> Route-Discovery Routine starten
      packet = createRouteRequest(msg);
    }
    return packet;
  }

  /**
   * Erzeugt ein Array an UDP-Packeten, die einem Multicast einer Route-Request
   * entsprechen. Es wird ein DatagramPacket für jeden Router im Umkreis von 
   * 10m (1 Arrayslot = 1m) erstellt. Die eigentlich zu versendende Message 
   * wird in this.waiting gespeichert.
   *
   * @param msg Die Nachricht, die zu versenden ist
   * @throws UnknownHostException - Ausnahmefall: Nur falls localhost nicht
   *                              bekannt sein sollte
   * @return Ein Array der zu versendenden Route-Request DatagramPackets
   */
  private DatagramPacket[] createRouteRequest(Message msg)
      throws UnknownHostException {
    LinkedList<Integer> list = new LinkedList<Integer>();
    list.add(this.port);
    
    // Versehen der Message mit unique ID
    String id = getUUID();
    Message newMessage = new Message(
        id,
        Command.RouteRequest,
        this.port,
        msg.getDestPort(),
        list,
        "");

    msg.setCommand(Command.Forward);

    // Zu sendende Nachtich zurückstellen
    waiting.put(newMessage.getMessageId(), msg);
    knownIds.put(newMessage.getMessageId(), Instant.now().getEpochSecond());
    return getMulticastPackets(newMessage);
  }

  /**
   * Erzeugt ein DatagramPacket für eine zu versendende Message
   *
   * @param msg Die Nachricht, die zu versenden ist
   * @throws UnknownHostException - Ausnahmefall: Nur falls localhost nicht
   *                              bekannt sein sollte
   * @return Ein der Message entsprechendes DatagramPacket
   */
  private DatagramPacket createDatagramPacket(Message msg, int port)
      throws UnknownHostException {
    InetAddress destAddress = InetAddress.getByName("localhost");
    byte[] message = msg.toString().getBytes();
    return new DatagramPacket(message, message.length, destAddress, port);
  }

  /**
   * Evaluiert den nächsten Port laut dem Routingpfad der eingegebenen
   * Nachricht. Wenn der Port im Pfad nicht enthalten seinen sollte,
   * wird Port -1 zurückgegeben (Sollte nie eintreten, da Nachricht dann
   * gar nicht an diesem Router ankommen kann)
   *
   * @param msg Die Nachricht, die weitergeleitet werden soll
   * @throws UnknownHostException - Ausnahmefall: Nur falls localhost nicht
   *                              bekannt sein sollte
   * @return Port des nächsten Routers
   */
  private int getNextPort(Message msg) {
    Iterator<Integer> it = msg.getPath().iterator();
    int nextPort = -1;
    while (it.hasNext()) {
      if (it.next() == this.port) {
        nextPort = it.next();
        break;
      }
    }
    return nextPort;
  }

  /**
   * Evaluiert den vorherigen Port laut dem Routingpfad der eingegebenen
   * Nachricht. Wenn der Port im Pfad nicht enthalten seinen sollte,
   * wird Port -1 zurückgegeben (Sollte nie eintreten, da Nachricht dann
   * gar nicht an diesem Router ankommen kann)
   *
   * @param msg Die Nachricht, deren Pfad betrachtet wird
   * @throws UnknownHostException - Ausnahmefall: Nur falls localhost nicht
   *                              bekannt sein sollte
   * @return Port des vorherigen Routers
   */
  private int getPreviousPort(Message msg) {
    Iterator<Integer> it = msg.getPath().descendingIterator();
    int nextPort = -1;
    while (it.hasNext()) {
      if (it.next() == this.port) {
        nextPort = it.next();
        break;
      }
    }

    return nextPort;
  }

  /**
   * Verarbeitet eine einkommende Nachricht mit dem Command RouteRequest,
   * welche diesen Router nicht als Ziel hat.
   *
   * @param msg Ankommende Route-Request
   * @throws UnknownHostException - Ausnahmefall: Nur falls localhost nicht
   *                              bekannt sein sollte
   * @return Array Aus DatagramPackets die den neuen RouteRequests entsprechen
   */
  private DatagramPacket[] processRouteRequest(Message msg)
      throws UnknownHostException {
    DatagramPacket[] packet = new DatagramPacket[0];

    // Prüfung, ob Request bereits über einen anderen Pfad einging / bekannt ist
    if (!knownIds.containsKey(msg.getMessageId())) {
      // merken der ID
      knownIds.put(msg.getMessageId(), Instant.now().getEpochSecond());

      // Pfadverlängerung um eigenen Router
      msg.addToPath(this.port);
      packet = getMulticastPackets(msg);
    }
    return packet;
  }

  /**
   * Erzeugt ein Array an DatagramPackets aus der Message msg. Für jeden
   * erreichbaren Router wird ein DatagramPacket mit entsprechendem Zielport 
   * erzeugt.
   *
   * @param msg Die Nachricht, die weitergeleitet werden soll
   * @throws UnknownHostException Ausnahmefall: Nur falls localhost nicht
   *                              bekannt sein sollte
   * @return DatagramPackets entsprechend der Message msg mit allen
   *         erreichbaren Routern
   */
  private DatagramPacket[] getMulticastPackets(Message msg)
      throws UnknownHostException {
    DatagramPacket[] packet = new DatagramPacket[0];
    try {
      sem.acquire();
      LinkedList<Router> reachable = this.field.getReachableRouter(this.port, this.xCoord, this.yCoord);
      sem.release();
      Iterator<Router> it = reachable.iterator();
      packet = new DatagramPacket[reachable.size()];
      int i = 0;
      while (it.hasNext()) {
        packet[i] = createDatagramPacket(msg, it.next().getPort());
        i++;
      }
    } catch (InterruptedException e) {
    }
    return packet;
  }

  /**
   * Verarbeitet eine eingehende RouteReply mit dem ausführenden Objekt als
   * Zielrouter der RouteReply bzw. dem Router von dem die RouteRequest ausging. 
   * Sollte der nächste Router laut dem entdeckten Pfad noch immer erreichbar 
   * sein, so wird der entdeckte Pfad in den Cache aufgenommen und die
   * Ursprungsnachricht wird vorbereitet und versendet. Andernfalls wird für den
   * Zielrouter des ermittelten Pfades eine neue RouteRequest erstellt.
   *
   * @param msg Angekommene RouteReply, deren Ergebnis eingepflegt werden soll
   * @throws UnknownHostException Ausnahmefall: Nur falls localhost nicht
   *                              bekannt sein sollte
   * @return DatagramPackets entsprechend der alten Message, für die der Pfad
   *         entdeckt wurde
   */
  private DatagramPacket[] processRouteReply(Message msg)
      throws UnknownHostException {
    DatagramPacket[] toSend = new DatagramPacket[1];

    try {
      sem.acquire();

      // Prüfung, dass Route noch verwendbar ist und nächster Router nich in 
      // Zwischenzeit out of Range gekommen ist
      if (field.isRouterInRange(getNextPort(msg), this.xCoord, this.yCoord)) {
        sem.release();
        // source port + 1 um den Port des EndDevices zum Zielrouter zu bekommen
        this.paths.put(
            msg.getSourcePort() + 1,
            new RoutingEntry(msg.getPath()));
        
        // heraussuchen und aufbereiten der zurückgestellten Nachricht
        Message oldMessage = waiting.get(msg.getMessageId());
        waiting.remove(msg.getMessageId());

        // Timer setzen, falls Nachricht verloren geht
        ackTimer myAckTimer = new ackTimer(oldMessage, this.port);
        timer.put(oldMessage.getMessageId(), myAckTimer);

        // verwenden des gerade entdeckten Pfades
        oldMessage.setPath(msg.getPath());
        toSend[0] = this.createDatagramPacket(oldMessage, getNextPort(oldMessage));
      } else {
        sem.release();

        // Routingpfad wieder unbrauchbar --> Prozedere wiederholen
        Message oldMessage = waiting.get(msg.getMessageId());
        toSend = createRouteRequest(oldMessage);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
    return toSend;
  }

  /**
   * Ermittelt, ob eine eingegangene Message entlang des Pfades weitergeleitet
   * werden kann. Sollte der nächste Router im Pfad erreichbar sein, so werden 
   * DatagramPackete für die weiterzuleitende Message, sowie das zum Vorgänger 
   * zu schickende Acknowledgement erstellt. Andernfalls wird eine RouteError 
   * Message erzeugt und zu einem DatagramPacket umgewandelt, welches dann zum 
   * SourceRouter der eingegangenen Nachricht gesendet wird.
   *
   * @param msg Die Nachricht, die weitergeleitet werden soll
   * @throws UnknownHostException - Ausnahmefall: Nur falls localhost nicht
   *                              bekannt sein sollte
   * @return DatagramPackets entsprechend der Message msg mit allen
   *         erreichbaren Routern
   */
  private DatagramPacket[] processForward(Message msg)
      throws UnknownHostException {
    DatagramPacket[] toSend = new DatagramPacket[1];
    int nextRouter = this.getNextPort(msg);
    try {
      sem.acquire();
      // Prüfung ob nächster Router erreichbar
      if (field.isRouterInRange(nextRouter, this.xCoord, this.yCoord)) {
        // Prüfung, ob vorangegangener Router noch erreichbar
        if (field.isRouterInRange(
            this.getPreviousPort(msg),
            this.xCoord,
            this.yCoord)) {
          // Beide erreichbar --> Ack vorbereiten schicken
          toSend = new DatagramPacket[2];
          Message ack = new Message(
              msg.getMessageId(),
              Command.Ack,
              this.port,
              this.getPreviousPort(msg),
              msg.getPath(),
              "");
          toSend[1] = this.createDatagramPacket(ack, ack.getDestPort());
        }
        sem.release();

        // Nachricht weiterleiten
        toSend[0] = this.createDatagramPacket(msg, nextRouter);

        // Timer starten, falls Nachricht verloren geht
        ackTimer myAckTimer = new ackTimer(msg, this.port);
        timer.put(msg.getMessageId(), myAckTimer);
      } else {
        sem.release();
        // Router nicht erreichbar --> Error zu Ursprungsrouter
        toSend[0] = createRouteError(msg);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
    return toSend;
  }

  /**
   * Erstellt aus einer Nachricht, die nicht weitergeleitet werden kann 
   * eine RouteError Nachricht.
   * 
   * @param msg Message-Objekt, welches nicht weitergeleitet werden kann
   * @return Ein DatagramPacket mit einem RouteError
   * @throws UnknownHostException
   */
  private DatagramPacket createRouteError(Message msg)
      throws UnknownHostException {
    String content = msg.getDestPort() + " " + msg.getMessageId();
    Message errMessage = new Message(
        getUUID(),
        Command.RouteError,
        this.port,
        msg.getSourcePort(),
        msg.getPath(),
        content);
    return createDatagramPacket(errMessage, this.getPreviousPort(errMessage));
  }

  /**
   * Verarbeitet einen eingegangenen RouteError. Der alte Pfad, über den die
   * Message, die den RouteError ausgelöst hatte, versendet wurde, wird aus 
   * dem Cache gelöscht. Anschließend wird in dem Cache nach einem Pfad 
   * gesucht, der den Zielrouter enthält. Existiert dieser, wird dieser Pfad 
   * in den Cache übernommen. In jedem Fall wird die Ursprungsnachricht
   * beim EndDevice über eine RetryMessage erneut angefragt.
   *
   * @param msg Eingegangener RouteError
   * @throws UnknownHostException - Ausnahmefall: Nur falls localhost nicht
   *                              bekannt sein sollte
   * @return DatagramPacket entsprechend der RetryMessage
   */
  private DatagramPacket[] processRouteError(Message msg)
      throws UnknownHostException {
    DatagramPacket[] toSend = new DatagramPacket[1];

    // Auslesen welche Nachricht fehlschlug und an welchen Router diese gerichtet war
    String[] contentParts = msg.getContent().split(" ", 2);

    // ehemals genutzten Pfad aus Cache entfernen --> Funktioniert nicht mehr
    this.paths.remove(Integer.parseInt(contentParts[0]));

    // Vorbereiten und verschicken einer RetryMessage um Message bei EndDevice neu anzufordern
    Message retryMessage = new Message(
        getUUID(),
        Command.Retry,
        this.port,
        this.myDevicePort,
        new LinkedList<>(),
        contentParts[1]);

    LinkedList<Integer> newPath = new LinkedList<>();
    // destPort -1 um den RouterPort, statt DevicePort zu erhalten (routerPort ist in dem path gespeichert)
    int destPort = Integer.parseInt(contentParts[0]) - 1;

    // Sucht, ob Router in einem anderen Pfad enthalten ist --> neuer Pfad
    for (Entry<Integer, RoutingEntry> e : this.paths.entrySet()) {
      RoutingEntry re = e.getValue();
      if (isInPath(re.getPath(), destPort)) {
        // es gibt einen Pfad, der den Zielrouter enthält
        newPath = getSubPath(re.getPath(), destPort);

        // destPort + 1 um den EndDevice Port zu erhalten
        // subPfad in Cache speichern
        this.paths.put(destPort + 1, new RoutingEntry(newPath));
        break;
      }
    }

    // Message neu bei EndDevice anfordern
    // gab es einen subpath, wird dieser in processSend automatisch verwendet
    toSend[0] = createDatagramPacket(retryMessage, this.myDevicePort);
    return toSend;
  }

  /**
   * Ermittelt, ob ein bestimmter Port in einem Pfad enthalten ist.
   *
   * @param path LinkedList aus den Ports (Anfang: Ausgangsrouter; Ende:
   *             Zielrouter)
   * @param port Port, nach dem in Pfad gesucht wird
   * @return Wahrheitswert, ob Port enthalten ist
   */
  private boolean isInPath(LinkedList<Integer> path, int port) {
    Iterator<Integer> it = path.iterator();
    while (it.hasNext()) {
      if (it.next() == port) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gibt aus einer LinkedList von Integern (Pfad) eine LinkedList der Knoten bis
   * zum gewünschten port destPort wieder.
   *
   * @param longPath Pfad, aus dem der Pfad zum Port destPort ausgeschnitten
   *                 werden soll
   * @param destPort Port, der als Ziel des neuen Pfades fungieren soll
   * @return kürzerer Pfad mit destPort als letzten Knoten der Liste
   */
  private LinkedList<Integer> getSubPath(
      LinkedList<Integer> longPath,
      int destPort) {
    LinkedList<Integer> newPath = new LinkedList<>();
    Iterator<Integer> it = longPath.iterator();
    while (it.hasNext()) {
      int currPort = it.next();
      newPath.add(currPort);
      if (currPort == destPort) {
        break;
      }
    }
    return newPath;
  }

  /**
   * Räumt den Pfad-Cache des Routers auf. Sortiert alle Pfade aus,
   * die 5 Minuten oder länge nicht genutzt wurden.
   */
  private void groomPaths() {
    long currTime = Instant.now().getEpochSecond();
    this.paths.entrySet()
        .removeIf(e -> (currTime - e.getValue().getLastUsed() >= 300));
  }

  /**
   * Konfiguriert den Logger und weißt ihm einen FileHandler und somit eine Log-Datei zu
   */
  private void setUpLogger() {
    // Setzt fest, welche Art von Logging Einträgen erlaubt sind --> Nicht weiter relevant
    this.logger.setLevel(Level.ALL);
    SimpleFormatter formatter = new SimpleFormatter();
    this.logger.addHandler(handler);
    handler.setFormatter(formatter);
    this.logger.info(
        "Router startet logging\n Position: ( x: " +
            this.xCoord +
            "; y: " +
            this.yCoord +
            " )\nPort: " +
            this.port +
            "\n");

  }

  /**
   * Logt die aktuelle Position des Routers. 
   * Aufruf meist von außen, wenn Router bewegt wurde.
   */
  public void logNewPosition() {
    this.logger.info(
        "\nNew Position: ( x: " + this.xCoord + "; y: " + this.yCoord + " )\n");
  }

  /**
   * Loggt die internen Attribute des Routers.
   * Aufruf nach jeder Verarbeitung einer Nachricht.
   */
  private void logStatus() {
    String toLog = "\n" +
        this.pathLogging() +
        "\n" +
        this.timerLogging() +
        "\n" +
        this.knownIdLogging() +
        "\n";
    this.logger.info(toLog);
  }

  /**
   * Gibt den Pfad-Cache des Routers in einem aufgeräumten String wieder.
   * @return String mit den Pfaden, die dem Router bekannt sind als String
   */
  private String pathLogging() {
    String res = "Path Cache:";
    SimpleDateFormat format = new SimpleDateFormat("H:mm:ss", Locale.GERMANY);
    for (Entry<Integer, RoutingEntry> e : this.paths.entrySet()) {
      Long timestamp = e.getValue().getLastUsed();
      // *1000, da time von 1970 in Millisekunden angegeben sein muss (timestamp ist
      // in Sek)
      Date time = new java.sql.Date(timestamp * 1000);
      String line = "\n" +
          Integer.toString(e.getKey()) +
          "( " +
          format.format(time) +
          " ): " +
          e.getValue().getPath().toString();
      res += line;
    }
    return res;
  }

  /**
   * Gibt die aktuell laufenden Timer-Threads des Routers in ordentlichem Format als String wieder.
   * @return Alle Timer, die der Router aktiviert hat als String
   */
  private String timerLogging() {
    String res = "Running Timer (Id - Sekunden):";
    for (Entry<String, ackTimer> e : this.timer.entrySet()) {
      res += "\n" + e.getKey() + " - " + e.getValue().getCount();
    }
    return res;
  }

  /**
   * Gibt die bekannten IDs des Router in einem geordneten String wieder.
   * @return Alle dem Router bekannten IDs als String
   */
  private String knownIdLogging() {
    int count = 0;
    String res = "Known Ids:";
    for (Entry<String, Long> e : this.knownIds.entrySet()) {
      if (count % 3 == 0) {
        res += "\n\t" + e.getKey();
      } else {
        res += "\t" + e.getKey();
      }
      count++;
    }
    return res;
  }

  public int getXCoord() {
    return this.xCoord;
  }

  public int getYCoord() {
    return this.yCoord;
  }

  public int getMyDevicePort() {
    return this.myDevicePort;
  }

  public void setXCoord(int xCoord) {
    this.xCoord = xCoord;
  }

  public void setYCoord(int yCoord) {
    this.yCoord = yCoord;
  }
}
