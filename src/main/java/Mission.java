
public class Mission {
    private int id;
    private MissionType missionType;
    private String area;
    private int duration; // minutes
    // updates: the mission must define how and how often the rover reports back to the mothership

    enum MissionType {
        EXPLORE,
        COLLECT_ROCKS,
        TEST_ATMOSPHERE,
    }

    int counter = 1;

    public Mission(MissionType mtype, String area, int dur){
        this.id = counter;
        counter++;
        this.missionType = mtype;
        this.area = area;
        this.duration = dur;
    }
}
