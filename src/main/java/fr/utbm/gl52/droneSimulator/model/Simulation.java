package fr.utbm.gl52.droneSimulator.model;

import java.util.ArrayList;

public class Simulation {
    private static ArrayList<Drone> drones = new ArrayList<>();
    private static ArrayList<Parcel> parcels = new ArrayList<>();
    private static ArrayList<Area> areas = new ArrayList<>();
    private static MainArea mainArea;

    private static Boolean play = true;

    private static Integer time;
    private static Float speed;
    private static Integer parcelNumber;
    private static Integer droneNumber;

    private static final Float mainAreaWidth = 16000f;
    private static final Float mainAreaHeight = 9000f;

    public Simulation() {
        time = 0;
        setSpeed(10f); // pour voir les éléments se déplacer
        droneNumber = 1;
        parcelNumber = 10;
    }

    public static void setSpeed(Float f) {
        speed = f; // TODO ajouter controle et exception
    }

    public static void addNumberToSpeed(Float nb) {
        setSpeed(getSpeed() + nb);
    }

    public static void addPercentageToSpeed(Float nb) {
        setSpeed(getSpeed() * (1 + nb));
    }


    public static void removeParcel(Parcel parcel) {
        parcels.remove(parcel);
    }
    public static void removeAllParcels() {
        parcels.clear();
    }

    public static void removeDrone(Drone drone) {
        drones.remove(drone);
    }
    public static void removeAllDrones() {
        drones.clear();
    }

    private static void initMainArea() {
        mainArea = new MainArea(0f, 0f, mainAreaWidth, mainAreaHeight);
    }

    public static void start() {
        initMainArea();
        popAreas();
        popParcels();
        popDrones();
        update();
    }

    private static void popParcels() {
        for (Integer i = 0; i < getParcelNumber(); ++i) {
            parcels.add(Parcel.createRandomized());
        }
    }

    private static void popAreas() {
        for (Integer i = 0; i < 1; ++i) {
            areas.add(Area.createRandomized());
        }
    }

    private static void popDrones() {
        for (Integer i = 0; i < getDroneNumber(); ++i) {
            drones.add(Drone.createRandomizedDrone());
        }
    }

    public static void update() {
        while (isPlay()) {
            // TODO ajust
            incrementTime();

            // TODO refactor
//            if (getTime() % 60 == 0){
//                System.out.println("ok");
//                parcels.add(Parcel.createRandomized());
//            }

            for (Drone drone : drones) {
                drone.handleParcelInteractions();
                drone.handleDroneInteractions();
                drone.move();
            }

            try {
                Thread.sleep((long) (1000 / 30 / getSpeed())); // TODO déplacer le drone en fonction du temps écoulé depuis le dernier rafraichissement du modèle
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void incrementTime() {
        setTime(getTime() + 1);
    }

    /* getteurs et setteurs triviaux */
    public static Boolean isPlay() {
        return play;
    }
    public static Float getSpeed() {
        return speed;
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
}