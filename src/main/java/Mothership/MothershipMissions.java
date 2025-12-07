package Mothership;

import Message.MissionMessage;
import Message.UpdateMission;
import Mission.Mission;
import Utils.Point3D;

import java.util.*;

public class MothershipMissions {
    private final PriorityQueue<Mission> missionsToDo = new PriorityQueue<>();
    private final HashMap<Integer, Mission> activeMissions = new HashMap<>();
    private final HashMap<Integer, Mission> completedMissions = new HashMap<>();
    private final HashMap<Integer, Mission> discardedMissions = new HashMap<>();

    public Collection<Mission> getActiveMissions () {
        return this.activeMissions.values();
    }

    public Collection<Mission> getPastMissions () {
        return this.completedMissions.values();
    }

    public Collection<Mission> getFutureMissions () {
        return this.missionsToDo.stream().toList();
    }

    public Mission getMission () {
        return missionsToDo.poll();
    }
    public void createRandomMissionIfEmpty () {
        if (missionsToDo.isEmpty()) missionsToDo.add(createRandomMission());
    }

    public Mission createRandomMissionToRover (int idRover) {
        Random rand = new Random();
        Mission.MissionType type = Mission.MissionType.values()[rand.nextInt(Mission.MissionType.values().length)];
        int coordsDistance = 20;
        Point3D coords = new Point3D(rand.nextInt(coordsDistance), rand.nextInt(coordsDistance), rand.nextInt(coordsDistance));
        int area =  rand.nextInt(1,30);
        int time = rand.nextInt(10,60);            // 1 min max
        int updateTime = rand.nextInt(1,time/3);    // 3 updates per mission min
        boolean isUrgent = rand.nextInt(100) < 10;        // 10% chance of being urgent

        return new Mission (idRover, type, coords, area, time, updateTime, isUrgent);
    }

    private Mission createRandomMission () {
        return createRandomMissionToRover(-1);
    }

    public MothershipMissions (Mothership mothership) {
        Random rand = new Random();
        new Thread (() -> {
            try {
            while (true) {
                //System.out.println("\n--creating mission");
                List<Integer> roverIDs = mothership.getRoverIDs().stream().toList();
                boolean chanceOfSpecificMission = rand.nextInt(100) < 30;
                if (!roverIDs.isEmpty() && chanceOfSpecificMission) {   // create a mission to a specific rover
                    int id = roverIDs.get(rand.nextInt(roverIDs.size()));
                    //System.out.println("specific mission to rover " + id+"\n");

                    Mission mission = createRandomMissionToRover(id);
                    activeMissions.put(mission.getMissionId(), mission);
                    mothership.sendMission(new MissionMessage(mission)); // send right away
                } else missionsToDo.add(createRandomMission());

                //System.out.println("mission created--\n");
                int sleepFor = rand.nextInt(30,60); // new missions every 30 secs minimum, 60 seconds max
                Thread.sleep(sleepFor * 1000L);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public void processMissionUpdate (UpdateMission msg) {
        int missionId = msg.getIdMission();
        Mission m = activeMissions.get(missionId);
        if (m == null) {
            System.out.println("[MOTHERSHIP MISSIONS] Updated mission not found!"); // shouldn't happen
            return;
        }
        int completionLevel = msg.getCompletionLevel();
        if (completionLevel < 0 || completionLevel > 100) {
            activeMissions.remove(missionId);
            discardedMissions.put(missionId, m);
            System.out.println("[MOTHERSHIP MISSIONS] Mission " + msg.getIdMission() + " discarded!");
        }
        else if (completionLevel == 100) {
            m.setCompleted();
            activeMissions.remove(missionId);
            completedMissions.put(missionId, m);
            System.out.println("[MOTHERSHIP MISSIONS] Mission " + msg.getIdMission() + " completed!");
        }
    }

    public void startMission (Mission m) {
        int id = m.getMissionId();
        activeMissions.put(id, m);
    }
}
