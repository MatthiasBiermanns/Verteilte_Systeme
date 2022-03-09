import java.util.HashMap;
import java.util.Random;

import Exceptions.PositionOccupied;
import Exceptions.RouterNotFound;

class Field{
  Router[][] field;
  HashMap<String,Router> router;

  public Field(int routerCnt, int xlength, int ylength) {
    field = new Router[xlength][ylength];
    for(int i = 0; i < routerCnt; i++) {
      createNewRouter();
    }
  }

  public void createNewRouter() {
    //createsRouter at random position
  }

  public void createNewRouter(int x, int y) throws PositionOccupied {
    //creates Router at specific position
  }


  public void moveRouter(int x, int y) throws RouterNotFound {
    //moves random
  }

  public void moveRouter(int x, int y, int newX, int newY) throws RouterNotFound, PositionOccupied {
    //moves specific
  }

  public void deleteRouter(int x, int y) throws RouterNotFound {
    
  }

  public static String getRandomHexString(int length) {
    Random r = new Random();
    StringBuffer sb = new StringBuffer();
    while(sb.length() < length){
      sb.append(Integer.toHexString(r.nextInt()));
    }

    return sb.toString().substring(0, length);
  }

  public static void main( String[] args) {

  }
}