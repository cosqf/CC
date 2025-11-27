package Message;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface MessageData {
    byte[] convertMessageDataToBytes();
    String toString();
    /*
    convertMessageDataToBytes[0] should always be the length of the array

    implementations also should have their own "public static ClassName convertBytesToMessageData(byte[] bytes)"
    that works as the opposite of convertMessageDataToBytes.
     */

    public static byte[] addSizeToArray(byte[] dataContentBytes) {
        try (ByteArrayOutputStream finalBos = new ByteArrayOutputStream();
             DataOutputStream finalDos = new DataOutputStream(finalBos)) {

            int contentSize = dataContentBytes.length;
            finalDos.writeInt(contentSize);
            finalDos.write(dataContentBytes);
            return finalBos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Error adding the size to the front of the array", e);
        }
    }
}
