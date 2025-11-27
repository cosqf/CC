package Message;

import Mission.Mission;
import Utils.Point3D;

import java.io.*;

public class MissionMessage implements MessageData{
    private final int missionId;
    private final int roverId;
    private final Mission.MissionType missionType;
    private final Point3D areaCoordinates;
    private final int areaRadius;
    private final int missionTime;
    private final int updateTime;
    private final boolean isUrgent;
    private final boolean isCompleted;

    public MissionMessage(int missionId, int roverId, Mission.MissionType missionType,
                          Point3D areaCoordinates, int areaRadius, int missionTime,
                          int updateTime, boolean isUrgent, boolean isCompleted) {
        this.missionId = missionId;
        this.roverId = roverId;
        this.missionType = missionType;
        this.areaCoordinates = areaCoordinates;
        this.areaRadius = areaRadius;
        this.missionTime = missionTime;
        this.updateTime = updateTime;
        this.isUrgent =  isUrgent;
        this.isCompleted = isCompleted;
    }
    public MissionMessage (Mission mission) {
        this.missionId = mission.getMissionId();
        this.roverId = mission.getRoverId();
        this.missionType = mission.getMissionType();
        this.areaCoordinates= mission.getAreaCoordinates();
        this.areaRadius= mission.getAreaRadius();
        this.missionTime = mission.getMissionTime();
        this.updateTime = mission.getUpdateTime();
        this.isUrgent = mission.isUrgent();
        this.isCompleted = mission.isCompleted();
    }

    public Mission getMission() {
        return new Mission (missionId, roverId, missionType, areaCoordinates, areaRadius, missionTime, updateTime, isUrgent, isCompleted);
    }
    public int getMissionId() {
        return missionId;
    }
    public int  getRoverId() {
        return roverId;
    }
    public Mission.MissionType getMissionType() {
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

    @Override
    public byte[] convertMessageDataToBytes() {
        byte[] dataContentBytes;

        try (ByteArrayOutputStream contentBos = new ByteArrayOutputStream();
             DataOutputStream contentDos = new DataOutputStream(contentBos)) {

            contentDos.writeInt(this.missionId);
            contentDos.writeInt(this.roverId);
            contentDos.writeInt(this.missionType.ordinal());
            contentDos.writeDouble(this.areaCoordinates.x);
            contentDos.writeDouble(this.areaCoordinates.y);
            contentDos.writeDouble(this.areaCoordinates.z);
            contentDos.writeInt(this.areaRadius);
            contentDos.writeInt(this.missionTime);
            contentDos.writeInt(this.updateTime);
            contentDos.writeByte(this.isUrgent ? 1 : 0);
            contentDos.writeByte(this.isCompleted ? 1 : 0);

            contentDos.flush();
            dataContentBytes = contentBos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Error during content serialization.", e);
        }
        return MessageData.addSizeToArray(dataContentBytes);
    }

    public static MissionMessage convertBytesToMessageData(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             DataInputStream dis = new DataInputStream(bis)) {

            dis.readInt();

            int missionId = dis.readInt();
            int roverId = dis.readInt();
            int missionTypeOrdinal = dis.readInt();
            Mission.MissionType missionType = Mission.MissionType.values()[missionTypeOrdinal];

            double x = dis.readDouble();
            double y = dis.readDouble();
            double z = dis.readDouble();
            Point3D coordinates = new Point3D(x, y, z);

            int areaRadius = dis.readInt();
            int missionTime = dis.readInt();
            int updateTime = dis.readInt();

            boolean isUrgent = dis.readByte() == 1;
            boolean isCompleted = dis.readByte() == 1;

            return new MissionMessage(missionId, roverId, missionType, coordinates, areaRadius, missionTime, updateTime, isUrgent, isCompleted);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("Invalid MissionType ordinal in byte data.", e);
        }
    }

    @Override
    public String toString() {
        return "MissionMessage { " +
                "missionId = " + missionId +
                ", roverId = " + roverId +
                ", missionType = " + missionType +
                ", areaCoordinates = " + areaCoordinates +
                ", areaRadius = " + areaRadius +
                ", missionTime = " + missionTime +
                ", updateTime = " + updateTime +
                ", isUrgent = " + isUrgent +
                ", isCompleted = " + isCompleted +
                '}';
    }
}
