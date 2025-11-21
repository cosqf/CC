package Mothership;

import java.net.InetAddress;
import java.io.DataOutputStream;
import Message.RoverTelemetryMessage;

public class RoverInfo {
    private final int roverId;
    private InetAddress roverIpAddress;
    private int missionLinkUdpPort;
    private DataOutputStream tcpOut;
    private RoverTelemetryMessage lastTelemetryMessage;
    private long lastActiveTimestamp;

    // --- NOVO CAMPO: Memória de Duplicados ---
    // Inicializado a -1 para que a primeira mensagem (seq 0) seja aceite.
    private int lastProcessedSequenceNumber = -1;

    public RoverInfo (int roverId, InetAddress roverIpAddress, int missionLinkUdpPort) {
        this.roverId = roverId;
        this.roverIpAddress = roverIpAddress;
        this.missionLinkUdpPort = missionLinkUdpPort;
        this.tcpOut = null;
        this.lastTelemetryMessage = null;
        this.lastActiveTimestamp = System.currentTimeMillis();
    }

    // --- NOVOS MÉTODOS NECESSÁRIOS PARA A MOTHERSHIP ---
    public int getLastProcessedSequenceNumber() {
        return lastProcessedSequenceNumber;
    }

    public void setLastProcessedSequenceNumber(int seq) {
        this.lastProcessedSequenceNumber = seq;
    }
    // ---------------------------------------------------

    public void updateLastActiveTimestamp(long lastActiveTimestamp) {
        this.lastActiveTimestamp = lastActiveTimestamp;
    }

    public void updateLastTelemetryMessage(RoverTelemetryMessage lastTelemetryMessage) {
        this.lastTelemetryMessage = lastTelemetryMessage;
    }

    // (Opcional) Podes adicionar getters para o ID se precisares
    public int getRoverId() {
        return roverId;
    }
}
