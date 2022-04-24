import java.net.*;
import java.util.HashMap;
import java.util.LinkedList;

public class EndDevice extends Device {
  private int myRouterPort;
  private HashMap<String, Message> sendMessages;

  /**
   * Erstellt ein neues Device.
   * 
   * @param xCoord x-Koordinate, an der das EndDevice sich befindet
   * @param yCoord y-Koordinate, an der das EndDevice sich befindet
   * @param field Feld, in dem sich das EndDevice befindet
   * @param port Port, über den das EndDevice erreicht werden kann (UDP) 
   * @param myRouterPort Port des zugehörigen RouterObjektes
   */
  public EndDevice(int xCoord, int yCoord, Field field, int port, int myRouterPort) {
    super(xCoord, yCoord, field, port);
    this.myRouterPort = myRouterPort;
    this.sendMessages = new HashMap<>();
  }

  /**
   * Wird ausgeführt, wenn der Thread startet.
   */
  public void run() {
    this.receiveMessage();
  }

  /**
   * Versendet eine Nachricht zu einem beliebigen anderen EndDevice übder das UDP-Protokoll. 
   * Erstellt wird dafür eine neue Message mit dem Command Send. Diese wird anschließend über 
   * das UDP-Protokoll zu den zugehörigen Router mit dem Port myRouterPort gesendet. Speichert
   * das neu erstellte Message-Objekt in der Map sendMessages, damit diese für eine potenzielle
   * Retry-Message zur verfügung steht und neu gesendet werden kann.
   * 
   * @param dest Port des Zielgerätes
   * @param msg Nachricht, die versendet werden soll
   */
  public void sendMessage(int dest, String msg) {
    try (DatagramSocket socket = new DatagramSocket()) {
      InetAddress serverAddress = InetAddress.getByName("localhost");
      Message message = new Message(getUUID(), Command.Send, this.port, dest, new LinkedList<Integer>(), msg);
      sendMessages.put(message.getMessageId(), message);
      byte[] bytes = message.toString().getBytes();

      DatagramPacket packet = new DatagramPacket(bytes, bytes.length, serverAddress, myRouterPort);
      socket.send(packet);

      // Schließen des Sockets, damit in receiveMessager() ein neues geöffnet werden kann
      socket.close();

      // Unterscheidet, ob das EndDevice bereits in einem eigenen Thread läuft, oder noch den Main-Thread belegt
      // Main-Thread wird ggf. belegt, da aus ihm heraus sendMessage aufgerufen wird 
      if(Thread.currentThread().getName().equals("main")) {
        // Belegt Main-Thread --> Startet eigenen um dort auf eine ggf. eingehende Retry-Message zu hören
        this.start();
      } else {
        // Belegt bereits einen eigenen Thread --> kann in diesem Thread warten
        this.receiveMessage();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Methode erstellt ein UDP-Socket und hört hier 10 Sekunden auf eine einkommende Nachricht.
   * Geht eine Nachricht ein, wird diese auf den Command überprüft (Weitere Implementierung für 
   * verschiedene andere Commands könnten hier hinzugefügt werden). 
   * 
   * Geht eine Message mit dem Command Retry ein, so wird die zugehörige alte Message aus dem 
   * "Speicher" sendMessages ausgelesen und sendMessage() wird mit den gleichen Attributen
   * wie die alte Message erneut aufgerufen.
   * 
   * Kommt keien Message in den 10 Sekunden herein, ist davon auszugehen, dass die Nachricht
   * erfolgreich beim Zieldevice angekommen ist und der Thread wird terminiert.
   */
  private void receiveMessage() {
    try (DatagramSocket socket = new DatagramSocket(this.port)) {
      DatagramPacket dp = new DatagramPacket(new byte[65507], 65507);
      try {
        socket.setSoTimeout(10000);
        socket.receive(dp);
        System.out.println("Message Received");
        Message msg = new Message(new String(dp.getData(), 0, dp.getLength()));
        if(msg.getCommand() == Command.Retry) {
          Message oldMsg = sendMessages.get(msg.getContent());

          // Schließen des Sockets, damit ein neues in sendMessage erzeugt werden kann
          socket.close();
          sendMessage(oldMsg.getDestPort(), oldMsg.getContent());
        } else {
          // wenn eine nachricht als Antwort erwartet wird
        }
      } catch (SocketTimeoutException e) {
        System.out.println(this.port + ": No retry needed");
        this.stop();
      }
    } catch ( Exception e) {
      e.printStackTrace();
    }
  }

  public int getMyRouterPort() {
    return this.myRouterPort;
  }
}
