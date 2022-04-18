import java.util.LinkedList;

import Exceptions.InvalidMessage;

public class Message {
  // Aufbau des Nachrichten-Strings:
  // MessageId/Befehl(Send,Forward,RouteReply,RouteError,RouteRequest)/3000/2658/3000-->2000-->2001-->2658/lorem
  // ipsum dolor sit amen

  private Command command;
  private int sourcePort;
  private int destPort;
  private LinkedList<Integer> path;
  private String messageId, content;

  /**
     * Erzeugzt ein Message-Objekt aus einem String folgenden Formates:
     * messageId/command/sourcePort/destPort/path/content
     * 
     * path kann durch die toString()-Methode einer Liste erzeugt werden und hat das Format:
     * [port1, port2, port3, ...]
     * 
     * @param      encodedPackat   String mit dem o. g. Format; kann durch Message.toString erzeugt werden
     * @throws     InvalidMessage - Der String enthält nicht das vorgegebene Format für die erzeugung einer Nachricht
     * @return     Eine dem String entsprechende Message
     */
  public Message(String encodedPacket) throws InvalidMessage {
    String[] parts = encodedPacket.split("/", 6);
    if (parts.length != 6) {
      throw new InvalidMessage();
    }
    this.messageId = parts[0];
    this.command = evaluateCommand(parts[1]);
    this.sourcePort = Integer.parseInt(parts[2]);
    this.destPort = Integer.parseInt(parts[3]);
    this.path = parseList(parts[4]);
    this.content = parts[5];
  }

  /**
     * Erzeugt ein Message-Objekt. 
     * 
     * @param      messageId Eindeutiger String (meist UUID)
     * @param      command Instanz der Enumeration Command
     * @param      sourcePort Port des Ausgangsrouters
     * @param      destPort Zielport der Nachricht
     * @param      path Liste aller Routerports, die das Packt übergehen muss
     * @param      content Enthält die eigentliche Nachricht die übertragen werden soll
     * @return     Eine den Eingabeparametern entsprechendes Message-Objekt
     */
  public Message(String messageId, Command command, int sourcePort, int destPort, LinkedList<Integer> path, String content) {
    this.messageId = messageId;
    this.command = command;
    this.sourcePort = sourcePort;
    this.destPort = destPort;
    this.path = path;
    this.content = content;
  }


  @Override
  public String toString() {
    return this.messageId + "/" + this.command + "/" + sourcePort + "/" + destPort + "/" + path.toString() + "/" + this.content;
  }

  public String toBeautyString() {
    return "\nID: \t\t\t\t" + this.messageId + "\nCommand:\t\t\t" + this.command 
         + "\nSource Port:\t\t\t" + sourcePort + "\nDestination Port: \t\t" 
         + destPort + "\nPath:\t\t\t\t" + path.toString() + "\nContent:\t\t\t" + this.content + "\n";
  }

  /**
     * Evaluiert aus einem String, welcher Enumeration-Instanz dieser entspricht und gibt diese zurück.
     * Sollte der String nicht zugeordnet werden können, wird Command.Unkown zurückgegeben.
     * 
     * @param      str String entsprechend einer der Enumeration Instanzen
     * @return     die entsprechende Enumeration Instanz der Enum Command
     */
  public Command evaluateCommand(String str) {
    str = str.toUpperCase();
    if (str.equals("SEND")) {
      return Command.Send;
    } else if (str.equals("FORWARD")) {
      return Command.Forward;
    } else if (str.equals("ROUTEREQUEST")) {
      return Command.RouteRequest;
    } else if (str.equals("ROUTEREPLY")) {
      return Command.RouteReply;
    } else if (str.equals("ROUTEERROR")) {
      return Command.RouteError;
    } else if (str.equals("ACK")) {
      return Command.Ack;
    } else if (str.equals("ACKNEEDED")) {
      return Command.AckNeeded;
    } else if (str.equals("RETRY")) {
      return Command.Retry;
    } else {
      return Command.Unknown;
    }
  }

  /**
     * Erzeugt eine LinkedList aus den Zahlen des gegebenen Strings. Der String muss dafür 
     * das folgende Format haben, welches aus erreicht wird, wenn eine LinkedList mit der 
     * toString()-Methode in einen String umformatiert wird. Diese Methode funktioniert für
     * LinkedLists mit Integer-Elementen
     * 
     * Format:
     * [entry1, entry2, entry3] 
     * 
     * @param      str einer LinkedList mit o. g. Format
     * @return     Eine LinkedList mit Integer-Elementen
     */
  public LinkedList<Integer> parseList(String str) {
    LinkedList<Integer> list = new LinkedList<>();
    str = str.substring(1, str.length() - 1);
    if(!str.equals("")) {
      String[] parts = str.split(", ");
      if(parts.length > 0) {
        for (int i = 0; i < parts.length; i++) {
          list.add(Integer.parseInt(parts[i]));
        }
      }
    }
    return list;
  }

  public String getMessageId() {
    return this.messageId;
  }

  public Command getCommand() {
    return this.command;
  }

  public int getSourcePort() {
    return this.sourcePort;
  }

  public int getDestPort() {
    return this.destPort;
  }

  public LinkedList<Integer> getPath() {
    return this.path;
  }

  public String getContent() {
    return this.content;
  }

  public void setMessageId(String id) {
    this.messageId = id;
  }

  public void setCommand(Command command) {
    this.command = command;
  }

  public void setSourcePort(int port) {
    this.sourcePort = port;
  }

  public void setDestPort(int port) {
    this.destPort = port;
  }

  public void setPath(LinkedList<Integer> path) {
    this.path = path;
  }

  public void setContent(String content) {
    this.content = content;
  }

  /**
     * Fügt an den Pfad der Message einen weiteren Port (Integer-Element) an.
     * 
     * @param      port hinzuzufügender Port
     */
  public void addToPath(int port) {
    this.path.add(port);
  }
}
