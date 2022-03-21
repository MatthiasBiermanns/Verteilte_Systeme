import java.time.Instant;
import java.util.HashMap;
import java.util.Map.Entry;
import java.net.*;
import java.io.*;

public class Router extends Thread {
  private int xCoord, yCoord, port;
  private String id;
  private HashMap<String, RoutingPath> paths;
  private byte[] buffer;

  public Router(String id, int xCoord, int yCoord, int port) {
    this.xCoord = xCoord;
    this.yCoord = yCoord;
    this.port = port;
    this.id = id;
    this.paths = new HashMap<>();
    this.buffer = new byte[65507];
  }

  public void run() {
    try (DatagramSocket socket = new DatagramSocket(port)) {
      while (true) {
        DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
        try {
          socket.receive(dp);
          String packet = new String(dp.getData(), 0, dp.getLength());

        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
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

  public String getRouterId() {
    return this.id;
  }

  public int getXCoord() {
    return this.xCoord;
  }

  public int getYCoord() {
    return this.yCoord;
  }

  public void setXCoord(int xCoord) {
    this.xCoord = xCoord;
  }

  public void setYCoord(int yCoord) {
    this.yCoord = yCoord;
  }
}
