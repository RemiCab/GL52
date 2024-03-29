package fr.utbm.gl52.droneSimulator.model;

import fr.utbm.gl52.droneSimulator.model.exception.OutOfMainAreaException;
import fr.utbm.gl52.droneSimulator.service.DbChargingStationService;
import fr.utbm.gl52.droneSimulator.service.DbDroneService;
import fr.utbm.gl52.droneSimulator.service.DbParameterService;
import fr.utbm.gl52.droneSimulator.service.entity.DbDrone;
import fr.utbm.gl52.droneSimulator.service.entity.DbParameter;
import fr.utbm.gl52.droneSimulator.view.SimulationWindowView;
import fr.utbm.gl52.droneSimulator.view.StatisticsWindowView;
import fr.utbm.gl52.droneSimulator.view.graphicElement.ParcelGraphicElement;
import javafx.application.Platform;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

import static fr.utbm.gl52.droneSimulator.view.SimulationWindowView.isViewFullyLoaded;
import static fr.utbm.gl52.droneSimulator.view.graphicElement.GraphicHelper.createSimpleWindow;

public class Simulation {
    public static final long secondsInAMinute = 60L;
    public static final long millisecondsInASecond = 1000L;
    private static final Integer DEFAULT_DURATION = 12;

    private static Boolean isAppClosed = false;

    private static ArrayList<Drone> drones = new ArrayList<>();
    private static ArrayList<Thread> droneThreads = new ArrayList<>();
    private static ArrayList<Parcel> parcels = new ArrayList<>();
    private static ArrayList<Area> areas = new ArrayList<>();
    private static List<ChargingStation> chargingStations = new ArrayList<>();
    private static MainArea mainArea;

    private static DbParameterService parameterService = new DbParameterService();
    private static DbDroneService droneService = new DbDroneService();
    private static DbChargingStationService chargingStationService = new DbChargingStationService();
    private static DbParameter parameters;

    private static Boolean play = true;

    private static Integer time;
    private static Float simulationSpeed;
    private static Float minSimulationSpeedAcceleration = 1f;
    private static Float maxSimulationSpeedAcceleration = 20f;
    private static Integer parcelNumber;
    private static Integer droneNumber;
    private static Integer chargingStation;

    private static Float[] droneWeightCapacity = new Float[2];
    private static Float[] droneBatteryCapacity = new Float[2];
    private static Integer[] simulationDurationRange = new Integer[2];
    private static Integer[] numberOfSimulationIterationRange = new Integer[2];
    private static Integer simulationDuration = 20;
    private static Integer numberOfSimulationIteration = numberOfSimulationIterationRange[0];
    private static Integer currentIteration = 1;

    private static Map<String, Integer[]> parcelTimeToDisappearRangeLinkedToDifficulty = new HashMap<>();
    private static Integer[] parcelTimeToDisappearRange = new Integer[2];

    private static final Float mainAreaWidth = 1800f;
    private static final Float mainAreaHeight = 600f;

    public static final String DEFAULT = "DEFAULT";
    public static final String RANDOM = "RANDOM";
    public static final String CUSTOM = "CUSTOM";

    private static final Integer imagesPerSecond = 30;
    private static final Float maxThreadSleepAcceleration = 30f;

    private static Long t1 = Instant.now().toEpochMilli();
    private static Long t2 = Instant.now().toEpochMilli();
    private static Long elapsedTime = 0L;

    private static Thread simulationThread = new Thread(Simulation::manageSimulationStop);

    public Simulation() {
        time = 0;
        setSimulationSpeed(1f);
        droneNumber = 3;
        parcelNumber = 25;
        chargingStation = 5;
        droneWeightCapacity[0] = 0.1f;
        droneWeightCapacity[1] = 20f;
        droneBatteryCapacity[0] = 5f;
        droneBatteryCapacity[1] = 55f;
        simulationDurationRange[0] = 20;
        simulationDurationRange[1] = 1440;
        numberOfSimulationIterationRange[0] = 1;
        numberOfSimulationIterationRange[1] = 10;
        initCompetitionDifficultyLevels();
        parcelTimeToDisappearRange = parcelTimeToDisappearRangeLinkedToDifficulty.get("soft");
    }

    /**
     * Prepare competition range value
     */
    private void initCompetitionDifficultyLevels() {
        Integer[] range = {60, 260};
        parcelTimeToDisappearRangeLinkedToDifficulty.put("soft", range);
        range[0] = 30;
        range[1] = 120;
        parcelTimeToDisappearRangeLinkedToDifficulty.put("medium", range);
        range[0] = 15;
        range[1] = 60;
        parcelTimeToDisappearRangeLinkedToDifficulty.put("hard", range);
    }

    /**
     * Init a new iteration of the simulation
     */
    private static synchronized void initIteration() {

        saveDroneStats();

        currentIteration++;

        storeDroneData();

        reloadDronesFromDatabase();

        reloadRandomParcels();

        cleanAndRestartSimulationWindow();

        resetChargingStations();

        resetSimulationClock();


        globalStart();
    }

    /**
     * Store drone statistics
     */
    private static void saveDroneStats() {
        for(Drone drone: drones){
            DbDrone dbDrone = droneService.getDroneBy(parameters.getIdSimu(), currentIteration, drone.getId());
            dbDrone.setChargingTime(drone.getChargingTime());
            dbDrone.setKilometers(drone.getDistance() / 1000);
            droneService.merge(dbDrone);
        }
    }

    /**
     * Reset simulation clock to 0.
     */
    private static void resetSimulationClock() {
        t1 = Instant.now().toEpochMilli();
        t2 = Instant.now().toEpochMilli();
        elapsedTime = 0L;
    }

    /**
     * reset charging stations to original state (free)
     */
    private static void resetChargingStations() {
        for (ChargingStation chargingStation : chargingStations) {
            chargingStation.freeChargingStation();
        }
    }

    /**
     * pop new random parcels
     */
    private static void reloadRandomParcels() {
        parcels.clear();
        parcels = new ArrayList<>();
        popParcels();
    }

    /**
     * Reload drones to state on database
     */
    private static void reloadDronesFromDatabase() {
        drones = new ArrayList<>();
        Drone drone;
        List<DbDrone> dbDrones = droneService.getDronesInFirstIterationSimu(parameters.getIdSimu());

        for (DbDrone dbDrone : dbDrones) {
            drone = new Drone(dbDrone.getIdDrone());
            drone.setWeightCapacity((float) dbDrone.getWeightCapacity());
            drone.setBatteryFullCapacity((float) dbDrone.getBatteryCapacity());
            drone.setBatteryCapacity(drone.getBatteryCapacity());
            try {
                drone.setX((float) dbDrone.getX());
                drone.setY((float) dbDrone.getY());
                drones.add(drone);
            } catch (OutOfMainAreaException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * clean all view parameters and put elements to start position
     */
    private static void cleanAndRestartSimulationWindow() {

        SimulationWindowView.cleanView();
        Platform.runLater(SimulationWindowView::displayDronesAndAssociatedTabs);
        Platform.runLater(SimulationWindowView::displayParcels);
        SimulationWindowView.setViewFullyLoaded(true);
    }

    public static void setSimulationSpeed(Float f) {
        simulationSpeed = f;
    }

    public static void removeParcel(Parcel parcel) {
        parcels.remove(parcel);
    }

    public static void initMainArea() {
        mainArea = new MainArea(0f, 0f, mainAreaWidth, mainAreaHeight);
    }

    public static void flushParameters() {
        parameters = parameterService.save(
                simulationDuration,
                numberOfSimulationIteration,
                parcelTimeToDisappearRange[0],
                parcelTimeToDisappearRange[1]
        );
        storeDroneData();
        storeChargingStationData();
    }

    public static void storeDroneData() {
        for (Drone drone : drones) {
            droneService.save(
                    parameters.getIdSimu(),
                    currentIteration,
                    drone.getId(),
                    0,
                    drone.getBatteryCapacity(),
                    drone.getWeightCapacity(),
                    0,
                    drone.getX(),
                    drone.getY()
            );
        }
    }

    public static void storeChargingStationData() {
        for (ChargingStation chargingStation : chargingStations) {
            chargingStationService.save(
                    parameters.getIdSimu(),
                    chargingStation.getX(),
                    chargingStation.getY()
            );
        }
    }

    /**
     * Start simulation with default parameters
     */
    public static void startDefault() {
        initMainArea();
        //popAreas();
        instanciateDefaultDrones();
        instanciateDefaultChargingStations();
        popParcels();
        globalStart();
    }

    /**
     * Create charging stations for the default case
     */
    private static void instanciateDefaultChargingStations() {
        ChargingStation chargingStation = new ChargingStation(0);
        try {
            chargingStation.setX(281f);
            chargingStation.setY(347f);
        } catch (OutOfMainAreaException e) {
            e.printStackTrace();
        }

        chargingStations.add(chargingStation);

        chargingStation = new ChargingStation(1);
        try {
            chargingStation.setX(798f);
            chargingStation.setY(349f);
        } catch (OutOfMainAreaException e) {
            e.printStackTrace();
        }

        chargingStations.add(chargingStation);

        chargingStation = new ChargingStation(2);
        try {
            chargingStation.setX(1421f);
            chargingStation.setY(336f);
        } catch (OutOfMainAreaException e) {
            e.printStackTrace();
        }

        chargingStations.add(chargingStation);
    }

    /**
     * Create drones for the default case
     */
    private static void instanciateDefaultDrones() {
        Drone drone;

        drone = new Drone(0);
        try {
            drone.setX(344f);
            drone.setY(246f);
        } catch (OutOfMainAreaException e) {
            e.printStackTrace();
        }
        drone.setBatteryCapacity(31.61f);
        drone.setWeightCapacity(11.65f);

        drones.add(drone);

        drone = new Drone(1);
        try {
            drone.setX(644f);
            drone.setY(469f);
        } catch (OutOfMainAreaException e) {
            e.printStackTrace();
        }
        drone.setBatteryCapacity(31.61f);
        drone.setWeightCapacity(11.65f);

        drones.add(drone);

        drone = new Drone(2);
        try {
            drone.setX(344f);
            drone.setY(246f);
        } catch (OutOfMainAreaException e) {
            e.printStackTrace();
        }
        drone.setBatteryCapacity(49.12f);
        drone.setWeightCapacity(6.51f);

        drones.add(drone);

        drone = new Drone(3);
        try {
            drone.setX(1186f);
            drone.setY(450f);
        } catch (OutOfMainAreaException e) {
            e.printStackTrace();
        }
        drone.setBatteryCapacity(16.29f);
        drone.setWeightCapacity(20f);

        drones.add(drone);
    }

    /**
     * Start simulation with random parameters
     */
    public static void startRandom() {
        initMainArea();
        //popAreas();
        popParcels();
        popDrones();
        popChargingStations();
        globalStart();
    }

    /**
     * Start the simulation with custom parameters
     */
    public static void startCustom() {
        popParcels();
        globalStart();
    }

    /**
     * Start all the threads
     */
    public static void globalStart() {
        setPlay(true);
        if (simulationThread.getState() != Thread.State.NEW) {
            simulationThread = new Thread(Simulation::manageSimulationStop);
        }
        simulationThread.start();
        for (Drone drone : drones) {
            Thread droneThread = new Thread(drone);
            droneThreads.add(droneThread);
            droneThread.start();
        }
    }

    /**
     * Simulate the competition by removing parcels form the map.
     */
    private static void makeParcelDisappearWhenPickedByCompetitors() {
        Parcel parcel;
        List<ParcelGraphicElement> parcelsGraphicToRemove = new ArrayList<>();

        for (ParcelGraphicElement parcelGraphic : SimulationWindowView.getParcelGraphicElements()) {
            parcel = (Parcel) parcelGraphic.getSimulationElement();
            if (isParcelDisappearing(parcel)) {
                parcelsGraphicToRemove.add(parcelGraphic);
                Parcel finalParcel = parcel;
                Platform.runLater(() -> getParcels().remove(finalParcel));
                System.out.println("Removing parcel " + finalParcel.getId() + " after " + finalParcel.getTimeToDisappear() + " min");
            }
        }
        Platform.runLater(() -> SimulationWindowView.removeParcelsGraphic(parcelsGraphicToRemove));
    }

    /**
     * Check if the parcel has reach is end of life (catch by other competitors) and it is not already loaded
     *
     * @param parcel Parcel to check
     * @return True if yes
     */
    private static boolean isParcelDisappearing(Parcel parcel) {
        return parcel.getTimeToDisappear() < MathHelper.convertMillisecondsToMinutes(elapsedTime) && !parcel.isInJourney();
    }

    public static void stop() {
        setPlay(false);
        for (Thread droneThread : droneThreads) {
            droneThread.interrupt();
        }
        simulationThread.interrupt();
        Thread.currentThread().interrupt();
    }

    /**
     * Create random parcels
     */
    private static void popParcels() {
        for (Integer i = 0; i < getParcelNumber(); ++i) {
            parcels.add(Parcel.createRandomized(i));
        }
        System.out.println(parcels.toString());
    }

    private static void popAreas() {
        for (Integer i = 0; i < 1; ++i) {
            areas.add(Area.createRandomized());
        }
    }

    /**
     * Create random drones
     */
    private static void popDrones() {
        for (Integer i = 0; i < getDroneNumber(); ++i) {
            drones.add(Drone.createRandomizedDrone(i));
        }
    }

    /**
     * Create random charging stations
     */
    private static void popChargingStations() {
        for (Integer i = 0; i < getChargingStationNumber(); ++i) {
            chargingStations.add(ChargingStation.createRandomizedChargingStations(i));
        }
    }

    /**
     * Stop simulation if time is exceeded
     */
    public static void manageSimulationStop() {
        while (isPlay()) {
            updatePlayStatusAccordingToDuration();
            makeParcelDisappearWhenPickedByCompetitors();
            popRandomParcels();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (!play) {
            System.out.println("Time elapsed");
            stop();
            if(!isAppClosed){
                rebootSimulationForNextIteration();
            }
        }
    }

    /**
     * Launch a new iteration if asked by the parameter
     */
    private static void rebootSimulationForNextIteration() {
        if (currentIteration < numberOfSimulationIteration) {
            initIteration();
        } else {
            saveDroneStats();
            try {
                new StatisticsWindowView();
                Platform.runLater(() -> createSimpleWindow(StatisticsWindowView.getParent()));
                SimulationWindowView.closeStage();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * pop random parcels during the simulation (at random time)
     */
    private static void popRandomParcels() {
        Integer random = RandomHelper.getRandInt(0, 100);

        if (random == 42) {
            Integer id = parcelNumber++;

            Parcel parcel = Parcel.createRandomized(id, elapsedTime);
            parcels.add(parcel);
            Platform.runLater(() -> SimulationWindowView.addParcelToView(parcel));
            System.out.println("parcel n " + id + "  has popped");
        }
    }

    public static void setTimeSimulationParameters(Integer numberOfSimulationIteration) {
        setTimeSimulationParameters(Simulation.DEFAULT_DURATION, numberOfSimulationIteration);
    }

    public static void setTimeSimulationParameters(Integer simulationDuration, Integer numberOfSimulationIteration) {
        Simulation.simulationDuration = simulationDuration;
        Simulation.numberOfSimulationIteration = numberOfSimulationIteration;
    }

    public static void setCompetitionDifficulty(String difficulty) {
        parcelTimeToDisappearRange = parcelTimeToDisappearRangeLinkedToDifficulty.get(difficulty);
        if (parcelTimeToDisappearRange == null) {
            throw new IllegalArgumentException(difficulty + " is not defined");
        }
    }

    /**
     * Iterate the clock according to elapsed time and simulation speed.
     */
    private static void updatePlayStatusAccordingToDuration() {
        if (isViewFullyLoaded()) {
            t2 = Instant.now().toEpochMilli();
            long simulationDurationInMilli = simulationDuration * secondsInAMinute * millisecondsInASecond;
            elapsedTime += (long) (StrictMath.abs(t2 - t1) * simulationSpeed);
            //System.out.println("time elapsed " + elapsedTime/60000);
            play = elapsedTime <= simulationDurationInMilli;
            t1 = Instant.now().toEpochMilli();
        }
    }

    private static void incrementTime() {
        setTime(getTime() + 1);
    }

    /* trivial getters et setters */
    public static Boolean isPlay() {
        return play;
    }

    public static Float getSimulationSpeed() {
        return simulationSpeed;
    }

    public static Float getMinSimulationSpeedAcceleration() {
        return minSimulationSpeedAcceleration;
    }

    public static Float getMaxSimulationSpeedAcceleration() {
        return maxSimulationSpeedAcceleration;
    }

    public static Integer getTime() {
        return time;
    }

    public static ArrayList<Drone> getDrones() {
        return drones;
    }

    public static Integer getDroneNumber() {
        return droneNumber;
    }

    public static ArrayList<Parcel> getParcels() {
        return parcels;
    }

    public static Integer getParcelNumber() {
        return parcelNumber;
    }

    public static Area getMainArea() {
        return mainArea;
    }

    public static ArrayList<Area> getAreas() {
        return areas;
    }

    public static void setPlay(Boolean b) {
        play = b;
    }

    public static void setTime(Integer i) {
        time = i;
    }

    public static void setDroneNumber(Integer i) {
        droneNumber = i;
    }

    public static void setParcelNumber(Integer i) {
        parcelNumber = i;
    }

    public static void setDrones(ArrayList<Drone> drones) {
        drones = drones;
    }

    public static void setParcels(ArrayList<Parcel> parcels) {
        parcels = parcels;
    }

    public static List<ChargingStation> getChargingStations() {
        return chargingStations;
    }

    public static Integer getChargingStationNumber() {
        return chargingStation;
    }

    public static void setChargingStationNumber(Integer chargingStation) {
        Simulation.chargingStation = chargingStation;
    }

    public static Float[] getDroneWeightCapacity() {
        return droneWeightCapacity;
    }

    public static Float[] getDroneBatteryCapacity() {
        return droneBatteryCapacity;
    }

    public static Integer[] getSimulationDurationRange() {
        return simulationDurationRange;
    }

    public static Integer[] getNumberOfSimulationIterationRange() {
        return numberOfSimulationIterationRange;
    }


    public static Integer getImagesPerSecond() {
        return imagesPerSecond;
    }

    public static Long getT2() {
        return t2;
    }

    public static Long getElapsedTime() {
        return elapsedTime;
    }

    public static Integer[] getParcelTimeToDisappearRange() {
        return parcelTimeToDisappearRange;
    }

    public static Map<String, Integer[]> getCompetitionDifficultyLevels() {
        return parcelTimeToDisappearRangeLinkedToDifficulty;
    }

    public static void setAppClosed(Boolean isAppClosed) {
        Simulation.isAppClosed = isAppClosed;
    }

    public static Integer getId(){
        return parameters.getIdSimu();
    }

    public static Integer getCurrentIteration(){
        return currentIteration;
    }
}