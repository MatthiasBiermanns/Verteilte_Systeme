package Exceptions;
public class RouterNotFound extends Exception {
  public RouterNotFound() {
    super("Router does not exist");
  }
  
  public RouterNotFound(String message) {
    super(message);
  }
}