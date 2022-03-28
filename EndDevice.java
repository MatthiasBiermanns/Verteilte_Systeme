import java.net.*;
import java.util.LinkedList;
import java.util.UUID;

import java.io.*;

public class EndDevice extends Device {
  private static String EXIT_STRING = "EXIT";
  private int myRouterPort;

  public EndDevice(int xCoord, int yCoord, Field field, int port, int myRouterPort) {
    super(xCoord, yCoord, field, port);
    this.myRouterPort = myRouterPort;
  }

  public void run() {
    while (true) {
      try {
        sleep(5000);
        this.sendMessage(this.port + 5, "Hallo", false);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private static String getUUID() {
    return UUID.randomUUID().toString();
  }

  public void readAndSend(int dest, boolean withResponse) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
      System.out.println("End your Message with " + EXIT_STRING + "\n");
      String msg = "";
      while (true) {
        try {
          String line = reader.readLine();
          if (line.equals(EXIT_STRING)) {
            break;
          }
          msg = msg + "\n" + line;
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      sendMessage(dest, msg, withResponse);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void sendMessage(int dest, String msg, boolean withResponse) {
    try (DatagramSocket socket = new DatagramSocket()) {
      InetAddress serverAddress = InetAddress.getByName("localhost");
      Message message = new Message(getUUID(), Command.Send, this.port, dest, new LinkedList<String>(), msg);
      byte[] bytes = message.toString().getBytes();

      DatagramPacket packet = new DatagramPacket(bytes, bytes.length, serverAddress, myRouterPort);
      socket.send(packet);
      if (withResponse) {
        this.receiveMessage();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void receiveMessage() {

  }

  public int getMyRouterPort() {
    return this.myRouterPort;
  }
}
