package Message;


import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;


public class Message {
    private static int msgIds = 1;

    // --- HEADER ---
    private final int sequenceNumber;
    private final int messageId;
    private final int ackNumber; // <--- ESTE CAMPO É ESSENCIAL
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
    public Message(int sequenceNumber, MessageDataTypes messageDataType, MessageData data) {
        this(sequenceNumber, msgIds++, -1, messageDataType, data);
    }

    // 2. Construtor com ACK Explícito (Para Piggybacking)
    public Message(int sequenceNumber, int ackNumber, MessageDataTypes messageDataType, MessageData data) {
        this(sequenceNumber, msgIds++, ackNumber, messageDataType, data);
    }

    // 3. Construtor Mestre (Usado na conversão de bytes)
    public Message(int sequenceNumber, int messageId, int ackNumber, MessageDataTypes messageDataType, MessageData data) {
        this.sequenceNumber = sequenceNumber;
        this.messageId = messageId;
        this.ackNumber = ackNumber;
        this.messageDataType = messageDataType;
        this.data = data;
    }

    // --- SERIALIZAÇÃO ---
    public byte[] convertMessageToBytes() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            out.write((byte) sequenceNumber);
            out.write((byte) messageId);
            out.write((byte) ackNumber); // <--- Escrever o ACK no cabeçalho
            out.write((byte) messageDataType.ordinal());

            byte[] dataBytes = data.convertMessageDataToBytes();
            for (byte b : dataBytes) { // O teu loop original (pode ser otimizado com out.write(bytes), mas mantive igual)
                out.write(b);
            }

            byte[] bytes = out.toByteArray();

            // Adicionar tamanho total
            byte[] bytesWithLength = new byte[bytes.length + 1];
            bytesWithLength[0] = (byte) out.size();
            System.arraycopy(bytes, 0, bytesWithLength, 1, bytes.length);

            return bytesWithLength;

        } catch (Exception e) {
            throw new RuntimeException("Error converting message to bytes", e);
        }
    }

    // --- DESERIALIZAÇÃO ---
    public static Message convertBytesToMessage(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        int totalLength = Byte.toUnsignedInt(buffer.get()); // Consome o tamanho

        int sequenceNumber = Byte.toUnsignedInt(buffer.get());
        int messageId = Byte.toUnsignedInt(buffer.get());
        int ackNumber = buffer.get(); // <--- Ler o ACK (como byte assinado para permitir -1)

        int messageDataTypeOrdinal = Byte.toUnsignedInt(buffer.get());
        MessageDataTypes dataType = MessageDataTypes.values()[messageDataTypeOrdinal];

        // Calcular tamanho dos dados restantes
        int headerSize = 4; // seq + id + ack + type
        int dataLen = totalLength - headerSize;

        // NOTA: O teu código original lia o tamanho dos dados a seguir.
        // Se o teu convertMessageDataToBytes inclui o tamanho no início, o código abaixo funciona.
        // Caso contrário, temos de ajustar. Assumindo que funciona como antes:

        // Se os dados tiverem o tamanho no primeiro byte (como tinhas antes):
        // Vamos usar a lógica de ler byte a byte como tinhas:
        int payloadSize = Byte.toUnsignedInt(buffer.get());
        byte[] dataBytes = new byte[payloadSize + 1];
        dataBytes[0] = (byte) payloadSize;
        for (int i = 1; i < payloadSize + 1; i++) dataBytes[i] = buffer.get();

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
                // Opcional: tratar caso desconhecido ou deixar mData como null
                break;
        }
        return new Message(sequenceNumber, messageId, ackNumber, dataType, mData);
    }

    @Override
    public String toString() {
        return "Message { Seq=" + sequenceNumber + ", Ack=" + ackNumber + ", Type=" + messageDataType + ", Data=" + data + " }";
    }
}
