package Exceptions;

public class InvalidInputException extends Exception {
  public InvalidInputException() {
    super("Input is not valid");
  }

  public InvalidInputException(String message) {
    super(message);
  }
}
