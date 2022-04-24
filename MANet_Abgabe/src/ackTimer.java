package MANet_Abgabe.src;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ackTimer extends Thread {
    private int count;
    private Message msg;
    private int routerPort;

    /**
     * Erstellt einen eigenen Timer-Thread und startet diesen. Thread sendet eine 
     * AckNeeded-Nachricht an den Router, von dem er erstellt wurde.
     * 
     * @param msg Message, für die der Timer erstellt wurde
     * @param routerPort Port des Routers, an den die AckNeeded Nachricht gehen soll 
     *                   (Sollte der Router sein, der auch diesen Timer erstellt)
     */
    public ackTimer(Message msg, int routerPort) {
        this.count = 10;
        this.msg = msg;
        this.routerPort = routerPort;
        this.start();
    }

    /**
     * Zählt einen counter (Wert 10) sekündlich herunter und sendet nach ablauf des
     * Counters eine AckNeeded Message.
     */
    public void run() {
        try {
            while(count > 0) {
                count++;
                Thread.sleep(1000);
            }
            this.sendAckMissing();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sendet eine AckNeeded Message an den bei der erstellung des Timers mitgegebenen Port.
     * Die AckNeeddedMessage enthält die MessageId, den SourcePort, DestPort und den Pfad der 
     * Nachricht, für die der Timer erstellt wurde, selbst als diese.
     */
    private void sendAckMissing() {
        try (DatagramSocket socket = new DatagramSocket()){
            InetAddress destAddress = InetAddress.getByName("localhost");
            Message ackMissMessage = new Message(msg.getMessageId(), Command.AckNeeded, msg.getSourcePort(), msg.getDestPort(), msg.getPath(), "");
            byte[] message = ackMissMessage.toString().getBytes();
            DatagramPacket dp = new DatagramPacket(message, message.length, destAddress, this.routerPort); 
            socket.send(dp);
        } catch ( Exception e) {
            e.printStackTrace();
        }
    }

    public int getCount() {
        return this.count;
    }
}
