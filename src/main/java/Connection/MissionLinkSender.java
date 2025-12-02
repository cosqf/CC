package Connection;

import Message.Message;
import Message.MessageUDP;
import Message.Package;
import Utils.UDPPrint;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class MissionLinkSender implements Runnable {
    private final DatagramSocket socket;
    private final BlockingQueue<Package> outgoingQueue;

    // Variáveis partilhadas para controlo
    private static final int MAX_TIMEOUTS = 10;
    private static final int TIMEOUT_MS = 4000; // 4 segundos para retransmitir
    private final ConcurrentHashMap<String, MessageUDP> lastSentReply = new ConcurrentHashMap<>();
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
    public void sendMessage(MessageUDP msg, String ip, int port) {
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
                // 1. Pegar na próxima mensagem da fila
                Package pkg = outgoingQueue.take();
                MessageUDP msg = pkg.getMessage();
                //System.out.println("-- [MISSION LINK SENDER]: " + msg.toString());

                InetAddress ipAddress = InetAddress.getByName(pkg.getToIp());
                int port = pkg.getToPort();

                if (msg.getMessageDataType() == Message.MessageDataTypes.ACK) {
                    byte[] data = msg.convertMessageToBytes();
                    DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, port);
                    socket.send(packet);
                    //System.out.println("ACK sent: " + msg.getMessageData().toString());
                    continue;
                }

                // Calcular ACK Esperado
                int payloadSize = 0;
                try {
                    if (msg.getMessageData() != null) {
                        payloadSize = msg.getMessageData().convertMessageDataToBytes().length;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                int expectedAck = msg.getSequenceNumber() + (payloadSize > 0 ? payloadSize : 1);

                PendingAck pAck = new PendingAck(expectedAck);
                String pendingKey = ipAddress.getHostAddress() + ":" + port;
                pendingAcks.put(pendingKey, pAck);

                // --- PASSO 1: FRAGMENTAR A MENSAGEM ---
                // Se for pequena, a lista terá apenas 1 elemento.
                // Se for grande, terá vários.
                List<MessageUDP> fragments = FragManager.fragmentMessage(msg);

                boolean sentSuccessfully = false;
                int attempts = 0;

                while (!sentSuccessfully) {
                    attempts++;

                    // 2. ENVIAR TODOS OS FRAGMENTOS
                    for (int i = 0; i < fragments.size(); i++) {
                        MessageUDP frag = fragments.get(i);
                        byte[] data = frag.convertMessageToBytes();
                        DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, port);

                        socket.send(packet);
                    }

                    // --- LOGGING ---
                    String fragInfo = (fragments.size() > 1) ? " (" + fragments.size() + " fragments)" : "";

                    if (attempts > 1) {
                        UDPPrint.log("SND", msg, "Retransmission #" + attempts + fragInfo + " -> " + pkg.getToIp(), true);
                        //System.out.println("expecting: " + pAck.waitingForAckNumber);
                    } else {
                        UDPPrint.log("SND", msg, "To: " + pkg.getToIp() + fragInfo + " (Waiting ACK " + expectedAck + ")", false);
                    }

                    // 3. ESPERAR PELO ACK (com Timeout)
                    synchronized (pAck.lock) {
                        if (!pAck.ackReceived) {
                            pAck.lock.wait(TIMEOUT_MS);
                        }
                        if (pAck.ackReceived) {
                            sentSuccessfully = true;
                            // remove pending ack entry
                            pendingAcks.remove(pendingKey);
                        } else {
                            if (attempts > MAX_TIMEOUTS) {
                                UDPPrint.log("SND", msg, "Max number of retransmissions hit, dropping message...", true);
                                // remove pending ack entry
                                pendingAcks.remove(pendingKey);
                                sentSuccessfully = true; // stop retransmitting
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

    public MessageUDP getLastSentReply (String address) {
        return this.lastSentReply.get(address);
    }
    public void setLastSentReply (String address, MessageUDP reply) {
        this.lastSentReply.put(address, reply);
    }
}