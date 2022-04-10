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
  private HashMap<String, Long> waitingForAck;
  private HashMap<String, Long> knownIds;

  public Router(int xCoord, int yCoord, int port, Field field, int myDevicePort) {
    super(xCoord, yCoord, field, port);
    this.myDevicePort = myDevicePort;
    this.paths = new HashMap<>();
    this.waiting = new HashMap<>();
    this.waitingForAck = new HashMap<>();
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
            String messageString = new String(dp.getData(), 0, dp.getLength());
            Message message = new Message(messageString);
            DatagramPacket[] toSend = evaluateMessage(message);

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

  public DatagramPacket[] evaluateMessage(Message msg) {
    // Prüfen, ob MessageId schonmal angekommen
    // Prüfen, ob für mich

    // Send: kommt von Endgerät --> Routing Routine durchgehen
    // Forward: einfaches Weiterleiten, kommt von anderem Router
    // RouteRequest: kommt von Router --> Pfad erweitern ggf. Zurücksenden
    // RouteReply: kommt von Zielrouter --> Einpflegen in Routes + Senden
    // RouteError: kommt von Router auf Pfad --> Neusenden einer Nachricht -->
    // RoutingRoutine
    // --> Im Content steht die Id für die verlorene Message

    DatagramPacket[] toSend = new DatagramPacket[0];

    if(msg.getCommand() != Command.RouteRequest) {
      System.out.println(this.port + ": " + msg.getCommand());
    }
    try {
      switch (msg.getCommand()) {
        case Send:
          toSend = this.processSend(msg);
          break;
        case RouteRequest:
          if (msg.getDestPort() == this.myDevicePort && !knownIds.containsKey(msg.getMessageId())) {
            knownIds.put(msg.getMessageId(), Instant.now().getEpochSecond());
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
          toSend = new DatagramPacket[1];
          if (msg.getDestPort() == this.port) {
            toSend[0] = this.processRouteReply(msg);
          } else {
            toSend[0] = this.createDatagramPacket(msg, this.getPreviousPort(msg));
          }
          break;
        case Forward:
          if (msg.getDestPort() == this.myDevicePort) {
            //weitere Code, falls Enddevice die Nachricht erreichen soll
            toSend = new DatagramPacket[1];
            Message ack = new Message(msg.getMessageId(), Command.Ack, this.port, this.getPreviousPort(msg), msg.getPath(), "");
            toSend[0] = this.createDatagramPacket(ack, ack.getDestPort());
            System.out.println("Router " + this.port + ": " + msg.getContent());
          } else {
            toSend = this.processForward(msg);
          }
          break;
        case RouteError:
          if (msg.getDestPort() == this.port) {
            this.processRouteError(msg);
          } else {
            this.forwardMessage(msg);
          }
          break;
        case Ack:
          waitingForAck.remove(msg.getMessageId());
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

  public DatagramPacket[] processSend(Message msg) throws UnknownHostException {
    DatagramPacket[] packet;
    if (paths.containsKey(msg.getDestPort())) {
      packet = new DatagramPacket[1];
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
      
      msg.setCommand(Command.Forward);
      waiting.put(newMessage.getMessageId(), msg);
      knownIds.put(newMessage.getMessageId(), Instant.now().getEpochSecond());
      packet = getMulticastPackets(newMessage);
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
    while (it.hasNext()) {
      if (it.next() == this.port) {
        nextPort = it.next();
        break;
      }
    }

    return nextPort;
  }

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

  public DatagramPacket[] processRouteRequest(Message msg) throws UnknownHostException {
    DatagramPacket[] packet = new DatagramPacket[0];
    if (!knownIds.containsKey(msg.getMessageId())) {
      knownIds.put(msg.getMessageId(), Instant.now().getEpochSecond());
      msg.addToPath(this.port);
      packet = getMulticastPackets(msg);
    }
    return packet;
  }

  public DatagramPacket[] getMulticastPackets(Message msg) throws UnknownHostException {
    DatagramPacket[] packet;
    LinkedList<Router> reachable = this.field.getReachableRouter(this.port, this.xCoord, this.yCoord);
    Iterator<Router> it = reachable.iterator();
    packet = new DatagramPacket[reachable.size()];
    int i = 0;
    while (it.hasNext()) {
      packet[i] = createDatagramPacket(msg, it.next().getPort());
      i++;
    }
    return packet;
  }

  public DatagramPacket processRouteReply(Message msg) throws UnknownHostException {
    for (Entry<Integer, RoutingEntry> e : this.paths.entrySet()) {
      long currTime = Instant.now().getEpochSecond();
      if (currTime - e.getValue().getLastUsed() >= 300) {
        paths.remove(e.getKey());
      }
    }
    this.paths.put(msg.getDestPort(), new RoutingEntry(msg.getPath()));
    System.out.println(this.port + ": the routing path is " + msg.getPath());
    Message toSend = waiting.get(msg.getMessageId());
    toSend.setPath(msg.getPath());
    return this.createDatagramPacket(toSend, getNextPort(toSend));
  }

  public DatagramPacket[] processForward(Message msg) throws UnknownHostException {
    DatagramPacket[] toSend = new DatagramPacket[1];
    int nextRouter = this.getNextPort(msg);
    if( field.isRouterInRange(nextRouter, this.xCoord, this.yCoord) ) {
      if( field.isRouterInRange(this.getPreviousPort(msg), this.xCoord, this.yCoord)) {
        toSend = new DatagramPacket[2];
        Message ack = new Message(msg.getMessageId(), Command.Ack, this.port, this.getPreviousPort(msg), msg.getPath(), "");
        toSend[1] = this.createDatagramPacket(ack, ack.getDestPort());
      } 
      toSend[0] = this.createDatagramPacket(msg, nextRouter);
      waitingForAck.put(msg.getMessageId(), Instant.now().getEpochSecond());
    } else {
      msg.setCommand(Command.RouteError);
      msg.setDestPort(msg.getSourcePort());
      msg.setSourcePort(this.port);
      
      toSend[0] = createDatagramPacket(msg, this.getPreviousPort(msg));
    }
    return toSend;
  }

  public void receiveMessage(Message msg) {

  }

  public void processRouteError(Message msg) {

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
