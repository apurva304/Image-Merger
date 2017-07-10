package sample;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class Controller {
    public Label label_progress;
    public ProgressBar progress_bar = new ProgressBar();
    public ToggleGroup orientation = new ToggleGroup();;
    List<File> selectedFiles;
    List<File>sublist;
    private String dirPath;
    private Service<Void>backgroundThread;

    public Button btn_input_image;
    public Button btn_browse_output_path;
    public Button generate;
    public RadioButton radio_land;
    public RadioButton radio_pot;
    public int iteration;
    public int filegenerated = 0;
    public boolean isportrait = false;

    public int columnNumber = 10;
    public int imagesPerPaper = 53;
    public int unrotatedImages = 50;
    public int rowNumber = 5;


    public void getInput(ActionEvent actionEvent) {
        try {
            showMultipleFileChooser();
        }catch (NullPointerException e){
            label_progress.setText("ERROR : Input file Selection Filed");
        }
    }

    public void getOutputPath(ActionEvent actionEvent) {
        try {
            DirectoryChooser dirChooser = new DirectoryChooser();
            File dir = dirChooser.showDialog(null);
            dirPath = dir.getAbsolutePath();
            System.out.println(dirPath);
        } catch (NullPointerException e){
            label_progress.setText("ERROR : Directory Selection Filed");
        }
    }

    public void generate(ActionEvent actionEvent) {
        if (orientation.getSelectedToggle().equals(radio_pot)){
            isportrait = true;
        }
        if (selectedFiles == null) {
            label_progress.setText("ERROR : Input file not Selected");
        } else if (dirPath == null) {
            label_progress.setText("ERROR : Directory to save files is not Selected");
        } else {
            backgroundThread = new Service<Void>() {
                @Override
                protected Task<Void> createTask() {
                    return new Task<Void>() {
                        @Override
                        protected Void call() throws Exception {
                            try {
                                for (int k = 0; k < iteration; k++) {
                                    try {
                                        sublist = selectedFiles.subList((k * imagesPerPaper) + 0, (k * imagesPerPaper) + imagesPerPaper);
                                        combineImagesIntoPDF(sublist,isportrait);
                                        filegenerated++;
                                        updateProgress(filegenerated, iteration);
                                    } catch (IndexOutOfBoundsException e) {
                                        sublist = selectedFiles.subList((k * imagesPerPaper) + 0, selectedFiles.size());
                                        combineImagesIntoPDF(sublist,isportrait);
                                        filegenerated++;
                                        updateProgress(filegenerated, iteration);
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            updateMessage("File Generation Completed");
                            return null;
                        }
                    };
                }
            };

            backgroundThread.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent event) {
                    progress_bar.progressProperty().unbind();
                    label_progress.textProperty().unbind();
                }
            });

            progress_bar.progressProperty().unbind();
            progress_bar.progressProperty().bind(backgroundThread.progressProperty());
            label_progress.textProperty().bind(backgroundThread.messageProperty());
            backgroundThread.start();
        }
    }


    private void showMultipleFileChooser() {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Image chooser");
        selectedFiles = fileChooser.showOpenMultipleDialog(null);
        System.out.println(selectedFiles.size());
        if (selectedFiles.size()%imagesPerPaper == 0){
            iteration = selectedFiles.size()/imagesPerPaper;
        }else{
            iteration = (selectedFiles.size()/imagesPerPaper) + 1;
        }
    }

    private void combineImagesIntoPDF(List<File>inputDirsAndFiles,boolean portraitOri) throws IOException {
        BufferedImage result = new BufferedImage(
                10800,3600, //work these out
                BufferedImage.TYPE_INT_RGB);
        result.setRGB(255,255,255);
        Graphics2D g =(Graphics2D) result.getGraphics();
        g.setColor(Color.white);
        g.setBackground(Color.white);
        g.fillRect ( 0, 0, result.getWidth(), result.getHeight() );
        int x =0,y = 0;
        int count = 1;
        for(File image : inputDirsAndFiles){

            BufferedImage bi = ImageIO.read(image);
            if (portraitOri) {
                createRotatedCopy(bi);
            }
            // add padding for the rotated image from left start

            //TODO check this if rotation required are higher than 3

            if (unrotatedImages < imagesPerPaper){
                if (count > unrotatedImages ){
                    bi = createRotatedCopy(bi);
                    x = bi.getHeight()*columnNumber;
                    y = bi.getHeight()*(count - unrotatedImages -1);
                }
//                if (count == unrotatedImages + 2){
//                    bi = createRotatedCopy(bi);
//                    x  = bi.getHeight()*columnNumber;
//                    y = bi.getHeight();
//                }
//                if (count == unrotatedImages + 3){
//                    bi = createRotatedCopy(bi);
//                    x  = bi.getHeight()*columnNumber;
//                    y = bi.getHeight()*2;
//                }
            }


            g.drawImage(bi,x, y, null);
            x += bi.getWidth();
          /*  if(x > result.getWidth()-614){
                x = 0;
                y += bi.getHeight();
            }*/
            if (count%columnNumber == 0){
                x = 0;
                y += bi.getHeight();
            }

            count++;
        }

        ImageIO.write(result,"png",new File(dirPath+"\\result"+System.currentTimeMillis()+".jpg"));
        System.out.println("file generated");

    }

    private static BufferedImage createRotatedCopy(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();


        BufferedImage rot = new BufferedImage(h, w, BufferedImage.TYPE_INT_RGB);

        double theta;

        theta = Math.PI / 2;


        AffineTransform xform = new AffineTransform();
        xform.translate(0.5*h, 0.5*w);
        xform.rotate(theta);
        xform.translate(-0.5*w, -0.5*h);
        Graphics2D g = (Graphics2D) rot.createGraphics();
        g.drawImage(img, xform, null);
        g.dispose();

        return rot;
    }
}
