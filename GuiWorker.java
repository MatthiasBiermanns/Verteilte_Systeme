/**
 * Class to get Testcase paralell. Extends class which implements Testcases.
 */
public class GuiWorker extends test implements Runnable {

  private Field myField;

  GuiWorker(Field field) {
    this.myField = field;
  }

  @Override
  public void run() {
    testRouteRequest(this.myField);
  }
}