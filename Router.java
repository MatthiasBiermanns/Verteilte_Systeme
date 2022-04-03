import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import Exceptions.InvalidMessage;

import java.net.*;
// import java.io.*;

public class Router extends Device {
  private HashMap<Integer, RoutingEntry> paths;
  private byte[] buffer;
  private int myDevicePort;
  private HashMap<String, Message> waiting;
  private HashMap<String, Long> knownIds;

  public Router(int xCoord, int yCoord, int port, Field field, int myDevicePort) {
    super(xCoord, yCoord, field, port);
    this.myDevicePort = myDevicePort;
    this.paths = new HashMap<>();
    this.waiting = new HashMap<>();
    this.knownIds = new HashMap<>();
    this.buffer = new byte[65507];
  }

  public Router() {
    super();
  }

  public void run() {
    try (DatagramSocket socket = new DatagramSocket(this.port)) {
      while (true) {
        DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
        try {
          socket.receive(dp);
          try {
            Message message = new Message(new String(dp.getData(), 0, dp.getLength()));
            DatagramPacket[] toSend = evaluateMessage(message);

            for(int i = 0; i < toSend.length; i++) {
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

  public DatagramPacket[] evaluateMessage(Message msg) {
    // Prüfen, ob MessageId schonmal angekommen
    // Prüfen, ob für mich

    // Send: kommt von Endgerät --> Routing Routine durchgehen
    // Forward: einfaches Weiterleiten, kommt von anderem Router
    // RouteRequest: kommt von Router --> Pfad erweitern ggf. Zurücksenden
    // RouteReply: kommt von Zielrouter --> Einpflegen in Routes + Senden
    // RouteError: kommt von Router auf Pfad --> Neusenden einer Nachricht --> RoutingRoutine

    DatagramPacket[] toSend = new DatagramPacket[0];
    try {
      switch (msg.getCommand()) {
        case Send:
          toSend =this.processSend(msg);
          break;
        case Forward:
          if(msg.getDestPort() == this.port) {
            this.receiveMessage(msg);
          } else {
            this.processForward(msg);
          }
          break;
        case RouteRequest:
          if(msg.getDestPort() == this.port) {
            msg.setCommand(Command.RouteReply);
            msg.addToPath(this.port);
            toSend = new DatagramPacket[1];
            toSend[0] = this.createDatagramPacket(msg, this.getPreviousPort(msg));
          } else {
            toSend = this.processRouteRequest(msg);
          }
          break;
        case RouteReply:
          if(msg.getDestPort() == this.port) {
            this.processRouteReply(msg);
          } else {
            this.forwardMessage(msg);
          }
          break;
        case RouteError:
          if(msg.getDestPort() == this.port) {
            this.processRouteError(msg);
          } else {
            this.forwardMessage(msg);
          }
          break;
        default:
          System.out.println("Error: Command unkwon");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return toSend;
  }

  public DatagramPacket[] processSend(Message msg) throws UnknownHostException {
    DatagramPacket[] packet = new DatagramPacket[1];
    if(paths.containsKey(msg.getDestPort())) {
      msg.setCommand(Command.Forward);
      RoutingEntry entry = paths.get(msg.getDestPort());
      entry.updateUsage();
      msg.setPath(entry.getPath());
      msg.setSourcePort(port);
      
      packet[0] = createDatagramPacket(msg, getNextPort(msg));
    } else {
      LinkedList<Integer> list = new LinkedList<Integer>();
      list.add(this.port);
      Message newMessage = new Message(getUUID(), Command.RouteRequest, this.port, msg.getDestPort(), list, "");
      
      packet[0] = createDatagramPacket(newMessage, getNextPort(msg));
      waiting.put(newMessage.getMessageId(), msg);
    }
    return packet;
  }

  public DatagramPacket createDatagramPacket(Message msg, int port) throws UnknownHostException {
    InetAddress destAddress = InetAddress.getByName("localhost");
    byte[] message = msg.toString().getBytes();
    return new DatagramPacket(message, message.length, destAddress, port);
  }

  public int getNextPort(Message msg) {
    Iterator<Integer> it = msg.getPath().iterator();
    int nextPort = -1;
    while( it.hasNext()) {
      if(it.next() == this.port) {
        nextPort = it.next();
        break;
      }
    }

    return nextPort;
  }
  
  public int getPreviousPort(Message msg) {
    Iterator<Integer> it = msg.getPath().descendingIterator();
    int nextPort = -1;
    while( it.hasNext()) {
      if(it.next() == this.port) {
        nextPort = it.next();
        break;
      }
    }
  
    return nextPort;

  }

  public void processForward(Message msg) {
    // Prüfen, ob der in path festgelegte Router zur Verfügung steht
    // Wenn ja Nachricht dahin senden
    // --> Warten auf acknowledgement
    // --> Nicht erfolgreich => RouteError Schicken

  }


  public DatagramPacket[] processRouteRequest(Message msg) {
    // Prüfen, ob schon erhalten
    // Prüfen, ob er Ziel ist
    // Wenn Ja
    // --> Sich anhängen und als RouteReply zurückschicken
    // Wenn Nein
    // --> SIch anhängen und Multicasten an alle anderen Router
    return new DatagramPacket[0];
  }

  public void processRouteReply(Message msg) {
    // Prüfen, ob 
    // Prüfen ob schon andere Reply angekommen ist
    // Wenn Ja --> Ignorieren
  }

  public void sendRouteRequest() {

  }

  public void processRouteError(Message msg) {

  }

  public void receiveMessage(Message msg) {

  }

  


  public void forwardMessage(Message msg) {

  }

  public void sendRouteReply(Message msg) {

  }

  public void sendRouteError() {

  }

  public void updateRoutePaths() {
    for (Entry<Integer, RoutingEntry> path : this.paths.entrySet()) {
      if (Instant.now().getEpochSecond() - path.getValue().getLastUsed() >= 600) {
        this.paths.remove(path.getKey());
      }
    }
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
