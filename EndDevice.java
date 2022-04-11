import java.net.*;
import java.util.HashMap;
import java.util.LinkedList;

import java.io.*;

public class EndDevice extends Device {
  private static String EXIT_STRING = "EXIT";
  private int myRouterPort;
  private HashMap<String, Message> sendMessages;

  public EndDevice(int xCoord, int yCoord, Field field, int port, int myRouterPort) {
    super(xCoord, yCoord, field, port);
    this.myRouterPort = myRouterPort;
    this.sendMessages = new HashMap<>();
  }

  public void run() {
    // while (true) {
    //   try {
    //     sleep(5000);
    //     this.sendMessage(this.port + 5, "Hallo");
    //   } catch (InterruptedException e) {
    //     e.printStackTrace();
    //   }
    // }
  }

  public void readAndSend(int dest) {
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
      sendMessage(dest, msg);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void sendMessage(int dest, String msg) {
    try (DatagramSocket socket = new DatagramSocket()) {
      InetAddress serverAddress = InetAddress.getByName("localhost");
      Message message = new Message(getUUID(), Command.Send, this.port, dest, new LinkedList<Integer>(), msg);
      sendMessages.put(message.getMessageId(), message);
      byte[] bytes = message.toString().getBytes();

      DatagramPacket packet = new DatagramPacket(bytes, bytes.length, serverAddress, myRouterPort);
      socket.send(packet);
      socket.close();
      this.receiveMessage();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void receiveMessage() {
    try (DatagramSocket socket = new DatagramSocket(this.port)) {
      DatagramPacket dp = new DatagramPacket(new byte[65507], 65507);
      try {
        socket.setSoTimeout(10000);
        socket.receive(dp);
        System.out.println("Message Received");
        Message msg = new Message(new String(dp.getData(), 0, dp.getLength()));
        if(msg.getCommand() == Command.Retry) {
          Message oldMsg = sendMessages.get(msg.getContent());
          socket.close();
          sendMessage(oldMsg.getDestPort(), oldMsg.getContent());
        } else {
          // wenn eine nachricht als Antwort erwartet wird
        }
      } catch (SocketTimeoutException e) {
        System.out.println(this.port + ": ran in timeout");
      }
    } catch ( Exception e) {
      e.printStackTrace();
    }
  }

  public int getMyRouterPort() {
    return this.myRouterPort;
  }
}
