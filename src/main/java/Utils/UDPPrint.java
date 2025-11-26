package Utils;

import Message.Message;

public class UDPPrint {
    public static final String RESET = "\033[0m";
    public static final String RED = "\033[0;31m";      // Vermelho (Texto)
    public static final String RED_BG = "\033[41m\033[30m"; // Vermelho (Fundo)
    public static final String GREEN = "\033[1;32m";    // Verde Vivo (Bold)
    public static final String CYAN = "\033[0;36m";     // Ciano

    // Log Genérico (Envios Normais)
    public static void log(String source, Message msg, String extraInfo, boolean isRetransmission) {
        String color = isRetransmission ? RED_BG : CYAN;
        printLine(color, source, msg, extraInfo);
    }

    // Log de Erro/Descarte (VERMELHO)
    public static void logError(String source, Message msg, String extraInfo) {
        printLine(RED, source, msg, "[DUPLICATE] " + extraInfo);
    }

    // Log de Sucesso/Processamento (VERDE)
    public static void logSuccess(String source, Message msg, String extraInfo) {
        printLine(GREEN, source, msg, "[SUCCESS] " + extraInfo);
    }

    private static void printLine(String color, String source, Message msg, String extraInfo) {
        int len = 0;
        try {
            if (msg.getMessageData() != null) {
                len = msg.getMessageData().convertMessageDataToBytes().length;
            }
        } catch (Exception e) { len = 0; }

        String flag = (msg != null) ? msg.getMessageDataType().toString() : "UNKNOWN";
        int seq = (msg != null) ? msg.getSequenceNumber() : -1;
        int ack = (msg != null) ? msg.getAckNumber() : -1;

        System.out.printf("%s[ML] %-6s | Seq=%-4d Ack=%-4d Len=%-4d | %s%s%n",
                color, source, seq, ack, len, extraInfo, RESET
        );
    }
}


/*
*
*   TRATAR PARTE DAS FRAGMENTAÇÕES
*
*   NAVE MAE DA INFORMAÇOES DE FRAGMENTAÇÃO QUANDO CRIAR O ROVER
*
*   ROVER TRATA DE TODA A MANIPULAÇÃO DE FRAGMENTAÇAO
*
*   NAVE MAE RECEBE JÁ AS MENSAGENS FRAGMENTADAS
*
* */