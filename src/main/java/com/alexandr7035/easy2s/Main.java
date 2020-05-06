package com.alexandr7035.easy2s;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.File;
import java.io.InputStream;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.FileChooser;

import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import javafx.fxml.FXMLLoader;



import javafx.scene.input.*;

import java.util.logging.*;


public class Main extends Application {
    
    private static final int WINDOW_WIDTH = 400;
    private static final int WINDOW_HEIGTH = 300;

    private Button printFirstBtn;
    private Button printSecondBtn;
    private Button setPrintedFileBtn;
    private Button resetPrintedFileBtn;
    private Button viewDocBtn;

    private Label printedFileField;
    private Label pagesCountLabel;

    private ProgressBar progressBar;
    private Task prepareDocTask;

    private GridPane mainLayout;
    private Scene scene;

    private FileChooser fileChooser;

    private Document printedDoc;

    private Logger logger;

    // Class constructor
    public Main() {
        
        Settings.initApp();

        // FIXME 
        InputStream stream = Main.class.getClassLoader().getResourceAsStream("logging.properties");
        try {
            LogManager.getLogManager().readConfiguration(stream);
            this.logger = Logger.getLogger(Main.class.getName());
            

            FileHandler fh = new FileHandler(Settings.LOG_PATTERN, 
                                            Settings.LOG_LIMIT,
                                            Settings.LOG_FILES_NUMBER,
                                            true);

            SimpleFormatter sf = new SimpleFormatter();  
            fh.setFormatter(sf);
            logger.addHandler(fh);

        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("");
        logger.info("the app has been started now");


        this.printedDoc = null;


    }

    @Override
    public void start(Stage stage) {
        
        // Load FXML
        this.loadFXML();
        this.scene = new Scene(mainLayout);
        stage.setScene(scene);
        // stage.initStyle(StageStyle.UNDECORATED);
        
        // Apply styles
        scene.getStylesheets().add(Main.class.getResource("/css/main.css").toExternalForm());

        // Set app title
        stage.setTitle(Settings.WINDOW_TITLE);

        // Widgets
        this.printedFileField = (Label) scene.lookup("#printedFileField");
        this.pagesCountLabel = (Label) scene.lookup("#pagesCountLabel");
        this.progressBar = (ProgressBar) scene.lookup("#progressBar");
        this.fileChooser = new FileChooser();

        // printFirstBtn
         // FIXME use lambda
        this.printFirstBtn = (Button) scene.lookup("#printFirstBtn");
        this.printFirstBtn.setOnAction(new EventHandler<ActionEvent>() {
 
            @Override
            public void handle(ActionEvent event) {
                logger.info("pressed button " + "<printFirstBtn>");

                if (PrintWrapper.printFirst(Main.this.printedDoc) == 0) {
                    logger.info("print[1] job successfullty sent to printer");
                } 
                else {
                    logger.warning("failed to send print[1] job to printer");
                }

            }
        });

        // printSecondBtn
        // FIXME use lambda
        this.printSecondBtn = (Button) scene.lookup("#printSecondBtn");
        this.printSecondBtn.setOnAction(new EventHandler<ActionEvent>() {
 
            @Override
            public void handle(ActionEvent event) {
                logger.info("pressed button " + "<printSecondBtn>");

                if (PrintWrapper.printSecond(Main.this.printedDoc) == 0) {
                    logger.info("print[2] job successfullty sent to printer");
                } 
                else {
                    logger.warning("failed to send print[2] job to printer");
                }
            }
        });


        // setPrintedFileBtn
        // FIXME use lambda
        this.setPrintedFileBtn = (Button) scene.lookup("#setPrintedFileBtn");
        //this.setPrintedFileBtn.setDisable(true);
        this.setPrintedFileBtn.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                logger.info("pressed button " + "<setPrintedFileBtn>");
                
                // Show file chooser and get path
                // Call setPrintedFile if file is selected
                File file = fileChooser.showOpenDialog(stage);

                if (file != null) {
                    Main.this.resetPrintedFile();
                    logger.info("choosed file " + file.getAbsolutePath());
                    Main.this.setPrintedFile(file.getAbsolutePath());
                }
                else {
                    logger.info("file is not choosed (openFileDialog closed)");
                }
            }
        });
        
        // resetPrintedFileBtn
        // FIXME use lambda
        this.resetPrintedFileBtn = (Button) scene.lookup("#resetPrintedFileBtn");
        this.resetPrintedFileBtn.setOnAction(new EventHandler<ActionEvent>() {
 
            @Override
            public void handle(ActionEvent event) {
                logger.info("pressed button " + "<resetPrintedFileBtn>");
                Main.this.resetPrintedFile();
            }
        });

        // viewDocBtn
        // FIXME use lambda
        this.viewDocBtn = (Button) scene.lookup("#viewDocBtn");
        this.viewDocBtn.setOnAction(new EventHandler<ActionEvent>() {
            
            @Override
            public void handle(ActionEvent event) {
                logger.info("pressed button " + "<viewDocBtn>");

                ArrayList<String> viewCommand = new ArrayList(Arrays.asList("xdg-open", 
                                                         Main.this.printedDoc.getPreparedDocPath()));

                logger.info("execute command " + viewCommand.toString());

                CmdExecutor.executeSilentCommand(viewCommand);
            }
        });
        
        // Call resetPrintedFile on start to disable buttons
        this.resetPrintedFile();

        // Allow dropping file to textArea
        this.enableFileDropping();

        stage.show();

    }

    private void loadFXML() {
        FXMLLoader loader = new FXMLLoader();

        loader.setLocation(getClass().getResource("/views/main.fxml"));
        this.mainLayout = null;
        
        try {
            this.mainLayout = (GridPane) loader.load(); 

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Called when printed file is dropped to printFileLabel
    // or when Select button is presed.
    // Tries to convert document to pdf (see Document.prepareDocument() method)
    // If succeeded, enables print and other buttons
    // If no, calls resetPrintedFile() method
    private void setPrintedFile(String filePath) {

        // FIXME can document be extended from File???
        File doc_file = new File(filePath);
        this.printedDoc = new Document(doc_file);

        // Set pages conunt to "..." while loading
        this.pagesCountLabel.setText("...");

        // Set printedFileField's text to '...' while loading
        this.printedFileField.setText("...");

        // Disable widgets
        this.setPrintedFileBtn.setDisable(true);
        this.viewDocBtn.setDisable(true);
        this.printFirstBtn.setDisable(true);
        this.printSecondBtn.setDisable(true);

        // Enabled for cancelling
        this.resetPrintedFileBtn.setDisable(false);

        // Use background Task to prepare doc
        prepareDocTask = new Task<Boolean>() {

            @Override
            protected Boolean call() throws Exception {
                return Main.this.printedDoc.prepareDoc();
            }
        };

        // Execute when task finishes
        prepareDocTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, 
            new EventHandler<WorkerStateEvent>() {
               @Override
                  public void handle(WorkerStateEvent t) {
                    // Get the result of the task (boolean)
                    // True if document was converted to pdf succesfully
                    Boolean task_result = (Boolean) prepareDocTask.getValue();

                    if (task_result == true) { 

                        logger.info("converted to pdf succesfully");

                        // Unbind progressBar 
                        progressBar.progressProperty().unbind();
                        progressBar.setProgress(0);
                        
                        // Enable print buttons
                        // printFirstButton is enabled only if PDF contains more than 1 page 
                        // (because it prints even pages)
                        if (printedDoc.getPagesCount() > 1 ) {
                             printFirstBtn.setDisable(false);
                        }
                        printSecondBtn.setDisable(false);
                        resetPrintedFileBtn.setDisable(false);
                        viewDocBtn.setDisable(false);
                        setPrintedFileBtn.setDisable(false);

                        // Set info to widgets
                        printedFileField.setText(doc_file.getName());
                        pagesCountLabel.setText("" + printedDoc.getPagesCount());

                        //System.out.println("PAGES: " + printedDoc.getPagesCount());
                        }

                        // If conversion to pdf is not successfull 
                        else {
                            logger.warning("failed to convert document to pdf, call resetPrintedFile()");
                           // See resetPrintedFile() method
                           resetPrintedFile();
                        }
                        
                        prepareDocTask = null;
                    }
                 
             });
             
        // Bind task to progressBar and run
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(prepareDocTask.progressProperty());
        logger.info("started converting document to PDF");
        new Thread(prepareDocTask).start();
        
      }

    
    // Clear all widgets and disable buttons as when program is started
    private void resetPrintedFile() {

        logger.info("resetPrintedFile method called");

        // Kill prepareDocTask if it was running
        if(prepareDocTask != null) {
            prepareDocTask.cancel();
            prepareDocTask = null;
            // Unbind progressBar 
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
            logger.info("killed running prepareDocTask");
        }

        // Reset widgets
        this.printedFileField.setText("🖨 Drop your 🖨\n document\n here ⬇️");
        this.pagesCountLabel.setText("—");

        // Disable buttons
        this.printFirstBtn.setDisable(true);
        this.printSecondBtn.setDisable(true);
        this.resetPrintedFileBtn.setDisable(true);
        this.viewDocBtn.setDisable(true);

        // Enable setPrintedFile bitton
        this.setPrintedFileBtn.setDisable(false);

        // Set printedDoc to null
        this.printedDoc = null;
    }

    
    // This part is responsible for dropping file to printFileLabel
    // If dropped, Main.setPrintedFile() method is called
    private void enableFileDropping() {
        this.printedFileField.setOnDragOver(new EventHandler<DragEvent>() {

            @Override
            public void handle(DragEvent event) {
                if (event.getGestureSource() != printedFileField
                        && event.getDragboard().hasFiles()) {
                    /* allow for both copying and moving, whatever user chooses */
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                }
                event.consume();
            }
        });

        this.printedFileField.setOnDragDropped(new EventHandler<DragEvent>() {

            @Override
            public void handle(DragEvent event) {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasFiles()) {
                    
                    // Reset printed file if was set before
                    Main.this.resetPrintedFile();
                    // Set printed file
                    logger.info("dropped document \"" + db.getFiles().get(0).getAbsolutePath() + "\" to <printedFileField>");
                    Main.this.setPrintedFile(db.getFiles().get(0).getAbsolutePath());
                    success = true;
                }
                /* let the source know whether the string was successfully 
                 * transferred and used */
                event.setDropCompleted(success);
                event.consume();
            }
        });

    }

    public static void main(String[] args) {
        launch(args);
    }
}
