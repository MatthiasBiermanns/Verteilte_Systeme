import java.util.HashMap;

public class test {
    public static void main( String[] args ) {
        try {
            Field myField = new Field(0, 25, 25);
            myField.createNewDevice(0, 0);
            myField.createNewDevice(5, 5);
            myField.createNewDevice(10, 10);
            myField.createNewDevice(15, 15);
            myField.createNewDevice(20, 20);
            myField.createNewDevice(24, 24);
            myField.startDevices();
            myField.printField();
            HashMap<Integer, Device> fieldMap = myField.getMap();
            EndDevice handy = (EndDevice) fieldMap.get(3001);
            Router router = (Router) fieldMap.get(3000);
            System.out.println(myField.getReachableRouter(router.getPort(), router.getXCoord(), router.getYCoord()));
            handy.sendMessage(3010, "Hi");
        } catch ( Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }
}
