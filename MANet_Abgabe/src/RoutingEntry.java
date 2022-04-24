package MANet_Abgabe.src;
import java.time.Instant;
import java.util.LinkedList;

public class RoutingEntry {
  private LinkedList<Integer> path;
  private long lastUsed;

  /**
     * Erzeugt einen RoutingEntry mit Zeitstempel der Erzeugung als Eintrag in den Path-Cache eines Routers.
     * 
     * @return     leeren Pfadeintrag
     */
  public RoutingEntry() {
    this.path = new LinkedList<>();
    this.lastUsed = Instant.now().getEpochSecond();
  }

  /**
     * Erzeug einen RoutingEntry mit Zeitstempel der Erzeugung als Eintrag in den Path-Cache eines Routers.
     * 
     * @param      path Pfad als liste an Ports
     * @return     Pfadeintrag mit path als Pfad
     */
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

  /**
   * Setzt den Zeitstempel auf den jetzigen Zeitpunkt
   */
  public void updateUsage() {
    this.lastUsed = Instant.now().getEpochSecond();
  }
}
