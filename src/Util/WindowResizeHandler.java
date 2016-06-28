package Util;

import Master.FXUIGameMaster;
import static Master.FXUIGameMaster.DEFAULT_CONTENT_HEIGHT;
import static Master.FXUIGameMaster.DEFAULT_CONTENT_WIDTH;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/**
 * Handles target window resizing duties for a given JavaFX Stage object. The
 * Stage object should have been populated and put up for display BEFORE making
 * use of this class and its functions. (i.e., stage.isShowing() should return
 * true at this point, and stage.getScene() should return a valid Scene object.)
 * 18 October 2015, 18:45
 */
public class WindowResizeHandler {
    public static final String versionInfo = "Window-Resize-Handler\nVersion 0031\nStamp 2016.5.02, 12:00\nStability: Beta";
	
    /*Remaining convenient class-level variables.*/
    private static final int TRIG_BY_WIDTH = 4, TRIG_BY_HEIGHT = 8;
    private Stage activeStage = null;
    private Scene activeScene = null;
    private double desiredAspectRatio = 0.0;
    private double windowDecorationWidth = 0.0, windowDecorationHeight = 0.0,
            activeScreenWidth, activeScreenHeight;
    private ChangeListener<Number> widthPropertyListener = null;
    private ChangeListener<Number> heightPropertyListener = null;
    private AtomicBoolean resizeBlocked = new AtomicBoolean(true);
    //private AtomicBoolean stillRunningResize = new AtomicBoolean(false);
    private AtomicBoolean resizeThreadIsActive = new AtomicBoolean(false);
    private boolean preferScrollNZoom = false;
    private double widthHistory = 0.0, heightHistory = 0.0;
    private double idealContentWidth = 0, idealContentHeight = 0;
    private boolean assumeCallerIsActive = false;
	//attach a slider object to receive feedback from resize operations (percentages only)
    // also dynamically enables/disables during resize operations
    private Slider sliderToAdjust = null;
    private Pane registeredBaseStackPane, registeredStaticContentPane;
    private ScrollPane registeredScrollPane;
    private Group scrollGroupInterface = new Group();
    private double savedZoomeMultiplier = 1.0d;


    /**
     * Class constructor which accepts more parameters. Creates an object
     * responsible for intelligent resizing of a single linked window (Stage),
     * taking window decorations into account. Compared to
     * {@link #WindowResizeHandler(Stage, double, double, double)}, allows you to inform the object of
     * the width and height of the window decorations. (The height of the window
     * decorations makes the height of a window/Stage technically be greater
     * than the height of the window contents/Scene.) (The same applies to the
     * width of the window decorations, if there is a detectable width.)
     * (Without this information being ascertained correctly, may result in
     * resizing errors!)
     *
     * @param desiredStage the stage which may or may not require resizing.
     * Contains the Scene which is treated as the "window contents".
     * @param baseStackPane the pane which encloses all other panes
     * @param desiredStaticContentPane the pane which contains all content, and
     * is placed inside other panes
     * @param desiredScrollPane the scrolling pane which conditionally contains
     * the content pane, or is disabled/left empty when scrolling is not needed.
     * @param aspectRatio target ratio of the content of the window (not
     * necessarily of the window itself!)
     * @param windowDecorationWidth the amount of space taken up vertically by
     * the OS's window decoration (borders, etc). e.g., if both sides are 5px,
     * then the value passed in would be 10.
     * @param windowDecorationHeight the amount of space taken up vertically by
     * the OS's window decoration (window title, minimize/maximize/close
     * buttons, etc). e.g. if the top is 55px and the bottom is 5px, the passed
     * in value would be 60.
     * @param contentWidth the ideal default width of the content (Scene),
     * assuming unlimited screen real estate.
     * @param contentHeight the ideal default height of the content (Scene),
     * assuming unlimited screen real estate.
     */
    public WindowResizeHandler(Stage desiredStage, Pane baseStackPane, Pane desiredStaticContentPane, ScrollPane desiredScrollPane,
    		double aspectRatio, double windowDecorationWidth, double windowDecorationHeight, double contentWidth, double contentHeight)
    {
        verifyIsFXThread();
        if (desiredStage == null || baseStackPane == null || desiredStaticContentPane == null || desiredScrollPane == null || aspectRatio <= 0) {
            throw new UnsupportedOperationException("Select parameters detected as invalid. Status of potentially invalid parameters: desiredStage = "
                    + desiredStage == null ? "null (invalid)" : "not null (valid)" + " aspectRatio = " + aspectRatio + "(should be > 0 to be valid)");
        } else if (!desiredStage.isShowing()) {
            throw new UnsupportedOperationException("Requires a technically visible Stage object. Aborting creation.");
        } else if (windowDecorationWidth < 0 || windowDecorationHeight < 0) {
            throw new UnsupportedOperationException("Window decoration dimension parameters detected as potentially invalid. Status of parameters: windowDecorationWidth = "
                    + windowDecorationWidth + (windowDecorationWidth < 0 ? "(invalid)" : "(valid)")
                    + " && windowDecorationHeight = "
                    + windowDecorationHeight + (windowDecorationHeight < 0 ? "(invalid)" : "(valid)"));
        } else {
            this.activeStage = desiredStage;
            this.registeredBaseStackPane = baseStackPane;
            this.registeredScrollPane = desiredScrollPane;
            this.registeredStaticContentPane = desiredStaticContentPane;
            if ((this.activeScene = desiredStage.getScene()) == null) {
                throw new UnsupportedOperationException("Couldn't get scene from stage (attempt to setup resize capability failed).");
            }
            setCallerActive();
            this.desiredAspectRatio = aspectRatio;
            this.windowDecorationWidth = windowDecorationWidth;
            this.windowDecorationHeight = windowDecorationHeight;
            storeIdealContentDimensions(1, 1, contentWidth, contentHeight);
            attachResizeListeners();
            fitToScreen();
        }
    }
    
    /**
     * Determines the bounds of the active screen (the screen on which the window 
	 * representing the assigned Stage "activeStage" appears) and stores the values
	 * in this class instance for later use by internal methods.
     * Necessary in multi-screen (multi-monitor) environments to assist with
     * proper automatic resizing.
     * 
     * If no screens are detected due to a bug, the stored bounds are set to the
     * default width and height of the app's content, with adjustments made
     * to consider the dimensions of any window decoration.
     */
    private double[] determineActiveScreenBounds(){
		if(activeStage == null){
			 throw new UnsupportedOperationException("Attempted to get active "
			 		+ "screen bounds before this class instance properly initialized. "
			 		+ "(Why is there no Stage object assigned to me? The active "
			 		+ "JavaFX window has an associated Stage object as a requirement for creation.)");
		}
        double currentCenterX = activeStage.getX() + (activeStage.getWidth()/2);
        double currentCenterY = activeStage.getY() + (activeStage.getHeight()/2);
        boolean screenFound = false;
        Rectangle2D givenBounds;
        ObservableList<Screen> screensIn = Screen.getScreens();
        
        for(int screenNo = 0; screensIn != null && screenNo < screensIn.size() && !screenFound;
                screenNo++)
        {
        	if(!activeStage.isFullScreen()){
        		givenBounds = screensIn.get(screenNo).getVisualBounds();
        	}
        	else{
        		givenBounds = screensIn.get(screenNo).getBounds();
        	}
            if(currentCenterX < givenBounds.getMaxX()
                    && currentCenterY < givenBounds.getMaxY()
                    && currentCenterX > givenBounds.getMinX()
                    && currentCenterY > givenBounds.getMinY())
            {
                //this screen being tested is the screen to use
                this.activeScreenWidth = givenBounds.getWidth();
                this.activeScreenHeight = givenBounds.getHeight();
                screenFound = true;
                FXUIGameMaster.diagnosticPrintln("Rendering on screen " + screenNo);
            }
            FXUIGameMaster.diagnosticPrintln("Screen: " + screenNo);
            FXUIGameMaster.diagnosticPrintln(screensIn.get(screenNo).getVisualBounds().toString());
        }
        if(!screenFound){
            this.activeScreenWidth = FXUIGameMaster.DEFAULT_CONTENT_WIDTH + this.windowDecorationWidth;
            this.activeScreenHeight = FXUIGameMaster.DEFAULT_CONTENT_HEIGHT + this.windowDecorationHeight;
            FXUIGameMaster.diagnosticPrintln("Using pixel-matched dimensions (DEFAULT CONTENT)");
        }
        return new double[]{this.activeScreenWidth,this.activeScreenHeight};
    }
	
	/**
     * Determines the bounds of the given screen (the screen currently
     * displaying the majority of the window assigned to a given Stage), 
	 * and passively returns the value without storing.
     * Necessary in multi-screen (multi-monitor) environments to assist with
     * proper initial window sizing.
     * 
     * If no screens are detected due to a bug, the returned bounds are set 
	 * to the default width and height of the app's content.
	 *@param passiveStage the Stage object representing the Window to be checked
	 *@return a Rectangle2D object representing the usable bounds.
     */
    public static Rectangle2D returnGivenScreenBounds(Stage passiveStage){
        if(passiveStage == null){
                 throw new UnsupportedOperationException("Attempted to analyze "
                 		+ "a window with a null Stage object. "
                 		+ "Cannot determine which screen thiis window appears "
                 		+ "on, as we cannot determine the coordinates of the "
                 		+ "window from a null Stage object. "
                 		+ "(Did you mean to call me later in your "
                 		+ "initialization process?)");
        }
        double currentCenterX = passiveStage.getX() + (passiveStage.getWidth()/2);
        double currentCenterY = passiveStage.getY() + (passiveStage.getHeight()/2);
        FXUIGameMaster.diagnosticPrintln("Center of window found at " + currentCenterX + " x <> y " + currentCenterY);
        boolean screenFound = false;
        Rectangle2D givenBounds = new Rectangle2D(0,0,0,0);
        ObservableList<Screen> screensIn = Screen.getScreens();
        FXUIGameMaster.diagnosticPrintln("Screen count: " + screensIn.size());	
        for(int screenNo = 0; screensIn != null && screenNo < screensIn.size() && !screenFound;
                screenNo++)
        {
            givenBounds = screensIn.get(screenNo).getVisualBounds();
            if(currentCenterX < givenBounds.getMaxX()
                    && currentCenterY < givenBounds.getMaxY()
                    && currentCenterX > givenBounds.getMinX()
                    && currentCenterY > givenBounds.getMinY())
            {
                //this screen being tested is the screen to use
                screenFound = true;
                FXUIGameMaster.diagnosticPrintln("Returning dimensions from screen " + screenNo);
            }
            FXUIGameMaster.diagnosticPrintln("Screen: " + screenNo);
            FXUIGameMaster.diagnosticPrintln(screensIn.get(screenNo).getVisualBounds().toString());
        }
        if(!screenFound){
            givenBounds = new Rectangle2D(0,0,FXUIGameMaster.DEFAULT_CONTENT_WIDTH,FXUIGameMaster.DEFAULT_CONTENT_HEIGHT);
            FXUIGameMaster.diagnosticPrintln("Returning pixel-matched dimensions (DEFAULT CONTENT)");
        }
	return givenBounds;
    }

    /**
     * Alerts this WindowResize object that we can expect the window (Stage) to
     * be active/open/non-null, allowing auto-resizing. This is only a hint;
     * internal checks are used to verify that the window/Stage is still active,
     * to avoid major catastrophe. Pair with with {@link #setCallerInactive()}
     * to enable/disable automatic resizing for this object on-the-fly.
     *
     * @return state of the associated internal check, which should be "true"
     * after calling this function.
     */
    public boolean setCallerActive() {
        return this.assumeCallerIsActive = true;
    }

    /**
     * Alerts this WindowResize object that we can expect the window (Stage) to
     * be inactive/hidden/closed/null, blocking auto-resizing.\n The associated
     * flag indicating "active" or "inactive" will be ignored if the associated
     * window/Stage has become inactive, so you are not required to explicitly
     * called this function. However, this flag (when set as "inactive" after
     * calling this function) will block any attempts to make use of this
     * WindowResize object until the flag is cleared/set back to "active" with
     * {@link #setCallerActive()}, should such utility prove useful (e.g.,
     * should you want to block & unblock automatic resizing at a certain point
     * of the project flow, or if we are shutting down the program and want to
     * avoid any further resource usage).
     *
     * @return state of the associated internal check, which should be "false"
     * after calling this function.
     */
    public boolean setCallerInactive() {
        return this.assumeCallerIsActive = false;
    }

    /**
     * Forces a check of the internal window object state. Should the associated
     * Stage or Scene (window and its contents) be null/inactive, tells this
     * object that the caller is "inactive"-- i.e., automatically blocks any
     * further attempts at auto-resizing until either (a) the WindowResize
     * object is refreshed or (b) this object is removed from memory.
     *
     * @return "true" if the window object is relatively safe to operative on,
     * "false" otherwise.
     */
    private boolean getWindowObjectState() {
        if (this.activeScene == null || this.activeStage == null) {
            FXUIGameMaster.diagnosticPrintln("There was either a faulty stage or faulty scene submitted for resizing. Please be wary...");
            return setCallerInactive();
        } else {
            return this.assumeCallerIsActive;
        }
    }

    /**
     * Verifies that the caller method is being run on a JavaFX thread. If not,
     * throws an exception. (Necessary as JFX objects generally require the JFX
     * thread for modification).
     */
    private void verifyIsFXThread() {
        if (!Platform.isFxApplicationThread()) {
            throw new UnsupportedOperationException("Use of this ResizeHandler object requires a JavaFX thread for instantiation, modification, or use! "
                    + "(Did you implement me in the wrong place?)");
        }
    }

    /**
     * Sets the ideal dimensions of the contents (Scene) for this window
     * (Stage). Given an input of the minimum dimensions allowed for an object,
     * validates the "ideal dimensions" stored for the current window. If valid,
     * sets the desired dimensions directly with no issue. If invalid,
     * calculates a better set of content (Scene) dimensions for this window
     * (the active Stage). When calculation is required, uses the dimensions of
     * the primary screen as a seed to set the "ideal" values. Requires a valid
     * window decoration height to avoid chopping off content!
     *
     * @param minWidth the minimum width allowed for the content.
     * @param minHeight the minimum height allowed for the content.
     */
    private void storeIdealContentDimensions(double minWidth, double minHeight, double idealWidth, double idealHeight) {
        if (!getWindowObjectState()) {
            return;
        }
        
        //if we have some invalid input, we must do some calculation...
        if (idealWidth < minWidth || idealHeight < minHeight) {
            FXUIGameMaster.diagnosticPrintln("Detecting ideal window size..." + idealWidth + " <<W ::: H>> " + idealHeight);
            determineActiveScreenBounds();
            double maxWindowWidth = activeScreenWidth;
            double maxWindowHeight = activeScreenHeight;

            double usableScreenAspectRatio = maxWindowWidth / maxWindowHeight;
            /* if the desired aspect ratio for the given window is greater than the usable aspect ratio for the screen,
             * then the desired aspect ratio is too wide. We will have to calculate the applicable height
             * using the content's desired aspect ratio.
             */
            if (this.desiredAspectRatio > usableScreenAspectRatio) {
                this.idealContentWidth = maxWindowWidth;
                this.idealContentHeight = maxWindowWidth / this.desiredAspectRatio;
            } /*	else, we need to use the maximum available height, then determine the width to apply.*/ else {
                this.idealContentHeight = maxWindowHeight;
                this.idealContentWidth = maxWindowHeight * this.desiredAspectRatio;
            }
        } //else, we just set the values directly.
        else {
            this.idealContentHeight = idealHeight;
            this.idealContentWidth = idealWidth;
            FXUIGameMaster.diagnosticPrintln("IdealContentWidth < :: > IdealContentHeight: " + this.idealContentWidth + " < :: > " + this.idealContentHeight);
        }
    }

    /**
     * Performs a singular operation to resize the window to fit within the
     * bounds of the screen. Will block then unblock the window's associated
     * resize listeners. Makes use of the ideal dimensions. NOT FOR USE IN
     * FULLSCREEN.
     * 
     * Please ensure you run this on the JavaFX thread.
     */
    private void fitToScreen() {
        verifyIsFXThread();
        if (!getWindowObjectState()) {
            return;
        }
        FXUIGameMaster.diagnosticPrintln("FitToScreen started."
        		+ "\nDetermining optimal window dimensions.");
        determineActiveScreenBounds();
        double maxWindowWidth = activeScreenWidth;
        double maxWindowHeight = activeScreenHeight;
        FXUIGameMaster.diagnosticPrintln("winDec width : height" + this.windowDecorationWidth + " : " + this.windowDecorationHeight);
        
        double newContentWidth = 0, newContentHeight = 0;

        double usableScreenAspectRatio = maxWindowWidth / maxWindowHeight;
        /* if the desired aspect ratio for the given window is greater than the 
         * usable aspect ratio for the screen, then the desired aspect ratio is 
         * too wide. We find that the window's width is correct, and thus we can
         * easily get the width of the CONTENT. When done with that, we must
         * then calc & adjust the height to the to the applicable height
         * using the content's desired aspect ratio and the CONTENT width.
         */
        if (this.desiredAspectRatio > usableScreenAspectRatio) {
            newContentWidth = maxWindowWidth - this.windowDecorationWidth;
            newContentHeight = newContentWidth / this.desiredAspectRatio;
        } 
        /* Else, we need to use the maximum available height, adjust for
         *applicable window decoration/borders, then determine the width to 
         * which we should resize the content in the window. We'll convert to
         * correct WINDOW size later.
         */ 
        else {
            newContentHeight = maxWindowHeight - this.windowDecorationHeight;
            newContentWidth = newContentHeight * this.desiredAspectRatio;
        }
        
        /*
         *How to scale the CONTENT, not the window itself.
         */
        double scalePercentageHeight = newContentHeight / this.idealContentHeight;
        double scalePercentageWidth = newContentWidth / this.idealContentWidth;

        FXUIGameMaster.diagnosticPrintln("Scales w vs h: " + scalePercentageWidth + " :: " + scalePercentageHeight);
        Scale scale = new Scale(scalePercentageWidth, scalePercentageHeight);

        //final double xPositionToUse = (maxWindowWidth - newContentWidth) / 2;
        //the above negated by use of built-in function to center on screen.
        /*finally, we actually set the size of the window*/
        detachResizeListeners();
        FXUIGameMaster.diagnosticPrintln("Snapping to dimensions...");
        activeStage.setWidth(newContentWidth + windowDecorationWidth);
        activeStage.setHeight(newContentHeight + windowDecorationHeight);
        activeStage.centerOnScreen();
        //activeScene.getRoot().getTransforms().setAll(scale); //TODO make it so you can dynamically enable slash disable this.
        conditionallyScale(activeScene, scale);
        activeScene.getRoot().setLayoutX(0);
        activeScene.getRoot().setLayoutY(0);
        FXUIGameMaster.diagnosticPrintln("FitToScreen complete.");
        attachResizeListeners();
    }
    
    /**
     * Automate the process of snapping contents to a fullscreen setup.
     * Please ensure you run this on the JavaFX thread.
     */
    private void fitToFullScreen() {
        verifyIsFXThread();
        if (!getWindowObjectState()) {
            return;
        }
        FXUIGameMaster.diagnosticPrintln("\n\nDetermining optimal window dimensions."
                + " (ENTERING FULLSCREEN SCALING MODE)");
        double[/*width, height*/] screenDims = determineActiveScreenBounds();
        /*
         * We can ask the active stage for its width/height directly this time
         * around; no trying to figure out screens, etc.
         */
        double maxWindowWidth = screenDims[0];
        double maxWindowHeight = screenDims[1];
        FXUIGameMaster.diagnosticPrintln(
        		"Main stage [erroneous?]: width::height -> " + maxWindowWidth + " :: " + maxWindowHeight + "\n"
        		+ "Window bounds we will use: width::height -> " + screenDims[0] + " :: " + screenDims[1]
        		);
        
        double usableScreenAspectRatio = maxWindowWidth / maxWindowHeight;
        double newContentHeight, newContentWidth, scalePercentageHeight,
        		scalePercentageWidth, xTranslate, yTranslate;
        
        /* if the desired aspect ratio for the given content is greater than the 
         * usable aspect ratio for the screen, then the desired aspect ratio is 
         * too wide. There will be bars at the top and/or bottom. We need to use
         * the maximum available width, and figure out a better height so our
         * resize(scale) of the content is correct.
         */
        if (this.desiredAspectRatio > usableScreenAspectRatio) {
            newContentWidth = maxWindowWidth;
            newContentHeight = newContentWidth / this.desiredAspectRatio;
            FXUIGameMaster.diagnosticPrintln("(A-type) newHeightDetermined: "
            		+ newContentHeight + "\nIdeal content w vs h: " 
            		+ this.idealContentWidth + " :: " + this.idealContentHeight);
        } /*	else, we need to use the maximum available height, 
        then determine the width to which we should resize(scale) the content.*/ 
        else {
            newContentHeight = maxWindowHeight/* + windowDecorationHeight*/;
            newContentWidth = newContentHeight * this.desiredAspectRatio;
            FXUIGameMaster.diagnosticPrintln("(B-type) newWidthDetermined: " 
            		+ newContentWidth + "\nIdeal content w vs h: " 
            		+ this.idealContentWidth + " :: " + this.idealContentHeight);
        }
        scalePercentageHeight = newContentHeight / this.idealContentHeight;
        scalePercentageWidth = newContentWidth / this.idealContentWidth;
        FXUIGameMaster.diagnosticPrintln("Scales w vs h: " + scalePercentageWidth 
        		+ " :: " + scalePercentageHeight);
        if((xTranslate = (maxWindowWidth - newContentWidth) / 2) < 0 || preferScrollNZoom)
        {xTranslate = 0;}
        if((yTranslate = (maxWindowHeight - newContentHeight) / 2) < 0 || preferScrollNZoom)
        {yTranslate = 0;}
        /*activeScene.getRoot().getTransforms()
        	.setAll(new Scale(scalePercentageWidth, scalePercentageHeight));*/
        conditionallyScale(activeScene, new Scale(scalePercentageWidth, scalePercentageHeight));
        activeScene.getRoot().setLayoutX(xTranslate);
        activeScene.getRoot().setLayoutY(yTranslate);
        //activeStage.toBack(); //attempt to make sure no dialogs get covered up // TODO: remove; replace with more effective solution which actually prevents
        //other dialogs from being hidden
        FXUIGameMaster.diagnosticPrintln("xTranslate and yTranslate? " + xTranslate + " ::: " +
        		yTranslate);
        FXUIGameMaster.diagnosticPrintln("Scaling should be complete."
                + " (EXITING FULLSCREEN SCALING MODE)\n\n");
    }

    /**
     * Used to enable intelligent resizing of the main window (aka "Stage
     * primaryStage") This enables assistance in resizing of the primary window
     * at launch as well as after launch. Attempts to keep the proper aspect
     * ratio of the contents of the main window. Has no effect on secondary
     * dialogs. (Note: use of this method after launch does not snap the OS
     * window bounds to the contents of the window when the aspect ratio is
     * different). Applicable to the Stage defined in this ResizeHandler
     * instance!
     * 
     * If the app suddenly stops responding to any attempts to resize, ensure
     * you do not have a rogue call to the companion detachResizeListeners()
     * {@link #detachResizeListeners()} method & have forgotten to call this!
     */
    private void attachResizeListeners() {
        verifyIsFXThread();
        if (this.resizeBlocked.get() == false) {
            FXUIGameMaster.diagnosticPrintln("Resizing listeners already on!");
            return;
        }
        this.widthPropertyListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldStageWidth, Number newStageWidth) {
                detachResizeListeners();
                resize(TRIG_BY_WIDTH);
            }
        };

        this.heightPropertyListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldStageHeight, Number newStageHeight) {
                detachResizeListeners();
                resize(TRIG_BY_HEIGHT);
            }
        };
        this.activeScene.widthProperty().addListener(this.widthPropertyListener);
        this.activeScene.heightProperty().addListener(this.heightPropertyListener);
        this.resizeBlocked.set(false);
        FXUIGameMaster.diagnosticPrintln("Resizing listeners: ON!");
    }

    /**
     * Fully disable the resize listeners for the window. (there will be no
     * attempt at intelligent reaction unless an earlier intelligent resize
     * operation is active) Applicable to the Stage defined in this
     * ResizeHandler instance!
     * 
     * If the app suddenly stops responding to any attempts to resize,
     * ensure you are not missing a call to the companion attachResizeListeners()
     * {@link #attachResizeListeners()} method.
     */
    private void detachResizeListeners() {
        if (this.resizeBlocked.get() == true) {
            FXUIGameMaster.diagnosticPrintln("Resizing listeners already off!");
            return;
        }
        verifyIsFXThread();
        this.resizeBlocked.set(true);
        this.activeScene.widthProperty().removeListener(this.widthPropertyListener);
        this.activeScene.heightProperty().removeListener(this.heightPropertyListener);
        FXUIGameMaster.diagnosticPrintln("Resizing listeners: OFF!");
    }

    /**
     * Should a Slider be delegated to control (or display) the size of the
     * window, tell the Resize Handler about it so the value may be
     * automatically updated. (Value updates based on percentage of window size,
     * from 0 to 1) The slider may control the size of the window without using
     * this; it will simply receive no automatic feedback, nor will it be
     * dynamically enabled/disabled during resize operations.
     *
     * @param sliderAttached the slider object to modify over time
     */
    public void attachResizeSlider(Slider sliderAttached) {
        this.sliderToAdjust = sliderAttached;
    }

    /**
     * Allows setting the window size based on a percentage of the determined
     * ideal window size (determined when the program launches).
     * 
     * Please run on the JavaFX thread.
     * 
     * TODO overhaul this method; it is unusable for fullscreen purposes, even
     * though code seems to indicate it SHOULD be usable. Either fix that so it
     * applies a Scale (rather than setting width & height), or remove it, and
     * ensure the code for windowed mode is correct.
     *
     * @param percentage : percentage of ideal window size to apply, in a range
     * from 0.00 not inclusive to 1.00 inclusive (representing 0% and 100%).
     */
    public void resizeByPercentage(double percentage) {
        Platform.runLater(()->{
            if (percentage <= 0) {
                return;
            }
            if(!activeStage.isFullScreen())
            {
                activeStage.setWidth(percentage * (this.idealContentWidth +
                        this.windowDecorationWidth));
                activeStage.setHeight(percentage * (this.idealContentHeight +
                        this.windowDecorationHeight));
            }
            else{
                activeStage.setWidth(percentage * this.idealContentWidth);
                activeStage.setHeight(percentage * this.idealContentHeight);
            }}
        );
    }

    /**
     * Used to assist in resizing of the primary window at launch as well as
     * after launch. Attempts to keep the proper aspect ratio of the contents of
     * the main window. Affects only the main window. (Note: use of this method
     * after launch does not snap the OS window bounds to the contents of the
     * window when the aspect ratio is different).
     *
     * @param triggerType whether triggered by a change in WIDTH or a change in
     * HEIGHT.
     */
    private void resize(int triggerType) {
    	if(resizeThreadIsActive.get()){
    		FXUIGameMaster.diagnosticPrintln("skipping resize trigger...(thread active)");
    		return;
    	}
        if (this.activeStage == null || this.activeScene == null) {
            FXUIGameMaster.diagnosticPrintln("skipping resize...(invalid Stage or invalid Scene)");
            return;
        } else {
            FXUIGameMaster.diagnosticPrintln("making resize thread.");
            this.widthHistory = this.activeStage.getWidth();
            this.heightHistory = this.activeStage.getHeight();
            new Thread(null, new Runnable() {
                @Override
                public void run() {
                	resizeThreadIsActive.set(true);
                    resizeHelper(triggerType);
                }
            }, "WRH.resize").start();
        }
    }

    private void resizeHelper(int triggerType) {
    	final Pane hideScreenBehindPane = FXUIGameMaster.requestPaneDisplay(makeAdjustmentScreen());
        FXUIGameMaster.diagnosticPrintln("Initial resizeHelper call stats "
        	+ "(W:H:windowDecorationHeight): " + this.widthHistory 
        	+ ":::" + this.heightHistory + ":::" + this.windowDecorationHeight);
        if (!getWindowObjectState()) {
            return; }

        //remember whether the window was fullscreen when this method was invoked
        long waitTime = 200;
        FXUIGameMaster.diagnosticPrintln("The app was set to fullscreen at the time of this "
                + "method call: " + activeStage.isFullScreen());
        boolean waitForFullScreenTransition = activeStage.isFullScreen();
        final double entryWidth = this.activeStage.getWidth(), entryHeight = this.activeStage.getHeight();

        //disable any attached slider to prevent accidents/overrides
        if (this.sliderToAdjust != null) {
            this.sliderToAdjust.setDisable(true); }
        

        /*
         * Wait for the user to stop tweaking the window; monitor the dimensions
         * and when there are no more changes, proceed with resize checks.
         */

        do {
            FXUIGameMaster.diagnosticPrintln("Waiting for user to stop altering dimensions.");
            this.widthHistory = this.activeStage.getWidth();
            this.heightHistory = this.activeStage.getHeight();
            if(waitForFullScreenTransition){
            	RiskUtils.sleep(6 * waitTime);
            }
            else{
            	RiskUtils.sleep(4 * waitTime);
            }
            FXUIGameMaster.diagnosticPrintln("The screen is now set to fullscreen at the end of this "
                    + "waiting process: " + activeStage.isFullScreen());
            FXUIGameMaster.diagnosticPrintln("Width history versus current width: " + this.widthHistory 
            		+ " :: " + this.activeStage.getWidth() + " >> Height: " + this.heightHistory 
            		+ " :: " + this.activeStage.getHeight());
        } while (this.widthHistory != this.activeStage.getWidth() || this.heightHistory != this.activeStage.getHeight());
        final boolean stageIsFullscreen = activeStage.isFullScreen();

        /*Used in the future to determine what steps of the resizing process must occur.*/
        AtomicBoolean stillRunningResize = new AtomicBoolean(true);

        double newContentWidth = 0.0d, newContentHeight = 0.0d;
        double scalePercentageWidth = 1.0d;
        double scalePercentageHeight = 1.0d;
        double expectedAspectRatio = 0.0d;
        
        if(Math.abs(this.widthHistory - entryWidth) > Math.abs(this.heightHistory - entryHeight)){
        	triggerType = TRIG_BY_WIDTH;
        }
        else{
        	triggerType = TRIG_BY_HEIGHT;
        }

        //if we modified the height manually, keep that height property and calculate the new width that must be set
        if (triggerType == TRIG_BY_HEIGHT) {
            FXUIGameMaster.diagnosticPrintln("Trigger type: height (so will calc and fix width)");
            newContentHeight = this.heightHistory - this.windowDecorationHeight;
            newContentWidth = newContentHeight * this.desiredAspectRatio;
        } //else if the width, keep that width and programmatically set the height
        else if (triggerType == TRIG_BY_WIDTH) {
            FXUIGameMaster.diagnosticPrintln("Trigger type: width (so will calc and fix height)");
            newContentWidth = this.widthHistory - this.windowDecorationWidth;
            newContentHeight = newContentWidth / this.desiredAspectRatio;
        } else {
            FXUIGameMaster.diagnosticPrintln("How did you get here? Nobody's s'pose to be here.");
        }
        scalePercentageHeight = newContentHeight / this.idealContentHeight;
        scalePercentageWidth = newContentWidth / this.idealContentWidth;
        expectedAspectRatio = newContentWidth / newContentHeight;

        FXUIGameMaster.diagnosticPrintln("calculated target aspect ratio: " + expectedAspectRatio + " ...versus expected: " + this.desiredAspectRatio);
        /*Determine the relative amount by which we must scale the content -- the scene -- within the window (stage).
         This makes the scene come close to appropriately fitting within the stage with a limited unsightly gap.*/
        FXUIGameMaster.diagnosticPrintln("Scales w vs h: " + scalePercentageWidth + " :: " + scalePercentageHeight);
        Scale scale = new Scale(scalePercentageWidth, scalePercentageHeight);
        FXUIGameMaster.diagnosticPrintln("acceptable new width:: " + newContentWidth);
        FXUIGameMaster.diagnosticPrintln("acceptable new height:: " + newContentHeight);

        
        //If we attempted to resize our content to a dimension beyond the screen's bounds, we just snap back to the max dimensions available.
        determineActiveScreenBounds();
        if ((newContentWidth >= activeScreenWidth - this.windowDecorationWidth)||
            (newContentHeight >= activeScreenHeight - this.windowDecorationHeight) ||
            stageIsFullscreen)
        {   /*In this case, handle the size calulation being outside acceptable 
            bounds, while the app is in windowed mode (not fullscreen)
            */
            if(!stageIsFullscreen){
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        FXUIGameMaster.diagnosticPrintln("Choosing the \"fitToScreen\" path.");
                        fitToScreen();
                    }
                });
            } /*now handle the size calulation being outside acceptable bounds but
            the screen IS indeed fullscreen
            */
            else /*if(stageIsFullscreen)*/{
            	RiskUtils.sleep(4*waitTime);
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        FXUIGameMaster.diagnosticPrintln("Choosing the \"fitToFullscreen\" path.");
                        fitToFullScreen();
                    }
                });
            }
            RiskUtils.sleep(2*waitTime);
        } 
        else {
            /*Store the new width and/or height in variables that can be 
            accessed in a separate runnable/thread.*/
            final double newWidthCopy = newContentWidth;
            final double newHeightCopy = newContentHeight;
            final double attemptedAspectRatio = newContentWidth / newContentHeight;

            /*Perform some changes using this thread, and remainder on the JavaFX thread. 
            Note that scaling must always occur, even if resizing does not.*/
            FXUIGameMaster.diagnosticPrintln("Target scene(content) height  ::: target scene(content) width");
            FXUIGameMaster.diagnosticPrintln(newHeightCopy + ";;;" + newWidthCopy);
            //activeStage.sizeToScene();
            FXUIGameMaster.diagnosticPrintln("Attempting to scale.\nCan this stage be resized? (t if yes, f is no): " + activeStage.isResizable());
            conditionallyScale(activeScene, scale);
            activeScene.getRoot().setLayoutX(0);
            activeScene.getRoot().setLayoutY(0);
            FXUIGameMaster.diagnosticPrintln("SCALE finished.");
            
            if (!stageIsFullscreen) {
                activeStage.setWidth(newWidthCopy+windowDecorationWidth);
                activeStage.setHeight(newHeightCopy+windowDecorationHeight);
            }
            else{ 
                FXUIGameMaster.diagnosticPrintln("You should never make it here, but...");
                activeStage.setWidth(newWidthCopy);
                activeStage.setHeight(newHeightCopy);
            }
            if (sliderToAdjust != null) {
                sliderToAdjust.setValue(activeScene.getWidth() / idealContentWidth);
                sliderToAdjust.getTooltip().setText("Current percentage of max size: " + String.format("%.2f", activeScene.getWidth() / idealContentWidth));
            }
            FXUIGameMaster.diagnosticPrintln("Let resize finish before resume...");
            RiskUtils.sleep(waitTime);
            FXUIGameMaster.diagnosticPrintln("new!!! " + desiredAspectRatio + " ...actually set to... " + (double) (activeScene.getWidth() / activeScene.getHeight()) + "...attempted to set: " + attemptedAspectRatio);
            FXUIGameMaster.diagnosticPrintln("new2!!! ...effective content (Scene) dimens set to... " + activeScene.getWidth() + "::<W ::: H>:: " + activeScene.getHeight());
            FXUIGameMaster.diagnosticPrintln("new2!!! ...effective screen (Stage) dimens set to... " + activeStage.getWidth() + "::<W ::: H>:: " + activeStage.getHeight());
        }
        
        //if the app is set up for Scroll n Zoom, readjust scaling
        if(preferScrollNZoom){
        	correctScrollNZoomScale(savedZoomeMultiplier);
        }
        //enable the resize listeners
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                attachResizeListeners();
            }
        });
        /*	Indicate that the thread to resize the window is no longer active.
         *	The app allows the thread to terminate to avoid permanent excess resource usage...in this case, at least.
         *	Any further use should re-fire the thread elsewhere.*/
        resizeThreadIsActive.set(false);
        if (this.sliderToAdjust != null && !this.activeStage.isFullScreen()) {
            //this.sliderToAdjust.setValue(this.activeStage.getWidth()/this.idealContentWidth);
            this.sliderToAdjust.setDisable(false);
        }
        FXUIGameMaster.requestPaneRemoval(hideScreenBehindPane);
        FXUIGameMaster.diagnosticPrintln("Exit resize thread.");
    }
    
    /**
     * For use with module that supports scroll'n'zoom + static window content display
     * Conditionally scales content. If window not set to scroll & zoom (if window content
     * not packed into a ScrollPane), will set scaling to desired percentage. If window
     * content is packed into ScrollPane, will scale content to 100% regardless
     * of request from resize mechanism. Also takes into account whether the
     * window is fullscreen or not.
     * @param sceneIn the scene to be scaled
     * @param scale the {@Code Scale} to be used if window content is static
     * @return "true" if resized to desired percentage, "false" if forced to 100%
     */
	private boolean conditionallyScale(Scene sceneIn, Scale scale){
		boolean toDesired = false;
    	if(!preferScrollNZoom){
    		FXUIGameMaster.diagnosticPrintln("Typ1: preferScrollNZoom + isFullScreen:: " + preferScrollNZoom + this.activeStage.isFullScreen());
    		FXUIGameMaster.diagnosticPrintln("Typ1 use scale " + scale.toString());
    		sceneIn.getRoot().getTransforms().removeIf(transform -> transform.getClass().equals(Scale.class));
    		sceneIn.getRoot().getTransforms().add(scale);
    		toDesired = true;
    	}
    	else /*if((!this.activeStage.isFullScreen() && preferScrollNZoom) || )*/{
    		FXUIGameMaster.diagnosticPrintln("Typ2 [1x1 scale]: preferScrollNZoom + isFullScreen:: " + preferScrollNZoom + this.activeStage.isFullScreen());
    		FXUIGameMaster.diagnosticPrintln("Typ2 [would have scaled to " + scale.toString() + "]");
    		sceneIn.getRoot().getTransforms().removeIf(transform -> transform.getClass().equals(Scale.class));
    		sceneIn.getRoot().getTransforms().add(new Scale(1,1));
    	}/*
    	else{
    		if(FXUIGameMaster.diagnosticPrintln("Typ3 [NO scale]: "
    				+ "preferScrollNZoom + isFullScreen:: " + preferScrollNZoom + this.activeStage.isFullScreen()))
    		{
	    		FXUIGameMaster.diagnosticPrintln("Typ3 [would have scaled to " + scale.toString() + "]");
	    		for(StackTraceElement stem : Thread.currentThread().getStackTrace()){
	    			FXUIGameMaster.diagnosticPrintln(stem.toString());
	    		}
    		}
    	}*/
    	return toDesired;
    }
    
    public boolean enableScrollNZoom(){
    	preferScrollNZoom = true;
    	if(registeredScrollPane.getContent() == null){
    		try{
    	    	//this.scrollGroupInterface.getChildren().clear();
    	    	registeredBaseStackPane.getChildren().clear();
    	    	this.scrollGroupInterface.getChildren().add(registeredStaticContentPane);
    	    	registeredScrollPane.setVisible(true);
    	        registeredScrollPane.setContent(this.scrollGroupInterface);
    	        registeredBaseStackPane.getChildren().add(registeredScrollPane);
        	}
        	catch(Exception e){
        		e.printStackTrace();
        	}
    	}
    	//fitToScreen();
        return preferScrollNZoom;
    }
    
    public boolean disableScrollNZoom(){
    	preferScrollNZoom = false;
    	if(registeredScrollPane.getContent() == this.scrollGroupInterface){
    		try{
    	    	registeredScrollPane.setContent(null);
    	    	this.scrollGroupInterface.getChildren().clear();
    	    	registeredScrollPane.setVisible(false);
    	        registeredBaseStackPane.getChildren().addAll(registeredStaticContentPane);
                undoScrollNZoomScale();
        	}
        	catch(Exception e){
        		e.printStackTrace();
        	}
    	}
    	//fitToScreen();
    	return preferScrollNZoom;
    }
    
    /**
     * Create a non-JavaFX thread (if necessary) to build & display size options
     * for the associated window (Stage) Tries to run the dialog's code on a
     * non-JFX thread as much as possible.
     * @param owner Stage/Window to be treated as owner of this window
     * @param flatten "true" if you just want to return a preformatted HBox for
     * view in the flat Options menu, or "false" if you want a separate window with no
     * HBox returned.
     * @return
     */
    public VBox showSizeOptions(Window owner, Boolean flatten) {
        //represents the dialog; true: the dialog is visible (& code is waiting), false: window isn't showing.
        AtomicBoolean dialogIsShowing = new AtomicBoolean(true);

        if (!this.assumeCallerIsActive) {
            return null;
        }
        
        if(flatten){
        	return sizeOptionsHelper(dialogIsShowing, owner, flatten);
        }

        if (Platform.isFxApplicationThread()) { //if this is the FX thread, make it all happen, and use showAndWait
        	sizeOptionsHelper(dialogIsShowing, owner, flatten);
        } else { //if this isn't the FX thread, we can pause logic with a call to RiskUtils.sleep()
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                	sizeOptionsHelper(dialogIsShowing, owner, flatten);
                }
            });

            do {
                RiskUtils.sleep(500);
            } while (dialogIsShowing.get());
        }
        return null;
    }

    /**
     * Build & show the dialog.
     *
     * @param fxThread pass "true" if the method is being run on the JavaFX app
     * thread (please avoid doing so)
     * @param dialogIsShowing used to control the flow of code; will be set to
     * "false" when dialog is closed.
     * @param flatten if the method is to return an HBox (true) or just show in 
     * a separate window (false)
     * @return "null" if shows in a window, or the HBox with the layout contents
     * if meant to be displayed in flat dialog.
     */
    private VBox sizeOptionsHelper(AtomicBoolean dialogIsShowing, Window owner, Boolean flatten) {
        if(owner==null)
        {
            owner = this.activeStage.getScene().getWindow();
        }
        try {
            final Stage dialog = new Stage();
            //dialog.initModality(Modality.APPLICATION_MODAL);
            if(!flatten){
            dialog.setTitle("Window Options");
            dialog.initOwner(owner);
            dialog.setX(owner.getX());
            dialog.setY(owner.getY() + 50);
            }
            
            final VBox layout = new VBox(15);
            layout.setAlignment(Pos.CENTER);
            if(!flatten){
            layout.setStyle("-fx-background-color: brown; -fx-padding: 5");
            }
            else{
            	layout.setStyle("-fx-padding: 5");
            }

            final Text queryText = new Text("     Alter window settings?     ");
            queryText.setTextAlignment(TextAlignment.CENTER);
            queryText.setFill(Color.WHITE);

            final Text querySymbol = new Text("px+/px-");
            querySymbol.setTextAlignment(TextAlignment.CENTER);
            querySymbol.setFont(Font.font("Arial", FontWeight.BOLD, 24));
            querySymbol.setFill(Color.WHITE);

            Text spaceBuffer = new Text("\n");
            spaceBuffer.setTextAlignment(TextAlignment.CENTER);
            spaceBuffer.setFont(Font.font("Arial", FontWeight.LIGHT, 16));

            final Button yeah = new Button("OK");

            yeah.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    dialogIsShowing.set(false);
                    dialog.close();
                }
            });
            
            Text windowSizeSliderLabel = new Text("Window size [%]");
            windowSizeSliderLabel.setFont(Font.font("Verdana", FontWeight.LIGHT, 14));
            windowSizeSliderLabel.setFill(Color.WHITE);

            Slider windowSizeSlider = new Slider(0.1f, 1.5f, 0.75f);
            windowSizeSlider.setScaleX(0.75);
            windowSizeSlider.setSnapToTicks(false);
            windowSizeSlider.setShowTickMarks(true);
            windowSizeSlider.setMajorTickUnit(0.25f);
            windowSizeSlider.setMinorTickCount(0);
            windowSizeSlider.setTooltip(new Tooltip("Window Size (percentage -- 10% to 150%)"));
            windowSizeSlider.valueProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> ov,
                        Number old_val, Number new_val) {
                    if (!windowSizeSlider.isDisabled()) {
                        resizeByPercentage(new_val.doubleValue());
                    }
                }
            });
            attachResizeSlider(windowSizeSlider);

            CheckBox doFullScreen = new CheckBox("Display in fullscreen?");
            doFullScreen.setTooltip(new Tooltip("Enable or Disable fullscreen mode (enabled ignores screen sizing requests)"));
            doFullScreen.setTextFill(Color.ANTIQUEWHITE);
            doFullScreen.setFont(Font.font("Arial", FontWeight.LIGHT, 12));
            doFullScreen.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    if (doFullScreen.isSelected()) {
                    	//detachResizeListeners(); TODO test w/ this disabled under multiscreen + Windows
                        windowSizeSlider.setDisable(true);
                        activeStage.setFullScreen(true);
                        //resize(TRIG_BY_HEIGHT);  TODO test w/ this disabled under multiscreen + Windows
                    } else {
                        activeStage.setFullScreen(false);
                        windowSizeSlider.setDisable(false);
                    }
                }
            });
            
            doFullScreen.setSelected(activeStage.isFullScreen());
            windowSizeSlider.setDisable(activeStage.isFullScreen());
            activeStage.fullScreenProperty().addListener(new ChangeListener<Boolean>(){
				@Override
				public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
					final boolean isFullScreen = true;
					if(newValue.booleanValue() == isFullScreen){
						windowSizeSlider.setDisable(true);
						doFullScreen.setSelected(true);
					}
					else{
						windowSizeSlider.setDisable(false);
						doFullScreen.setSelected(false);
					}
					
				}
            	
            });
            
            
            /**
             * Set up control of yellow eyestrain filter, AutoBrite function,
             * and manual brightness control.
             */
           
            
            ChoiceBox<Object> eyeReliefChoice = new ChoiceBox<Object>();
            eyeReliefChoice.setItems(FXCollections.observableArrayList(
                "eyeLief OFF", "eyeLief LOW", 
                 "eyeLief HIGH")
            );
            eyeReliefChoice.setTooltip(new Tooltip("Enable or Disable yellow filter "
            		+ "for nighttime gameplay, or play in dark environments"
            		+ " [more effective when game is dimmed, less effective when"
            		+ " game brightness is maximized]"));
            eyeReliefChoice.getSelectionModel().select(FXUIGameMaster.getActiveEyeLiefStrength());
            eyeReliefChoice.getSelectionModel().selectedIndexProperty().addListener(
            	new ChangeListener<Number>(){
					@Override
					public void changed(ObservableValue<? extends Number> observable, Number oldValue,
							Number newValue) {
						// TODO Auto-generated method stub
						final int correctedVal = newValue.intValue();
						FXUIGameMaster.diagnosticPrintln("eyeLief val: " + correctedVal);
						if(correctedVal == FXUIGameMaster.EYELIEF_OFF
								|| correctedVal == FXUIGameMaster.EYELIEF_LO 
								|| correctedVal == FXUIGameMaster.EYELIEF_HI){
							FXUIGameMaster.applyEyeLief(correctedVal);							
						}
						else{
							FXUIGameMaster.diagnosticPrintln("Invalid eyeLief setting selected: " + correctedVal);
						}
					}
            	}
            );
            
            CheckBox autoDim = new CheckBox("AutoBrite(auto dimming)");
            autoDim.setTooltip(new Tooltip("Automatically dim window based on time of day. "
            		+ "Noon = brighter. Midnight = darker. If yellow filter applied, "
            		+ "Noon = less effective, Midnight = more effective."));
            autoDim.setTextFill(Color.ANTIQUEWHITE);
            autoDim.setFont(Font.font("Arial", FontWeight.LIGHT, 12));
            autoDim.setSelected(FXUIGameMaster.isAutoBrightnessActive());
            
            CheckBox scrollNZoomOpt = new CheckBox("Scroll'n'Zoom");
            scrollNZoomOpt.setTooltip(new Tooltip("Make it such that the window content"
            		+ "\ncan be scrolled & zoomed, rather than having the content"
            		+ "\n match the size of the window."));
            scrollNZoomOpt.setTextFill(Color.ANTIQUEWHITE);
            scrollNZoomOpt.setFont(Font.font("Arial", FontWeight.LIGHT, 12));
            scrollNZoomOpt.setSelected(preferScrollNZoom);
            
            final double zoomSettingAtEntry = savedZoomeMultiplier;
            ChoiceBox<Object> zoomSettingChoice = new ChoiceBox<Object>();
            zoomSettingChoice.setItems(FXCollections.observableArrayList(
            		"Last setting (" + String.format("%.1f", zoomSettingAtEntry) + "x)",
                "No zoom (1.0x)", "1.1x", "1.2x", "1.3x", "1.4x", "1.5x", 
                 "2.0x", "4.0x", "6.0x", "8.0x", "10.0x")
            );
	        zoomSettingChoice.setTooltip(new Tooltip("Select the zoom level."
	        		+ "\nHigher zoom levels require more processing power, & may be laggy."));
	        zoomSettingChoice.getSelectionModel().select(0); //default to current setting
	        zoomSettingChoice.getSelectionModel().selectedIndexProperty().addListener(
	            new ChangeListener<Number>(){
	                @Override
	                public void changed(ObservableValue<? extends Number> observable, Number oldValue,
	                                Number newValue) {
	                    double multiplier = 0.0d;
	                    switch (newValue.intValue()){
	                        case 0:
	                            multiplier = zoomSettingAtEntry;
	                            break;
	                        case 1:
	                            multiplier = 1.0d;
	                            break;
	                        case 2:
	                        	multiplier = 1.1d;
	                        	break;
	                        case 3:
	                        	multiplier = 1.2d;
	                        	break;
	                        case 4:
	                        	multiplier = 1.3d;
	                        	break;
	                        case 5:
	                        	multiplier = 1.4d;
	                        	break;
	                        case 6:
	                            multiplier = 1.5d;
	                            break;
	                        case 7:
	                            multiplier = 2.0d;
	                            break;
	                        case 8:
	                            multiplier = 4.0d;
	                            break;
                            case 9:
                                multiplier = 6.0d;
                                break;
                            case 10:
                                multiplier = 8.0d;
                                break;
                            case 11:
                                multiplier = 10.0d;
                                break;
	                        default:
	                        	multiplier = 1.0d;
	                        	break;
	                                
	                    }
	                    savedZoomeMultiplier = multiplier;
	                    correctScrollNZoomScale(savedZoomeMultiplier);
	                    FXUIGameMaster.diagnosticPrintln(multiplier + "x zoom multiplier selected.");
	                }
	            }
	        );
	        zoomSettingChoice.setStyle("-fx-font: "+ (12) +"px \"System\";");
	        zoomSettingChoice.setDisable(!scrollNZoomOpt.isSelected());
	        
	        Text zoomSettingLabel = new Text("Content zoom level");
            zoomSettingLabel.setFont(Font.font("Verdana", FontWeight.LIGHT, 12));
            zoomSettingLabel.setFill(Color.WHITE);
            /*
            scrollNZoomOpt.selectedProperty().addListener(new ChangeListener<Boolean>(){
				@Override
				public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
					
				}
            });
            */
            scrollNZoomOpt.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    if(scrollNZoomOpt.isSelected()){
                    	enableScrollNZoom();
                    }
                    else{
                    	zoomSettingChoice.getSelectionModel().select(1);
                    	disableScrollNZoom();
                    }
                    zoomSettingChoice.setDisable(!scrollNZoomOpt.isSelected());
                    
                	//specialty to trigger a screen refresh in some situations
                    if(!activeStage.isFullScreen()){
                		//resizeByPercentage((activeStage.getWidth() - 1)/idealContentWidth);
                    	detachResizeListeners();
                        resize(TRIG_BY_HEIGHT);
                	}
                    else{
                    	//fitToFullScreen();
                    	detachResizeListeners();
                        resize(TRIG_BY_HEIGHT);
                    }
                }
            });
	        
            
            String britenessSliderEnabled = "brightness(Manually Set) [%]";
            String britenessSliderDisabled = "brightness(n/a--AutoBrite active)";
            Text britenessSliderLabel = new Text(FXUIGameMaster.isAutoBrightnessActive() ? britenessSliderDisabled : britenessSliderEnabled);
            britenessSliderLabel.setFont(Font.font("Arial", FontWeight.LIGHT, 12));
            britenessSliderLabel.setFill(Color.WHITE);

			Slider britenessSlider = new Slider(0.5f, 1.0f, FXUI_Crossbar.getBritenessOpacity());
            britenessSlider.setScaleX(0.75);
            britenessSlider.setSnapToTicks(false);
            britenessSlider.setShowTickMarks(true);
            britenessSlider.setMajorTickUnit(0.25f);
            britenessSlider.setMinorTickCount(0);
            britenessSlider.setTooltip(new Tooltip("Window Brightness (percentage)"));
            britenessSlider.valueProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> ov,
                        Number old_val, Number new_val) {
                    if (!britenessSlider.isDisabled()) {
                        FXUIGameMaster.requestToSetBrightness(new_val.doubleValue());
                    }
                }
            });
            britenessSlider.setDisable(FXUIGameMaster.isAutoBrightnessActive());
            
            autoDim.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    if (autoDim.isSelected()) {
                    	FXUIGameMaster.enableAutoAdjustBrightness();
                    } else if (!autoDim.isSelected()) {
                        FXUIGameMaster.disableAutoBrightness();
                        FXUIGameMaster.requestToSetBrightness(britenessSlider.getValue());
                    } else {
                        //do nothing
                    }
                }
            });
            
            
            
            //Disable and enable manual dimming based on AutoBrite status.
            autoDim.selectedProperty().addListener(new ChangeListener<Boolean>(){
				@Override
				public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
					britenessSlider.setDisable(newValue.booleanValue());
					britenessSliderLabel.setText(newValue.booleanValue() ? britenessSliderDisabled : britenessSliderEnabled);
				}
            });
            
            double widthOfLines = 240d;
            double strokeThicknessOfLines = 3.0d;
            Color colorOfLines = Color.CHOCOLATE;
            Line bufferLineOne = new Line(0,0,widthOfLines,0);
            Line bufferLineTwo = new Line(0,0,widthOfLines,0);
            Line bufferLineThree = new Line(0,0,widthOfLines,0);
            Line bufferLineFour = new Line(0,0,widthOfLines,0);
            bufferLineOne.setStrokeWidth(strokeThicknessOfLines);
            bufferLineTwo.setStrokeWidth(strokeThicknessOfLines);
            bufferLineThree.setStrokeWidth(strokeThicknessOfLines);
            bufferLineFour.setStrokeWidth(strokeThicknessOfLines);
            bufferLineOne.setStroke(colorOfLines);
            bufferLineTwo.setStroke(colorOfLines);
            bufferLineThree.setStroke(colorOfLines);
            bufferLineFour.setStroke(colorOfLines);
            Line bufferLineFive = new Line(0,0,widthOfLines,0);
            bufferLineFive.setStrokeWidth(strokeThicknessOfLines);
            bufferLineFive.setStroke(colorOfLines);
            Text brightnessCategoryLabel = new Text("Brightness/AutoBrite");
            Text eyeReliefCategoryLabel = new Text("eyeLief (eyestrain reduction)");
            brightnessCategoryLabel.setFont(Font.font("Verdana", FontWeight.LIGHT, 14));
            brightnessCategoryLabel.setFill(Color.WHITE);
            eyeReliefCategoryLabel.setFont(Font.font("Verdana", FontWeight.LIGHT, 14));
            eyeReliefCategoryLabel.setFill(Color.WHITE);
            
            layout.getChildren().addAll(
                    querySymbol, queryText, bufferLineOne, doFullScreen,
                    bufferLineTwo, windowSizeSliderLabel, windowSizeSlider, scrollNZoomOpt, zoomSettingLabel, zoomSettingChoice,
                    bufferLineThree, brightnessCategoryLabel, autoDim, britenessSliderLabel, britenessSlider,
                    bufferLineFour, eyeReliefCategoryLabel, eyeReliefChoice
            );
            if(!flatten){
            	layout.getChildren().addAll(bufferLineFive, yeah, spaceBuffer);
            }

            dialog.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent t) {
                    dialogIsShowing.set(false);
                    attachResizeSlider(null);
                }
            });
            layout.disabledProperty().addListener(new ChangeListener<Boolean>(){
				@Override
				public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
					if(newValue.booleanValue()){
						attachResizeSlider(null);
					}
					else{
						attachResizeSlider(windowSizeSlider);
					}
				}
            });
            if(!flatten){
            Pane finalPane = new Pane(layout);
            finalPane.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));
            dialog.setScene(new Scene(finalPane));
            dialog.show();
            }
            else{
            	return layout;
            }
        } catch (Exception e) {
            System.out.println("ERROR: resize dialog display failed:: " + e);
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Corrects the desired multiplier for the given screen, and applies the corrected
     * zoom multiplier as a scale to the correct content pane to achieve the zoom effect.
     * (i.e., corrects the multiplier to account for a given screen resolution, relative to
     * the default content resolution)
     * @param multiplier the uncorrected multiplier
     */
    private void correctScrollNZoomScale(double multiplier){
    	FXUIGameMaster.diagnosticPrintln("Adjusting magnification...");
    	new Thread(null, new Runnable() {
            @Override
            public void run() {
            	double preMultiplier = 1.0d;
                if(Math.abs(DEFAULT_CONTENT_WIDTH - activeStage.getWidth()) > Math.abs(DEFAULT_CONTENT_HEIGHT - activeStage.getHeight())){
                	preMultiplier = activeStage.getWidth() / DEFAULT_CONTENT_WIDTH;
                }
                else{
                	preMultiplier = activeStage.getHeight() / DEFAULT_CONTENT_HEIGHT;
                }
                preMultiplier = preMultiplier > 1 ? preMultiplier : 1;
                System.out.println("pane scale transforms: " + registeredStaticContentPane.getTransforms().toString());
                registeredStaticContentPane.getTransforms().setAll(new Scale(preMultiplier * multiplier, preMultiplier * multiplier, 0, 0));
            }
        }, "scrollNZoomScale").start();
    }
    
    private void undoScrollNZoomScale(){
        FXUIGameMaster.diagnosticPrintln("Disabling magnification...");
    	new Thread(null, new Runnable() {
            @Override
            public void run() {
                //final double multiplier = 1.0d;
            	//double preMultiplier = 1.0d;
                //registeredStaticContentPane.getTransforms().setAll(new Scale(preMultiplier * multiplier, preMultiplier * multiplier, 0, 0));
                registeredStaticContentPane.getTransforms().removeIf((transform) ->  transform.getClass().isInstance(new Scale()));
            }
        }, "undo_scrollNZoomScale").start();
    }
    
    /**
     * When making adjustments, show a protective screen to indicate that
     * resizing & scaling operations are occurring.
     */
    private Pane makeAdjustmentScreen(){
    	final Rectangle backgroundRect = new Rectangle(0,0,DEFAULT_CONTENT_WIDTH,DEFAULT_CONTENT_HEIGHT);
    	backgroundRect.setFill(Color.MIDNIGHTBLUE);
    	backgroundRect.setOpacity(0.7d);
    	final Text bottomText = new Text("please wait");
    	bottomText.setTextAlignment(TextAlignment.CENTER);
        bottomText.setFont(Font.font("Arial", FontWeight.BOLD, 96));
        bottomText.setFill(Color.BLACK);
        bottomText.setStroke(Color.BLUEVIOLET);
    	final Rectangle foregroundRect = new Rectangle(0,DEFAULT_CONTENT_HEIGHT/3,DEFAULT_CONTENT_WIDTH,DEFAULT_CONTENT_HEIGHT/3);
    	foregroundRect.setFill(Color.CORNSILK);
    	final Text topText = new Text("making adjustments");
    	topText.setTextAlignment(TextAlignment.CENTER);
        topText.setFont(Font.font("System", FontWeight.BOLD, 48));
        topText.setFill(Color.BLACK);
        topText.setStroke(Color.BLUEVIOLET);
        GaussianBlur gBlur = new GaussianBlur(1);
        Glow gGlow = new Glow(1.0d);
        gGlow.setInput(gBlur);
    	topText.setEffect(gGlow);
    	bottomText.setEffect(gGlow);
    	
        final VBox foreTextBox = new VBox(5);
        foreTextBox.getChildren().setAll(topText, bottomText);
        foreTextBox.setPrefWidth(DEFAULT_CONTENT_WIDTH);
        foreTextBox.setLayoutX(0);
        foreTextBox.setLayoutY(0);
        foreTextBox.setPrefHeight(DEFAULT_CONTENT_HEIGHT);
        foreTextBox.setAlignment(Pos.CENTER);
        Pane forePane = new Pane();
        forePane.getChildren().addAll(backgroundRect, foregroundRect, foreTextBox);
        forePane.setCache(true);
        forePane.setCacheHint(CacheHint.SPEED);
        return forePane;
    }
}
