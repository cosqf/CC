package Connection;

import Message.Message;
import Message.Package;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;



public class MissionLinkSender implements Runnable {
    private final DatagramSocket socket;
    private final BlockingQueue<Package> outgoingQueue;

    // Variáveis partilhadas para controlo
    private static final int TIMEOUT_MS = 4000; // 2 segundos para retransmitir
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

                // OTIMIZAÇÃO: Resolver o endereço APENAS UMA VEZ antes de tentar enviar
                InetAddress ipAddress = InetAddress.getByName(pkg.getToIp());
                int port = pkg.getToPort();

                // Configurar o que estamos à espera (Seq + Tamanho do Payload)
                int payloadSize = (msg.getMessageData() != null) ? msg.getMessageData().convertMessageDataToBytes().length : 0;
                int expectedAck = msg.getSequenceNumber() + (payloadSize > 0 ? payloadSize : 1);

                synchronized (lock) {
                    waitingForAckNumber = expectedAck;
                    ackReceived = false;
                }

                boolean sentSuccessfully = false;
                int attempts = 0; // Só para debug visual

                while (!sentSuccessfully) {
                    attempts++;

                    // 2. ENVIAR (ou Reenviar)
                    byte[] data = msg.convertMessageToBytes();
                    // Usamos o IP já resolvido fora do loop
                    DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, port);

                    socket.send(packet);

                    if (attempts > 1) System.out.println("[ML-Sender] RETRANSMITINDO (Tentativa " + attempts + ")...");
                    else
                        System.out.println("[ML-Sender] Enviado Seq: " + msg.getSequenceNumber() + ". À espera de ACK: " + expectedAck);

                    // 3. ESPERAR PELO ACK (com Timeout)
                    synchronized (lock) {
                        if (!ackReceived) {
                            lock.wait(TIMEOUT_MS);
                        }

                        if (ackReceived) {
                            System.out.println("[ML-Sender] ACK recebido! Avançando.");
                            sentSuccessfully = true;
                        } else {
                            System.out.println("[ML-Sender] Timeout! ACK não chegou.");
                            // O loop repete-se
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}