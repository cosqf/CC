package Message;

public class Package {
    private final String toIp;
    private final int toPort;
    private final MessageUDP message;

    public Package(String toIp, int toPort, MessageUDP message) {
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
    public MessageUDP getMessage() {
        return message;
    }
}
