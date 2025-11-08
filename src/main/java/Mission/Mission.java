package Mission;

import Message.MessageData;
import Utils.Point3D;

public class Mission {
    private final int missionId;
    private final int roverId;
    private final MissionType missionType;
    private final Point3D areaCoordinates;
    private final int areaRadius;
    private final int missionTime;
    private final int updateTime;
    // updates: the mission must define how and how often the rover reports back to the mothership

    public enum MissionType {
        EXPLORE,
        COLLECT_ROCKS,
        TEST_ATMOSPHERE,
    }

    private static int counter = 1;

    public Mission(int roverId, MissionType missionType, Point3D areaCoordinates, int areaRadius, int missionTime, int updateTime) {
        this.missionId = counter;
        counter++;
        this.roverId = roverId;
        this.missionType = missionType;
        this.areaCoordinates = areaCoordinates;
        this.areaRadius = areaRadius;
        this.missionTime = missionTime;
        this.updateTime = updateTime;
    }

    public int getMissionId() {
        return missionId;
    }
    public int  getRoverId() {
        return roverId;
    }
    public MissionType getMissionType() {
        return missionType;
    }
    public Point3D getAreaCoordinates() {
        return areaCoordinates;
    }
    public int getAreaRadius() {
        return areaRadius;
    }
    public int getMissionTime() {
        return missionTime;
    }
    public int getUpdateTime() {
        return updateTime;
    }

}
