package Message;


import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class Message  {
    private static int msgIds = 1;
    private final int sequenceNumber;
    private final int messageId;            // each "conversation" will share an id
    private final MessageDataTypes messageDataType;
    private final MessageData data;

    public enum MessageDataTypes {
        MISSION,
        REQUEST_MISSION,
        MISSION_UPDATE,
        ROVER_INIT,
        ROVER_TELEMETRY,
        ACK;
    }

    public int getSequenceNumber() {
        return this.sequenceNumber;
    }
    public int getMessageId() {
        return this.messageId;
    }
    public MessageDataTypes getMessageDataType() {
        return this.messageDataType;
    }
    public MessageData getMessageData() {
        return this.data;
    }

    public Message(int sequenceNumber, MessageDataTypes messageDataType, MessageData data) {
        this.sequenceNumber = sequenceNumber;
        this.messageId = msgIds++;
        this.messageDataType = messageDataType;
        this.data = data;
    }

    public Message(int sequenceNumber, int messageId, MessageDataTypes messageDataType, MessageData data) {
        this.sequenceNumber = sequenceNumber;
        this.messageId = messageId;
        this.messageDataType = messageDataType;
        this.data = data;
    }

    public byte[] convertMessageToBytes() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            out.write((byte) sequenceNumber);
            out.write((byte) messageId);
            out.write((byte) messageDataType.ordinal());

            byte[] dataBytes = data.convertMessageDataToBytes();
            for (Byte b : dataBytes) {
                out.write(b);
            }

            byte[] bytes = out.toByteArray();

            byte[] bytesWithLength = new byte[bytes.length+1];
            bytesWithLength[0] = (byte) out.size();
            System.arraycopy(bytes, 0, bytesWithLength, 1, bytes.length);

            return bytesWithLength;

        } catch (Exception e) {
            throw new RuntimeException("Error converting message to bytes", e);
        }
    }

    public static Message convertBytesToMessage(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        int totalLength = Byte.toUnsignedInt(buffer.get());

        int sequenceNumber = Byte.toUnsignedInt(buffer.get());
        int messageId = Byte.toUnsignedInt(buffer.get());
        int messageDataTypeOrdinal = Byte.toUnsignedInt(buffer.get());
        MessageDataTypes dataType = MessageDataTypes.values()[messageDataTypeOrdinal];

        int dataLen = Byte.toUnsignedInt(buffer.get());
        byte[] dataBytes = new byte[dataLen+1];
        dataBytes[0] = (byte) dataLen;
        for (int i = 1; i<dataLen+1; i++) dataBytes[i] = buffer.get();

        MessageData mData = null;
        switch (dataType) {
            case MISSION         -> mData = MissionMessage.convertBytesToMessageData(dataBytes);
            case MISSION_UPDATE  -> mData = UpdateMission.convertBytesToMessageData(dataBytes);
            case REQUEST_MISSION -> mData = RequestMission.convertBytesToMessageData(dataBytes);
            case ROVER_TELEMETRY -> mData = RoverTelemetryMessage.convertBytesToMessageData(dataBytes);
            case ROVER_INIT      -> mData = RoverInitMessage.convertBytesToMessageData(dataBytes);
            case ACK             -> mData = ACKMessage.convertBytesToMessageData(dataBytes);
        }
        return new Message (sequenceNumber, messageId, dataType, mData);
    }

    @Override
    public String toString() {
        return "Message { " +
                "sequenceNumber = " + sequenceNumber +
                ", messageId = " + messageId +
                ", messageDataType = " + messageDataType +
                ", data = " + data.toString() +
                '}';
    }
}
