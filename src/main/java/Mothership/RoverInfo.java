package Mothership;

import java.net.InetAddress;
import Message.RoverTelemetryMessage;
import Message.Message;

public class RoverInfo {
    private final int roverId;
    private InetAddress roverIpAddress;
    private int missionLinkUdpPort;
    private RoverTelemetryMessage lastTelemetryMessage;
    private long lastActiveTimestamp;

    // Variáveis de Controlo
    private int lastProcessedSequenceNumber = -1;
    private Message lastSentMessage = null; // A variável que faltava

    public RoverInfo (int roverId, InetAddress roverIpAddress, int missionLinkUdpPort) {
        this.roverId = roverId;
        this.roverIpAddress = roverIpAddress;
        this.missionLinkUdpPort = missionLinkUdpPort;
        this.lastTelemetryMessage = null;
        this.lastActiveTimestamp = System.currentTimeMillis();
    }

    // --- Getters e Setters da Cache ---
    public Message getLastSentMessage() {
        return lastSentMessage;
    }

    public void setLastSentMessage(Message msg) {
        this.lastSentMessage = msg;
    }

    // --- Getters e Setters da Sequência ---
    public int getLastProcessedSequenceNumber() {
        return lastProcessedSequenceNumber;
    }

    public void setLastProcessedSequenceNumber(int seq) {
        this.lastProcessedSequenceNumber = seq;
    }

    // --- Outros Métodos ---
    public void updateLastActiveTimestamp(long lastActiveTimestamp) {
        this.lastActiveTimestamp = lastActiveTimestamp;
    }

    public void updateLastTelemetryMessage(RoverTelemetryMessage lastTelemetryMessage) {
        this.lastTelemetryMessage = lastTelemetryMessage;
    }

    public void setRoverConnection (InetAddress ip, int port) {
        this.roverIpAddress = ip;
        this.missionLinkUdpPort = port;
    }

    public int getRoverId() {
        return roverId;
    }

    public InetAddress getRoverIpAddress(){
        return this.roverIpAddress;
    }

    public RoverTelemetryMessage getLastTelemetryMessage(){
        return this.lastTelemetryMessage;
    }



    public String toStringForAPI() {
        final int WIDTH = 80;

        String SEPARATOR_LINE = "+" + "-".repeat(WIDTH - 2) + "+";

        String rover = String.format("| Rover %d:%-" + (WIDTH - 11) + "s|", this.roverId, "");
        String ipAddress = String.format("| IP address -> %-" + (WIDTH - 18) + "s |", this.roverIpAddress);
        String status = this.lastTelemetryMessage.getMissionState().toString();
        String statusLine = String.format("| Status -> %-" + (WIDTH - 14) + "s |", status);
        String battery = this.lastTelemetryMessage.getBatteryLevel() + "%";
        String batteryLine = String.format("| Battery -> %-" + (WIDTH - 15) + "s |", battery);

        return SEPARATOR_LINE + "\n" +
                rover + "\n" +
                SEPARATOR_LINE + "\n" +
                ipAddress + "\n" +
                statusLine + "\n" +
                batteryLine + "\n" +
                SEPARATOR_LINE + "\n";
    }
}