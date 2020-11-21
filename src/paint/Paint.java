/*
Mihal Zavalani
 */
package paint;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import static javafx.scene.paint.Color.TRANSPARENT;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Text;
import javafx.scene.transform.Translate;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Pair;
import javax.imageio.ImageIO;

/**
 *
 *
 * Emulate MS Paint and its features
 *
 * @author Mihal
 * @version 1.3
 */
public class Paint extends Application {

    //  ImageView picture = new ImageView();
    File file;
    Label label = new Label("1.0");
    boolean saved = false;
    Pair initialPos;

    /* Variables for implementing polygon input. */
    private double[] x, y;    // Arrays containing the points of 
    //   the polygon.  Up to 500 points 
    //   are allowed.

    private int pointCnt;  // The number of points that have been input.

    private boolean complete;   // Set to true when the polygon is complete.
    // When this is false, only a series of lines are drawn.
    // When it is true, a filled polygon is drawn.

    ColorPicker cp = new ColorPicker();
    ColorPicker cpFill = new ColorPicker(Color.TRANSPARENT);

    double orgSceneX, orgSceneY;
    double orgTranslateX, orgTranslateY;
    Group imageLayer = new Group(); //group of images
    ImageView imageView;

    Stage primaryStage;
    WritableImage writableImage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        //undo and redo
        Stack<Shape> undoHistory = new Stack();
        Stack<Shape> redoHistory = new Stack();
//setup
        primaryStage.setTitle("Paint");
        Canvas canvas = new Canvas(700, 900);

        final GraphicsContext graphicsContext = canvas.getGraphicsContext2D();

        VBox root = new VBox();             //making the window
        Pane canvasHolder = new Pane(canvas);

        VBox menuBox = new VBox();                      //creating the canvas 1
        menuBox.setPrefWidth(1000);
        root.getChildren().addAll(menuBox, canvasHolder);

        Scene scene = new Scene(root, 900, 700);

        primaryStage.setScene(scene);

        x = new double[500];  // create arrays to hold the polygon's points
        y = new double[500];
        pointCnt = 0;

        drawPolygon(canvas);
//menuitems and togglebuttons
        final Menu FileBTN = new Menu("File");
        final Menu MainHelpBtn = new Menu("Help");

        MenuItem Open = new MenuItem("Open Image <3");
        MenuItem Save = new MenuItem("Save");//menu subdivisions
        MenuItem SaveAs = new MenuItem("Save As");
        MenuItem Exit = new MenuItem("Exit");
        MenuItem About = new MenuItem("About");

        FileBTN.getItems().addAll(Open, Save, SaveAs, Exit);    // adding subdivisions to menu file button
        MainHelpBtn.getItems().addAll(About);

        MenuBar bar = new MenuBar();                    //creating menubar
        bar.getMenus().addAll(FileBTN, MainHelpBtn);                 //adds main file btn to menubar
        menuBox.getChildren().add(bar);                 //adds menubar to vbox
        bar.prefWidthProperty().bind(menuBox.widthProperty());    //positions the menubar

        ToggleButton drawBtn = new ToggleButton("Draw");
        ToggleButton eraserBtn = new ToggleButton("Eraser");
        ToggleButton linebtn = new ToggleButton("Line");
        ToggleButton rectbtn = new ToggleButton("Rectange");
        ToggleButton circlebtn = new ToggleButton("Circle");
        ToggleButton elpslebtn = new ToggleButton("Ellipse");
        ToggleButton polygonbtn = new ToggleButton("Polygon");
        ToggleButton rectEraser = new ToggleButton("Teleporter");

        ToggleButton textbtn = new ToggleButton("Text");

        ToggleButton colorDropper = new ToggleButton("Color Dropper");

        ToggleButton[] toolsArr = {drawBtn, eraserBtn, linebtn, rectbtn, circlebtn, elpslebtn, rectEraser, polygonbtn, textbtn, colorDropper};

        ToggleGroup tools = new ToggleGroup();

        for (ToggleButton tool : toolsArr) {
            tool.setMinWidth(90);
            tool.setToggleGroup(tools);
            tool.setCursor(Cursor.HAND);
        }

        Tooltip drawTip = new Tooltip("Draw a free line");
        drawBtn.setTooltip(drawTip);
        Tooltip lineTip = new Tooltip("Draw a straight line");
        linebtn.setTooltip(lineTip);
        Tooltip rectTip = new Tooltip("Draw a rectangle");
        rectbtn.setTooltip(rectTip);
        Tooltip circTip = new Tooltip("Draw a circle");
        circlebtn.setTooltip(circTip);

        //////logging//////////
        TimerTask time = new TimerTask() {

            @Override
            public void run() {

                Platform.runLater(new Runnable() {
                    public void run() {
                        if (drawBtn.isSelected()) {
                            System.out.println("Free Draw is selected at " + new Date());
                        } else if (eraserBtn.isSelected()) {
                            System.out.println("Eraser is selected at " + new Date());
                        } else if (linebtn.isSelected()) {
                            System.out.println("Straight Line tool is selected at " + new Date());

                        } else if (rectbtn.isSelected()) {
                            System.out.println("Rectangle tool is selected at " + new Date());
                        } else if (circlebtn.isSelected()) {
                            System.out.println("Circle tool is selected at " + new Date());
                        } else if (elpslebtn.isSelected()) {
                            System.out.println("Elipse tool is selected at " + new Date());
                        } else if (textbtn.isSelected()) {
                            System.out.println("Text tool is selected at " + new Date());
                        } else if (polygonbtn.isSelected()) {
                            System.out.println("Polygon tool is selected at " + new Date());
                        } else if (colorDropper.isSelected()) {
                            System.out.println("Color Dropper tool is selected at " + new Date());
                        } else {
                            System.out.println("Nothing is selected at " + new Date());
                        }

                    }

                });

            }
        };

        Timer timer = new Timer("MyTimer");//create a new Timer
        timer.scheduleAtFixedRate(time, 0, 1000);
        /////////////////////colorbar/////////////
        cp.setOnAction(e -> {
            graphicsContext.setStroke(cp.getValue());
        });
        cpFill.setOnAction(e -> {
            graphicsContext.setFill(cpFill.getValue());
        });
        root.getChildren().addAll(cp, cpFill);

        EventHandler<ActionEvent> FileOpen = new EventHandler<ActionEvent>() {          //event handling for opening a pic 
            public void handle(ActionEvent e) {
                FileChooser fileChooser = new FileChooser();

                FileChooser.ExtensionFilter extFilterJPG = new FileChooser.ExtensionFilter("JPG files (*.jpg)", "*.JPEG");
                FileChooser.ExtensionFilter extFilterPNG = new FileChooser.ExtensionFilter("PNG files (*.png)", "*.PNG");
                FileChooser.ExtensionFilter extFilterJPEG = new FileChooser.ExtensionFilter("GIF files (*.gif)", "*.GIF");
                fileChooser.getExtensionFilters().addAll(extFilterJPG, extFilterPNG, extFilterJPEG);

                File file = fileChooser.showOpenDialog(null);   //popping up an "Open File" file chooser dialog

                try {

                    BufferedImage buffIMG = ImageIO.read(file);             //creates image by looking at selected file
                    WritableImage image = SwingFXUtils.toFXImage(buffIMG, null);  //converting from swing to fx image
                    setCanvas(canvas, image);
                } catch (IOException ex) {
                    Logger.getLogger(Paint.class.getName()).log(Level.SEVERE, null, ex);   //handling non images
                }
            }
        };
        Open.setOnAction(FileOpen);       //setting this action to the Open Option on the menu

        Stage save = new Stage();                                       //Event for Saving a File

        EventHandler<ActionEvent> FileSaveAs = new EventHandler<ActionEvent>() {      //save event handler
            public void handle(ActionEvent e) {
                saved = true;
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save Image");     //setting title for new window that pops up when we click on save button

                FileChooser.ExtensionFilter extFilterJPG = new FileChooser.ExtensionFilter("JPG files (*.jpg)", "*.JPEG");
                FileChooser.ExtensionFilter extFilterPNG = new FileChooser.ExtensionFilter("PNG files (*.png)", "*.PNG");
                FileChooser.ExtensionFilter extFilterJPEG = new FileChooser.ExtensionFilter("GIF files (*.gif)", "*.GIF");
                fileChooser.getExtensionFilters().addAll(extFilterJPG, extFilterPNG, extFilterJPEG);

                file = fileChooser.showSaveDialog(save);       //making sure the window that pops up is a save window

                if (file != null) {
                    try {
                        WritableImage writableImage = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
                        imageLayer.snapshot(null, writableImage);
                        RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);

                        ImageIO.write(renderedImage, "png", file);//saving image as png
                        ImageIO.write(renderedImage, "jpeg", file);
                        ImageIO.write(renderedImage, "gif", file);
                    } catch (IOException ex) {
                        Logger.getLogger(Paint.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    Alert alert = new Alert(Alert.AlertType.WARNING);

                    alert.setTitle("WARNING");
                    alert.setHeaderText(null);
                    alert.setContentText("JPEG, which stands for Joint Photographic Experts Groups is a “lossy” format meaning that the image is compressed to make a smaller file.\n"
                            + " The compression does create a loss in quality.\n"
                            + "GIF and PNG are lossless");

                    alert.showAndWait();
                }
            }
        };
        SaveAs.setOnAction(FileSaveAs);   //setting event to saveAs button

        Save.setOnAction(e -> {
            FileChooser.ExtensionFilter extFilterJPG = new FileChooser.ExtensionFilter("JPEG files (*.jpg)", "*.JPEG");
            FileChooser.ExtensionFilter extFilterPNG = new FileChooser.ExtensionFilter("PNG files (*.png)", "*.PNG");
            FileChooser.ExtensionFilter extFilterJPEG = new FileChooser.ExtensionFilter("GIF files (*.gif)", "*.GIF");

            if (file != null) {
                try {
                    saved = true;
                    WritableImage writableImage = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
                    imageLayer.snapshot(null, writableImage);
                    RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);
                    ImageIO.write(renderedImage, "png", file);
                    ImageIO.write(renderedImage, "jpeg", file);
                    ImageIO.write(renderedImage, "gif", file);

                } catch (IOException ex) {
                    Logger.getLogger(Paint.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        });

        EventHandler<ActionEvent> Shutdown = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                if (saved == true) {
                    Platform.exit();
                    System.exit(0);
                }
                Alert Close = new Alert(AlertType.NONE);
                Close.setAlertType(AlertType.CONFIRMATION);     //asking for approval
                Close.setContentText("Do you want to save your image");

                Optional<ButtonType> result = Close.showAndWait();      //waiting for response

                if ((result.isPresent()) && (result.get() == ButtonType.OK)) {

                    FileChooser.ExtensionFilter extFilterJPG = new FileChooser.ExtensionFilter("JPG files (*.jpg)", "*.JPEG");
                    FileChooser.ExtensionFilter extFilterPNG = new FileChooser.ExtensionFilter("PNG files (*.png)", "*.PNG");
                    FileChooser.ExtensionFilter extFilterJPEG = new FileChooser.ExtensionFilter("GIF files (*.gif)", "*.GIF");

                    if (file != null) {
                        try {
                            saved = true;
                            WritableImage writableImage = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
                            imageLayer.snapshot(null, writableImage);
                            RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);
                            ImageIO.write(renderedImage, "png", file);
                            ImageIO.write(renderedImage, "jpeg", file);
                            ImageIO.write(renderedImage, "gif", file);

                        } catch (IOException ex) {
                            Logger.getLogger(Paint.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                    primaryStage.close();

                }
            }
        };

        Exit.setOnAction(Shutdown); //setting event to close button on menu

        EventHandler<ActionEvent> Abt = new EventHandler<ActionEvent>() {     //Event for when the about button is presses in the menuBar  
            @Override
            public void handle(ActionEvent event) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setAlertType(AlertType.INFORMATION);
                alert.setTitle("About");
                alert.setHeaderText(null);
                alert.setContentText("Mihal's Pain(t) Version 1.3 -  09/21/2020\n"
                        + "\n"
                        + "New features:\n"
                        + "\n"
                        + "* The software shall allow you add text to your image. \n"
                        + "\n"
                        + "* The software shall drawn an additional shape of your choice.\n"
                        + "\n"
                        + "* The software shall have an eraser tool.\n"
                        + "\n"
                        + "* The software shall have a redo button, along with an undo button.  \n"
                        + "\n"
                        + "* The software will indicate which tool is selected and allow for no tool to be selected.\n"
                        + "\n"
                        + "* The software shall have drawing tools will let the user see what they are doing as they are doing it \n"
                        + "\n"
                        + "* Your help should provide access to the release notes and some information about how to use the tool.\n"
                        + "\n"
                        + "* The software shall have the ability to select and move a piece of image (not just delete, move!).  \n"
                        + "\n"
                        + "* The software shall allow the user to specify the number of sides a polygon will have and draw it.  \n"
                        + "\n"
                        + "Known issues:\n"
                        + "\n"
                        + "  * Image Rectangle Selector not working\n"
                        + "\n"
                        + "For the tools, you pick one option and it will do different things.\n"
                        + "Draw is a free line.\n"
                        + "Eraser is an eraser.\n"
                        + "Line is a straight line.\n"
                        + "Rectangle, circle and ellipse are straightforward.\n"
                        + "For the text, when the button is clicked, text that is on the label will appear wherever you click on the canvas.\n"
                        + "Polygon allows you to have multiple points that connect until you want to close the figure and make a shape.\n"
                        + "The bar at the bottom controls line width.\n"
                        + "The two color pickers at the top control line and inner color."
                );

                alert.showAndWait();
            }
        };
        About.setOnAction(Abt);
//zoom in/out
        Button zoomIn = new Button("Zoom In");
        Button zoomOut = new Button("Zoom Out");
        root.getChildren().addAll(zoomIn, zoomOut);

        zoomIn.setOnAction(e -> {
            double zoomAmt = 1.1;
            imageLayer.setScaleX(root.getScaleX() * zoomAmt);
            imageLayer.setScaleY(root.getScaleY() * zoomAmt);

        });

        zoomOut.setOnAction(e -> {
            double zoomAmt = 1.1;
            imageLayer.setScaleX(root.getScaleX() / zoomAmt);
            imageLayer.setScaleY(root.getScaleY() / zoomAmt);

        });

        //slider for width////////////
        Slider slider = new Slider();

        slider.setShowTickLabels(true);
        slider.valueProperty().addListener(e -> {

            double value = slider.getValue();
            String str = String.format("%.if", value);
            label.setText(str);
            graphicsContext.setLineWidth(value);

        });

        slider.valueProperty().addListener(
                new ChangeListener<Number>() {

            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

                label.setText("value: " + newValue);
                graphicsContext.setLineWidth((double) newValue);
            }
        });

        slider.setMinWidth(1);
        slider.setMaxWidth(100);
//adding text
        TextArea text = new TextArea();
        text.setPrefRowCount(1);
        //////////////////////////canvas mouse events and shapes////////////////////
        Line line = new Line();
        Rectangle rect = new Rectangle();
        Circle circ = new Circle();
        Ellipse elps = new Ellipse();

        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED,
                new EventHandler<MouseEvent>() {
            @Override

            public void handle(MouseEvent event) {
                if (drawBtn.isSelected()) {
                    graphicsContext.setStroke(cp.getValue());
                    graphicsContext.beginPath();
                    graphicsContext.lineTo(event.getX(), event.getY());
                    saved = false;
                } else if (eraserBtn.isSelected()) {
                    double lineWidth = graphicsContext.getLineWidth();
                    graphicsContext.clearRect(event.getX() - lineWidth / 2, event.getY() - lineWidth / 2, lineWidth, lineWidth);
                } else if (linebtn.isSelected()) {
                    graphicsContext.setStroke(cp.getValue());
                    line.setStartX(event.getX());
                    line.setStartY(event.getY());
                    graphicsContext.beginPath();
                    saved = false;
                } else if (rectbtn.isSelected()) {
                    graphicsContext.setStroke(cp.getValue());
                    graphicsContext.setFill(cpFill.getValue());
                    rect.setX(event.getX());
                    rect.setY(event.getY());
                    saved = false;
                } else if (circlebtn.isSelected()) {
                    graphicsContext.setStroke(cp.getValue());
                    graphicsContext.setFill(cpFill.getValue());
                    circ.setCenterX(event.getX());
                    circ.setCenterY(event.getY());
                    saved = false;
                } else if (elpslebtn.isSelected()) {
                    graphicsContext.setStroke(cp.getValue());
                    graphicsContext.setFill(cpFill.getValue());
                    elps.setCenterX(event.getX());
                    elps.setCenterY(event.getY());
                    saved = false;
                } else if (colorDropper.isSelected()) {
                    WritableImage canvImg = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
                    SnapshotParameters params = new SnapshotParameters();
                    params.setFill(TRANSPARENT);
                    canvImg = canvas.snapshot(params, null);
                    int x = (int) event.getX();
                    int y = (int) event.getY();
                    PixelReader reader = canvImg.getPixelReader();
                    Color color = reader.getColor(x, y);
                    cp.setValue(color);

                    graphicsContext.setStroke(color);
                    colorDropper.setSelected(false);
                } else if (textbtn.isSelected()) {

                    graphicsContext.setStroke(cp.getValue());
                    graphicsContext.setFill(cpFill.getValue());

                    graphicsContext.strokeText(text.getText(), event.getX(), event.getY());
                    undoHistory.push(new Text());
                } else if (polygonbtn.isSelected()) {
                    if (complete) {
                        // Start a new polygon at the point that was clicked.
                        complete = false;
                        x[0] = event.getX();
                        y[0] = event.getY();
                        pointCnt = 1;
                    } else if (pointCnt > 0 && pointCnt > 0 && (Math.abs(x[0] - event.getX()) <= 3)
                            && (Math.abs(y[0] - event.getY()) <= 3)) {
                        // User has clicked near the starting point.
                        // The polygon is complete.
                        complete = true;
                    } else if (event.getButton() == MouseButton.SECONDARY || pointCnt == 500) {
                        // The polygon is complete.
                        complete = true;
                    } else {
                        // Add the point where the user clicked to the list of
                        // points in the polygon, and draw a line between the
                        // previous point and the current point.  A line can
                        // only be drawn if there are at least two points.
                        x[pointCnt] = event.getX();
                        y[pointCnt] = event.getY();
                        pointCnt++;
                    }
                    drawPolygon(canvas);  // in all cases, redraw the picture.
                }

            }

        }
        );

        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED,
                new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event
            ) {
                if (drawBtn.isSelected()) {
                    graphicsContext.lineTo(event.getX(), event.getY());
                    graphicsContext.stroke();
                    saved = false;
                } else if (eraserBtn.isSelected()) {
                    double lineWidth = graphicsContext.getLineWidth();
                    graphicsContext.clearRect(event.getX() - lineWidth / 2, event.getY() - lineWidth / 2, lineWidth, lineWidth);

                }

            }
        }
        );

        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED,
                new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event
            ) {
                if (drawBtn.isSelected()) {
                    graphicsContext.lineTo(event.getX(), event.getY());
                    graphicsContext.stroke();

                    saved = false;
                } else if (eraserBtn.isSelected()) {
                    double lineWidth = graphicsContext.getLineWidth();
                    graphicsContext.clearRect(event.getX() - lineWidth / 2, event.getY() - lineWidth / 2, lineWidth, lineWidth);
                } else if (linebtn.isSelected()) {
                    line.setEndX(event.getX());
                    line.setEndY(event.getY());
                    graphicsContext.strokeLine(line.getStartX(), line.getStartY(), line.getEndX(), line.getEndY());
                    saved = false;
                    graphicsContext.getCanvas().removeEventHandler(MouseEvent.MOUSE_DRAGGED, this);
                    undoHistory.push(new Line(line.getStartX(), line.getStartY(), line.getEndX(), line.getEndY()));

                } else if (rectbtn.isSelected()) {
                    saved = false;
                    rect.setWidth(Math.abs((subtract(event.getX(), rect.getX()))));
                    rect.setHeight(Math.abs((event.getY() - rect.getY())));//rect.setX((rect.getX() > e.getX()) ? e.getX(): rect.getX());
                    if (rect.getX() > event.getX()) {
                        rect.setX(event.getX());
                    }

                    if (rect.getY() > event.getY()) {
                        rect.setY(event.getY());
                    }

                    graphicsContext.fillRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
                    graphicsContext.strokeRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());

                    undoHistory.push(new Rectangle(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight()));
                } else if (circlebtn.isSelected()) {
                    saved = false;
                    circ.setRadius((Math.abs(event.getX() - circ.getCenterX()) + Math.abs(event.getY() - circ.getCenterY())) / 2);

                    if (circ.getCenterX() > event.getX()) {
                        circ.setCenterX(event.getX());
                    }
                    if (circ.getCenterY() > event.getY()) {
                        circ.setCenterY(event.getY());
                    }

                    graphicsContext.fillOval(circ.getCenterX(), circ.getCenterY(), circ.getRadius(), circ.getRadius());
                    graphicsContext.strokeOval(circ.getCenterX(), circ.getCenterY(), circ.getRadius(), circ.getRadius());

                    undoHistory.push(new Circle(circ.getCenterX(), circ.getCenterY(), circ.getRadius()));
                } else if (elpslebtn.isSelected()) {
                    saved = false;
                    elps.setRadiusX(Math.abs(event.getX() - elps.getCenterX()));
                    elps.setRadiusY(Math.abs(event.getY() - elps.getCenterY()));

                    if (elps.getCenterX() > event.getX()) {
                        elps.setCenterX(event.getX());
                    }
                    if (elps.getCenterY() > event.getY()) {
                        elps.setCenterY(event.getY());
                    }

                    graphicsContext.strokeOval(elps.getCenterX(), elps.getCenterY(), elps.getRadiusX(), elps.getRadiusY());
                    graphicsContext.fillOval(elps.getCenterX(), elps.getCenterY(), elps.getRadiusX(), elps.getRadiusY());

                    undoHistory.push(new Ellipse(elps.getCenterX(), elps.getCenterY(), elps.getRadiusX(), elps.getRadiusY()));
                }
//               
                redoHistory.clear();
                Shape lastUndo = undoHistory.lastElement();
                lastUndo.setFill(graphicsContext.getFill());
                lastUndo.setStroke(graphicsContext.getStroke());
                lastUndo.setStrokeWidth(graphicsContext.getLineWidth());
            }

        }
        );

////////////////////////////scrollbars/////////////////
        imageLayer.getChildren().add(canvas);

        ScrollPane scrollPane = new ScrollPane(imageLayer);

        scrollPane.setPrefSize(
                500, 500);
        scrollPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        scrollPane.setFitToWidth(
                true);
        scrollPane.setFitToHeight(
                true);
        scrollPane.setStyle(
                "-fx-focus-color: transparent;");
        root.getChildren()
                .add(scrollPane);

        ContextMenu contextMenu = new ContextMenu();

        MenuItem cropMenuItem = new MenuItem("Crop");

        cropMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                if (rectEraser.isSelected()) {
                    // get bounds for image crop
                    RubberBandSelection rubberBandSelection = new RubberBandSelection(imageLayer);
                    Bounds selectionBounds = rubberBandSelection.getBounds();

                    // crop the image
                    crop(selectionBounds, canvas);

                }
            }
        });

        contextMenu.getItems().add(cropMenuItem);

        // set context menu on image layer
        imageLayer.setOnMousePressed(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {
                if (event.isSecondaryButtonDown() && rectEraser.isSelected()) {
                    contextMenu.show(imageLayer, event.getScreenX(), event.getScreenY());
                }
            }
        });

        /////////////TOOLBAR///////////
        ToolBar drawTools = new ToolBar();

        drawTools.setOrientation(Orientation.VERTICAL);

        drawTools.getItems()
                .addAll(drawBtn, eraserBtn, linebtn, rectbtn, circlebtn, elpslebtn, textbtn, text, polygonbtn, zoomIn, zoomOut, colorDropper, rectEraser);
        root.getChildren()
                .add(drawTools);

        ///////////////////////////undo////////////////////////////////
        Button undo = new Button("Undo");

        root.getChildren()
                .add(undo);
        undo.setOnAction(e
                -> {
            saved = false;
            if (!undoHistory.empty()) {
                graphicsContext.clearRect(0, 0, 1080, 790);
                Shape removedShape = undoHistory.lastElement();
                if (removedShape.getClass() == Line.class) {
                    Line tempLine = (Line) removedShape;
                    tempLine.setFill(graphicsContext.getFill());
                    tempLine.setStroke(graphicsContext.getStroke());
                    tempLine.setStrokeWidth(graphicsContext.getLineWidth());
                    redoHistory.push(new Line(tempLine.getStartX(), tempLine.getStartY(), tempLine.getEndX(), tempLine.getEndY()));

                } else if (removedShape.getClass() == Rectangle.class) {
                    Rectangle tempRect = (Rectangle) removedShape;
                    tempRect.setFill(graphicsContext.getFill());
                    tempRect.setStroke(graphicsContext.getStroke());
                    tempRect.setStrokeWidth(graphicsContext.getLineWidth());
                    redoHistory.push(new Rectangle(tempRect.getX(), tempRect.getY(), tempRect.getWidth(), tempRect.getHeight()));
                } else if (removedShape.getClass() == Circle.class) {
                    Circle tempCirc = (Circle) removedShape;
                    tempCirc.setStrokeWidth(graphicsContext.getLineWidth());
                    tempCirc.setFill(graphicsContext.getFill());
                    tempCirc.setStroke(graphicsContext.getStroke());
                    redoHistory.push(new Circle(tempCirc.getCenterX(), tempCirc.getCenterY(), tempCirc.getRadius()));
                } else if (removedShape.getClass() == Ellipse.class) {
                    Ellipse tempElps = (Ellipse) removedShape;
                    tempElps.setFill(graphicsContext.getFill());
                    tempElps.setStroke(graphicsContext.getStroke());
                    tempElps.setStrokeWidth(graphicsContext.getLineWidth());
                    redoHistory.push(new Ellipse(tempElps.getCenterX(), tempElps.getCenterY(), tempElps.getRadiusX(), tempElps.getRadiusY()));
                }
                Shape lastRedo = redoHistory.lastElement();
                lastRedo.setFill(removedShape.getFill());
                lastRedo.setStroke(removedShape.getStroke());
                lastRedo.setStrokeWidth(removedShape.getStrokeWidth());
                undoHistory.pop();

                for (int i = 0; i < undoHistory.size(); i++) {
                    Shape shape = undoHistory.elementAt(i);
                    if (shape.getClass() == Line.class) {
                        Line temp = (Line) shape;
                        graphicsContext.setLineWidth(temp.getStrokeWidth());
                        graphicsContext.setStroke(temp.getStroke());
                        graphicsContext.setFill(temp.getFill());
                        graphicsContext.strokeLine(temp.getStartX(), temp.getStartY(), temp.getEndX(), temp.getEndY());
                    } else if (shape.getClass() == Rectangle.class) {
                        Rectangle temp = (Rectangle) shape;
                        graphicsContext.setLineWidth(temp.getStrokeWidth());
                        graphicsContext.setStroke(temp.getStroke());
                        graphicsContext.setFill(temp.getFill());
                        graphicsContext.fillRect(temp.getX(), temp.getY(), temp.getWidth(), temp.getHeight());
                        graphicsContext.strokeRect(temp.getX(), temp.getY(), temp.getWidth(), temp.getHeight());
                    } else if (shape.getClass() == Circle.class) {
                        Circle temp = (Circle) shape;
                        graphicsContext.setLineWidth(temp.getStrokeWidth());
                        graphicsContext.setStroke(temp.getStroke());
                        graphicsContext.setFill(temp.getFill());
                        graphicsContext.fillOval(temp.getCenterX(), temp.getCenterY(), temp.getRadius(), temp.getRadius());
                        graphicsContext.strokeOval(temp.getCenterX(), temp.getCenterY(), temp.getRadius(), temp.getRadius());
                    } else if (shape.getClass() == Ellipse.class) {
                        Ellipse temp = (Ellipse) shape;
                        graphicsContext.setLineWidth(temp.getStrokeWidth());
                        graphicsContext.setStroke(temp.getStroke());
                        graphicsContext.setFill(temp.getFill());
                        graphicsContext.fillOval(temp.getCenterX(), temp.getCenterY(), temp.getRadiusX(), temp.getRadiusY());
                        graphicsContext.strokeOval(temp.getCenterX(), temp.getCenterY(), temp.getRadiusX(), temp.getRadiusY());
                    }
                }
            } else {
                System.out.println("there is no action to undo");
            }
        }
        );

        Button redo = new Button("Redo");

        root.getChildren()
                .add(redo);

        redo.setOnAction(e
                -> {
            if (!redoHistory.empty()) {
                Shape shape = redoHistory.lastElement();
                graphicsContext.setLineWidth(shape.getStrokeWidth());
                graphicsContext.setStroke(shape.getStroke());
                graphicsContext.setFill(shape.getFill());

                redoHistory.pop();
                if (shape.getClass() == Line.class) {
                    Line tempLine = (Line) shape;
                    graphicsContext.strokeLine(tempLine.getStartX(), tempLine.getStartY(), tempLine.getEndX(), tempLine.getEndY());
                    undoHistory.push(new Line(tempLine.getStartX(), tempLine.getStartY(), tempLine.getEndX(), tempLine.getEndY()));
                } else if (shape.getClass() == Rectangle.class) {
                    Rectangle tempRect = (Rectangle) shape;
                    graphicsContext.fillRect(tempRect.getX(), tempRect.getY(), tempRect.getWidth(), tempRect.getHeight());
                    graphicsContext.strokeRect(tempRect.getX(), tempRect.getY(), tempRect.getWidth(), tempRect.getHeight());

                    undoHistory.push(new Rectangle(tempRect.getX(), tempRect.getY(), tempRect.getWidth(), tempRect.getHeight()));
                } else if (shape.getClass() == Circle.class) {
                    Circle tempCirc = (Circle) shape;
                    graphicsContext.fillOval(tempCirc.getCenterX(), tempCirc.getCenterY(), tempCirc.getRadius(), tempCirc.getRadius());
                    graphicsContext.strokeOval(tempCirc.getCenterX(), tempCirc.getCenterY(), tempCirc.getRadius(), tempCirc.getRadius());

                    undoHistory.push(new Circle(tempCirc.getCenterX(), tempCirc.getCenterY(), tempCirc.getRadius()));
                } else if (shape.getClass() == Ellipse.class) {
                    Ellipse tempElps = (Ellipse) shape;
                    graphicsContext.fillOval(tempElps.getCenterX(), tempElps.getCenterY(), tempElps.getRadiusX(), tempElps.getRadiusY());
                    graphicsContext.strokeOval(tempElps.getCenterX(), tempElps.getCenterY(), tempElps.getRadiusX(), tempElps.getRadiusY());

                    undoHistory.push(new Ellipse(tempElps.getCenterX(), tempElps.getCenterY(), tempElps.getRadiusX(), tempElps.getRadiusY()));
                }
                Shape lastUndo = undoHistory.lastElement();
                lastUndo.setFill(graphicsContext.getFill());
                lastUndo.setStroke(graphicsContext.getStroke());
                lastUndo.setStrokeWidth(graphicsContext.getLineWidth());
            } else {
                System.out.println("cannot redo");
            }
        }
        );

        ////////////////////////////keyboard handlerssssss///////////////////
        scene.addEventFilter(KeyEvent.KEY_PRESSED,
                new EventHandler<KeyEvent>() {
            final KeyCombination keyComb = new KeyCodeCombination(KeyCode.S,
                    KeyCombination.CONTROL_ANY);

            public void handle(KeyEvent ke) {
                if (keyComb.match(ke)) {
                    FileChooser.ExtensionFilter extFilterJPG = new FileChooser.ExtensionFilter("JPG files (*.jpg)", "*.JPG");
                    FileChooser.ExtensionFilter extFilterPNG = new FileChooser.ExtensionFilter("PNG files (*.png)", "*.PNG");
                    FileChooser.ExtensionFilter extFilterJPEG = new FileChooser.ExtensionFilter("GIF files (*.gif)", "*.GIF");

                    if (file != null) {
                        try {
                            saved = true;
                            WritableImage writableImage = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
                            canvas.snapshot(null, writableImage);
                            RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);
                            ImageIO.write(renderedImage, "png", file);
                            ImageIO.write(renderedImage, "jpg", file);
                            ImageIO.write(renderedImage, "gif", file);

                        } catch (IOException ex) {
                            Logger.getLogger(Paint.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    ke.consume(); // <-- stops passing the event to next node
                }
            }
        });

        scene.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            final KeyCombination keyComb = new KeyCodeCombination(KeyCode.F,
                    KeyCombination.ALT_ANY);

            public void handle(KeyEvent ke) {
                if (keyComb.match(ke)) {
                    FileChooser fileChooser = new FileChooser();    //creating file chooser
                    File file = fileChooser.showOpenDialog(null);   //popping up an "Open File" file chooser dialog

                    try {

                        BufferedImage buffIMG = ImageIO.read(file);             //creates image by looking at selected file
                        WritableImage image = SwingFXUtils.toFXImage(buffIMG, null);  //converting from swing to fx image
                        setCanvas(canvas, image);
                    } catch (IOException ex) {
                        Logger.getLogger(Paint.class.getName()).log(Level.SEVERE, null, ex);   //handling non images
                    }
                    ke.consume(); // <-- stops passing the event to next node
                }
            }
        });

        TimerTask timerTask = new TimerTask() {

            @Override
            public void run() {

                Platform.runLater(new Runnable() {
                    public void run() {
                        FileChooser.ExtensionFilter extFilterJPG = new FileChooser.ExtensionFilter("JPEG files (*.jpg)", "*.JPEG");
                        FileChooser.ExtensionFilter extFilterPNG = new FileChooser.ExtensionFilter("PNG files (*.png)", "*.PNG");
                        FileChooser.ExtensionFilter extFilterJPEG = new FileChooser.ExtensionFilter("GIF files (*.gif)", "*.GIF");

                        if (file != null) {
                            try {
                                saved = true;

                                WritableImage writableImage = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
                                canvas.snapshot(null, writableImage);
                                RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);
                                ImageIO.write(renderedImage, "png", file);
                                ImageIO.write(renderedImage, "jpeg", file);
                                ImageIO.write(renderedImage, "gif", file);

                            } catch (IOException ex) {
                                Logger.getLogger(Paint.class.getName()).log(Level.SEVERE, null, ex);
                            }

                        }

                    }

                });

            }
        };

        timer.scheduleAtFixedRate(timerTask, 0, 1000);

        primaryStage.setOnCloseRequest((WindowEvent t) -> {
            t.consume();
            if (saved == true) {
                Platform.exit();
                System.exit(0);
                timer.cancel();
            }
            Alert Close = new Alert(AlertType.NONE);
            Close.setAlertType(AlertType.CONFIRMATION);     //asking for approval
            Close.setContentText("Do you want to save your image");

            Optional<ButtonType> result = Close.showAndWait();      //waiting for response

            if ((result.isPresent()) && (result.get() == ButtonType.OK)) {

                FileChooser.ExtensionFilter extFilterJPG = new FileChooser.ExtensionFilter("JPG files (*.jpg)", "*.JPEG");
                FileChooser.ExtensionFilter extFilterPNG = new FileChooser.ExtensionFilter("PNG files (*.png)", "*.PNG");
                FileChooser.ExtensionFilter extFilterJPEG = new FileChooser.ExtensionFilter("GIF files (*.gif)", "*.GIF");

                if (file != null) {
                    try {
                        saved = true;
                        WritableImage writableImage = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
                        imageLayer.snapshot(null, writableImage);
                        RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);
                        ImageIO.write(renderedImage, "png", file);
                        ImageIO.write(renderedImage, "jpeg", file);
                        ImageIO.write(renderedImage, "gif", file);

                    } catch (IOException ex) {
                        Logger.getLogger(Paint.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                Platform.exit();
                timer.cancel();

            } else {
                Platform.exit();
                timer.cancel();
            }
        });

/////////////////////////////////////////////////////////////////////
        root.getChildren().add(slider);
        primaryStage.show();  //showing stage
    }

    private void crop(Bounds bounds, Canvas canvas) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Image");

        File file = fileChooser.showSaveDialog(primaryStage);
        if (file == null) {
            return;
        }

        int width = (int) bounds.getWidth();
        int height = (int) bounds.getHeight();

        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        parameters.setViewport(new Rectangle2D(bounds.getMinX(), bounds.getMinY(), width, height));

        WritableImage wi = new WritableImage(width, height);
        imageLayer.snapshot(parameters, wi);

        BufferedImage bufImageARGB = SwingFXUtils.fromFXImage(wi, null);
        BufferedImage bufImageRGB = new BufferedImage(bufImageARGB.getWidth(), bufImageARGB.getHeight(), BufferedImage.OPAQUE);

        Graphics2D graphics = bufImageRGB.createGraphics();
        graphics.drawImage(bufImageARGB, 0, 0, null);

        try {

            ImageIO.write(bufImageRGB, "jpg", file);

            System.out.println("Image saved to " + file.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
        }

        graphics.dispose();

    }

    private void setImage(WritableImage img) {
        this.writableImage = img;

    }

    private WritableImage getImage() {
        return writableImage;

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * setCanvas takes an image and puts it on top of a canvas by using the
     * canvas width and length properties
     *
     * @param canvas is a canvas object that is used to get the canvas width and
     * length
     * @param img is whatever image we want that is being drawn over the canvas
     *
     */
    public static void setCanvas(Canvas canvas, Image img) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.drawImage(img, 0, 0, canvas.getWidth(), canvas.getHeight());

    }

    private void drawPolygon(Canvas canvas) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.WHITE);

        if (pointCnt == 0) {
            return;
        }
        g.setLineWidth(2);
        g.setStroke(cp.getValue());

        if (complete) { // draw a polygon
            g.setFill(cpFill.getValue());
            g.fillPolygon(x, y, pointCnt);
            g.strokePolygon(x, y, pointCnt);
        } else { // show the lines the user has drawn so far
            g.setFill(Color.BLACK);
            g.fillRect(x[0] - 2, y[0] - 2, 4, 4);  // small square marks first point
            for (int i = 0; i < pointCnt - 1; i++) {
                g.strokeLine(x[i], y[i], x[i + 1], y[i + 1]);
            }
        }
    }

    /**
     * subtract takes two numbers and subtracts them
     *
     * @param a is the first number with a decimal
     * @param b is the second decimal nr being subtracted from the first one
     * return double a-b
     */
    public static double subtract(double a, double b) {
        return a - b;
    }

    /////////////////////////////pic mover/////////////////////////
    /**
     * Drag rectangle with mouse cursor in order to get selection bounds
     */
    public static class RubberBandSelection {

        final DragContext dragContext = new DragContext();
        Rectangle rect = new Rectangle();

        Group group;

        public Bounds getBounds() {
            return rect.getBoundsInParent();
        }

        public Rectangle getRect() {
            return rect;
        }

        public RubberBandSelection(Group group) {

            this.group = group;

            rect = new Rectangle(0, 0, 0, 0);
            rect.setStroke(Color.BLUE);
            rect.setStrokeWidth(1);
            rect.setStrokeLineCap(StrokeLineCap.ROUND);
            rect.setFill(Color.LIGHTBLUE.deriveColor(0, 1.2, 1, 0.6));

            group.addEventHandler(MouseEvent.MOUSE_PRESSED, onMousePressedEventHandler);
            group.addEventHandler(MouseEvent.MOUSE_DRAGGED, onMouseDraggedEventHandler);
            group.addEventHandler(MouseEvent.MOUSE_RELEASED, onMouseReleasedEventHandler);

        }

        EventHandler<MouseEvent> onMousePressedEventHandler = new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {

                if (event.isSecondaryButtonDown()) {
                    return;
                }

                // remove old rect
                rect.setX(0);
                rect.setY(0);
                rect.setWidth(0);
                rect.setHeight(0);

                group.getChildren().remove(rect);

                // prepare new drag operation
                dragContext.mouseAnchorX = event.getX();
                dragContext.mouseAnchorY = event.getY();

                rect.setX(dragContext.mouseAnchorX);
                rect.setY(dragContext.mouseAnchorY);
                rect.setWidth(0);
                rect.setHeight(0);

                group.getChildren().add(rect);

            }
        };

        EventHandler<MouseEvent> onMouseDraggedEventHandler = new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {

                if (event.isSecondaryButtonDown()) {
                    return;
                }

                double offsetX = event.getX() - dragContext.mouseAnchorX;
                double offsetY = event.getY() - dragContext.mouseAnchorY;

                if (offsetX > 0) {
                    rect.setWidth(offsetX);
                } else {
                    rect.setX(event.getX());
                    rect.setWidth(dragContext.mouseAnchorX - rect.getX());
                }

                if (offsetY > 0) {
                    rect.setHeight(offsetY);
                } else {
                    rect.setY(event.getY());
                    rect.setHeight(dragContext.mouseAnchorY - rect.getY());
                }
            }
        };

        EventHandler<MouseEvent> onMouseReleasedEventHandler = new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {

                if (event.isSecondaryButtonDown()) {
                    return;
                }

                // remove rectangle
                // note: we want to keep the ruuberband selection for the cropping => code is just commented out
                /*
                rect.setX(0);
                rect.setY(0);
                rect.setWidth(0);
                rect.setHeight(0);

                group.getChildren().remove( rect);
                 */
            }
        };

        private static final class DragContext {

            public double mouseAnchorX;
            public double mouseAnchorY;

        }
    }
}
