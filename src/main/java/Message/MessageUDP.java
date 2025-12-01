package Message;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

// MENSAGEM UDP (Estende Message)
public class MessageUDP extends Message {

    // --- CAMPOS EXTRAS UDP ---
    private final int sequenceNumber;
    private final int ackNumber;

    // Fragmentação
    private final int fragmentID;
    private final int fragmentIndex;
    private final int totalFragments;

    public MessageUDP(int seq, int ack, int fragID, int fragIdx, int totalFrags,
                      MessageDataTypes type, MessageData data) {
        super(type, data);
        this.sequenceNumber = seq;
        this.ackNumber = ack;
        this.fragmentID = fragID;
        this.fragmentIndex = fragIdx;
        this.totalFragments = totalFrags;
    }
    public MessageUDP(int seq, int ack, int fragID, int fragIdx, int totalFrags,
                      Message message) {
        super(message);
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
        byte[] contentBytes;
        try (ByteArrayOutputStream contentOutBytes = new ByteArrayOutputStream();
             DataOutputStream contentOut = new DataOutputStream(contentOutBytes)) {

            // HEADER UDP
            contentOut.writeInt(sequenceNumber);
            contentOut.writeInt(messageId);
            contentOut.writeInt(ackNumber);
            contentOut.writeInt(messageDataType.ordinal());

            // Fragmentação
            contentOut.writeInt(fragmentID);
            contentOut.writeInt(fragmentIndex);
            contentOut.writeInt(totalFragments);

            // PAYLOAD
            byte[] dataBytes = data.convertMessageDataToBytes();
            contentOut.write(dataBytes);

            contentOut.flush();
            contentBytes = contentOutBytes.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error serializing content.", e);
        }
        return MessageData.addSizeToArray(contentBytes);
    }

    public static MessageUDP convertBytesToMessageUDP(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        int totalLength = buffer.getInt();
        int sequenceNumber = buffer.getInt();
        int messageId = buffer.getInt();
        int ackNumber = buffer.getInt();
        int messageDataTypeOrdinal = buffer.getInt();

        int fragmentID = buffer.getInt();
        int fragmentIndex = buffer.getInt();
        int totalFragments = buffer.getInt();

        MessageDataTypes dataType = MessageDataTypes.values()[messageDataTypeOrdinal];

        int headerSize = 7 * 4; // int size is 4 bytes, and "total length" doesn't include itself
        int dataLen = totalLength - headerSize;

        if (dataLen < 0) {
            throw new RuntimeException("Pacote corrompido: Header maior que tamanho total.");
        }

        byte[] dataBytes = new byte[dataLen];
        buffer.get(dataBytes, 0, dataLen);

        MessageData mData;
        // Se for um fragmento (parte de um todo), NÃO tentamos converter
        // Guardamos apenas os bytes brutos dentro de um FragData
        if (totalFragments > 1)  mData = new FragData(dataBytes);
        else {
            // Se for pacote inteiro (1/1), convertemos normalmente para Missão/Telemetria
            mData = parseMessageData(dataType, dataBytes);
        }

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