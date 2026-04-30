/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mapademo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 *
 * @author jose
 */
public class App extends Application {
    // Referencias a los archivos
    String loginPath = "/mapademo/login/LoginPage.fxml";
    String singinPath = "/mapademo/singin/SinginPage.fxml";
    String mainPath = "/mapademo/main/FXMLDocument.fxml";
    Stage stageRef;
    
    private static App instance;
    
    public App(){
        instance = this;
    }
    
    public static App getInstance(){
        return instance;
    }
    
    @Override
    public void start(Stage stage) throws Exception {
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/resources/logo.png")));
        stageRef = stage;
        switchToLogin();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
    public void switchToLogin() throws Exception {
        // Cargamos el archivo del login e instanciamos la escena
        Parent root = FXMLLoader.load(getClass().getResource(loginPath));
        Scene scene = new Scene(root);
        
        // Actualizamos la nueva escena
        stageRef.hide();
        stageRef.setTitle("Login");
        stageRef.setScene(scene);
        stageRef.show();
    }
    
    public void switchToSingin() throws Exception {
        // Cargamos el archivo del login e instanciamos la escena
        Parent root = FXMLLoader.load(getClass().getResource(singinPath));
        Scene scene = new Scene(root);
        
        // Actualizamos la nueva escena
        stageRef.hide();
        stageRef.setTitle("SingIn");
        stageRef.setScene(scene);
        stageRef.show();
    }
    
    public void switchToMain() throws Exception {
        // Cargamos el archivo del login e instanciamos la escena
        Parent root = FXMLLoader.load(getClass().getResource(mainPath));
        Scene scene = new Scene(root);
        
        // Actualizamos la nueva escena
        stageRef.hide();
        stageRef.setTitle("Run Tracker - IPC");
        stageRef.setScene(scene);
        stageRef.show();
    }
    
    
}
