package Message;

public class Package {
    private final String toIp;
    private final int toPort;
    private final Message message;

    public Package(String toIp, int toPort, Message message) {
        this.toIp = toIp;
        this.toPort = toPort;
        this.message = message;
    }
    public String getToIp() {
        return toIp;
    }
    public int getToPort() {
        return toPort;
    }
    public Message getMessage() {
        return message;
    }
}
