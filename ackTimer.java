import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ackTimer extends Thread {
    // TODO: Konzept überarbeiten
    // Idee noch nicht ausgereift
    private int count;
    private Message msg;
    private int routerPort;

    public ackTimer(Message msg, int routerPort) {
        this.count = 300;
        this.msg = msg;
        this.routerPort = routerPort;
        this.start();
    }

    public void run() {
        try {
            while(count >= 0) {
                count++;
                Thread.sleep(500);
            }
            this.sendAckMissing();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendAckMissing() {
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
}