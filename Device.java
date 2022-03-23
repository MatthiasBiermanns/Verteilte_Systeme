import java.net.*;

import Exceptions.RouterNotFound;
import java.io.*;

public class Device {
  private int xCoord, yCoord;
  private String deviceId;
  private static String EXIT_STRING = "EXIT"; 
  public Field field;
  public Device(int xCoord, int yCoord, String deviceId, Field field) {
    this.xCoord = xCoord;
    this.yCoord = yCoord;
    this.deviceId = deviceId;
    this.field = field;
  }

  public void readAndSend(String dest, boolean withResponse) {
    try (BufferedReader reader = new BufferedReader( new InputStreamReader(System.in))) {
      System.out.println("End your Message with " + EXIT_STRING + "\n");
      String msg = "";
      while(true) {
        try{
          String line = reader.readLine();
          if(line.equals(EXIT_STRING)) {
            break; 
          }
          msg = msg + "\n" + line;
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      sendMessage(dest, msg, withResponse);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  public void sendMessage(String dest, String msg, boolean withResponse) {
    try(DatagramSocket socket = new DatagramSocket()) {
      InetAddress serverAddress = InetAddress.getByName("localhost");
      byte[] message = msg.getBytes();

      try {
        Router router = this.field.getClosestReachableRouter(this.xCoord, this.yCoord, 10);
        DatagramPacket packet = new DatagramPacket(message, message.length, serverAddress, router.getPort());
        socket.send(packet);
      } catch( RouterNotFound e) {
        System.out.println();
        e.printStackTrace();
      }

    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  public void receiveMessage() {

  }

  public int getXCoord() {
    return this.xCoord;
  }

  public int getYCoord() {
    return this.yCoord;
  }

  public String getDeviceId() {
    return this.deviceId;
  }

  public void setXCoord(int xCoord) {
    this.xCoord = xCoord;
  }

  public void setYCoord(int yCoord) {
    this.yCoord = yCoord;
  }
}
