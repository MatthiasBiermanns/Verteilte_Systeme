import java.util.LinkedList;

public class GuiUpdateMessage {

  private int xCord;
  private int yCord;
  private Command command;
  private LinkedList<Integer> path;

  GuiUpdateMessage(
    int xCord,
    int yCord,
    Command command,
    LinkedList<Integer> path
  ) {
    this.xCord = xCord;
    this.yCord = yCord;
    this.command = command;
    this.path = path;
  }

  /**
   * Constructor creates an Instance from a given message.
   * @param message in Format: "(xCord) (yCord) (command) (path)"
   */
  GuiUpdateMessage(String message) {
    String[] parts = message.split(" ", 4);
    xCord = Integer.parseInt(parts[0]);
    yCord = Integer.parseInt(parts[1]);
    command = evaluateCommand(parts[2]);
    path = parseList(parts[3]);
  }

  /**
   * @param str
   * @return String converted to LinkedList<Integer>
   */
  public LinkedList<Integer> parseList(String str) {
    LinkedList<Integer> list = new LinkedList<>();
    str = str.substring(1, str.length() - 1);
    if (!str.equals("")) {
      String[] parts = str.split(", ");
      if (parts.length > 0) {
        for (int i = 0; i < parts.length; i++) {
          list.add(Integer.parseInt(parts[i]));
        }
      }
    }
    return list;
  }

  /**
   *
   * @param str
   * @return str converted to Command
   */
  private Command evaluateCommand(String str) {
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

  /**
   * @return int return the xCord
   */
  public int getXCord() {
    return xCord;
  }

  /**
   * @return int return the yCord
   */
  public int getYCord() {
    return yCord;
  }

  /**
   * @return Command return the command
   */
  public Command getCommand() {
    return command;
  }

  /**
   * @return LinkedList<Integer> return the path
   */
  public LinkedList<Integer> getPath() {
    return path;
  }

  @Override
  public String toString() {
    return (
      "" +
      xCord +
      " " +
      yCord +
      " " +
      command.toString() +
      " " +
      path.toString()
    );
  }
}
