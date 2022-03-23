package Exceptions;
public class DeviceNotFound extends Exception {
  public DeviceNotFound() {
    super("Router does not exist");
  }
  
  public DeviceNotFound(String message) {
    super(message);
  }
}