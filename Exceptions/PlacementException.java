package Exceptions;
public class PlacementException extends Exception{
  public PlacementException() {
    super("Object cannot be placed here");
  }
  
  public PlacementException(String message) {
    super(message);
  }
}
