package Connection;

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

    // shared control variables
    private static final int MAX_TIMEOUTS = 5;
    private static final int TIMEOUT_MS = 5000; // 5 segundos para retransmitir
    private final ConcurrentHashMap<String, MessageUDP> lastSentReply = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PendingAck> pendingAcks = new ConcurrentHashMap<>();

    private volatile boolean stopCurrentTransmission = false;
    private volatile PendingAck currentPendingAck = null;

    private static class PendingAck {
        volatile int waitingForAckNumber;
        final Object lock = new Object();
        volatile boolean ackReceived = false;
        PendingAck(int expected) { this.waitingForAckNumber = expected; }
    }
    private boolean running = true;

    public MissionLinkSender(DatagramSocket socket, BlockingQueue<Package> outgoingQueue) {
        this.socket = socket;
        this.outgoingQueue = outgoingQueue;
    }
    public void sendMessage(MessageUDP msg, String ip, int port) {
        try {
            Package pck = new Package(ip, port, msg);
            outgoingQueue.put(pck);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void confirmAck(int ackNumber, String address) {
        PendingAck p = pendingAcks.get(address);
        if (p == null) return;
        System.out.println("RECEIVED ACK : " + ackNumber + " AND WAS WAITING FOR " + p.waitingForAckNumber);
        synchronized (p.lock) {
            if (ackNumber >= p.waitingForAckNumber) {
                p.ackReceived = true;
                p.lock.notifyAll();
            }
        }
    }

    public void cancelCurrentTransmission() {
        stopCurrentTransmission = true;
        PendingAck p = currentPendingAck;
        if (p != null) {
            synchronized (p.lock) {
                p.ackReceived = true; //pretend an ACK was received to leave the waiting phase
                p.lock.notifyAll();
            }
        }
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Package pkg = outgoingQueue.take();
                MessageUDP msg = pkg.getMessage();

                stopCurrentTransmission = false;

                InetAddress ipAddress = InetAddress.getByName(pkg.getToIp());
                int port = pkg.getToPort();

                // doesn't wait for confirmation if it's a pure ACK
                if (msg.getMessageDataType() == MessageUDP.MessageDataTypes.ACK) {
                    byte[] data = msg.convertMessageToBytes();
                    DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, port);
                    socket.send(packet);
                    continue;
                }

                //calculate the expected ACK
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
                this.currentPendingAck = pAck;

                String pendingKey = ipAddress.getHostAddress() + ":" + port;
                pendingAcks.put(pendingKey, pAck);

                List<MessageUDP> fragments = FragManager.fragmentMessage(msg);

                boolean sentSuccessfully = false;
                int attempts = 0;

                //retransmission loop
                while (!sentSuccessfully && !stopCurrentTransmission) {
                    attempts++;

                    for (MessageUDP frag : fragments) {
                        byte[] data = frag.convertMessageToBytes();
                        DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, port);

                        socket.send(packet);
                    }

                    String fragInfo = (fragments.size() > 1) ? " (" + fragments.size() + " fragments)" : "";

                    if (attempts > 1) {
                        UDPPrint.log("SND", msg, "Retransmission #" + attempts + fragInfo + " -> " + pkg.getToIp(), true);
                        //System.out.println("expecting: " + pAck.waitingForAckNumber);
                    } else {
                        UDPPrint.log("SND", msg, "To: " + pkg.getToIp() + fragInfo + " (Waiting ACK " + expectedAck + ")", false);
                        System.out.println(msg);
                    }

                    synchronized (pAck.lock) {
                        if (!pAck.ackReceived && !stopCurrentTransmission) {
                            pAck.lock.wait(TIMEOUT_MS);
                        }

                        if (pAck.ackReceived || stopCurrentTransmission) {
                            sentSuccessfully = true;
                            pendingAcks.remove(pendingKey);
                        } else {
                            if (attempts > MAX_TIMEOUTS) {
                                UDPPrint.log("SND", msg, "Max number of retransmissions hit, dropping message...", true);
                                pendingAcks.remove(pendingKey);
                                sentSuccessfully = true;
                            }
                        }
                    }
                }

                this.currentPendingAck = null;

            } catch (IOException | InterruptedException e) {
                if (running) {
                    System.out.println("[ML SENDER] Connection closed or lost.");
                    e.printStackTrace();
                }
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