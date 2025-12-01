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
    private Message lastSentMessage = null;

    // --- VARIÁVEL PARA CONTAR O ENVIO ---
    private int outputSequenceNumber = 0;

    public RoverInfo (int roverId, InetAddress roverIpAddress, int missionLinkUdpPort) {
        this.roverId = roverId;
        this.roverIpAddress = roverIpAddress;
        this.missionLinkUdpPort = missionLinkUdpPort;
        this.lastTelemetryMessage = null;
        this.lastActiveTimestamp = System.currentTimeMillis();
    }

    // --- SEQUÊNCIA DE SAÍDA (Novo) ---
    public int getAndIncrementOutputSequenceNumber() {
        return this.outputSequenceNumber++;
    }

    // --- CACHE (O que te faltava) ---
    public Message getLastSentMessage() {
        return lastSentMessage;
    }

    public void setLastSentMessage(Message msg) {
        this.lastSentMessage = msg;
    }

    // --- Getters e Setters Normais ---
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

        String SEPARATOR_LINE = "+" + "-".repeat(WIDTH - 2) + "+" + "\n";

        String rover = String.format("| Rover %d:%-" + (WIDTH - 11 - ((int) Math.log10(Math.abs(this.roverId)) + 1)) + "s |\n", this.roverId, "");
        String ipAddress = String.format("| IP address -> %-" + (WIDTH - 18) + "s |\n", this.roverIpAddress);
        String status = "N/A";
        String battery = "N/A";
        if (this.lastTelemetryMessage != null) {
            status = this.lastTelemetryMessage.getMissionState().toString();
            battery = this.lastTelemetryMessage.getBatteryLevel() + "%";
        }
        String statusLine = String.format("| Status -> %-" + (WIDTH - 14) + "s |\n", status);
        String batteryLine = String.format("| Battery -> %-" + (WIDTH - 15) + "s |\n", battery);

        return SEPARATOR_LINE +
                rover +
                SEPARATOR_LINE +
                ipAddress +
                statusLine +
                batteryLine +
                SEPARATOR_LINE;
    }
}