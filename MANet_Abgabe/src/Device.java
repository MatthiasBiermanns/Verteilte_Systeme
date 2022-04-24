package MANet_Abgabe.src;
import java.util.UUID;

public abstract class Device extends Thread {
  protected int xCoord, yCoord, port;
  protected Field field;

  /**
   * Construktor kann nicht ausgeführt werden (Abstract Class). Enthält und legt 
   * die Attribute fest, die sowohl das EndDevice als auch der Router haben.
   * 
   * @param xCoord X-Koordinate der Device-Instanz
   * @param yCoord Y-Koordinate der Device-Instanz
   * @param field Feld in dem sich die Device-Instanz befindet
   * @param port Port der Device-Instanz
   */
  public Device(int xCoord, int yCoord, Field field, int port) {
    this.xCoord = xCoord;
    this.yCoord = yCoord;
    this.field = field;
    this.port = port;
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

  public int getPort() {
    return this.port;
  }
  
  public void setXCoord(int xCoord) {
    this.xCoord = xCoord;
  }

  public void setYCoord(int yCoord) {
    this.yCoord = yCoord;
  }

  protected static String getUUID() {
    return UUID.randomUUID().toString();
  }

}
