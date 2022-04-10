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
    } else if (str.equals("RETRY")) {
      return Command.Retry;
    } else {
      return Command.Unknown;
    }
  }

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

  public void addToPath(int port) {
    this.path.add(port);
  }
}
