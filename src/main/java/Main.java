import Rover.Rover;

import java.io.IOException;

public class Main { // ground control
    static void main() throws IOException {
        System.out.println("Hello world");

        Mothership mothership = new Mothership(12345);
        mothership.Connect();

        Rover rover1 = new Rover(1);
        Rover rover2 = new Rover(2);
        Rover rover3 = new Rover(3);

        rover1.Connect();
        rover2.Connect();
        rover3.Connect();
    }
}
