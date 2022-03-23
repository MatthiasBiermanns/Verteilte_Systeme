import java.net.*;

import Exceptions.DeviceNotFound;
import java.io.*;

public class EndDevice extends Device{
  private static String EXIT_STRING = "EXIT"; 

  public EndDevice(String deviceId, int xCoord, int yCoord, Field field) {
    super(deviceId, xCoord, yCoord, field);
  }

  public void run(){

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
      } catch( DeviceNotFound e) {
        System.out.println();
        e.printStackTrace();
      }

    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  public void receiveMessage() {

  }
}
