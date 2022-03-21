package Exceptions;

public class InvalidMessage extends Exception {
  public InvalidMessage() {
    super("Message does not match the needed format");
  }
  
  public InvalidMessage(String message) {
    super(message);
  }
}
