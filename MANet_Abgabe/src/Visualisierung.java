import Exceptions.InvalidInputException;

/**Klasse zur Visualisierung eines Routerfelds.
 */
public class Visualisierung {

  public static void main(String[] args) throws Exception {
    Field myField = sampleMethod();
    new Gui(myField, "TestX");
  }

  /**
   * Erstellt ein Feld mit den spezifizierten Merkmalen routerCnt: 200, xLength=yLength: 100.
   * @return Field
   * @throws InvalidInputException
   */
  private static Field sampleMethod() throws InvalidInputException {
    Field myField = new Field(200, 100, 100);
    // myField.createNewDevice();
    return myField;
  }
}
