import java.io.*;
import java.net.*;

/**
 * For testing purposes only.
 * Creates a client with which a status message to the GuiServer can be simulated by a console input.
 */
public class GuiClient {

  public static int DEFAULT_PORT = 4998;
  public static String address = "localhost";

  public static void main(String[] args) {
    DatagramSocket socket;
    InetAddress dest;
    byte[] data;
    BufferedReader userIn;

    try {
      socket = new DatagramSocket();
      dest = InetAddress.getByName(address);

      while (true) {
        userIn = new BufferedReader(new InputStreamReader(System.in));
        data = userIn.readLine().getBytes();
        DatagramPacket packet = new DatagramPacket(
          data,
          data.length,
          dest,
          DEFAULT_PORT
        );
        socket.send(packet);
      }
    } catch (SocketException e) {
      System.err.println(e);
    } catch (UnknownHostException e) {
      System.err.println(e);
    } catch (IOException e) {}
  }
}
