import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import Exceptions.PositionOccupied;
import Exceptions.RouterNotFound;

class Field {
  final static int ID_LENGTH = 16;
  int xLength, yLength;
  Router[][] field;
  HashMap<String, Router> router;

  public Field(int routerCnt, int xLength, int yLength) {
    this.xLength = xLength;
    this.yLength = yLength;
    field = new Router[xLength][yLength];
    for (int i = 0; i < routerCnt; i++) {
      createNewRouter();
    }
  }

  public void createNewRouter() {
    // createsRouter at random position
    int x = (int)(Math.random() * xLength); 
    int y = (int)(Math.random() * yLength);

    do{
      Router r = new Router(getRandomHexString(ID_LENGTH), x, y);

    } while ( router.containsKey(r.getId));

    
  }

  public void createNewRouter(int x, int y) throws PositionOccupied {
    // creates Router at specific position
  }

  public void moveRouter(int x, int y) throws RouterNotFound {
    // moves random
  }

  public void moveRouter(int x, int y, int newX, int newY) throws RouterNotFound, PositionOccupied {
    // moves specific
  }

  public void deleteRouter(int x, int y) throws RouterNotFound {

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

  }
}