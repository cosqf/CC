package Message;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Message {
    protected static int msgIds = 1;

    protected int messageId;
    protected MessageDataTypes messageDataType;
    protected MessageData data;

    public enum MessageDataTypes {
        MISSION,
        REQUEST_MISSION,
        MISSION_UPDATE,
        ROVER_INIT,
        ROVER_TELEMETRY,
        ACK;
    }

    public Message(MessageDataTypes type, MessageData data) {
        this.messageId = msgIds++;
        this.messageDataType = type;
        this.data = data;
    }

    public Message(Message msg) {
        this.messageId = msg.messageId;
        this.messageDataType = msg.messageDataType;
        this.data = msg.data;
    }

    public Message(int messageId, MessageDataTypes type, MessageData data) {
        this.messageId = messageId;
        this.messageDataType = type;
        this.data = data;
    }

    public byte[] convertMessageToBytes() {
        byte[] contentBytes;
        try (ByteArrayOutputStream contentOutBytes = new ByteArrayOutputStream();
             DataOutputStream contentOut = new DataOutputStream(contentOutBytes)) {

            contentOut.writeInt(messageId);
            contentOut.writeInt(messageDataType.ordinal());

            byte[] dataBytes = data.convertMessageDataToBytes();
            contentOut.write(dataBytes);

            contentOut.flush();
            contentBytes = contentOutBytes.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Error serializing content.", e);
        }
        return MessageData.addSizeToArray(contentBytes);
    }

    public static Message convertBytesToMessage(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        int totalLength = buffer.getInt();

        int messageId = buffer.getInt();
        int messageDataTypeOrdinal = buffer.getInt();

        MessageDataTypes dataType = MessageDataTypes.values()[messageDataTypeOrdinal];

        int dataPayloadSize = buffer.getInt();
        byte[] data = new byte[dataPayloadSize];
        buffer.get(data, 0, dataPayloadSize);

        byte[] dataBytes = MessageData.addSizeToArray(data);

        MessageData mData = parseMessageData(dataType, dataBytes);

        return new Message(messageId, dataType, mData);
    }

    public static MessageData parseMessageData(MessageDataTypes type, byte[] dataBytes) {
        if (dataBytes == null) return null;
        return switch (type) {
            case MISSION -> MissionMessage.convertBytesToMessageData(dataBytes);
            case REQUEST_MISSION -> RequestMission.convertBytesToMessageData(dataBytes);
            case MISSION_UPDATE -> UpdateMission.convertBytesToMessageData(dataBytes);
            case ROVER_INIT -> RoverInitMessage.convertBytesToMessageData(dataBytes);
            case ROVER_TELEMETRY -> RoverTelemetryMessage.convertBytesToMessageData(dataBytes);
            case ACK -> ACKMessage.convertBytesToMessageData(dataBytes);
            default -> null;
        };
    }

    public int getMessageId() { return messageId; }
    public MessageDataTypes getMessageDataType() { return messageDataType; }
    public MessageData getMessageData() { return data; }
}
