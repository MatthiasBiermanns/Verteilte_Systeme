import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;

import Exceptions.InvalidInputException;
import Exceptions.PlacementException;
import Exceptions.RouterNotFound;

class Field {
  final static int ID_LENGTH = 16;
  private int xLength, yLength;
  private Semaphore fieldSem = new Semaphore(1, true);
  private Router[][] field;
  private HashMap<String, Router> routerMap = new HashMap<String, Router>();

  public Field(int routerCnt, int xLength, int yLength) throws InvalidInputException {
    if(xLength < 0 || yLength < 0 || (xLength == 0 && yLength == 0)) {
      throw new InvalidInputException();
    }

    this.xLength = xLength;
    this.yLength = yLength;
    field = new Router[xLength][yLength];
    for (int i = 0; i < routerCnt; i++) {
      try {
        createNewRouter();
      } catch (PlacementException e) {
        break;
      }
    }
  }

  public void createNewRouter() throws PlacementException {
    // createsRouter at random position
    try {
      fieldSem.acquire();

      int xCoord, yCoord;
      int cnt = 0;
      do {
        xCoord = (int) (Math.random() * xLength);
        yCoord = (int) (Math.random() * yLength);
        cnt++;
      } while (field[xCoord][yCoord] != null && cnt < 500);

      if (cnt >= 500) {
        throw new PlacementException();
      }

      Router r;
      do {
        r = new Router(getRandomHexString(ID_LENGTH), xCoord, yCoord);
      } while (routerMap.containsKey(r.getRouterId()));

      field[xCoord][yCoord] = r;
      routerMap.put(r.getRouterId(), r);

      fieldSem.release();
    } catch (InterruptedException e) {

    }

  }

  public void createNewRouter(int xCoord, int yCoord) throws PlacementException {
    // creates Router at specific position
    try {
      fieldSem.acquire();

      if (xCoord > this.xLength || yCoord > this.yLength || field[xCoord][yCoord] == null) {
        throw new PlacementException();
      }

      Router r;
      do {
        r = new Router(getRandomHexString(ID_LENGTH), xCoord, yCoord);
      } while (routerMap.containsKey(r.getRouterId()));

      field[xCoord][yCoord] = r;
      routerMap.put(r.getRouterId(), r);

      fieldSem.release();
    } catch (InterruptedException e) {

    }
  }

  public void moveRouter(String id) throws RouterNotFound, PlacementException {
    try {
      fieldSem.acquire();

      if (!routerMap.containsKey(id)) {
        throw new RouterNotFound();
      }
      Router r = routerMap.get(id);

      int newX, newY;
      int cnt = 0;
      do {
        newX = (int) (Math.random() * xLength);
        newY = (int) (Math.random() * yLength);
        cnt++;
      } while (field[newX][newY] != null && cnt < 500);

      if (cnt >= 500) {
        throw new PlacementException();
      }

      field[r.getXCoord()][r.getYCoord()] = null;
      field[newX][newY] = r;
      r.setXCoord(newX);
      r.setYCoord(newY);

      fieldSem.release();
    } catch (InterruptedException e) {

    }
  }

  public void moveRouter(String id, int newX, int newY) throws RouterNotFound, PlacementException {
    //moves specific router to specific position
    try{
      fieldSem.acquire();

      if (!routerMap.containsKey(id)) {
        throw new RouterNotFound();
      }
      Router r = routerMap.get(id);

      if(newX > this.xLength || newY > this.yLength || field[newX][newY] != null) {
        throw new PlacementException();
      }

      field[r.getXCoord()][r.getYCoord()] = null;
      field[newX][newY] = r;
      r.setXCoord(newX);
      r.setYCoord(newY);

      fieldSem.release();
    } catch (InterruptedException e) {

    }

  }

  public void deleteRouter(String id) throws RouterNotFound {
    try {
      fieldSem.acquire();

      if (!routerMap.containsKey(id)) {
        throw new RouterNotFound();
      }

      Router r = routerMap.get(id);
      routerMap.remove(id);
      field[r.getXCoord()][r.getYCoord()] = null;

      fieldSem.release();
    } catch (InterruptedException e) {

    }
  }

  public static String getRandomHexString(int length) {
    Random r = new Random();
    StringBuffer sb = new StringBuffer();
    while (sb.length() < length) {
      sb.append(Integer.toHexString(r.nextInt()));
    }

    return sb.toString().substring(0, length);
  }

  public ArrayList<String> getRouterInRadius(int x, int y, double radius) {

    return new ArrayList<String>();
  }

  public HashMap<String, Router> getRouterMap() {
    return this.routerMap;
  }

  public static void main(String[] args) {
    try{
      Field field = new Field(10, 8, 10);
  
      HashMap<String, Router> map = field.getRouterMap();
      for (Entry<String, Router> entry : map.entrySet()) {
        System.out.println(entry.getKey());
      }
    } catch (InvalidInputException e) {
      
    }
  }
}