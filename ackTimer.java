public class ackTimer extends Thread {
    private int count;
    private Message msg;

    public ackTimer(Message msg) {
        this.msg = msg;
        this.count = 300;
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
        try{
            
        } catch ( Exception e) {
            e.printStackTrace();
        }
    }
}
