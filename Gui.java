import Exceptions.DeviceNotFound;
import Exceptions.InvalidInputException;
import java.awt.*;
import java.awt.event.*;
// import java.util.HashMap; // only for moveDeviceInGui()
// import java.util.Iterator; // only for moveDeviceInGui()
import javax.swing.*;
import javax.swing.border.LineBorder;

public class Gui extends JFrame implements ActionListener {

  public static void main(String[] args) {
    Field myField;
    try {
      myField = new Field(30, 100, 100);
      new Gui(myField, "Mein Fenster");
    } catch (InvalidInputException e) {
      e.printStackTrace();
    }
  }

  private Device[][][] field;
  // private HashMap<Integer, Device> map; // only for moveDeviceInGui()
  private Field myField;
  private JLabel[][] feld;
  private GuiServer gs;
  private Thread t1;

  public Gui(Field myField, String title) {
    this.field = myField.getField();
    // this.map = myField.getMap(); // only for moveDeviceInGui()
    this.myField = myField;
    gs = new GuiServer();
    t1 = new Thread(gs, "GuiServer");
    t1.start();
    createAndShowApp(title);
  }

  private void createAndShowApp(String title) {
    this.setTitle(title);
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    JPanel topPanel = new JPanel();
    topPanel.setLayout(new BorderLayout());

    JPanel gridPanel = setUpGrid();
    gridPanel.setBounds(50, 50, 1000, 1000);
    gridPanel.setLayout(new GridLayout(100, 100));
    topPanel.add(gridPanel);

    JButton button = new JButton();
    button.setText("Start");
    button.addActionListener(this);
    button.setActionCommand("start");
    topPanel.add(button, BorderLayout.PAGE_END);

    this.pack();
    Insets insets = this.getInsets();
    this.setSize(
        new Dimension(
          insets.left + insets.right + 800,
          insets.top + insets.bottom + 800
        )
      );

    this.add(topPanel);
    this.setVisible(true);
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
          this.feld[i][j].setBackground(Color.BLACK);
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
      startSimulation();
    }
  }

  /**
   * Starts simulation and Updates Grid with Server-Input.
   */
  private void startSimulation() {
    int timerDelay = 20;
    toSimulate();
    new Timer(
      timerDelay,
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          GuiUpdateMessage m = gs.getX();

          if (m != null) {
            feld[m.getXCord()][m.getYCord()].setBackground(
                getColorToCommand(m.getCommand(), m.getPort())
              );
            revalidate();

            repaint();
          }
        }
      }
    )
      .start();
  }

  /**
   * Defines which example should be simulated.
   */
  private void toSimulate() {
    Thread t2 = new Thread(new GuiWorker(this.myField));
    t2.start();
  }

  /**
   * Method to determine which color the panel should get regarding to the command the Router got.
   * @param command Command router received.
   * @return Color for respective command.
   */
  private Color getColorToCommand(Command command, int port) {
    Color color;

    switch (command) {
      case Send:
        color = Color.CYAN;
        break;
      case Forward:
        color = Color.GREEN;
        break;
      case RouteRequest:
        color = Color.MAGENTA;
        break;
      case RouteError:
        color = Color.RED;
        break;
      case Retry:
        color = Color.WHITE;
        break;
      case Unknown:
        System.out.println("<<<<<<<<<MOVE>>>>>>>>>");
        try {
          Device dev = myField.getDevice(port);
          feld[dev.xCoord][dev.yCoord].setBackground(Color.BLACK);
        } catch (DeviceNotFound e) {
          e.printStackTrace();
        }
        color = UIManager.getColor("Panel.background");
        break;
      default:
        color = Color.BLACK;
        break;
    }
    return color;
  }
  // private void moveDeviceInGui() {
  // Get device object
  //   HashMap<Integer, Device> map = this.map;
  //   int hops = (int) (Math.random() * map.size()) - 1;
  //   Iterator<Device> iter = map.values().iterator();
  //   Device dev = iter.next();
  //   for (int i = hops; i > 0; i--) {
  //     dev = iter.next();
  //   }

  // Find device in GUI
  //   int x = dev.getXCoord();
  //   int y = dev.getYCoord();
  //   feld[x][y].setBackground(Color.BLUE);

  // Move device in Field
  //   try {
  //     myField.moveDevice(dev.getXCoord(), dev.getYCoord());
  //   } catch (Exception ex) {
  //     ex.printStackTrace();
  //   }

  // Move device in GUI
  //   x = dev.getXCoord();
  //   y = dev.getYCoord();
  //   feld[x][y].setBackground(Color.RED);

  //   revalidate();
  //   repaint();
  // }

}
