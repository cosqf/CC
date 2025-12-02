package Connection;

import Message.MessageUDP;
import Message.MessageData;
import Message.FragData; // Nome alterado de FragmentData para FragData
import Message.Message;      // Para aceder ao parseMessageData
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FragManager { // Nome da classe alterado

    // Tamanho máximo do payload de cada fragmento (Bytes)
    // 512 é seguro para UDP na internet.
    public static int MAX_FRAGMENT_SIZE = 1500;

    /**
     * 1. CORTAR: Divide uma mensagem grande em vários pacotes pequenos
     */
    public static List<MessageUDP> fragmentMessage(MessageUDP originalMessage) {
        List<MessageUDP> fragments = new ArrayList<>();

        // 1. Converter o Payload original (ex: MissionMessage) em bytes brutos
        byte[] fullPayload = originalMessage.getMessageData().convertMessageDataToBytes();
        int totalBytes = fullPayload.length;

        // --- DEBUG PRINT 1: Ver o tamanho real vs o limite ---
        //System.out.println("[FragManager] Checking payload: " + totalBytes + " bytes | Limit: " + MAX_FRAGMENT_SIZE);
        // ----------------------------------------------------

        // 2. Se a mensagem for pequena, não fragmenta (retorna lista com 1 elemento)
        if (totalBytes <= MAX_FRAGMENT_SIZE) {
            MessageUDP single = new MessageUDP(
                    originalMessage.getSequenceNumber(),
                    originalMessage.getAckNumber(),
                    0, 0, 1, // FragID=0, Idx=0, Total=1 (Pacote Único)
                    originalMessage.getMessageDataType(),
                    originalMessage.getMessageData()
            );
            fragments.add(single);
            return fragments;
        }

        // --- LÓGICA DE CORTE ---

        // Calcular quantos pedaços vamos precisar
        int totalFragments = (int) Math.ceil((double) totalBytes / MAX_FRAGMENT_SIZE);

        // Gerar um ID aleatório para este grupo de fragmentos (1-254)
        int fragmentID = (originalMessage.getMessageId() % 250) + 1;

        // --- DEBUG PRINT 2: Confirmar o corte ---
        //System.out.println("[FragManager] ✂️ Splitting " + totalBytes + " bytes into " + totalFragments + " fragments (ID " + fragmentID + ")");
        // ----------------------------------------

        for (int i = 0; i < totalFragments; i++) {
            // Calcular início e fim da fatia
            int start = i * MAX_FRAGMENT_SIZE;
            int end = Math.min(start + MAX_FRAGMENT_SIZE, totalBytes);

            // Cortar a fatia do array original
            byte[] chunk = Arrays.copyOfRange(fullPayload, start, end);

            // Criar o payload "burro" de bytes (FragData)
            FragData fragData = new FragData(chunk);

            // Criar a mensagem UDP com o cabeçalho de fragmentação
            MessageUDP fragmentMsg = new MessageUDP(
                    originalMessage.getSequenceNumber(),
                    originalMessage.getAckNumber(),
                    fragmentID,
                    i,
                    totalFragments,
                    originalMessage.getMessageDataType(),
                    fragData
            );

            fragments.add(fragmentMsg);
        }
        return fragments;
    }

    /**
     * 2. COLAR: Reconstrói a mensagem original a partir das peças
     */
    public static MessageUDP reassembleMessage(List<MessageUDP> fragments) {
        if (fragments == null || fragments.isEmpty()) return null;

        // 1. Ordenar por índice (garantir que colamos 0, 1, 2...)
        fragments.sort(Comparator.comparingInt(MessageUDP::getFragmentIndex));

        // 2. Juntar os bytes
        ByteArrayOutputStream fullDataStream = new ByteArrayOutputStream();
        try {
            for (MessageUDP frag : fragments) {
                // O payload tem de ser FragData
                if (frag.getMessageData() instanceof FragData) {
                    FragData fd = (FragData) frag.getMessageData();
                    fullDataStream.write(fd.getDataChunk());
                } else {
                    // Fallback de segurança
                    fullDataStream.write(frag.getMessageData().convertMessageDataToBytes());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        byte[] fullBytes = fullDataStream.toByteArray();

        // 3. Usar o cabeçalho do primeiro fragmento como molde
        MessageUDP template = fragments.getFirst();

        // 4. Converter os bytes completos no Objeto original (ex: MissionMessage)
        MessageData originalData = Message.parseMessageData(template.getMessageDataType(), fullBytes);

        // 5. Retornar mensagem completa e limpa
        return new MessageUDP(
                template.getSequenceNumber(),
                template.getAckNumber(),
                0, 0, 1, // Reset à fragmentação
                template.getMessageDataType(),
                originalData
        );
    }
}