public abstract class Device extends Thread {
  protected int xCoord, yCoord;
  protected Field field;
  protected String deviceId;

  public Device(String deviceId, int xCoord, int yCoord, Field field) {
    this.deviceId = deviceId;
    this.xCoord = xCoord;
    this.yCoord = yCoord;
    this.field = field;
  }

  public Device() {

  }

  public abstract void run();
  
  public int getXCoord() {
    return this.xCoord;
  }

  public int getYCoord() {
    return this.yCoord;
  }

  public Field getField() {
    return this.field;
  }

  public String getDeviceId() {
    return this.deviceId;
  }
  
  public void setXCoord(int xCoord) {
    this.xCoord = xCoord;
  }

  public void setYCoord(int yCoord) {
    this.yCoord = yCoord;
  }

}
