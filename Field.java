import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Semaphore;


import Exceptions.PositionOccupied;
import Exceptions.RouterNotFound;

class Field {
  final static int ID_LENGTH = 16;
  private int xLength, yLength;
  private Semaphore fieldSem = new Semaphore(1, true);
  private Router[][] field;
  private HashMap<String, Router> router = new HashMap<String, Router>();

  public Field(int routerCnt, int xLength, int yLength) {
    this.xLength = xLength;
    this.yLength = yLength;
    field = new Router[xLength][yLength];
    for (int i = 0; i < routerCnt; i++) {
      try{
        createNewRouter();
      } catch (PositionOccupied e) {
        break;
      }
    }
  }

  public void createNewRouter() throws PositionOccupied {
    // createsRouter at random position
    try {
      fieldSem.acquire();

      int x, y;
      int cnt = 0;
      do {
        x = (int) (Math.random() * xLength);
        y = (int) (Math.random() * yLength);
        cnt++;
      } while (field[x][y] != null && cnt < 500);

      if (cnt >= 500) {
        throw new PositionOccupied();
      }

      Router r;
      do {
        r = new Router(getRandomHexString(ID_LENGTH), x, y);
      } while (router.containsKey(r.getRouterId()));

      field[x][y] = r;
      router.put(r.getRouterId(), r);

      fieldSem.release();
    } catch (InterruptedException e) {

    }

  }

  public void createNewRouter(int x, int y) throws PositionOccupied {
    // creates Router at specific position
    try {
      fieldSem.acquire();

      if (field[x][y] == null) {
        throw new PositionOccupied();
      }

      Router r;
      do {
        r = new Router(getRandomHexString(ID_LENGTH), x, y);
      } while (router.containsKey(r.getRouterId()));

      field[x][y] = r;
      router.put(r.getRouterId(), r);

      fieldSem.release();
    } catch (InterruptedException e) {

    }
  }

  public void moveRouter(String id) throws RouterNotFound, PositionOccupied {
    try {
      fieldSem.acquire();

      if (!router.containsKey(id)) {
        throw new RouterNotFound();
      }
      Router r = router.get(id);

      int newX, newY;
      int cnt = 0;
      do {
        newX = (int) (Math.random() * xLength);
        newY = (int) (Math.random() * yLength);
        cnt++;
      } while (field[newX][newY] != null && cnt < 500);

      if (cnt >= 500) {
        throw new PositionOccupied();
      }

      field[r.getXCoord()][r.getYCoord()] = null;

      fieldSem.release();
    } catch (InterruptedException e) {

    }
  }

  public void moveRouter(String id, int newX, int newY) throws RouterNotFound, PositionOccupied {
    // moves specific
  }

  public void deleteRouter(String id) throws RouterNotFound {
    try {
      fieldSem.acquire();

      if (!router.containsKey(id)) {
        throw new RouterNotFound();
      }

      Router r = router.get(id);
      router.remove(id);
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

  public static void main(String[] args) {
    Field field = new Field(10, 8, 10);
  }
}