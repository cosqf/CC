package Connection;

import Message.Message;
import Message.Package;
import Utils.UDPPrint;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;

public class MissionLinkSender implements Runnable {
    private final DatagramSocket socket;
    private final BlockingQueue<Package> outgoingQueue;

    // Variáveis partilhadas para controlo
    private static final int MAX_TIMEOUTS = 10;
    private static final int TIMEOUT_MS = 4000; // 4 segundos para retransmitir
    private final Object lock = new Object(); // Para sincronizar
    private volatile int waitingForAckNumber = -1; // Qual o ACK que estamos à espera?
    private volatile boolean ackReceived = false; // O ACK chegou?

    public MissionLinkSender(DatagramSocket socket, BlockingQueue<Package> outgoingQueue) {
        this.socket = socket;
        this.outgoingQueue = outgoingQueue;
    }

    // Método chamado pelo Receiver quando chega um ACK
    public void confirmAck(int ackNumber) {
        synchronized (lock) {
            // Se o ACK confirma o que estamos à espera (ou é maior/mais recente)
            if (ackNumber >= waitingForAckNumber) {
                ackReceived = true;
                lock.notify(); // Acorda a thread Sender!
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                // 1. Pegar na próxima mensagem da fila
                Package pkg = outgoingQueue.take();
                Message msg = pkg.getMessage();

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

                synchronized (lock) {
                    waitingForAckNumber = expectedAck;
                    ackReceived = false;
                }

                boolean sentSuccessfully = false;
                int attempts = 0;

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
                    synchronized (lock) {
                        if (!ackReceived) {
                            lock.wait(TIMEOUT_MS);
                        }
                        if (ackReceived) {
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}