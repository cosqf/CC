import Message.Message;
import Message.RoverTelemetryMessage;
import Mission.Mission;
import Rover.Rover;
import Rover.PhysicalState;
import Utils.Point3D;
import Message.*;

import java.io.IOException;
import java.util.ArrayList;

public class Main { // ground control
    static void main() throws IOException {
        //testing message conversions
        ArrayList <String> inventory = new ArrayList<>();
        inventory.add ("Rock");
        inventory.add ("Paper");
        inventory.add ("Scissors");

        ArrayList <PhysicalState> ps = new ArrayList<>();
        ps.add(new PhysicalState("camara", 100));
        ps.add(new PhysicalState("roda", 70));
        ps.add(new PhysicalState("braco", 30));
        RoverTelemetryMessage m = new RoverTelemetryMessage(1,new Point3D(1,2,3),
                Rover.MissionState.CHARGING, 10, inventory, ps);

        Message message = new Message(1, Message.MessageDataTypes.ROVER_TELEMETRY, m);
        byte[] msgbytes = message.convertMessageToBytes();
        Message msg = Message.convertBytesToMessage(msgbytes);

        UpdateMission updateMission = new UpdateMission(1, 2, 50, m.convertMessageDataToBytes());
        Message updateMessage = new Message(1, Message.MessageDataTypes.MISSION_UPDATE, updateMission);
        Message upmsg = Message.convertBytesToMessage(updateMessage.convertMessageToBytes());

        System.out.println(upmsg.toString());
        /*
        RequestMission reqM = new RequestMission(1);
        Message message = new Message(1,Message.MessageDataTypes.REQUEST_MISSION, reqM);
        Message msg = Message.convertBytesToMessage( message.convertMessageToBytes());
        */

    }
}
