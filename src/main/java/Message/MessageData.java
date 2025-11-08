package Message;

public interface MessageData {
    byte[] convertMessageDataToBytes();
    /*
    convertMessageDataToBytes[0] should always be the length of the array

    implementations also should have their own "public static ClassName convertBytesToMessageData(byte[] bytes)"
    that works as the opposite of convertMessageDataToBytes.
     */
}
