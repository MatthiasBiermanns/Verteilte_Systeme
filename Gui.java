import Exceptions.DeviceNotFound;
import Exceptions.InvalidInputException;
import Exceptions.PlacementException;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.border.LineBorder;

public class Gui extends JFrame implements ActionListener {

  public static void main(String[] args) {
    Field myField;
    try {
      myField = new Field(30, 100, 100);
      Gui g = new Gui(myField, "Mein Fenster");
    } catch (InvalidInputException e) {
      e.printStackTrace();
    }
  }

  private Device[][][] field;
  private HashMap<Integer, Device> map;
  private Field myField;
  private String title;
  private JLabel[][] feld;

  public Gui(Field myField, String title) {
    this.field = myField.getField();
    this.map = myField.getMap();
    this.myField = myField;
    this.title = title;

    createAndShowApp();
  }

  private void createAndShowApp() {
    JFrame myFrame = new JFrame(title);
    myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    JPanel topPanel = new JPanel();
    topPanel.setLayout(new BorderLayout());

    JPanel gridPanel = setUpGrid();
    topPanel.add(gridPanel);

    JButton button = new JButton();
    button.setText("Start");
    button.addActionListener(this);
    button.setActionCommand("start");
    topPanel.add(button, BorderLayout.PAGE_END);

    myFrame.pack();
    Insets insets = myFrame.getInsets();
    myFrame.setSize(
      new Dimension(
        insets.left + insets.right + 800,
        insets.top + insets.bottom + 800
      )
    );

    myFrame.add(topPanel);
    myFrame.setVisible(true);
  }

  private JPanel setUpGrid() {
    JPanel gridPanel = new JPanel();
    gridPanel.setBounds(100, 100, 500, 500);
    gridPanel.setLayout(new GridLayout(field.length, field.length));
    this.feld = new JLabel[field.length][field.length];

    for (int i = 0; i < field.length; i++) {
      for (int j = 0; j < field[i].length; j++) {
        this.feld[i][j] = new JLabel();
        if (field[i][j][0] != null) {
          this.feld[i][j].setBackground(Color.GREEN);
        }
        this.feld[i][j].setBorder(new LineBorder(Color.gray));
        this.feld[i][j].setOpaque(true);
        gridPanel.add(this.feld[i][j]);
      }
    }
    return gridPanel;
  }

  public void actionPerformed(ActionEvent e) {
    if (e.getActionCommand().equals("start")) {
        moveDeviceInGui();
    }
  }

  private void moveDeviceInGui() {
    HashMap<Integer, Device> map = this.map;
    Device dev = map.values().iterator().next();
    int x = dev.getXCoord();
    int y = dev.getYCoord();
    feld[x][y].setBackground(Color.BLUE);

    try {
      myField.moveDevice(dev.getXCoord(), dev.getYCoord());
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    x = dev.getXCoord();
    y = dev.getYCoord();
    feld[x][y].setBackground(Color.RED);
    revalidate();
    repaint();
  }
}
