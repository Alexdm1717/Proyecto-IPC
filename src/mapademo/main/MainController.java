/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package mapademo.main;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;

/**
 * FXML Controller class
 *
 * @author alexi
 */
public class MainController implements Initializable {

    @FXML
    private ImageView map;
    @FXML
    private Button switch_map_button;
    @FXML
    private Slider zoom_slider;
    @FXML
    private Button zoom_up_button;
    @FXML
    private Button zoom_down_button;
    @FXML
    private ScrollPane map_scrollpane;
    @FXML
    private Pane map_pane;
    
    private Group zoomGroup;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        zoom_slider.setMin(0.5);
        zoom_slider.setMax(1.5);
        zoom_slider.setValue(1.0);
        
        // Listener que invoca zoom() cada vez que el slider cambia de valor.
        // Usamos una expresión lambda en lugar de una clase anónima por brevedad.
        zoom_slider.valueProperty().addListener(
                (observable, oldVal, newVal) -> zoom((Double) newVal)
        );
    }
    
    @FXML
    void zoomIn(ActionEvent event){
        double sliderVal = zoom_slider.getValue();
        zoom_slider.setValue(sliderVal + 0.1);
    }
    
    @FXML
    void zoomOut(ActionEvent event){
        double sliderVal = zoom_slider.getValue();
        zoom_slider.setValue(sliderVal - 0.1);
    }
    
    
    private void zoom(double scaleValue){
        // Guardamos la posición del scroll antes de escalar
        double scrollH = map_scrollpane.getHvalue();
        double scrollV = map_scrollpane.getVvalue();

        // Aplicamos el zoom escalando el Group en ambos ejes
        zoomGroup.setScaleX(scaleValue);
        zoomGroup.setScaleY(scaleValue);

        // Restauramos la posición del scroll para que el centro visual
        // permanezca estable durante el zoom
        map_scrollpane.setHvalue(scrollH);
        map_scrollpane.setVvalue(scrollV);
    }
    
    @FXML
    private void cambiarMapa(ActionEvent event) throws IOException{
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File("."));
        
        // Configuramos un filtro para solo selecionar solo imagenes
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagenes", "*.png", "*.jpg", "*.jpeg")
        );
        
        // Asignamos el archivo seleccionado temporalmente
        File ogFile = fc.showOpenDialog(zoom_slider.getScene().getWindow());
        
        if (ogFile == null){return;}
        
        String newPath = guardarImagen(ogFile);
        buildMap(ogFile);
    }
    
    
    /**
    *  Gestiona la selecion de imagenes
    * Guardando una copia del archivo dentro del proyeecto
    * Y devolviendo la ruta local para la base de datos
    */
    private String guardarImagen(File archivoOriginal){
        
        // Definimos la carpeta de destino
        File directorioDestino = new File("maps");
        if (!directorioDestino.exists()) {
            directorioDestino.mkdirs();
        }
        
        // Preparar el archivo de destino
        File archivoDestino = new File(directorioDestino, archivoOriginal.getName());
        String rutaRelativa = "maps/" + archivoOriginal.getName();
        
        try{
            // Verificamos si ya existe
            if(!archivoDestino.exists()){
                Files.copy(archivoOriginal.toPath(), archivoDestino.toPath());
                System.out.println("Imagen copiada con exito");
            }
            else {
                System.out.println("La imagen ya existe");
            }
            
            return rutaRelativa;
        } catch(IOException e){
            System.out.println(e);
            return null;
        }
    }
    
    private void buildMap(File imgFile){
        // Comprobacion defensiva
        if (!imgFile.exists()){
            map_scrollpane.setContent(
                new Label("Imagen no encontrada")
            );
            System.out.println("Imagen no encontrada");
            return;
        }
        
        // Cargamos la imagen y obtenemos sus dimensiones reales en pixeles
        Image img = new Image(imgFile.toURI().toString());
        double W = img.getWidth();
        double H = img.getHeight();
        
        // ── mapPane: lienzo del mapa ───────────────────────────────────
        // Usamos un Pane (y no un Group) para poder posicionar los nodos
        // hijos con coordenadas absolutas (setLayoutX / setLayoutY).
        map_pane = new Pane();
        map_pane.setPrefSize(W, H);
        map_pane.setMinSize(W, H);
        map_pane.setMaxSize(W, H);
        
        // Añadimos la imagen como fondo del Pane
        ImageView iv = new ImageView(img);
        iv.setFitWidth(W);
        iv.setFitHeight(H);
        map_pane.getChildren().add(iv);
        
        // ── Manejador de clics sobre el mapa ──────────────────────────
        // Gestionamos el clic derecho (menú contextual) y el clic izquierdo
        // en modo inserción (FIX 2).
        
        // ── Jerarquía de Groups para el zoom ──────────────────────────
        // contentGroup es el nodo raíz que recibe el ScrollPane.
        // zoomGroup es el que se escala; anidar un Group dentro de otro
        // evita que el ScrollPane reajuste su contenido durante el escalado.
        zoomGroup = new Group();
        Group contentGroup = new Group();
        zoomGroup.getChildren().add(map_pane);
        contentGroup.getChildren().add(zoomGroup);
        
        // Aplicamos el zoom actual(valor del slider)
        double zoom = zoom_slider.getValue();
        zoomGroup.setScaleX(zoom);
        zoomGroup.setScaleY(zoom);
        
        // Asignamos el contentGroup como contenido del ScrollPane
        map_scrollpane.setContent(contentGroup);
    }
}
