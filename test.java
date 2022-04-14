public class test {
    public static void main( String[] args ) {
        try {
            Field myField = new Field(0, 10, 10);
            myField.createNewDevice();
            myField.createNewDevice();
            myField.createNewDevice();
            myField.createNewDevice();
            myField.createNewDevice();
            myField.printField();
        } catch ( Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }
}
