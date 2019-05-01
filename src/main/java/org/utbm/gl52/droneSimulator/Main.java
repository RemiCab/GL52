package org.utbm.gl52.droneSimulator;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.utbm.gl52.droneSimulator.view.StartPageView;
import java.io.IOException;

public class Main extends Application {

    public void start(Stage primaryStage) throws IOException {
        StartPageView startPageView = new StartPageView();

        Scene scene = new Scene(startPageView.getParent());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Drone Siumulator");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
