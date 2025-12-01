package Message;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

// MENSAGEM UDP (Estende Message)
public class MessageUDP extends Message {

    // --- CAMPOS EXTRAS UDP ---
    private int sequenceNumber;
    private int ackNumber;

    // Fragmentação
    private int fragmentID;
    private int fragmentIndex;
    private int totalFragments;

    public MessageUDP(int seq, int ack, int fragID, int fragIdx, int totalFrags,
                      MessageDataTypes type, MessageData data) {
        super(type, data);
        this.sequenceNumber = seq;
        this.ackNumber = ack;
        this.fragmentID = fragID;
        this.fragmentIndex = fragIdx;
        this.totalFragments = totalFrags;
    }

    // Construtor privado para reconstrução
    private MessageUDP(int msgId, int seq, int ack, int fragID, int fragIdx, int totalFrags,
                       MessageDataTypes type, MessageData data) {
        super(msgId, type, data);
        this.sequenceNumber = seq;
        this.ackNumber = ack;
        this.fragmentID = fragID;
        this.fragmentIndex = fragIdx;
        this.totalFragments = totalFrags;
    }

    @Override
    public byte[] convertMessageToBytes() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // HEADER UDP
            out.write((byte) sequenceNumber);
            out.write((byte) messageId);
            out.write((byte) ackNumber);
            out.write((byte) messageDataType.ordinal());

            // Fragmentação
            out.write((byte) fragmentID);
            out.write((byte) fragmentIndex);
            out.write((byte) totalFragments);

            // PAYLOAD
            byte[] dataBytes = data.convertMessageDataToBytes();
            out.write(dataBytes);

            byte[] bytes = out.toByteArray();

            // Adicionar tamanho total
            byte[] bytesWithLength = new byte[bytes.length + 1];
            bytesWithLength[0] = (byte) bytesWithLength.length;
            System.arraycopy(bytes, 0, bytesWithLength, 1, bytes.length);

            return bytesWithLength;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao serializar MessageUDP", e);
        }
    }

    public static MessageUDP convertBytesToMessageUDP(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        // Verifica tamanho mínimo
        if (bytes.length < 8) {
            throw new RuntimeException("Pacote UDP demasiado pequeno.");
        }

        int totalLength = Byte.toUnsignedInt(buffer.get());
        int sequenceNumber = Byte.toUnsignedInt(buffer.get());
        int messageId = Byte.toUnsignedInt(buffer.get());
        int ackNumber = buffer.get();

        int messageDataTypeOrdinal = Byte.toUnsignedInt(buffer.get());

        int fragmentID = Byte.toUnsignedInt(buffer.get());
        int fragmentIndex = Byte.toUnsignedInt(buffer.get());
        int totalFragments = Byte.toUnsignedInt(buffer.get());

        MessageDataTypes dataType = MessageDataTypes.values()[messageDataTypeOrdinal];

        int headerSize = 8;
        int dataLen = totalLength - headerSize;

        if (dataLen < 0) {
            throw new RuntimeException("Pacote corrompido: Header maior que tamanho total.");
        }

        byte[] dataBytes = new byte[dataLen];
        buffer.get(dataBytes, 0, dataLen);

        // --- CORREÇÃO CRÍTICA AQUI ---
        MessageData mData;

        // Se for um fragmento (parte de um todo), NÃO tentamos converter
        // Guardamos apenas os bytes brutos dentro de um FragData
        if (totalFragments > 1) {
            mData = new FragData(dataBytes);
        } else {
            // Se for pacote inteiro (1/1), convertemos normalmente para Missão/Telemetria
            mData = Message.parseMessageData(dataType, dataBytes);
        }
        // -----------------------------

        return new MessageUDP(messageId, sequenceNumber, ackNumber,
                fragmentID, fragmentIndex, totalFragments,
                dataType, mData);
    }

    // Getters
    public int getSequenceNumber() { return sequenceNumber; }
    public int getAckNumber() { return ackNumber; }
    public int getFragmentID() { return fragmentID; }
    public int getFragmentIndex() { return fragmentIndex; }
    public int getTotalFragments() { return totalFragments; }

    public boolean isFragmented() { return totalFragments > 1; }

    // --- O SEGREDO PARA LIMPAR OS LOGS ---
    @Override
    public String toString() {
        return "UDP { " +
                "Seq=" + sequenceNumber +
                " | Ack=" + ackNumber +
                " | Type=" + messageDataType +
                " | Frag=" + (fragmentIndex + 1) + "/" + totalFragments +
                " | Data=" + data +
                " }";
    }
}