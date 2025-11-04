import Rover.Rover;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Mothership { // controller

    ServerSocket ss;

    public Mothership(int port) throws IOException {
        this.ss = new ServerSocket(port);
    }

    public void Connect() throws IOException {
        while (true) {
            Socket socket = ss.accept();
            Thread worker = new Thread(new Connection(socket));
            worker.start();
        }
    }
}
