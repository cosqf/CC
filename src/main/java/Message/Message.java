package Message;


import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;


public class Message {
    private static int msgIds = 1;

    // --- HEADER ---
    private final int sequenceNumber;
    private final int messageId;
    private final int ackNumber; // <--- ESTE CAMPO É ESSENCIAL
    private final boolean isFragmented;
    private final MessageDataTypes messageDataType;

    // --- PAYLOAD ---
    private final MessageData data;

    public enum MessageDataTypes {
        MISSION,
        REQUEST_MISSION,
        MISSION_UPDATE,
        ROVER_INIT,
        ROVER_TELEMETRY,
        ACK;
    }

    // --- GETTERS ---
    public int getSequenceNumber() { return this.sequenceNumber; }
    public int getMessageId() { return this.messageId; }
    public int getAckNumber() { return this.ackNumber; } // Getter do ACK
    public MessageDataTypes getMessageDataType() { return this.messageDataType; }
    public MessageData getMessageData() { return this.data; }

    // --- CONSTRUTORES ---

    // 1. Construtor Simples (Sem ACK, usa -1 por defeito)
    public Message(int sequenceNumber, MessageDataTypes messageDataType, boolean isFragmented, MessageData data) {
        this(sequenceNumber, msgIds++, -1, messageDataType, isFragmented, data);
    }

    // unless specified, message is assumed to not be fragmented
    public Message(int sequenceNumber, MessageDataTypes messageDataType, MessageData data) {
        this(sequenceNumber, msgIds++, -1, messageDataType, false, data);
    }

    // 2. Construtor com ACK Explícito (Para Piggybacking)
    public Message(int sequenceNumber, int ackNumber, MessageDataTypes messageDataType, boolean isFragmented, MessageData data) {
        this(sequenceNumber, msgIds++, ackNumber, messageDataType, isFragmented, data);
    }
    public Message(int sequenceNumber, int ackNumber, MessageDataTypes messageDataType, MessageData data) {
        this(sequenceNumber, msgIds++, ackNumber, messageDataType, false, data);
    }

    // 3. Construtor Mestre (Usado na conversão de bytes)
    public Message(int sequenceNumber, int messageId, int ackNumber, MessageDataTypes messageDataType, boolean isFragmented, MessageData data) {
        this.sequenceNumber = sequenceNumber;
        this.messageId = messageId;
        this.ackNumber = ackNumber;
        this.messageDataType = messageDataType;
        this.isFragmented = isFragmented;
        this.data = data;
    }

    // --- SERIALIZAÇÃO ---
    public byte[] convertMessageToBytes() {
        byte[] contentBytes;
        try (ByteArrayOutputStream contentOutBytes = new ByteArrayOutputStream();
             DataOutputStream contentOut = new DataOutputStream(contentOutBytes)) {

            contentOut.writeInt(sequenceNumber);
            contentOut.writeInt(messageId);
            contentOut.write(ackNumber);
            contentOut.writeInt(messageDataType.ordinal());
            contentOut.write(isFragmented ? 1 : 0);

            byte[] dataBytes = data.convertMessageDataToBytes();
            contentOut.write(dataBytes);

            contentOut.flush();
            contentBytes = contentOutBytes.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Error serializing content.", e);
        }
        return MessageData.addSizeToArray(contentBytes);
    }

    // --- DESERIALIZAÇÃO ---
    public static Message convertBytesToMessage(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        int totalLength = buffer.getInt();

        int sequenceNumber = buffer.getInt();
        int messageId = buffer.getInt();
        int ackNumber = buffer.get();
        int messageDataTypeOrdinal = buffer.getInt();
        boolean isFragmented = buffer.get() != 0;

        MessageDataTypes dataType = MessageDataTypes.values()[messageDataTypeOrdinal];

        int dataPayloadSize = buffer.getInt();
        byte[] data = new byte[dataPayloadSize];
        buffer.get(data, 0, dataPayloadSize);
        
        byte[] dataBytes = MessageData.addSizeToArray(data);

        MessageData mData = null;
        switch (dataType) {
            case MISSION:
                mData = MissionMessage.convertBytesToMessageData(dataBytes);
                break;
            case MISSION_UPDATE:
                mData = UpdateMission.convertBytesToMessageData(dataBytes);
                break;
            case REQUEST_MISSION:
                mData = RequestMission.convertBytesToMessageData(dataBytes);
                break;
            case ROVER_TELEMETRY:
                mData = RoverTelemetryMessage.convertBytesToMessageData(dataBytes);
                break;
            case ROVER_INIT:
                mData = RoverInitMessage.convertBytesToMessageData(dataBytes);
                break;
            case ACK:
                mData = ACKMessage.convertBytesToMessageData(dataBytes);
                break;
            default:
                // Handle unknown message types
                break;
        }

        return new Message(sequenceNumber, messageId, ackNumber, dataType, isFragmented, mData);
    }

    @Override
    public String toString() {
        return "Message { Seq=" + sequenceNumber + ", Ack=" + ackNumber + ", Type=" + messageDataType + ", Data=" + data + " }";
    }
}
