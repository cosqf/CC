package Message;

import Mission.Mission;
import Utils.Point3D;

public class MissionMessage implements MessageData{
    private final int missionId;
    private final int roverId;
    private final Mission.MissionType missionType;
    private final Point3D areaCoordinates;
    private final int areaRadius;
    private final int missionTime;
    private final int updateTime;

    public MissionMessage(int missionId, int roverId, Mission.MissionType missionType, Point3D areaCoordinates, int areaRadius, int missionTime, int updateTime) {
        this.missionId = missionId;
        this.roverId = roverId;
        this.missionType = missionType;
        this.areaCoordinates = areaCoordinates;
        this.areaRadius = areaRadius;
        this.missionTime = missionTime;
        this.updateTime = updateTime;
    }
    public MissionMessage (Mission mission) {
        this.missionId = mission.getMissionId();
        this.roverId = mission.getRoverId();
        this.missionType = mission.getMissionType();
        this.areaCoordinates= mission.getAreaCoordinates();
        this.areaRadius= mission.getAreaRadius();
        this.missionTime = mission.getMissionTime();
        this.updateTime = mission.getUpdateTime();
    }

    @Override
    public byte[] convertMessageDataToBytes() {
        byte[] bytes  = new byte[10];
        bytes[0] = (byte) 9;
        bytes[1] = (byte) missionId;
        bytes[2] = (byte) roverId;
        bytes[3] = (byte) this.missionType.ordinal();
        bytes[4] = (byte) this.areaCoordinates.x;
        bytes[5] = (byte) this.areaCoordinates.y;
        bytes[6] = (byte) this.areaCoordinates.z;
        bytes[7] = (byte) this.areaRadius;
        bytes[8] = (byte) this.missionTime;
        bytes[9] = (byte) this.updateTime;

        return bytes;
    }

    public static MissionMessage convertBytesToMessageData(byte[] bytes) {
        int missionId = bytes[1];
        int roverId = bytes[2];
        Mission.MissionType missionType = Mission.MissionType.values()[bytes[3]];
        int x = bytes[4];
        int y = bytes[5];
        int z = bytes[6];
        Point3D coordinates = new Point3D(x,y,z);
        int areaRadius = bytes[7];
        int missionTime = bytes[8];
        int updateTime = bytes[9];

        return new MissionMessage(missionId, roverId, missionType, coordinates, areaRadius, missionTime, updateTime);
    }
}
