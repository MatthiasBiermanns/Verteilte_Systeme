import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;

import Exceptions.InvalidInputException;
import Exceptions.PlacementException;
import Exceptions.DeviceNotFound;

class Field {
  final static int ID_LENGTH = 32;
  static int nextPort = 3000;
  private int xLength, yLength;
  private Semaphore fieldSem = new Semaphore(1, true);
  private Device[][][] field;
  private HashMap<Integer, Device> map = new HashMap<Integer, Device>();

  public static void main(String[] args) {
    try {
      Field myField = new Field(20, 10, 10);
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
        while (true) {
          myField.printField();

          String line = reader.readLine();
          String[] parts = line.split(" ");

          myField.moveDevice(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
              Integer.parseInt(parts[3]));
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public Field(int routerCnt, int xLength, int yLength) throws InvalidInputException {
    if (xLength <= 0 || yLength <= 0 ) { 
      throw new InvalidInputException();
    }

    this.xLength = xLength;
    this.yLength = yLength;
    field = new Device[xLength][yLength][2];
    for (int i = 0; i < routerCnt; i++) {
      try {
        createNewDevice();
      } catch (PlacementException e) {
        e.printStackTrace();
        break;
      }
    }
  }

  public void createNewDevice() throws PlacementException {
    try {
      fieldSem.acquire();

      int xCoord, yCoord;
      int cnt = 0;
      do {
        xCoord = (int) (Math.random() * xLength);
        yCoord = (int) (Math.random() * yLength);
        cnt++;
      } while (field[xCoord][yCoord][0] != null && cnt < 500);

      if (cnt >= 500) {
        throw new PlacementException();
      }

      Router r = new Router(xCoord, yCoord, nextPort, this, nextPort + 1);
      EndDevice d = new EndDevice(xCoord, yCoord, this, nextPort + 1, nextPort);
      nextPort++;
      nextPort++;

      field[xCoord][yCoord][0] = r;
      field[xCoord][yCoord][1] = d;
      map.put(r.getPort(), r);
      map.put(d.getPort(), d);

      fieldSem.release();
    } catch (InterruptedException e) {

    }

  }

  public void createNewDevice(int xCoord, int yCoord) throws PlacementException {
    // creates Router at specific position
    try {
      fieldSem.acquire();

      if (xCoord > this.xLength || yCoord > this.yLength || field[xCoord][yCoord][0] == null) {
        throw new PlacementException();
      }

      Router r = new Router(xCoord, yCoord, nextPort, this, nextPort + 1);
      EndDevice d = new EndDevice(xCoord, yCoord, this, nextPort + 1, nextPort);
      nextPort++;
      nextPort++;

      field[xCoord][yCoord][0] = r;
      field[xCoord][yCoord][1] = d;
      map.put(r.getPort(), r);
      map.put(d.getPort(), d);

      fieldSem.release();
    } catch (InterruptedException e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  public void moveDevice(int oldX, int oldY) throws DeviceNotFound, PlacementException {
    try {
      fieldSem.acquire();

      if (field[oldX][oldY][0] == null) {
        throw new DeviceNotFound();
      }

      int newX, newY;
      int cnt = 0;
      do {
        newX = (int) (Math.random() * xLength);
        newY = (int) (Math.random() * yLength);
        cnt++;
      } while (field[newX][newY][0] != null && cnt < 500);

      if (cnt >= 500) {
        throw new PlacementException();
      }

      Router r = (Router) field[oldX][oldY][0];
      EndDevice d = (EndDevice) field[oldX][oldY][1];
      r.setXCoord(newX);
      r.setYCoord(newY);
      d.setXCoord(newX);
      d.setYCoord(newY);
      field[newX][newY][0] = r;
      field[newX][newY][1] = d;
      field[oldX][oldY][0] = null;
      field[oldX][oldY][1] = null;

      fieldSem.release();
    } catch (InterruptedException e) {

    }
  }

  public void moveDevice(int oldX, int oldY, int newX, int newY) throws DeviceNotFound, PlacementException {
    // moves specific router to specific position
    try {
      fieldSem.acquire();

      if (field[oldX][oldY][0] == null) {
        throw new DeviceNotFound();
      }

      if (newX > this.xLength || newY > this.yLength || field[newX][newY][0] != null) {
        throw new PlacementException();
      }

      Router r = (Router) field[oldX][oldY][0];
      EndDevice d = (EndDevice) field[oldX][oldY][1];
      r.setXCoord(newX);
      r.setYCoord(newY);
      d.setXCoord(newX);
      d.setYCoord(newY);
      field[newX][newY][0] = r;
      field[newX][newY][1] = d;
      field[oldX][oldY][0] = null;
      field[oldX][oldY][1] = null;

      fieldSem.release();
    } catch (InterruptedException e) {

    }
  }

  public LinkedList<Router> getReachableRouter(int x, int y) {
    LinkedList<Router> reachableRouter = new LinkedList<Router>();

    for (Entry<Integer, Device> entry : this.map.entrySet()) {
      Device d = entry.getValue();
      if (d instanceof Router) {
        Router r = (Router) d;
        if (getDistance(x, y, r.getXCoord(), r.getYCoord()) <= 10) {
          reachableRouter.add(r);
        }
      }
    }
    return reachableRouter;
  }

  public static double getDistance(int x1, int y1, int x2, int y2) {
    int xDist = (x1 - x2) >= 0 ? x1 - x2 : x2 - x1;
    int yDist = (y1 - y2) >= 0 ? y1 - y2 : y2 - y1;

    if (xDist == 0) {
      return yDist;
    }
    if (yDist == 0) {
      return xDist;
    }
    return Math.sqrt(xDist + yDist);
  }

  public void deleteDevice(int x, int y) throws DeviceNotFound {
    try {
      fieldSem.acquire();

      if (field[x][y] == null) {
        throw new DeviceNotFound();
      }

      Device[] device = field[x][y];
      map.remove(device[0].getPort());
      map.remove(device[1].getPort());
      field[x][y][0] = null;
      field[x][y][1] = null;

      fieldSem.release();
    } catch (InterruptedException e) {

    }
  }

  public void printField() {
    for (int i = 0; i < field.length; i++) {
      for (int j = 0; j < field[i].length; j++) {
        if (field[i][j][0] != null) {
          System.out.print(field[i][j][0].getPort() + " - ");
        } else {
          System.out.print("___ - ");
        }
      }
      System.out.println("\n");
    }
  }

  public HashMap<Integer, Device> getMap() {
    return this.map;
  }

  public Device[][][] getField(){
    return this.field;
  }
}