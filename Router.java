import Exceptions.InvalidMessage;
import java.io.IOException;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Map.Entry;
import java.util.logging.*;

public class Router extends Device {

  private HashMap<Integer, RoutingEntry> paths;
  private byte[] buffer;
  private int myDevicePort;
  private HashMap<String, Message> waiting;
  private HashMap<String, ackTimer> timer;
  private Logger logger;

  // TODO: weiter bearbeiten --> Zuordnung nicht optimal
  // Idee: Maybe Message Objekt in Map speichern --> Lösung suchen
  private HashMap<String, Long> knownIds;

  /**
   * Erzeugt einen neuen komplett unbelasteten Router
   *
   * @param xCoord x-Koordinate in field
   * @param yCoord y-Koordinate in field
   * @param port UDP-Port über den der Router zu erreichen ist
   * @param field das feld in dem der Router sich befindet
   * @param myDevicePort Port des zugewiesenen Devices (Typischerweise: eigener Port + 1)
   *
   * @return ein neues Router-Objekt
   */
  public Router(
    int xCoord,
    int yCoord,
    int port,
    Field field,
    int myDevicePort
  ) {
    super(xCoord, yCoord, field, port);
    this.myDevicePort = myDevicePort;
    this.paths = new HashMap<>();
    this.waiting = new HashMap<>();
    this.knownIds = new HashMap<>();
    this.timer = new HashMap<>();
    this.buffer = new byte[65507];
    this.logger = Logger.getLogger("Logger_" + this.port);
    this.setUpLogger();
  }

  /**
   * Diese Methode wird bei start des Router-Threads ausgeführt und wartet bis zur terminierung
   * dessen auf ankommende Packete des UDP-Protokolls for den this.port. Anschließend werden abhängig
   * von der einkommenden Nachricht weitere UDP-Packete versendet und es wird auf weitere einkommende Packete gewartet
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
            this.log(message);
            DatagramPacket[] toSend = evaluateMessage(message);

            // Builds necessary data for Guiserver.
            byte[] data = new GuiUpdateMessage(
              this.xCoord,
              this.yCoord,
              message.getCommand(),
              message.getPath()
            )
              .toString()
              .getBytes();
            // sends data to Gui Server for visual updates
            socket.send(
              new DatagramPacket(
                data,
                data.length,
                InetAddress.getByName("localhost"),
                4998
              )
            );

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

  public void log(Message msg) {
    
  }

  /**
   * Wird von Router-Thread automatisch durch die run() Methode aufgerufen
   *
   * Interpretiert den Inhalt einer Message anhand des Commands und dem Ziel- bzw. Ausgangsrouter (Port)
   *
   *
   * @param      msg   Die Nachricht, die zu interpretieren ist
   * @return     Ein Array der zu versendenden DatagramPackets
   *
   */
  public DatagramPacket[] evaluateMessage(Message msg) {
    DatagramPacket[] toSend = new DatagramPacket[0];

    this.logger.info(msg.toBeautyString());
    try {
      switch (msg.getCommand()) {
        case Send:
          toSend = this.processSend(msg);

          break;
        case RouteRequest:
          if (
            msg.getDestPort() == this.myDevicePort &&
            !knownIds.containsKey(msg.getMessageId())
          ) {
            knownIds.put(msg.getMessageId(), Instant.now().getEpochSecond());
            msg.setCommand(Command.RouteReply);
            msg.addToPath(this.port);
            msg.setDestPort(msg.getSourcePort());
            msg.setSourcePort(this.port);
            toSend = new DatagramPacket[1];
            toSend[0] =
              this.createDatagramPacket(msg, this.getPreviousPort(msg));
          } else {
            toSend = this.processRouteRequest(msg);
          }
          break;
        case RouteReply:
          if (msg.getDestPort() == this.port) {
            toSend = this.processRouteReply(msg);
            this.logger.info(this.pathLogging());
          } else {
            if (
              field.isRouterInRange(
                this.getPreviousPort(msg),
                this.xCoord,
                this.yCoord
              )
            ) {
              toSend = new DatagramPacket[1];
              toSend[0] =
                this.createDatagramPacket(msg, this.getPreviousPort(msg));
            }
          }
          break;
        case Forward:
          if (msg.getDestPort() == this.myDevicePort) {
            // weitere Code, falls Enddevice die Nachricht erreichen soll
            toSend = new DatagramPacket[1];
            Message ack = new Message(
              msg.getMessageId(),
              Command.Ack,
              this.port,
              this.getPreviousPort(msg),
              msg.getPath(),
              ""
            );
            toSend[0] = this.createDatagramPacket(ack, ack.getDestPort());
            System.out.println("Router " + this.port + ": " + msg.getContent());
          } else {
            toSend = this.processForward(msg);
          }
          break;
        case RouteError:
          if (msg.getDestPort() == this.port) {
            toSend = this.processRouteError(msg);
            this.logger.info(this.pathLogging());
          } else {
            int prevRouter = getPreviousPort(msg);
            if (field.isRouterInRange(prevRouter, this.xCoord, this.yCoord)) {
              toSend = new DatagramPacket[1];
              toSend[0] = createDatagramPacket(msg, prevRouter);
            }
          }
          break;
        case Ack:
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
          //TODO: sonderfall für wir sind der sourceRouter
          toSend = new DatagramPacket[1];
          toSend[0] = createRouteError(msg);
          break;
        default:
          System.out.println(msg.getCommand());
          System.out.println("Error: Command unknown");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return toSend;
  }

  /**
   * Startet eine Routine (groomPaths) zum aufräumen aktuell gecacheter Routingpfade und
   * überprüft ob für die zu versendende Message bereits ein Routingpfad zur Verfügung steht.
   * --> Falls Ja: Rückgabe eines DatagramPackets entsprechend der zu versendenden Message
   * --> Falls Nein: Zurückstellen der Message in this.waiting und einleiten der Route-Discovery Routine
   *
   * @param      msg   Die Nachricht, die versendet werden soll
   * @throws     UnknownHostException - Ausnahmefall: Nur falls localhost nicht bekannt sein sollte
   * @return     Ein Array der zu versendenden DatagramPackets
   */
  public DatagramPacket[] processSend(Message msg) throws UnknownHostException {
    DatagramPacket[] packet;
    this.groomPaths();
    if (paths.containsKey(msg.getDestPort())) {
      packet = new DatagramPacket[1];

      msg.setCommand(Command.Forward);
      RoutingEntry entry = paths.get(msg.getDestPort());
      entry.updateUsage();
      msg.setPath(entry.getPath());
      msg.setSourcePort(port);

      packet[0] = createDatagramPacket(msg, getNextPort(msg));
      ackTimer myAckTimer = new ackTimer(msg, this.port);
      timer.put(msg.getMessageId(), myAckTimer);
    } else {
      packet = createRouteRequest(msg);
    }
    return packet;
  }

  /**
   * Erzeugt ein Array an UDP-Packeten, die einem Multicast einer Route-Request entsprechen.
   * Es wird ein DatagramPacket für jeden Router im Umkreis von 10m (1 Arrayslot = 1m)
   * erstellt. Die eigentlich zu versendende Message wird in this.waiting gespeichert.
   *
   * @param      msg   Die Nachricht, die zu versenden ist
   * @throws     UnknownHostException - Ausnahmefall: Nur falls localhost nicht bekannt sein sollte
   * @return     Ein Array der zu versendenden Route-Request DatagramPackets
   */
  public DatagramPacket[] createRouteRequest(Message msg)
    throws UnknownHostException {
    LinkedList<Integer> list = new LinkedList<Integer>();
    list.add(this.port);
    Message newMessage = new Message(
      getUUID(),
      Command.RouteRequest,
      this.port,
      msg.getDestPort(),
      list,
      ""
    );

    msg.setCommand(Command.Forward);
    waiting.put(newMessage.getMessageId(), msg);
    knownIds.put(newMessage.getMessageId(), Instant.now().getEpochSecond());
    return getMulticastPackets(newMessage);
  }

  /**
   * Erzeugt ein DatagramPacket für eine zu versendende Message
   *
   * @param      msg   Die Nachricht, die zu versenden ist
   * @throws     UnknownHostException - Ausnahmefall: Nur falls localhost nicht bekannt sein sollte
   * @return     Ein der Message entsprechendes DatagramPacket
   */
  public DatagramPacket createDatagramPacket(Message msg, int port)
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
   * @param      msg   Die Nachricht, die weitergeleitet werden soll
   * @throws     UnknownHostException - Ausnahmefall: Nur falls localhost nicht bekannt sein sollte
   * @return     Port des nächsten Routers
   */
  public int getNextPort(Message msg) {
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
   * @param      msg   Die Nachricht, deren Pfad betrachtet wird
   * @throws     UnknownHostException - Ausnahmefall: Nur falls localhost nicht bekannt sein sollte
   * @return     Port des vorherigen Routers
   */
  public int getPreviousPort(Message msg) {
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
   * @param      msg Ankommende Route-Request
   * @throws     UnknownHostException - Ausnahmefall: Nur falls localhost nicht bekannt sein sollte
   * @return     Array Aus DatagramPackets die den neuen RouteRequests entsprechen
   */
  public DatagramPacket[] processRouteRequest(Message msg)
    throws UnknownHostException {
    DatagramPacket[] packet = new DatagramPacket[0];
    if (!knownIds.containsKey(msg.getMessageId())) {
      knownIds.put(msg.getMessageId(), Instant.now().getEpochSecond());
      msg.addToPath(this.port);
      packet = getMulticastPackets(msg);
    }
    return packet;
  }

  /**
   * Erzeugt ein Array an DatagramPackets aus der Message msg. Für jeden erreichbaren
   * Router wird ein DatagramPacket mit entsprechendem Zielport erzeugt.
   *
   * @param      msg   Die Nachricht, die weitergeleitet werden soll
   * @throws     UnknownHostException - Ausnahmefall: Nur falls localhost nicht bekannt sein sollte
   * @return     DatagramPackets entsprechend der Message msg mit allen
   *             erreichbaren Routern
   */
  public DatagramPacket[] getMulticastPackets(Message msg)
    throws UnknownHostException {
    DatagramPacket[] packet;
    LinkedList<Router> reachable =
      this.field.getReachableRouter(this.port, this.xCoord, this.yCoord);
    Iterator<Router> it = reachable.iterator();
    packet = new DatagramPacket[reachable.size()];
    int i = 0;
    while (it.hasNext()) {
      packet[i] = createDatagramPacket(msg, it.next().getPort());
      i++;
    }
    return packet;
  }

  /**
   * Verarbeitet eine eingehende RouteReply mit dem ausführenden Objekt als Zielrouter der RouteReply
   * bzw. dem Router von dem die RouteRequest ausging. Sollte der nächste Router laut dem entdeckten
   * Pfad noch immer erreichbar sein, so wird der entdeckte Pfad in den Cache aufgenommen und die
   * Ursprungsnachricht wird vorbereitet und versendet. Andernfalls wird für den Zielrouter des ermittelten
   * Pfades eine neue RouteRequest erstellt.
   *
   * @param      msg   Angekommene RouteReply, deren Ergebnis eingepflegt werden soll
   * @throws     UnknownHostException - Ausnahmefall: Nur falls localhost nicht bekannt sein sollte
   * @return     DatagramPackets entsprechend der alten Message, für die der Pfad entdeckt wurde
   */
  public DatagramPacket[] processRouteReply(Message msg)
    throws UnknownHostException {
    DatagramPacket[] toSend = new DatagramPacket[1];

    if (field.isRouterInRange(getNextPort(msg), this.xCoord, this.yCoord)) {
      //source port + 1 to get port of the receiver enddevice
      this.paths.put(msg.getSourcePort() + 1, new RoutingEntry(msg.getPath()));
      System.out.println(this.port + ": the routing path is " + msg.getPath());
      Message oldMessage = waiting.get(msg.getMessageId());
      waiting.remove(msg.getMessageId());
      ackTimer myAckTimer = new ackTimer(oldMessage, this.port);
      timer.put(oldMessage.getMessageId(), myAckTimer);
      oldMessage.setPath(msg.getPath());
      toSend[0] =
        this.createDatagramPacket(oldMessage, getNextPort(oldMessage));
    } else {
      Message oldMessage = waiting.get(msg.getMessageId());
      toSend = createRouteRequest(oldMessage);
    }
    return toSend;
  }

  /**
   * Ermittelt, ob eine eingegangene Message entlang des Pfades weitergeleitet werden kann.
   * Sollte der nächste Router im Pfad erreichbar sein, so werden DatagramPackets für die weiterzuleitende
   * Message, sowie das zum Vorgänger zu schickende Acknowledgement erstellt. Andernfalls wird eine
   * RouteError Message erzeugt und zu einem DatagramPacket umgewandelt, welches dann zum SourceRouter
   * der eingegangenen Nachricht gesendet wird.
   *
   * @param      msg   Die Nachricht, die weitergeleitet werden soll
   * @throws     UnknownHostException - Ausnahmefall: Nur falls localhost nicht bekannt sein sollte
   * @return     DatagramPackets entsprechend der Message msg mit allen
   *             erreichbaren Routern
   */
  public DatagramPacket[] processForward(Message msg)
    throws UnknownHostException {
    DatagramPacket[] toSend = new DatagramPacket[1];
    int nextRouter = this.getNextPort(msg);
    if (field.isRouterInRange(nextRouter, this.xCoord, this.yCoord)) {
      if (
        field.isRouterInRange(
          this.getPreviousPort(msg),
          this.xCoord,
          this.yCoord
        )
      ) {
        toSend = new DatagramPacket[2];
        Message ack = new Message(
          msg.getMessageId(),
          Command.Ack,
          this.port,
          this.getPreviousPort(msg),
          msg.getPath(),
          ""
        );
        toSend[1] = this.createDatagramPacket(ack, ack.getDestPort());
      }
      toSend[0] = this.createDatagramPacket(msg, nextRouter);
      ackTimer myAckTimer = new ackTimer(msg, this.port);
      timer.put(msg.getMessageId(), myAckTimer);
    } else {
      toSend[0] = createRouteError(msg);
    }
    return toSend;
  }

  public DatagramPacket createRouteError(Message msg)
    throws UnknownHostException {
    String content = msg.getDestPort() + " " + msg.getMessageId();
    Message errMessage = new Message(
      getUUID(),
      Command.RouteError,
      this.port,
      msg.getSourcePort(),
      msg.getPath(),
      content
    );
    return createDatagramPacket(errMessage, this.getPreviousPort(errMessage));
  }

  /**
   * Verarbeitet einen eingegangenen RouteError. Der alte Pfad, über den die Message, die den
   * RouteError ausgelöst hatte, versendet wurde, wird aus dem Cache gelöscht. Anschließend wird
   * in dem Cache nach einem Pfad gesucht, der den Zielrouter enthält.
   * Existiert dieser, wird dieser Pfad in den Cache übernommen. In jedem Fall wird die Ursprungsnachricht
   * beim EndDevice über eine RetryMessage erneut angefragt.
   *
   * @param      msg   Eingegangener RouteError
   * @throws     UnknownHostException - Ausnahmefall: Nur falls localhost nicht bekannt sein sollte
   * @return     DatagramPacket entsprechend der RetryMessage
   */
  public DatagramPacket[] processRouteError(Message msg)
    throws UnknownHostException {
    DatagramPacket[] toSend = new DatagramPacket[1];

    String[] contentParts = msg.getContent().split(" ", 2);
    this.paths.remove(Integer.parseInt(contentParts[0]));
    Message retryMessage = new Message(
      getUUID(),
      Command.Retry,
      this.port,
      this.myDevicePort,
      new LinkedList<>(),
      contentParts[1]
    );

    LinkedList<Integer> newPath = new LinkedList<>();
    // destPort -1 to get the routerPort (routerPort ist in dem path gespeichert)
    int destPort = Integer.parseInt(contentParts[0]) - 1;
    for (Entry<Integer, RoutingEntry> e : this.paths.entrySet()) {
      RoutingEntry re = e.getValue();
      if (isInPath(re.getPath(), destPort)) {
        newPath = getSubPath(re.getPath(), destPort);

        //destPort + 1 to get the EnddevicePort
        this.paths.put(destPort + 1, new RoutingEntry(newPath));
        break;
      }
    }

    toSend[0] = createDatagramPacket(retryMessage, this.myDevicePort);
    return toSend;
  }

  /**
   * Ermittelt, ob ein bestimmter Port in einem Pfad enthalten ist.
   *
   * @param      path LinkedList aus den Ports (Anfang: Ausgangsrouter; Ende: Zielrouter)
   * @param      port Port, nach dem in Pfad gesucht wird
   * @return     Wahrheitswert, ob Port enthalten ist
   */
  public boolean isInPath(LinkedList<Integer> path, int port) {
    Iterator<Integer> it = path.iterator();
    while (it.hasNext()) {
      if (it.next() == port) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gibt aus einer LinkedList von Integern (Pfad) eine LinkedList der Knoten bis zum
   * gewünschten port destPort wieder.
   *
   * @param      longPath   Pfad, aus dem der Pfad zum Port destPort ausgeschnitten werden soll
   * @param      destPort   Port, der als Ziel des neuen Pfades fungieren soll
   * @return     kürzerer Pfad mit destPort als letzten Knoten der Liste
   */
  public LinkedList<Integer> getSubPath(
    LinkedList<Integer> longPath,
    int destPort
  ) {
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
  public void groomPaths() {
    long currTime = Instant.now().getEpochSecond();
    this.paths.entrySet()
      .removeIf(e -> (currTime - e.getValue().getLastUsed() >= 300));
  }

  public void setUpLogger() {
    try {
      this.logger.setLevel(Level.ALL);
      FileHandler handler = new FileHandler(
        Field.STANDARD_PATH + "log_" + this.port + ".txt"
      );
      SimpleFormatter formatter = new SimpleFormatter();
      this.logger.addHandler(handler);
      handler.setFormatter(formatter);
      this.logger.info("Router startet logging");
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }

  public String pathLogging() {
    String res = "";
    //TODO: Problemlösung
    for( Entry<Integer, RoutingEntry> e : this.paths.entrySet()) {
      Long timestamp = e.getValue().getLastUsed();
      Date time = new java.sql.Date(timestamp);
      SimpleDateFormat format = new SimpleDateFormat("H:mm:ss", Locale.GERMANY);
      String line = Integer.toString(e.getKey())+ "( " + format.format(time) + " ): " + e.getValue().getPath().toString() + "\n";
      res += line;
    }
    return res;
  }

  public String timerLogging() {
    //TODO: Logging complete
    return "";
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
