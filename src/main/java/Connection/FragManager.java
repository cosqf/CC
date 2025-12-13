package Connection;

import Message.MessageUDP;
import Message.MessageData;
import Message.FragData;
import Message.Message;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FragManager {

    public static int MAX_FRAGMENT_SIZE = 1400;

    //divides a message into several smaller packets
    public static List<MessageUDP> fragmentMessage(MessageUDP originalMessage) {
        List<MessageUDP> fragments = new ArrayList<>();

        //convert the message to bytes
        byte[] fullPayload = originalMessage.getMessageData().convertMessageDataToBytes();
        int totalBytes = fullPayload.length;

        //if the message isn't big enough to fragment, return the list with one element
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

        //calculate how many fragments will be needed
        int totalFragments = (int) Math.ceil((double) totalBytes / MAX_FRAGMENT_SIZE);

        //generate a random ID for the group of fragments
        int fragmentID = (originalMessage.getMessageId() % 250) + 1;

        for (int i = 0; i < totalFragments; i++) {
            int start = i * MAX_FRAGMENT_SIZE;
            int end = Math.min(start + MAX_FRAGMENT_SIZE, totalBytes);

            byte[] chunk = Arrays.copyOfRange(fullPayload, start, end);
            FragData fragData = new FragData(chunk);

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

    //reconstructs the original message
    public static MessageUDP reassembleMessage(List<MessageUDP> fragments) {
        if (fragments == null || fragments.isEmpty()) return null;

        //sort by indexes in ascending order
        fragments.sort(Comparator.comparingInt(MessageUDP::getFragmentIndex));

        ByteArrayOutputStream fullDataStream = new ByteArrayOutputStream();
        try {
            for (MessageUDP frag : fragments) {
                if (frag.getMessageData() instanceof FragData) {
                    FragData fd = (FragData) frag.getMessageData();
                    fullDataStream.write(fd.getDataChunk());
                } else {
                    fullDataStream.write(frag.getMessageData().convertMessageDataToBytes());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        byte[] fullBytes = fullDataStream.toByteArray();

        //use the first fragment's header as a template
        MessageUDP template = fragments.getFirst();

        MessageData originalData = Message.parseMessageData(template.getMessageDataType(), fullBytes);

        return new MessageUDP(
                template.getSequenceNumber(),
                template.getAckNumber(),
                0, 0, 1,
                template.getMessageDataType(),
                originalData
        );
    }
}