package Connection;

import java.util.Map;

public class NetworkConfig {
    public static final Map<ID, String> ID_TO_IP_MAP = Map.of(
            //ID.MOTHERSHIP_IP,        "10.0.0.1",
            ID.MOTHERSHIP_IP,        "localhost", // for local testing
            ID.TELEMETRY_STREAM_PORT, "5000",
            ID.MISSION_LINK_PORT,     "6000",
            ID.API_SERVER,            "7000",
            ID.GROUND_CONTROL,        "10.0.8.10"
            //ID.GROUND_CONTROL,        "localhost" // for local testing
    );

    public enum ID{
        MOTHERSHIP_IP,
        TELEMETRY_STREAM_PORT,
        MISSION_LINK_PORT,
        API_SERVER,
        GROUND_CONTROL
    }

    public String getIp (ID id) {
        return ID_TO_IP_MAP.get(id);
    }
}
