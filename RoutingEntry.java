import java.time.Instant;
import java.util.LinkedList;

public class RoutingEntry {
  private LinkedList<Integer> path;
  private long lastUsed;

  public RoutingEntry() {
    this.path = new LinkedList<>();
    this.lastUsed = Instant.now().getEpochSecond();
  }

  public RoutingEntry(LinkedList<Integer> path) {
    this.path = path;
    this.lastUsed = Instant.now().getEpochSecond();
  }

  public LinkedList<Integer> getPath() {
    return this.path;
  }

  public long getLastUsed() {
    return this.lastUsed;
  }

  public void setPath(LinkedList<Integer> path) {
    this.path = path;
  }

  public void setLastUsed(long lastUsage) {
    this.lastUsed = lastUsage;
  }

  public void updateUsage() {
    this.lastUsed = Instant.now().getEpochSecond();
  }
}
