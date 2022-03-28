import java.time.Instant;
import java.util.HashMap;
import java.util.Map.Entry;

import Exceptions.InvalidMessage;

import java.net.*;
//import java.io.*;

public class Router extends Device {
  private HashMap<Integer, RoutingPath> paths;
  private byte[] buffer;
  private int myDevicePort;

  public Router(int xCoord, int yCoord, int port, Field field, int myDevicePort) {
    super(xCoord, yCoord, field, port);
    this.myDevicePort = myDevicePort;
    this.paths = new HashMap<>();
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
            evaluateMessage(message);
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

  public void evaluateMessage(Message msg) {
    // Prüfen, ob MessageId schonmal angekommen
    // Prüfen, ob für mich

    switch (msg.getCommand()) {
      case Forward:
        if(msg.getDestPort() == this.port) {
          this.receiveMessage(msg);
        } else {
          this.processForward(msg);
        }
        break;
      case Send:
        this.processSend(msg);
        break;
      case RouteRequest:
        if(msg.getDestPort() == this.port) {
          this.sendRouteReply(msg);
        } else {
          this.processRouteRequest(msg);
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
  }

  public void processSend(Message msg) {
    // überprüfen ob aktueller path verfügbar
    // Wenn ja--> Senden darüber
    // Wenn nein--> Route Request absenden
    // --> Message zurückstellen und auf RouteReply warten
    if(paths.containsKey(msg.getDestPort())) {
      msg.setCommand(Command.Forward);
      msg.setPath(paths.get(msg.getDestPort()).getPath());
      msg.setSourcePort(port);

    } else {

    }
  }

  public void processForward(Message msg) {
    // Prüfen, ob der in path festgelegte Router zur Verfügung steht
    // Wenn ja Nachricht dahin senden
    // --> Warten auf acknowledgement
    // --> Nicht erfolgreich => RouteError Schicken

  }


  public void processRouteRequest(Message msg) {
    // Prüfen, ob schon erhalten
    // Prüfen, ob er Ziel ist
    // Wenn Ja
    // --> Sich anhängen und als RouteReply zurückschicken
    // Wenn Nein
    // --> SIch anhängen und Multicasten an alle anderen Router
  }

  public void processRouteReply(Message msg) {
    // Prüfen, ob 
    // Prüfen ob schon andere Reply angekommen ist
    // Wenn Ja --> Ignorieren
  }

  public void processRouteError(Message msg) {

  }

  public void receiveMessage(Message msg) {

  }

  public void sendMessage(Message msg) {

  }

  public void sendRouteRequest() {

  }

  public void forwardMessage(Message msg) {

  }

  public void sendRouteReply(Message msg) {

  }

  public void sendRouteError() {

  }

  public void updateRoutePaths() {
    for (Entry<Integer, RoutingPath> path : this.paths.entrySet()) {
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
