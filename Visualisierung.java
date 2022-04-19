public class Visualisierung {

  public static void main(String[] args) throws Exception {
    Field myField = sampleMethod();
    new Gui(myField, "TestX");
  }

  private static Field sampleMethod() throws Exception {
    Field myField = new Field(100, 100, 100);
    // myField.createNewDevice();
    return myField;
  }
}
