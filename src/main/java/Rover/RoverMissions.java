package Rover;

import Message.UpdateMission;
import Mission.Mission;
import Utils.Point3D;
import Utils.UDPPrint;

import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class RoverMissions {
    private final Rover rover;
    private final RoverConnection connection;
    private final PriorityBlockingQueue<Mission> missionsToDo;
    private volatile Mission currentMission = null;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> missionUpdateTask; // storing the mission currently sending update messages
    private long missionStartTime = 0;

    public RoverMissions(Rover rover, RoverConnection roverConnection) {
        this.rover = rover;
        this.connection = roverConnection;
        this.missionsToDo = new PriorityBlockingQueue<>();
    }
    public void addMission(Mission mission) {
        if (!this.missionsToDo.contains(mission)){
            UDPPrint.logSuccess("RCV", null, "NEW MISSION ACCEPTED AND STORED!");
            this.missionsToDo.put(mission);
        }
    }

    final long   LOOP_INTERVAL = 1000; // update every second
    final double CHARGE_RATE = 1; // per second
    final int    FIX_RATE = 5;
    final double SPEED = 1; // per second
    final double CONSUMPTION_WALKING = 0.1; // per sec
    final double CONSUMPTION_COLLECTING_ROCKS = 1;
    final double CONSUMPTION_EXPLORE = 2;
    final double CONSUMPTION_GETTING_SAMPLES = 0.5;

    public void run () {
        new Thread(() -> {
            try {
                long busyUntil = 0;
                while (true) {
                    switch (rover.getState()) {
                        case IDLE:
                            busyUntil = idle();
                            break;
                        case CHARGING:
                            charge(busyUntil);
                            busyUntil = 0;
                            break;
                        case ON_THE_WAY:
                            busyUntil = onTheWay(busyUntil);
                            break;
                        case IN_MISSION:
                            busyUntil = doMission(busyUntil);
                            break;
                        case ERROR:
                            repair(busyUntil);
                            busyUntil = 0;
                            break;
                        default:
                            this.rover.setState(Rover.MissionState.ERROR);
                            break;
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public long idle() throws InterruptedException {
        System.out.println("In idle");

        if (!rover.getPosition().equals(rover.getBase().getPosition())) System.out.println("Idle but not at base!");

        if (doesAnyPartNeedsFixing()){
            this.rover.setState(Rover.MissionState.ERROR);
            System.out.println("Going to repair!");
            return System.currentTimeMillis() + Math.round(timeToRepair()) * 1000L;
        }

        List<String> inventory = rover.getInventory();
        if (!inventory.isEmpty()) {
            System.out.println("Cleaning inventory!");
            if(rover.getBase() != null)
                inventory.forEach(item -> rover.getBase().addItem(item));
            this.rover.clearInventory();
        }

        if ((currentMission == null || currentMission.isCompleted()) && rover.getBatteryLevel() < 50) {
            this.rover.setState(Rover.MissionState.CHARGING);
            System.out.println("Going to charge!");
            return System.currentTimeMillis() + Math.round(timeToFullyCharge()) * 1000L;
        }

        if (currentMission == null || currentMission.isCompleted()) {

            if (missionsToDo.isEmpty()) {
                connection.requestMission();
            }

            Mission candidate = missionsToDo.take();
            System.out.println("Analyzing candidate Mission ID: " + candidate.getMissionId());

            boolean willBatterySurvive = willBatterySurvive(candidate);

            // impossible mission
            if (!willBatterySurvive && this.rover.getBatteryLevel() >= 99) {
                System.out.println("Can't do mission! Discarding ID " + candidate.getMissionId());

                connection.discardMission(candidate, rover.getId());

                return System.currentTimeMillis();
            }

            // mission accepted, but needs to charge
            if (!willBatterySurvive) {
                this.currentMission = candidate;
                this.rover.setState(Rover.MissionState.CHARGING);
                System.out.println("Mission accepted but needs charge. Charging...");
                return System.currentTimeMillis() + Math.round(timeToFullyCharge() * 1000L);
            }

            // mission accepted
            this.currentMission = candidate; // Aprovada!
            System.out.println("Has mission! ID " + currentMission.getMissionId());
        }

        System.out.println("Leaving idle, becoming on the way!");
        rover.setState(Rover.MissionState.ON_THE_WAY);
        return (long) (System.currentTimeMillis() + timeBetweenPlaces(rover.getPosition(), currentMission.getAreaCoordinates()) * 1000L);
    }

    public long doMission (long busyUntil) throws InterruptedException {
        System.out.println("In mission");
        long currentTime = System.currentTimeMillis();
        Random random = new Random();
        while (currentTime <= busyUntil) {
            long timeElapsed = Math.min(busyUntil - currentTime, LOOP_INTERVAL);
            decrementBattery (timeElapsed);

            // inventory management
            Mission.MissionType missionType = currentMission.getMissionType();
            if     (random.nextInt(100) < 5 &&                              // 5% chance every second
                    missionCollectsItems(missionType) &&                           // mission involves items
                    rover.getMaxInventorySpace() > rover.getInventory().size()){   // must have inventory space
                System.out.println("[MISSION] New item added to inventory!");
                if      (missionType == Mission.MissionType.COLLECT_ROCKS)   rover.addToInventory("ROCK");
                else if (missionType == Mission.MissionType.TEST_ATMOSPHERE) rover.addToInventory("SAMPLE");
            }

            Thread.sleep(LOOP_INTERVAL);
            currentTime = System.currentTimeMillis();
        }
        decrementParts ();
        this.currentMission.setCompleted();
        if (canDoNextMission()) {
            currentMission = missionsToDo.take();
            rover.setState(Rover.MissionState.ON_THE_WAY);
            System.out.println("[MISSION -> ON THE WAY TO NEW MISSION] Finished!");    // decision to on the way to new mission
            return System.currentTimeMillis() + currentMission.getMissionTime() * 1000L;
        }
        else {
            rover.setState(Rover.MissionState.ON_THE_WAY);
            sendUpdate();
            System.out.println("[MISSION -> ON THE WAY] Finished!");        // decision to on the way to base
            return (long) (System.currentTimeMillis() + timeBetweenPlaces(rover.getPosition(), rover.getBase().getPosition()) * 1000);
        }
    }

    public void charge (long busyUntil) throws InterruptedException {
        System.out.println("Charging!");
        long currentTime = System.currentTimeMillis();

        while (currentTime <= busyUntil) {
            // only update battery if the time elapsed is within the remaining busyUntil time
            long timeElapsed = Math.min(busyUntil - currentTime, LOOP_INTERVAL);

            double batteryIncrement = (timeElapsed / 1000.0) * CHARGE_RATE;
            double newBatteryLevel = rover.getBatteryLevel() + batteryIncrement;

            rover.setBatteryLevel(Math.min(100, newBatteryLevel)); // cap at 100
            System.out.printf("[CHARGING] Battery Telemetry: +%.2f%%. Current Level: %.2f\n",
                    batteryIncrement, rover.getBatteryLevel());

            Thread.sleep(LOOP_INTERVAL);
            currentTime = System.currentTimeMillis();
        }
        rover.setBatteryLevel(100);
        rover.setState(Rover.MissionState.IDLE);        // decision to idle
        System.out.printf("[CHARGING -> IDLE] CHARGE COMPLETE! Battery now %.2f.\n", rover.getBatteryLevel());
    }

    private long onTheWay(long busyUntil) throws InterruptedException {
        System.out.println("On the way!");
        long currentTime = System.currentTimeMillis();
        Point3D objective;
        if (currentMission.isCompleted()) objective = rover.getBase().getPosition();
        else objective = currentMission.getAreaCoordinates();

        while (currentTime <= busyUntil) {
            long timeElapsed = Math.min(busyUntil - currentTime, LOOP_INTERVAL);
            decrementBattery (timeElapsed);

            double distanceIncrement = timeElapsed * SPEED / 1000;
            Point3D newPosition = Point3D.findMiddlePoint(rover.getPosition(), objective, distanceIncrement);

            rover.setPosition(newPosition);
            if (!currentMission.isCompleted())
                System.out.printf("[ON THE WAY -> MISSION] Currently at " + newPosition + " \n");
            else System.out.printf("[ON THE WAY -> BASE] Currently at " + newPosition + " \n");

            Thread.sleep(LOOP_INTERVAL);
            currentTime = System.currentTimeMillis();
        }
        decrementParts();

        if (!currentMission.isCompleted()) { // has just arrived at the mission site
            // decision to become in mission
            rover.setPosition(currentMission.getAreaCoordinates());
            rover.setState(Rover.MissionState.IN_MISSION);
            System.out.println("[ON THE WAY -> MISSION] Has arrived!");
            startNewMission (currentMission);
            return System.currentTimeMillis() + currentMission.getMissionTime() * 1000L;
        }
        // pick another mission if there's enough battery
        else if (canDoNextMission()) {
            currentMission = missionsToDo.take();
            rover.setState(Rover.MissionState.ON_THE_WAY);
            System.out.println("[ON THE WAY -> NEW MISSION] Has arrived!");
            return System.currentTimeMillis() + currentMission.getMissionTime() * 1000L;

        } else { // arrived to base
            // decision to become idle
            rover.setPosition(rover.getBase().getPosition());
            rover.setState(Rover.MissionState.IDLE);

            System.out.println("[ON THE WAY -> BASE] Has arrived!");
            return 0;
        }
    }

    public void repair (long busyUntil) throws InterruptedException {
        System.out.println("Repairing!");
        long currentTime = System.currentTimeMillis();

        while (currentTime <= busyUntil) {
            long timeElapsed = Math.min(busyUntil - currentTime, LOOP_INTERVAL);
            for (PhysicalState ps : rover.getPhysicalStates()) {
                if (ps.getCondition() >= 100) continue;
                int increment = Math.toIntExact((timeElapsed / 1000L) * FIX_RATE);
                int conditionLevel = ps.getCondition() + increment;

                ps.setCondition(Math.min(100, conditionLevel)); // cap at 100
                System.out.printf("[REPAIRING] Repairing part '%s': +%d%%. Current Level: %d\n",
                        ps.getName(),increment, ps.getCondition());

                Thread.sleep(LOOP_INTERVAL);
                currentTime = System.currentTimeMillis();
            }
        }
        rover.getPhysicalStates().forEach(p -> p.setCondition(100));
        rover.setState(Rover.MissionState.IDLE);        // decision to idle
        System.out.println("[REPAIRING -> IDLE] REPAIR COMPLETE!");
    }

    private boolean doesAnyPartNeedsFixing () {
        for (PhysicalState ps : rover.getPhysicalStates()) {
            if (ps.getCondition() <= 10) return true;
        }
        return false;
    }

    private double timeToRepair() {
        if (rover.getPhysicalStates().isEmpty()) return 0;

        double totalTime = 0.0;

        for (PhysicalState ps : rover.getPhysicalStates()) {
            int currentCondition = ps.getCondition();

            if (currentCondition < 100) {
                double conditionNeeded = 100.0 - currentCondition;
                double timeForComponent = conditionNeeded / FIX_RATE;

                totalTime += timeForComponent;
            }
        }
        return totalTime;
    }

    private void decrementParts () throws IllegalArgumentException {
        Random rand = new Random();
        for (PhysicalState ps : rover.getPhysicalStates()) {
            int decrement = rand.nextInt(10);

            int newLevel = ps.getCondition() - decrement;
            if (newLevel < 0) newLevel = 0;
            else if (newLevel > 100) newLevel = 100;

            ps.setCondition(newLevel);
        }
    }
    private void decrementBattery (double timeElapsed) throws IllegalArgumentException {
        if (timeElapsed < 0) return;

        double batteryDecrement;
        if (rover.getState() == Rover.MissionState.ON_THE_WAY) batteryDecrement = timeElapsed * CONSUMPTION_WALKING / 1000 ;
        else batteryDecrement = timeElapsed * getBatteryRate(currentMission.getMissionType()) / 1000 ;

        double newBatteryLevel = rover.getBatteryLevel() - batteryDecrement;
        if (newBatteryLevel < 0) newBatteryLevel = 0;
        else if (newBatteryLevel > 100) newBatteryLevel = 100;

        rover.setBatteryLevel(newBatteryLevel);
        //System.out.printf("[BATTERY USAGE] Battery Telemetry: -%.2f%%. Current Level: %.2f\n", batteryDecrement, rover.getBatteryLevel());
    }

    private boolean willBatterySurvive (Mission m) {
        double consumptionPerMission;
        switch (m.getMissionType()){
            case EXPLORE         -> consumptionPerMission = CONSUMPTION_EXPLORE;
            case COLLECT_ROCKS   -> consumptionPerMission = CONSUMPTION_COLLECTING_ROCKS;
            case TEST_ATMOSPHERE -> consumptionPerMission = CONSUMPTION_GETTING_SAMPLES;
            default ->  consumptionPerMission = CONSUMPTION_WALKING;
        }

        double consumption = m.getMissionTime() * consumptionPerMission
                + timeBetweenPlaces(rover.getPosition(), m.getAreaCoordinates()) * CONSUMPTION_WALKING * 2;

        System.out.printf("Estimated mission consumption: %.2f\n", consumption);
        return (rover.getBatteryLevel() > consumption);
    }

    private double timeBetweenPlaces (Point3D a, Point3D b) {
        double dist = Math.sqrt(Math.pow((b.x-a.x),2) + Math.pow((b.y-a.y),2) + Math.pow((b.z-a.z),2));

        return dist/SPEED;
    }

    private boolean canDoNextMission() {
        if (missionsToDo.isEmpty()) return false;
        Mission next = missionsToDo.peek();
        Mission.MissionType missionType = next.getMissionType();
        return (!missionCollectsItems(missionType)   // if the mission involves inventory, needs at least one free space
                || this.rover.getInventory().size() < this.rover.getMaxInventorySpace())
            && willBatterySurvive(next); // needs battery
    }

    private double timeToFullyCharge () {
        return (100-this.rover.getBatteryLevel()) / CHARGE_RATE; // 100 secs to fully charge
    }

    private double getBatteryRate(Mission.MissionType missionType) {
        if (missionType == null) return CONSUMPTION_WALKING;
        else return switch (missionType) {
            case EXPLORE -> CONSUMPTION_EXPLORE;
            case TEST_ATMOSPHERE -> CONSUMPTION_GETTING_SAMPLES;
            case COLLECT_ROCKS -> CONSUMPTION_COLLECTING_ROCKS;
        };
    }

    private boolean missionCollectsItems(Mission.MissionType missionType) {
        return missionType == Mission.MissionType.COLLECT_ROCKS || missionType == Mission.MissionType.TEST_ATMOSPHERE;
    }

    // Sending mission updates vvv

    public void startNewMission(Mission newMission) {
        this.currentMission = newMission;
        this.missionStartTime = System.currentTimeMillis();

        if (missionUpdateTask != null && !missionUpdateTask.isDone()) {
            missionUpdateTask.cancel(false);
        }
        missionUpdateTask = scheduler.scheduleAtFixedRate(this::sendUpdate,
                0, // initial delay
                newMission.getUpdateTime(),
                TimeUnit.SECONDS);
    }

    private void sendUpdate() {
        if (currentMission == null) return;

        Mission currM = this.currentMission;
        long currentTime = System.currentTimeMillis();

        long totalDuration = currM.getMissionTime() * 1000L;
        long timeElapsed = currentTime - missionStartTime;

        int progressPercent = 0;
        if (totalDuration > 0)
            progressPercent = (int) Math.min(100, Math.round((double) timeElapsed / totalDuration * 100));

        byte[] extraData;
        if (currM.getMissionType() == Mission.MissionType.EXPLORE) {
            int size = 16384;
            Random ran = new Random();
            extraData = new byte[size];
            for (int i = 0; i<size; i++) {
                extraData[i] = (byte) ran.nextInt(255);
            }

        } else extraData = new byte[0];

        UpdateMission updateMission = new UpdateMission(
                currM.getMissionId(),
                rover.getId(),
                progressPercent,
                extraData
        );

        if (!missionUpdateTask.isDone()) {
            try {
                rover.sendUpdateMission(updateMission);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (progressPercent >= 100) {
                if (missionUpdateTask != null) {
                    missionUpdateTask.cancel(false);
                }
            }
        }
    }
    public void cleanup() {
        scheduler.shutdownNow();
    }
}
