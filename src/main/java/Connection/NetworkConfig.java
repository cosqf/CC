package Connection;

import java.util.Map;

public class NetworkConfig {
    public static final Map<ID, String> ID_TO_IP_MAP = Map.of(
            ID.MOTHERSHIP_IP, "10.0.1.10",
            ID.TELEMETRY_STREAM_PORT, "5000",
            ID.MISSION_LINK_PORT, "600",
            ID.GROUND_CONTROL, "10.0.11.11",
            ID.ROVER_1, "10.0.0.10",
            ID.ROVER_2, "10.0.2.10",
            ID.ROVER_3, "10.0.4.10",
            ID.ROVER_4, "10.0.5.10",
            ID.ROVER_5, "10.0.3.10"
    );

    public enum ID{
        MOTHERSHIP_IP,
        TELEMETRY_STREAM_PORT,
        MISSION_LINK_PORT,
        GROUND_CONTROL,
        ROVER_1,
        ROVER_2,
        ROVER_3,
        ROVER_4,
        ROVER_5,
    }

    public String getIp (ID id) {
        return ID_TO_IP_MAP.get(id);
    }
}
