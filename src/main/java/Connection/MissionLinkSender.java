package Connection;

import Message.Message;
import Message.Package;
import Utils.UDPPrint;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class MissionLinkSender implements Runnable {
    private final DatagramSocket socket;
    private final BlockingQueue<Package> outgoingQueue;

    // Variáveis partilhadas para controlo
    private static final int MAX_TIMEOUTS = 10;
    private static final int TIMEOUT_MS = 4000; // 4 segundos para retransmitir
    private final ConcurrentHashMap<String, Message> lastSentReply = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PendingAck> pendingAcks = new ConcurrentHashMap<>();

    private static class PendingAck {
        volatile int waitingForAckNumber;
        final Object lock = new Object();
        volatile boolean ackReceived = false;
        PendingAck(int expected) { this.waitingForAckNumber = expected; }
    }

    private Thread runningThread;
    private boolean running = true;

    public MissionLinkSender(DatagramSocket socket, BlockingQueue<Package> outgoingQueue) {
        this.socket = socket;
        this.outgoingQueue = outgoingQueue;
    }
    public void sendMessage(Message msg, String ip, int port) {
        Package pck = new Package(ip, port, msg);
        outgoingQueue.add(pck);
    }

    // Método chamado pelo Receiver quando chega um ACK
    public void confirmAck(int ackNumber, String address) {
        PendingAck p = pendingAcks.get(address);
        if (p == null) return;
        synchronized (p.lock) {
            if (ackNumber >= p.waitingForAckNumber) {
                p.ackReceived = true;
                p.lock.notify();
            }
        }
    }

    public void stop() {
        running = false;
        runningThread.interrupt();
    }

    @Override
    public void run() {
        this.runningThread = Thread.currentThread();
        while (running) {
            try {
                Package pkg = outgoingQueue.take();
                Message msg = pkg.getMessage();
                System.out.println("-- MISSION LINK SENDER: SENDING MESSAGE --");

                InetAddress ipAddress = InetAddress.getByName(pkg.getToIp());
                int port = pkg.getToPort();

                // Calcular ACK Esperado
                int payloadSize = 0;
                try {
                    if (msg.getMessageData() != null) {
                        payloadSize = msg.getMessageData().convertMessageDataToBytes().length;
                    }
                } catch (Exception e) { payloadSize = 0; }

                int expectedAck = msg.getSequenceNumber() + (payloadSize > 0 ? payloadSize : 1);

                PendingAck pAck = new PendingAck(expectedAck);
                pendingAcks.put(ipAddress.getHostAddress(), pAck);

                boolean sentSuccessfully = false;
                int attempts = 0;

                synchronized (pAck.lock) {
                    if (!pAck.ackReceived) {
                        pAck.lock.wait(TIMEOUT_MS);
                    }
                    if (pAck.ackReceived) { sentSuccessfully = true; }
                    else { // timeout -> possibly retry
                        if (attempts > MAX_TIMEOUTS) { sentSuccessfully = true; }
                    }
                }

                while (!sentSuccessfully) {
                    attempts++;

                    // 2. ENVIAR (ou Reenviar)
                    byte[] data = msg.convertMessageToBytes();
                    DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, port);
                    socket.send(packet);

                    // --- LOGGING ESTILO WIRESHARK ---
                    if (attempts > 1) {
                        // Retransmissão (Fundo Vermelho)
                        UDPPrint.log("SND", msg, "Retransmission #" + attempts + " -> " + pkg.getToIp(), true);
                        System.out.println(msg);
                    } else {
                        // Envio Normal (Ciano)
                        UDPPrint.log("SND", msg, "To: " + pkg.getToIp() + " (Waiting ACK " + expectedAck + ")", false);
                    }
                    // --------------------------------

                    // 3. ESPERAR PELO ACK (com Timeout)
                    synchronized (pAck.lock) {
                        if (!pAck.ackReceived) {
                            pAck.lock.wait(TIMEOUT_MS);
                        }
                        if (pAck.ackReceived) {
                            // Sucesso! (Opcional: log verde discreto)
                            // System.out.println(WiresharkLogger.GREEN + "   └── [ML-SND] Confirmado!" + WiresharkLogger.RESET);
                            sentSuccessfully = true;
                        } else { // Timeout! O loop vai repetir e imprimir a vermelho na próxima volta
                            if (attempts > MAX_TIMEOUTS) {
                                UDPPrint.log("SND", msg, "Max number of retransmissions hit, dropping message...", true);
                                sentSuccessfully = true;
                            }
                        }
                    }
                }
            } catch (IOException | InterruptedException e) {
                if (running) System.out.println("[ML SENDER] Connection closed or lost.");
                e.printStackTrace();
                running = false;
            }
        }
        System.out.println("Closing ML sender!");
    }


    public Message getLastSentReply (String address) {
        return this.lastSentReply.get(address);
    }
    public void setLastSentReply (String address, Message reply) {
        this.lastSentReply.put(address, reply);
    }

}