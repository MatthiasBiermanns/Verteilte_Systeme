import Exceptions.InvalidInputException;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.LineBorder;

public class Gui {

  public static void main(String[] args) {
    Field myField;
    try {
      myField = new Field(20, 10, 10);
      Gui g = new Gui(myField.getField(), "Mein Fenster");
    } catch (InvalidInputException e) {
      e.printStackTrace();
    }
  }

  //private int[][] field;
  String title;

  public Gui(Device[][][] field, String title) {
    //this.field = field;
    this.title = title;
    JFrame myFrame = new JFrame(title);
    myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    JPanel panel = new JPanel();
    myFrame.add(panel);
    panel.setBounds(100, 100, 500, 500);
    panel.setLayout(new GridLayout(field.length, field.length));

    JLabel[][] feld = new JLabel[field.length][field.length];
    for (int i = 0; i < field.length; i++) {
      for (int j = 0; j < field[i].length; j++) {
        String rPort = "";
        String dPort = "";
        if (field[i][j][0] != null) {
          rPort = "Router: " + field[i][j][0].getPort();
          dPort = "\nEndgerät: " + field[i][j][1].getPort();
        }
        feld[i][j] = new JLabel();
        feld[i][j].setText(
            "<html><body>" + rPort + "<br>" + dPort + "</body></html>"
          );
        feld[i][j].setBorder(new LineBorder(Color.BLACK));
        feld[i][j].setOpaque(true);
        panel.add(feld[i][j]);
      }
    }

    myFrame.pack();
    Insets insets = myFrame.getInsets();
    myFrame.setSize(
      new Dimension(
        insets.left + insets.right + 500,
        insets.top + insets.bottom + 500
      )
    );
    myFrame.setVisible(true);
  }
}
