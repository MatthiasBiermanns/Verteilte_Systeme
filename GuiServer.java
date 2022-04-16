import java.io.IOException;
import java.net.*;

public class GuiServer implements Runnable {

  private GuiUpdateMessage x;

  /**
   * Creates a socket and listens on port 4998 for Messages.
   * Translates received Message to a GuiUpdateMessage and sets x.
   */
  public void runServer() {
    DatagramSocket socket;
    byte[] buf = new byte[256];

    try {
      socket = new DatagramSocket(4998);
      while (true) {
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        System.out.println("Receiving now:");
        socket.receive(packet);

        String received = new String(packet.getData(), 0, packet.getLength());
        x = new GuiUpdateMessage(received);
        System.out.println(packet.getPort() + ": " + received);
      }
    } catch (SocketException e1) {
      e1.printStackTrace();
    } catch (IOException e2) {
      e2.printStackTrace();
    }
  }

  public static void main(String[] args) {
    GuiServer gs = new GuiServer();
    gs.runServer();
  }

  /**
   * Get class param x
   * @return GuiUpdateMessage x
   */
  public GuiUpdateMessage getX() {
    return x;
  }

  @Override
  public void run() {
    runServer();
  }
}
