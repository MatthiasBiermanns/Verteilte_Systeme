import java.util.LinkedList;

import Exceptions.InvalidMessage;

public class Message {
  //Aufbau des Nachrichten-Strings:
  //Befehl(Send,forword,RouteReply_RouteError,RouteRequest)/3000/2658(bei Send: 2700,2800)/3000-->2000-->2001-->2658/lorem ipsum dolor sit amen

  private Command command;
  private int sourcePort;
  private int destPort;
  private LinkedList<String> path;
  private String content;

  public Message(String encodedPacket) throws InvalidMessage{
    String[] parts = encodedPacket.split("/", 5);
    if(parts.length != 5) {
      throw new InvalidMessage();
    }
    this.command = evaluateCommand(parts[0]);
    this.sourcePort = Integer.parseInt(parts[1]);
    this.destPort = Integer.parseInt(parts[2]);
    this.path = parseList(parts[3]);
    this.content = parts[4];
  }

  public Command evaluateCommand(String str) {
    if(str.equals("SEND")) {
      return Command.Send;
    } else if (str.equals("FORWARD")) {
      return Command.Forward;
    } else if (str.equals("ROUTE_REQUEST")) {
      return Command.RouteRequest;
    } else if (str.equals("ROUTE_REPLY")) {
      return Command.RouteReply;
    } else if (str.equals("ROUTE_ERROR")) {
      return Command.RouteError;
    } else {
      return Command.Unknown;
    }
  }

  public LinkedList<String> parseList(String str) {
    LinkedList<String> list = new LinkedList<>();
    str = str.substring(1, str.length()-1);
    String[] parts = str.split(", ");
    for(int i = 0; i < parts.length; i++) {
      list.add(parts[i]);
    }
    return list;
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

  public LinkedList<String> getPath() {
    return this.path;
  }

  public String getContent() {
    return this.content;
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

  public void setPath(LinkedList<String> path) {
    this.path = path;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public void addToPath(String knot) {
    this.path.add(knot);
  }
}
