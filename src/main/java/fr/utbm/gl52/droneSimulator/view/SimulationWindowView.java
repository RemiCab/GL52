package fr.utbm.gl52.droneSimulator.view;

import fr.utbm.gl52.droneSimulator.model.*;
import fr.utbm.gl52.droneSimulator.view.graphicElement.*;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;

import java.io.IOException;
import java.util.*;

public class SimulationWindowView {

    private static Parent root;

    private static List<ParcelGraphicElement> parcelGraphicElements = new ArrayList<>();

    private static Map<Drone, TextArea> consoleMap = new HashMap<>();

    private static Boolean isViewFullyLoaded = false;

    /**
     * Initialize the simulation
     *
     * @param simulationMode Mode of simulation defined in Simulation
     *
     * @throws IOException The associated FXML file is not found
     */
    public static void init(String simulationMode) throws IOException{

        startModel(simulationMode);

        FXMLLoader loader = new FXMLLoader(
                SimulationWindowView.class.getResource("/fxml/SimulationWindow.fxml")
        );
        loader.load();

        root = loader.getRoot();
        Pane pane = (Pane) root.lookup("#simulationPane");

        startView(pane);

        refreshSimulationSpeed();

        isViewFullyLoaded = true;
    }

    /**
     * Place all elements on the simulation pane
     *
     * @param pane The simulation pane
     */
    private static void startView(Pane pane) {
        GraphicElement.setModelViewCoefficient(0.65f);
        CenteredAndErgonomicGraphicElement.setZoomCoefficient(20f);

        displayMainArea(pane);
        displayAreas(pane);
        displayParcels(pane);
        displayChargingStation(pane);
        displayDronesAndAssociatedTabs(pane);
    }

    /**
     * Add drones on simulation pane and add one tab per drone
     *
     * @param pane The simulation pane
     */
    private static void displayDronesAndAssociatedTabs(Pane pane) {
        for (Drone drone : Simulation.getDrones()) {
            displayDrone(pane, drone);
            displayDroneLogTab(drone);
        }
    }

    /**
     * Add a drone to the simulation pane
     *
     * @param pane Simulation Pane
     * @param drone drone to display
     */
    private static void displayDrone(Pane pane, Drone drone) {
        DroneGraphicElement droneGraphicElement = new DroneGraphicElement(drone);
        Shape shape = droneGraphicElement.getShape();
        Text id = droneGraphicElement.getGraphicalId();
        pane.getChildren().add(shape);
        pane.getChildren().add(id);
    }

    /**
     * Add a drone tab
     *
     * @param drone drone to display
     */
    private static void displayDroneLogTab(Drone drone) {
        TabPane tabPaneLog = (TabPane) root.lookup("#tabPaneLogs");
        TextArea text;
        Tab tab;
        text = new TextArea();
        text.setEditable(false);
        text.textProperty().addListener((ChangeListener<Object>) (observable, oldValue, newValue) -> {
            text.setScrollTop(Double.MAX_VALUE); //this will scroll to the bottom
            //use Double.MIN_VALUE to scroll to the top
        });
        consoleMap.put(drone, text);
        tab = new Tab();
        tab.setClosable(false);
        tab.setContent(text);
        tab.setText("Drone " + drone.getId());
        tabPaneLog.getTabs().add(tab);
    }

    /**
     * Add parcels to the simulation pane
     *
     * @param pane Simulation Pane
     */
    private static void displayParcels(Pane pane) {
        for (Parcel parcel : Simulation.getParcels()) {
            ParcelGraphicElement parcelGraphicElement = new ParcelGraphicElement(parcel);
            parcelGraphicElements.add(parcelGraphicElement);
            Text id = parcelGraphicElement.getGraphicalId();
            Shape shape = parcelGraphicElement.getShape();
            pane.getChildren().add(shape);
            pane.getChildren().add(id);
        }
    }

    /**
     * Add charging stations to the simulation pane
     *
     * @param pane Simulation Pane
     */
    private static void displayChargingStation(Pane pane) {
        for (ChargingStation chargingStation : Simulation.getChargingStations()) {
            ChargingStationGraphicElement chargingStationGraphicElement = new ChargingStationGraphicElement(chargingStation);
            Shape shape = chargingStationGraphicElement.getShape();
            Text id = chargingStationGraphicElement.getGraphicalId();
            pane.getChildren().add(shape);
            pane.getChildren().add(id);
        }
    }

    /**
     * Add areas to the simulation pane
     *
     * @param pane Simulation Pane
     */
    private static void displayAreas(Pane pane) {
        for (Area area : Simulation.getAreas()) {
            Shape shape = AreaGraphicElement.getShape(area);
            pane.getChildren().add(shape);
        }
    }

    /**
     * Add the main area to the simulation pane
     *
     * @param pane Simulation Pane
     */
    private static void displayMainArea(Pane pane) {
        Shape mainAreaGraphicElementShape = MainAreaGraphicElement.getShape(Simulation.getMainArea());
        pane.getChildren().add(mainAreaGraphicElementShape);
    }

    /**
     * Refresh the simulation speed on the screen
     */
    public static void refreshSimulationSpeed() {
        Text speedText = (Text) root.lookup("#speedText");
        String text = String.format("Speed: x%.1f", Simulation.getSimulationSpeed());
        speedText.setText(text);
    }

    /**
     * Start the model according to the chosen simulation mode
     *
     * @param simulationMode The mode of simulation
     */
    private static void startModel(String simulationMode) {
        new Simulation();
        switch (simulationMode) {
            case Simulation.DEFAULT:
                Simulation.startDefault();
                break;
            case Simulation.RANDOM:
                Simulation.startRandom();
                break;
            case Simulation.CUSTOM:
                Simulation.startCustom();
                break;
            default:
                throw new IllegalArgumentException("the mode '" + simulationMode + "' doesn't exist");
        }
    }

    /**
     * Add an event to a drone tab
     *
     * @param drone  Concerned drone
     * @param event Event to log
     */
    public static void logDroneEventInTab(Drone drone, String event){
        TextArea text = consoleMap.get(drone);

        if(drone == null){
            throw new IllegalArgumentException("This drone does not exist");
        } else {
            text.appendText(event + "\n");
        }
    }

    /**
     * Remove the parcel from the simulation if it exists
     *
     * @param parcelToRemove Parcel graphic element to remove
     */
    public static void removeParcelGraphicIfExists(ParcelGraphicElement parcelToRemove) {
        if(parcelToRemove != null) {

            removeParcelFromPane(parcelToRemove);
            parcelGraphicElements.remove(parcelToRemove);
        }
    }

    /**
     * Remove a list of parcel graphic elements
     *
     * @param parcels Parcels to remove
     */
    public static void removeParcelsGraphic(List<ParcelGraphicElement> parcels){
        for(ParcelGraphicElement parcel: parcels){
            removeParcelFromPane(parcel);
        }
        parcelGraphicElements.removeAll(parcels);
    }

    /**
     * Remove a parcel from the view
     *
     * @param parcel Parcel to remove
     */
    public static void removeParcelFromPane(ParcelGraphicElement parcel){
        Pane pane = (Pane) root.lookup("#simulationPane");
        pane.getChildren().remove(parcel.getShape());
        pane.getChildren().remove(parcel.getGraphicalId());
    }

    public static javafx.scene.Parent getParent() {
        return root;
    }

    public static List<ParcelGraphicElement> getParcelGraphicElements() {
        return parcelGraphicElements;
    }

    public static Boolean isViewFullyLoaded() {
        return isViewFullyLoaded;
    }
}
