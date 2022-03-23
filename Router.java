import java.time.Instant;
import java.util.HashMap;
import java.util.Map.Entry;

import Exceptions.InvalidMessage;

import java.net.*;
//import java.io.*;

public class Router extends Device {
  private int port;
  private HashMap<String, RoutingPath> paths;
  private byte[] buffer;

  public Router(String deviceId, int xCoord, int yCoord, int port, Field field) {
    super(deviceId, xCoord, yCoord, field);
    this.port = port;
    this.paths = new HashMap<>();
    this.buffer = new byte[65507];
  }

  public Router() {
    super();
  }

  public void run() {
    try (DatagramSocket socket = new DatagramSocket(port)) {
      while (true) {
        DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
        try {
          socket.receive(dp);
          try {
            Message message = new Message( new String(dp.getData(), 0, dp.getLength()));
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

  }

  public void send() {

  }

  public void sendRouteRequest() {

  }

  public void sendRouteReply() {

  }

  public void sendRouteError() {

  }

  public void updateRoutePaths() {
    for (Entry<String, RoutingPath> path : this.paths.entrySet()) {
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

  public int getPort() {
    return this.port;
  }
  
  public void setXCoord(int xCoord) {
    this.xCoord = xCoord;
  }

  public void setYCoord(int yCoord) {
    this.yCoord = yCoord;
  }

}
