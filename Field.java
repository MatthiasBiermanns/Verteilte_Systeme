import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;

import Exceptions.InvalidInputException;
import Exceptions.PlacementException;
import Exceptions.RouterNotFound;

class Field {
  final static int ID_LENGTH = 16;
  static int nextPort = 3000;
  private int xLength, yLength;
  private Semaphore fieldSem = new Semaphore(1, true);
  private Router[][] field;
  private HashMap<Integer, Router> routerMap = new HashMap<Integer, Router>();

  public Field(int routerCnt, int xLength, int yLength) throws InvalidInputException {
    if (xLength < 0 || yLength < 0 || (xLength == 0 && yLength == 0)) {
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

  public static void main(String[] args) {
    try {
      Field field = new Field(10, 8, 10);

      HashMap<Integer, Router> map = field.getRouterMap();
      for (Entry<Integer, Router> entry : map.entrySet()) {
        System.out.println(entry.getKey());
      }

      System.out.println(getDistance(1, 0, 3, 3));
      System.out.println(getDistance(0, 4, 3, 4));
    } catch (InvalidInputException e) {

    }
  }

  public Router getClosestReachableRouter(int x, int y, double range) throws RouterNotFound{
    ArrayList<Router> reachableRouter = getReachableRouter(x, y, range);

    if(reachableRouter.isEmpty()) {
      throw new RouterNotFound("No router in range");
    }
    Router router =  new Router();
    double closestDist = 99999;

    Iterator<Router> i = reachableRouter.iterator();

    while(i.hasNext()) {
      Router tempRouter = i.next();
      double distance = getDistance(x, y, tempRouter.getXCoord(), tempRouter.getYCoord());
      if(distance < closestDist) {
        router = tempRouter;
      }
    }
    return router;
  }

  public ArrayList<Router> getReachableRouter(int x, int y, double range) {
    ArrayList<Router> reachableRouter = new ArrayList<Router>();

    for (Entry<Integer, Router> entry : this.getRouterMap().entrySet()) {
      Router r = entry.getValue();
      if (getDistance(x, y, r.getXCoord(), r.getYCoord()) <= range) {
        reachableRouter.add(r);
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

      //Router r;
      //do {
      Router r = new Router(xCoord, yCoord, nextPort, this);
      nextPort++;
      //} while (routerMap.containsKey(r.getPort()));

      field[xCoord][yCoord] = r;
      routerMap.put(r.getPort(), r);

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

      //Router r;
      //do {
      Router r = new Router(xCoord, yCoord, nextPort, this);
      nextPort++;
      //} while (routerMap.containsKey(r.getRouterId()));

      field[xCoord][yCoord] = r;
      routerMap.put(r.getPort(), r);

      fieldSem.release();
    } catch (InterruptedException e) {

    }
  }

  public void moveRouter(int port) throws RouterNotFound, PlacementException {
    try {
      fieldSem.acquire();

      if (!routerMap.containsKey(port)) {
        throw new RouterNotFound();
      }
      Router r = routerMap.get(port);

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

  public void moveRouter(int port, int newX, int newY) throws RouterNotFound, PlacementException {
    // moves specific router to specific position
    try {
      fieldSem.acquire();

      if (!routerMap.containsKey(port)) {
        throw new RouterNotFound();
      }
      Router r = routerMap.get(port);

      if (newX > this.xLength || newY > this.yLength || field[newX][newY] != null) {
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

  public void deleteRouter(int port) throws RouterNotFound {
    try {
      fieldSem.acquire();

      if (!routerMap.containsKey(port)) {
        throw new RouterNotFound();
      }

      Router r = routerMap.get(port);
      routerMap.remove(port);
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

  public HashMap<Integer, Router> getRouterMap() {
    return this.routerMap;
  }

}