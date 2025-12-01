package Mission;

import Utils.Point3D;


public class Mission implements Comparable<Mission> {
    private final int missionId;
    private int roverId;
    private final MissionType missionType;
    private final Point3D areaCoordinates;
    private final int areaRadius;
    private final int missionTime;
    private final int updateTime;
    private final boolean isUrgent;
    private boolean isCompleted = false;
    private static final java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(1);


    public enum MissionType {
        EXPLORE,
        COLLECT_ROCKS,
        TEST_ATMOSPHERE,
    }

    public Mission(int roverId, MissionType missionType, Point3D areaCoordinates, int areaRadius, int missionTime, int updateTime, boolean isUrgent) {
        this.missionId = counter.getAndIncrement();
        this.roverId = roverId;
        this.missionType = missionType;
        this.areaCoordinates = areaCoordinates;
        this.areaRadius = areaRadius;
        this.missionTime = missionTime;
        this.updateTime = updateTime;
        this.isUrgent = isUrgent;
    }
    public Mission(int missionId, int roverId, MissionType missionType, Point3D areaCoordinates, int areaRadius, int missionTime, int updateTime, boolean isUrgent, boolean isCompleted) {
        this.missionId = missionId;
        this.roverId = roverId;
        this.missionType = missionType;
        this.areaCoordinates = areaCoordinates;
        this.areaRadius = areaRadius;
        this.missionTime = missionTime;
        this.updateTime = updateTime;
        this.isUrgent = isUrgent;
        this.isCompleted = isCompleted;
    }

    public int getMissionId() {
        return missionId;
    }
    public int getRoverId() {
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
    public boolean isUrgent() {
        return isUrgent;
    }
    public boolean isCompleted() {
        return this.isCompleted;
    }

    public void setCompleted() {
        this.isCompleted = true;
    }
    public void setRoverId(int roverId) {this.roverId = roverId;}

    @Override
    public int compareTo(Mission mission) {
        if (this.isUrgent == mission.isUrgent) return 0;
        if (this.isUrgent) return -1;
        return 1;
    }
    @Override
    public String toString() {
        return "Mission{" +
                "missionId=" + missionId +
                ", roverId=" + roverId +
                ", missionType=" + missionType +
                ", areaCoordinates=" + areaCoordinates +
                ", areaRadius=" + areaRadius +
                ", missionTime=" + missionTime +
                ", updateTime=" + updateTime +
                ", isUrgent=" + isUrgent +
                ", isCompleted=" + isCompleted +
                '}';
    }
    public String toStringForAPI() {
        final int WIDTH = 80;

        String SEPARATOR_LINE = "+" + "-".repeat(WIDTH - 2) + "+\n";

        String mission = String.format("| Mission %d:%-" + (WIDTH - 13 - ((int) Math.log10(Math.abs(this.missionId)) + 1)) + "s |\n", this.missionId, "");
        String rover = String.format("| Rover -> %d%-" + (WIDTH - 13 - String.valueOf(this.roverId).length()) + "s |\n", this.roverId, "");
        String mtype = String.format("| Mission Type -> %-" + (WIDTH - 20) + "s |\n", this.missionType.toString());
        String coords = String.format("| Coordinates -> %-" + (WIDTH - 19) + "s |\n", this.areaCoordinates.toString());
        String radius = String.format("| Radius -> %d%-" + (WIDTH - 14 - ((int) Math.log10(Math.abs(this.areaRadius)) + 1)) + "s |\n", this.areaRadius, "");
        String duration = String.format("| Duration -> %d%-" + (WIDTH - 16 - ((int) Math.log10(Math.abs(this.missionTime)) + 1)) + "s |\n", this.missionTime, "");
        String update = String.format("| Update Interval -> %d%-" + (WIDTH - 23 - ((int) Math.log10(Math.abs(this.updateTime)) + 1)) + "s |\n", this.updateTime, "");

        String urgency = (isUrgent) ? "Yes" : "No";
        String urgencyLine = String.format("| Urgent -> %-" + (WIDTH - 14) + "s |\n", urgency);

        String status = (isCompleted) ? "Completed" : "Ongoing";
        String statusLine = String.format("| Status -> %-" + (WIDTH - 14) + "s |\n", status);

        return SEPARATOR_LINE +
                mission +
                SEPARATOR_LINE +
                rover +
                mtype +
                coords +
                radius +
                duration +
                update +
                urgencyLine +
                statusLine +
                SEPARATOR_LINE;
    }
}
