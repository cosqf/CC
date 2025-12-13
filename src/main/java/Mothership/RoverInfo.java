package Mothership;

import java.net.InetAddress;
import Message.RoverTelemetryMessage;
import Message.MessageUDP;

public class RoverInfo {
    private final int roverId;
    private final InetAddress roverIpAddress;
    private final int roverPort;
    private RoverTelemetryMessage lastTelemetryMessage;
    private long lastActiveTimestamp;

    private int lastProcessedSequenceNumber = -1;
    private MessageUDP lastSentMessage = null;

    private int outputSequenceNumber = 0;

    public RoverInfo (int roverId, InetAddress roverIpAddress, int roverPort) {
        this.roverId = roverId;
        this.roverIpAddress = roverIpAddress;
        this.roverPort = roverPort;
        this.lastTelemetryMessage = null;
        this.lastActiveTimestamp = System.currentTimeMillis();
    }

    public int getAndIncrementOutputSequenceNumber() {
        return this.outputSequenceNumber++;
    }

    public MessageUDP getLastSentMessage() {
        return lastSentMessage;
    }

    public void setLastSentMessage(MessageUDP msg) {
        this.lastSentMessage = msg;
    }

    public int getLastProcessedSequenceNumber() {
        return lastProcessedSequenceNumber;
    }

    public void setLastProcessedSequenceNumber(int seq) {
        this.lastProcessedSequenceNumber = seq;
    }

    public void updateLastActiveTimestamp(long lastActiveTimestamp) {
        this.lastActiveTimestamp = lastActiveTimestamp;
    }

    public void updateLastTelemetryMessage(RoverTelemetryMessage lastTelemetryMessage) {
        this.lastTelemetryMessage = lastTelemetryMessage;
    }

    public int getRoverId() {
        return roverId;
    }

    public InetAddress getRoverIpAddress(){
        return this.roverIpAddress;
    }
    public int getRoverPort() {
        return this.roverPort;
    }

    public RoverTelemetryMessage getLastTelemetryMessage(){
        return this.lastTelemetryMessage;
    }

    public String toStringForAPI() {
        final int WIDTH = 80;

        String SEPARATOR_LINE = "+" + "-".repeat(WIDTH - 2) + "+" + "\n";

        String rover = String.format("| Rover %d:%-" + (WIDTH - 11 - ((int) Math.log10(Math.abs(this.roverId)) + 1)) + "s |\n", this.roverId, "");
        String ipAddress = String.format("| IP address -> %-" + (WIDTH - 18) + "s |\n", this.roverIpAddress);
        String status = "N/A";
        String battery = "N/A";
        if (this.lastTelemetryMessage != null) {
            status = this.lastTelemetryMessage.getMissionState().toString();
            battery = Math.round(this.lastTelemetryMessage.getBatteryLevel() * 100.0) / 100.0 + "%";
        }
        String statusLine = String.format("| Status -> %-" + (WIDTH - 14) + "s |\n", status);
        String batteryLine = String.format("| Battery -> %-" + (WIDTH - 15) + "s |\n", battery);

        String time = (System.currentTimeMillis()-this.lastActiveTimestamp)/1000 + " seconds ago";
        String lastActive = String.format("| Last Active -> %-" + (WIDTH - 19) + "s |\n", time);

        return SEPARATOR_LINE +
                rover +
                SEPARATOR_LINE +
                ipAddress +
                statusLine +
                batteryLine +
                lastActive +
                SEPARATOR_LINE;
    }
}