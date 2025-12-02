package Message;

import java.io.Serializable;
import java.util.Arrays;


// Esta classe serve apenas para transportar um "pedaço" de bytes
// quando uma mensagem é demasiado grande e precisa de ser partida.
public class FragData implements MessageData,Serializable {
    private final byte[] dataChunk;

    public FragData(byte[] dataChunk) {
        this.dataChunk = dataChunk;
    }

    public byte[] getDataChunk() {
        return dataChunk;
    }

    @Override
    public byte[] convertMessageDataToBytes() {
        // Retorna os bytes crus deste fragmento
        return dataChunk;
    }

    // Método estático para reconstruir (necessário para a deserialização genérica)
    public static FragData convertBytesToMessageData(byte[] bytes) {
        return new FragData(bytes);
    }

    @Override
    public String toString() {
        // Converte os bytes para uma representação legível (ex: [10, 25, 0, ...])
        String content = Arrays.toString(dataChunk);

        // Limita o tamanho do print para não encher a consola se o fragmento for grande
        if (content.length() > 50) {
            content = content.substring(0, 47) + "...]";
        }

        return "FragData { Size=" + dataChunk.length + ", Bytes=" + content + " }";
    }
}