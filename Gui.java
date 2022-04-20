import Exceptions.DeviceNotFound;
import Exceptions.InvalidInputException;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.List;
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
  private Field myField;
  private JLabel[][] feld;

  public Gui(Field myField, String title) {
    this.field = myField.getField();
    this.myField = myField;
    createAndShowApp(title);
  }

  private void createAndShowApp(String title) {
    this.setTitle(title);
    this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

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

  private List<GuiUpdateMessage> messages;

  /**
   * Starts simulation and Updates Grid with Server-Input.
   */
  private void startSimulation() {
    toSimulate();
    SwingWorker<Void, GuiUpdateMessage> guiServer = new SwingWorker<>() {
      @Override
      protected Void doInBackground() throws Exception {
        byte[] buf = new byte[256];

        try (DatagramSocket socket = new DatagramSocket(4998)) {
          while (true) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            System.out.println("Receiving now:");
            socket.receive(packet);

            String received = new String(
              packet.getData(),
              0,
              packet.getLength()
            );
            System.out.println(packet.getPort() + ": " + received);
            publish(new GuiUpdateMessage(received));
          }
        } catch (SocketException e1) {
          e1.printStackTrace();
        } catch (IOException e2) {
          e2.printStackTrace();
        }
        return null;
      }

      @Override
      protected void process(List<GuiUpdateMessage> chunks) {
        repaintGrid();
        if (messages == null) messages = chunks;
        messages.addAll(chunks);
        visualize();
      }
    };

    guiServer.execute();
  }

  private synchronized void visualize() {
    // System.out.println(">>>>>>>>>Chunk size = " + messages.size());
    for (int i = 0; i < messages.size(); i++) {
      GuiUpdateMessage m = messages.remove(i);
      feld[m.getXCord()][m.getYCord()].setBackground(
          getColorToCommand(m.getCommand(), m.getPort(), m.getDestPort())
        );
      revalidate();

      repaint();
    }
    // System.out.println("<<<<<<<<<Chunk size = " + messages.size());
    if (messages.size() > 0) visualize();
  }

  private void repaintGrid() {
    for (int i = 0; i < field.length; i++) {
      for (int j = 0; j < field[i].length; j++) {
        if (field[i][j][0] != null) {
          this.feld[i][j].setBackground(Color.BLACK);
        } else {
          this.feld[i][j].setBackground(UIManager.getColor("Panel.background"));
        }
      }
    }
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
  private Color getColorToCommand(Command command, int port, int destPort) {
    Color color;

    switch (command) {
      case Send:
        color = Color.CYAN;
        break;
      case Forward:
        color = Color.GREEN;
        break;
      case RouteRequest:
        color = (destPort != port) ? Color.MAGENTA : Color.BLUE;
        break;
      case RouteError:
        color = Color.RED;
        break;
      case Retry:
        color = Color.WHITE;
        break;
      case Unknown:
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
}
