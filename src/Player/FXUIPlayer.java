/*FXUI Player Class
*Albert Wallace, 2015. Version number now found in Player class definition.
*for Seth Denney's RISK, JavaFX UI-capable version
*
*Base build from original "player" interface, 
*incorporating elements of nothing but http://stackoverflow.com/questions/16823644/java-fx-waiting-for-user-input
*so thanks stackoverflow!
**/
package Player;

import Map.Country;
import Map.RiskMap;
import Master.FXUIGameMaster;
import static Master.FXUIGameMaster.COUNTRIES_BY_NAME;
import Response.AdvanceResponse;
import Response.AttackResponse;
import Response.CardTurnInResponse;
import Response.DefendResponse;
import Response.FortifyResponse;
import Response.ReinforcementResponse;
import Util.Card;
import Util.FXUI_Crossbar;
import Util.RiskConstants;
import Util.RiskUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.BlendMode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

// TODO revise handling of "system exits". Consider updating the crossbar, so FXUIGameMaster can respond appropriately with a well-placed check?
/**
 * Encapsulates the UI elements a human may use to respond to the GameMaster as
 * a valid Player.
 *
 * Requires FXUI GameMaster. Not compatible with original GameMaster;
 * implemented UI elements require triggering from active JavaFX application.
 *
 * UI elements are JavaFX, done with Java JDK 8.
 *
 */
public class FXUIPlayer implements Player {

    public static final String versionInfo = "FXUI-RISK-Player\nVersion 0113\nStamp 2016.6.28, 08:50\nStability: Alpha(01)";
    private static FXUI_Crossbar crossbar = new FXUI_Crossbar();
    private static Pane paneToAddControls = null;
    private static HashMap<String, Text> textNodeMap = null;
    private static boolean controlsInMainWindow = false;
    private static Window owner = null;
    private double windowXCoord = 0;
    private double windowYCoord = 0;
    private AtomicBoolean autoProgressDefense = new AtomicBoolean(false);
    private static final String defaultBackgroundColorStr = "-fx-base: mistyrose; ";
    private static final String defaultLayoutPadding = "-fx-padding: 20; ";
    private static final double mainWindowPosOffset = -15d;

    public static void setOwnerWindow(Window ownerIn) {
        FXUIPlayer.owner = ownerIn;
    }

    /**
     * Try to connect the main window to this class, so this class can
     * add/remove controls in the main window as necessary.
     *
     * @param paneForControls
     * @param controlTextLoc
     */
    public static void setMainWindowConnection(Pane paneForControls, HashMap<String, Text> controlTextLoc) {
        if (paneForControls == null || controlTextLoc == null) {
            throw new UnsupportedOperationException("Can't add null control info to FXUIPlayer.");
        }
        FXUIPlayer.paneToAddControls = paneForControls;
        FXUIPlayer.textNodeMap = controlTextLoc;
        FXUIPlayer.controlsInMainWindow = true;
    }

    /**
     * Getter for the FXUI GameMaster-Player Crossbar
     *
     * @return crossbar, the desired crossbar, static across all instances.
     */
    public static FXUI_Crossbar getCrossbar() {
        return crossbar;
    }

    /**
     * Setter for the FXUI GameMaster-Player Crossbar
     *
     * @param crossbar, the crossbar to use, static across all instances.
     */
    public static void setCrossbar(FXUI_Crossbar crossbar) {
        FXUIPlayer.crossbar = crossbar;
    }

    private String name;
    private static final int MAX_NAME_LENGTH = 22;
    private int reinforcementsApplied = 0;
    private int maxAtkDiceAvailable = 0;
    //private boolean passTurn = false;
    //private String attackTarget = blankText, attackSource = blankText;
    private boolean keepRunning = false;
    private final ExitStateSubHelper exitDecider = new ExitStateSubHelper();
    private static Stage persistentDialog = null;

    private boolean lastCoordIsKnown = false;
    private static HashMap<Stage, Node> nodesForBriteMap = new HashMap<>();
    private static HashMap<Stage, Scene> scenesForStrainReliefMap = new HashMap<>();

    /**
     * Note: there is an artificial limitation (imposed by this class) where
     * only one user may be a human player. If/when provisions are made to allow
     * naming of characters, the limitation can be safely removed with no loss
     * of functionality... ...except for where the crossbar is concerned. It
     * treats any/all human players as one.
     */
    public FXUIPlayer() {
        this("Human Player");
    }

    public FXUIPlayer(String nameIn) {
        this.name = nameIn;

    }

    public FXUIPlayer(boolean askForName, Collection<String> unavailableNames) {
        String desiredName = null;
        if (askForName && null != (desiredName = askForDesiredName(unavailableNames))) {
            this.name = desiredName;
        } else {
            this.name = "Human " + this.hashCode();
        }
    }

    private static Stage preparePersistentDialog() {
        if (persistentDialog == null || !persistentDialog.isShowing()) {
            Platform.runLater(() -> {
                persistentDialog = new Stage();
                FXUIPlayer.crossbar.setCurrentPlayerDialog(persistentDialog);
                //now let us continue with window/element setup
                persistentDialog.setTitle("Please wait...");
                if (FXUIPlayer.owner != null) {
                    persistentDialog.initOwner(FXUIPlayer.owner);
                }
            });
        }
        return persistentDialog;
    }

    private static void displayWithPersistentDialog(Scene contents) {

    }

    private static void clearPersistentDialogContents() {
        preparePersistentDialog();
    }

    /**
     * Given a valid dialog, extracts the X and Y coordinates of this window as
     * located on screen. Saves the coordinates to a couple of class instance
     * variables.
     *
     * @param dialog the window from which we will be gathering the coordinates
     */
    private void saveLastKnownWindowLocation(Stage dialog) {
        this.windowXCoord = dialog.getX();
        this.windowYCoord = dialog.getY();
        this.lastCoordIsKnown = true;
    }

    /**
     * Given a valid dialog, places the dialog at coordinates previously
     * recorded from a prior dialog. If no dialog's coordinates had been set,
     * uses whatever position is set in the associated vars.
     *
     * @param dialog dialog to be placed at the last remembered dialog coords.
     */
    private void putWindowAtLastKnownLocation(Stage dialog) {
        final int single_screen = 1;
        if (!this.lastCoordIsKnown && FXUIPlayer.owner != null) {
            dialog.setX(FXUIPlayer.owner.getX());
            dialog.setY(FXUIPlayer.owner.getY() + FXUIGameMaster.DEFAULT_DIALOG_OFFSET);
        } else if (Screen.getScreens().size() == single_screen) {
            double widthOfPriScreen = Screen.getPrimary().getVisualBounds().getWidth() - 5;
            double heightOfPriScreen = Screen.getPrimary().getVisualBounds().getHeight() - 25;
            dialog.setX(this.windowXCoord < 0 || this.windowXCoord > widthOfPriScreen ? 0 : this.windowXCoord);
            dialog.setY(this.windowYCoord < 0 || this.windowXCoord > heightOfPriScreen ? 0 : this.windowYCoord);
        } else {
            dialog.setX(this.windowXCoord);
            dialog.setY(this.windowYCoord);
        }
    }

    /**
     * Waits for distinct player dialogs to close. Requires dialogs to be
     * registered with the local Crossbar. Else, if no dialog is registered with
     * the local crossbar, immediately returns. If used with the incorrect
     * dialog, will stall indefinitely until the correct, associated dialog is
     * closed. Will be interrupted (and return) if an attempt to end the game is
     * registered by the local Crossbar. Also prompts class to clear old
     * references to past dialogs (Stages) which had been registered for
     * brightness & eye-strain management.
     */
    private void waitForDialogToClose(FXUI_Crossbar xbar) {
        RiskUtils.sleep(1000);
        do {
            RiskUtils.sleep(100);
        } while (xbar.getCurrentPlayerDialog() != null && xbar.getCurrentPlayerDialog().isShowing() && !xbar.isHumanEndingGame());
        deregisterInactivesFromBrightnessControl();
        deregisterInactivesFromEyeStrainControl();
    }

    /**
     * Decides if the prior closing of a dialog was intended as part of the
     * game's progression, or if the user closed the dialog methods for other
     * reasons. If the dialog was closed through other means, prompts the user
     * to confirm whether the window was closed in error, assuming the proper
     * flag was left set/unset. The local ExitDecider contains the flag to
     * determine whether the close was expected or unexpected. The local
     * Crossbar contains the flag to determine if we have already indicated that
     * the app *is* being closed.
     */
    private void checkIfCloseMeansMore(ExitStateSubHelper exitHelper, FXUI_Crossbar xbar) {
        if (exitHelper.isSystemExit() && !xbar.isHumanEndingGame()) {
            //ask if the user actually wants the game to end
            this.keepRunning = FXUIGameMaster.doYouWantToMakeAnExit(false, 0) <= 0;
        }
    }

    /**
     * Register a given Node for automatic or manual changing of the window's
     * brightness (opacity), in an attempt to prevent blinding light during
     * nighttime, based on the setting done by FXUIGameMaster & stored in the
     * FXUI_Crossbar.
     *
     * @param stageIn the Stage which houses the Node used as the primary layout
     * controller.
     * @param nodeIn the primary layout controller & root of the Scene for the
     * supplied Stage.
     */
    private void registerNodeForBrightnessControl(Stage stageIn, Node nodeIn) {
        nodesForBriteMap.put(stageIn, nodeIn);
        applyBrightnessControlToKnownNodes(FXUI_Crossbar.getBritenessOpacity());
    }

    /**
     * Checks for any inactive Stages and, for those found, deregisters their
     * associated layout Node from eye strain control. (Prevents the software
     * from trying to change the brightness of a Node which is no longer
     * active).
     */
    private void deregisterInactivesFromBrightnessControl() {
        for (Entry<Stage, Node> entrySN : nodesForBriteMap.entrySet()) {
            if (!entrySN.getKey().isShowing()) {
                nodesForBriteMap.remove(entrySN.getKey());
            }
        }

    }

    /**
     * Register a given Scene for automatic or manual changing of the background
     * color, in an attempt to reduce eye strain, based on the setting done by
     * FXUIGameMaster & stored in the FXUI_Crossbar.
     *
     * @param stageIn
     * @param sceneIn
     */
    private void registerSceneForEyeStrainControl(Stage stageIn, Scene sceneIn) {
        scenesForStrainReliefMap.put(stageIn, sceneIn);
        applyEyeStrainControlToKnownScenes(FXUI_Crossbar.getStrainReliefColor());
    }

    /**
     * Checks for any inactive Stages and, for those found, deregisters their
     * associated Scene from eye strain control. (Prevents the software from
     * trying to change the color of a Scene which is no longer active).
     */
    private void deregisterInactivesFromEyeStrainControl() {
        for (Entry<Stage, Scene> entrySS : scenesForStrainReliefMap.entrySet()) {
            if (!entrySS.getKey().isShowing()) {
                scenesForStrainReliefMap.remove(entrySS.getKey());
            }
        }

    }

    /**
     * Apply a given color to all known Scene objects (FXUIPlayer-related Scene
     * objects only) in an attempt to reduce eye strain.
     *
     * @param colorToApply
     */
    public static void applyEyeStrainControlToKnownScenes(Color colorToApply) {
        for (Entry<Stage, Scene> sceneApp : scenesForStrainReliefMap.entrySet()) {
            sceneApp.getValue().setFill(colorToApply);
        }
    }

    /**
     * Apply a given brightness to all known Node objects (FXUIPlayer-related
     * Node objects only, such as a primary Pane, ScrollPane, or VBox/HBox),
     * based on the prior setting controlled by the FXUIGameMaster class.
     *
     * @param opacity
     */
    public static void applyBrightnessControlToKnownNodes(double opacity) {
        for (Entry<Stage, Node> nodeApp : nodesForBriteMap.entrySet()) {
            nodeApp.getValue().setOpacity(opacity);
            nodeApp.getValue().setBlendMode(BlendMode.ADD);
        }
    }

    /**
     * Check to see if a potential name is valid.
     *
     * @param potentialName name to check
     * @param unavailableNames
     * @return "true" if acceptable, "false" otherwise
     */
    private boolean validateName(String potentialName, Collection<String> unavailableNames) {
        FXUIGameMaster.diagnosticPrintln("(" + potentialName + ")");
        if (potentialName == null) {
            return false;
        }
        potentialName = potentialName.trim();
        if (potentialName.length() < 1) {
            return false;
        }
        if (unavailableNames != null && unavailableNames.contains(potentialName)) {
            return false;
        }
        //String desiredCharSet = "[a-zA-Z0-9]{1-21}\\s[a-zA-Z0-9]{1-21}|[a-zA-Z0-9]{1,21}"; //chars with one space inbetween
        String desiredCharSet = "[a-zA-Z0-9]{1," + MAX_NAME_LENGTH + "}((\\s)?[a-zA-Z0-9]{0," + MAX_NAME_LENGTH + "})?"; //chars with one space in between
        Pattern patternToFind = Pattern.compile(desiredCharSet);
        // TODO optimize the above line; maybe relocate it to the functions which
        // would likely call this method. That way, it's only compiled once.
        Matcher matchContainer = patternToFind.matcher(potentialName);
        return matchContainer.matches() && potentialName.length() < MAX_NAME_LENGTH + 1;
    }

    /**
     * Getter for the player name. Ideally, the name is only set once, so no
     * separate "setter" exists.
     *
     * @return name
     */
    public String getName() {
        return this.name;
    }
    
    /**
     * Prepare a VBox for use with each dialog window.
     * Formats with color, spacing, etc.
     * @return the layout VBox
     */
    private VBox getVBoxFormattedForLayout(){
    	final VBox fmtLayout = new VBox(10);
    	fmtLayout.setAlignment(Pos.CENTER);
        fmtLayout.setStyle(defaultLayoutPadding + defaultBackgroundColorStr);
    	return fmtLayout;
    }
    
    /**
     * Edit a given ScrollPane for use in a dialog. Allows for
     * desired colors of a given parent node to show through, etc.
     * MUST CALL AFTER WINDOW IS SHOWING.
     * @return
     */
    private void formatSPaneInlayAfterShow(ScrollPane sPaneToEdit){
    	sPaneToEdit.setPannable(true);
    	sPaneToEdit.setStyle(defaultBackgroundColorStr);
		//sPaneToEdit.setBackground(null);
		Node viewportAccess = sPaneToEdit.lookup(".viewport");
		viewportAccess.setStyle("-fx-background-color: transparent");
    }

    /**
     * Presents a dialog to ask a user for the name they want Designed to run on
     * a main game thread that is NOT the JavaFX thread.
     *
     * @param unavailableNames names that are already taken
     * @return
     */
    private String askForDesiredName(Collection<String> unavailableNames) {
        final int maxTimes = 5;
        final AtomicInteger timesLeft = new AtomicInteger(maxTimes);
        System.out.println("Getting name of human...");

        TextField potentialName = new TextField();
        //Make the window and keep displaying until the user has confirmed selection
        do {
            this.keepRunning = false;
            final VBox layout = getVBoxFormattedForLayout();
            final Text guideText = new Text(); //generic prompt info
            final Text guideText2 = new Text(); //in-depth prompt info
            final Text statusText = new Text(); //status: acceptable or unacceptable
            timesLeft.set(maxTimes);
            Platform.runLater(new Runnable() {
                @Override
                public void run() {

                    /**
                     * *********
                     * Begin mandatory processing on FX thread. (Required for
                     * Stage objects.)
                     */
                    final Stage dialog = new Stage();
                    FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
                    potentialName.setDisable(false);

                    //now let us continue with window/element setup
                    dialog.setTitle("Set Player Name.");
                    if (FXUIPlayer.owner != null) {
                        dialog.initOwner(FXUIPlayer.owner);
                    }

                    guideText.setText("Please give us a name for your player, Human.");
                    guideText.setTextAlignment(TextAlignment.CENTER);
                    guideText.setFont(Font.font("System", 16));

                    guideText2.setText("\n" + MAX_NAME_LENGTH + " character limit,\nalphanumeric chacters allowed,\n1 space allowed,\nleading or trailing space ignored");
                    guideText2.setTextAlignment(TextAlignment.CENTER);
                    guideText2.setFont(Font.font("System", 13));

                    statusText.setText("--------");

                    potentialName.setPromptText("[enter name here]");

                    Button checkName = new Button("check name");
                    fireButtonAfter3SHover(checkName);
                    Button acceptIt = new Button("accept/ok");
                    fireButtonAfter3SHover(acceptIt);
                    Button autoSet = new Button("skip(auto-set)");
                    repeatFireOnLongPress(autoSet);

                    potentialName.setOnKeyTyped(new EventHandler<KeyEvent>() {
                        @Override
                        public void handle(KeyEvent t) {
                            //if(validateName(t.getCharacter())){
                            //put stuff here if you want to ignore any invalid input off-the-bat
                            //e.g., if they enter a single invalid character, 
                            //we can catch it when they enter it
                            //}
                            timesLeft.set(maxTimes);
                            if (validateName(potentialName.getText() + t.getCharacter(), unavailableNames)) {
                                statusText.setText("name OK!!");
                                statusText.setFill(Color.BLACK);
                            } else {
                                statusText.setText("invalid!!");
                                statusText.setFill(Color.BLACK);
                            }
                        }
                    });
                    checkName.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent t) {
                            potentialName.setDisable(true);
                            if (validateName(potentialName.getText(), unavailableNames)) {
                                statusText.setText("name OK!!");
                                statusText.setFill(Color.BLUE);
                            } else {
                                statusText.setText("invalid!!");
                                statusText.setFill(Color.RED);
                            }
                            potentialName.setDisable(false);
                        }
                    });
                    acceptIt.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent t) {
                            potentialName.setDisable(true);
                            if (validateName(potentialName.getText(), unavailableNames)) {
                                exitDecider.setAsNonSystemClose();
                                saveLastKnownWindowLocation(dialog);
                                dialog.close();
                            } else {
                                statusText.setText("invalid!!");
                                statusText.setFill(Color.RED);
                                potentialName.setDisable(false);
                            }
                        }
                    });

                    autoSet.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent t) {
                            if (validateName(potentialName.getText(), unavailableNames)) {
                                exitDecider.setAsNonSystemClose();
                                saveLastKnownWindowLocation(dialog);
                                dialog.close();
                            } else {
                                timesLeft.decrementAndGet();
                                statusText.setText("press " + timesLeft + "x to auto-set");
                                statusText.setFill(Color.BLACK);
                                if (timesLeft.get() == 0) {
                                    potentialName.setText(null);
                                    exitDecider.setAsNonSystemClose();
                                    saveLastKnownWindowLocation(dialog);
                                    dialog.close();
                                }
                            }
                        }
                    });

                    layout.getChildren().addAll(guideText, guideText2, statusText, potentialName, checkName, acceptIt, autoSet);
                    //formally add linear layout to scene, and display the dialog
                    dialog.setScene(new Scene(layout));
                    registerNodeForBrightnessControl(dialog, layout);
                    registerSceneForEyeStrainControl(dialog, dialog.getScene());
                    dialog.show();
                }
            });

            /**
             * End mandatory FX thread processing. Immediately following this,
             * pause to wait for FX dialog to be closed!
             */
            waitForDialogToClose(FXUIPlayer.crossbar);
            checkIfCloseMeansMore(exitDecider, FXUIPlayer.crossbar);
            reinforcementsApplied = 0;
            FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
        } while (this.keepRunning);
        return potentialName.getText();
    }

    /**
     * Specify an allocation of the player's initial reinforcements. RESPONSE
     * REQUIRED
     *
     * @param map
     * @param reinforcements
     * @return initial allocation
     */
    public ReinforcementResponse getInitialAllocation(RiskMap map, int reinforcements) {
        //if the player asked to end the game, don't even display the dialog
        if (crossbar.isHumanEndingGame()) {
            return null;
        }
        if (FXUIPlayer.controlsInMainWindow) {
            return getInitialAllocationFlat(map, reinforcements);
        }

        //else...make the window and keep displaying until the user has confirmed selection
        final ReinforcementResponse rsp = new ReinforcementResponse();
        do {
            this.keepRunning = false;

            final Set<Country> myCountries = RiskUtils.getPlayerCountries(map, this.name);
            final HashMap<String, Integer> countryUsedReinforcementCount = new HashMap<String, Integer>();
            final HashMap<String, Text> countryTextCache = new HashMap<String, Text>();

            final VBox layout = getVBoxFormattedForLayout();
            final Text guideText = new Text(); //generic instructions for initial allocation
            final Text statusText = new Text(); //status: total reinforcements available, reinf used, reinf available.
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    /**
                     * *********
                     * Begin mandatory processing on FX thread. (Required for
                     * Stage objects.)
                     */

                    final Stage dialog = new Stage();

                    //now let us continue with window/element setup
                    dialog.setTitle("Initial Troop Allocation!");
                    if (FXUIPlayer.owner != null) {
                        dialog.initOwner(FXUIPlayer.owner);
                    }
                    putWindowAtLastKnownLocation(dialog);
                    
                    guideText.setText("You have been assigned starting countries (seen below)\nand " + reinforcements + " initial troops;"
                            + "\nplease allocate those troops now.\nOne troop per country minimum;\nMust use all available troops.");
                    guideText.setTextAlignment(TextAlignment.CENTER);
                    layout.getChildren().add(guideText);

                    statusText.setText("Total: " + reinforcements + "\nUsed: " + reinforcementsApplied + "\nAvailable: " + (reinforcements - reinforcementsApplied));

                    /*
					* Text to indicate country + count being sent to that country for each country you own,
					* plus the buttons to increment/decrement said count. (Target minimum count for each country is 1).
					* Text + buttons are immediately added to the target vertical layout one row/country at a time.
                     */
                    //FXUIPlayer.paneToAddControls = paneForControls;
                    //FXUIPlayer.textNodeMap = controlTextLoc;
                    //FXUIPlayer.controlsInMainWindow = true;
                    final ArrayList<Node> mainWindowControlCache = new ArrayList<Node>();

                    for (final Country ctIn : myCountries) {
                        final HBox singleCountryDisp = new HBox(4);
                        singleCountryDisp.setAlignment(Pos.CENTER);
                        map.getCountryArmies(ctIn);
                        countryUsedReinforcementCount.put(ctIn.getName(), 1);
                        reinforcementsApplied++;
                        countryTextCache.put(ctIn.getName(), new Text(ctIn.getName() + " + 1"));
                        singleCountryDisp.getChildren().add(countryTextCache.get(ctIn.getName()));

                        //button to increment reinforcement count for selected country
                        Button plus = new Button("+");
                        repeatFireOnLongPress(plus);
                        plus.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent event) {
                                final String countryAffected = ctIn.getName();
                                if (reinforcementsApplied + 1 <= reinforcements) {
                                    reinforcementsApplied++;
                                    countryUsedReinforcementCount.put(countryAffected, countryUsedReinforcementCount.get(countryAffected) + 1);
                                }
                                refreshReinforcementDisplay(false, countryTextCache, countryUsedReinforcementCount, statusText, reinforcements);
                            }
                        });
                        //button to decrement reinforcement count for selected country
                        Button minus = new Button("-");
                        repeatFireOnLongPress(minus);
                        minus.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent t) {
                                final String countryAffected = ctIn.getName();
                                if (reinforcementsApplied - 1 >= 0 && countryUsedReinforcementCount.get(countryAffected) - 1 >= 1) {
                                    reinforcementsApplied--;
                                    countryUsedReinforcementCount.put(countryAffected, countryUsedReinforcementCount.get(countryAffected) - 1);
                                }
                                refreshReinforcementDisplay(false, countryTextCache, countryUsedReinforcementCount, statusText, reinforcements);
                            }
                        });
                        singleCountryDisp.getChildren().addAll(plus, minus);
                        layout.getChildren().add(singleCountryDisp);
                        
                    }

                    refreshReinforcementDisplay(false, countryTextCache, countryUsedReinforcementCount, statusText, reinforcements);

                    //button to attempt to accept final reinforcement allocation
                    Button acceptIt = new Button("Accept/OK");
                    fireButtonAfter3SHover(acceptIt);
                    acceptIt.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent t) {
                            if (reinforcementsApplied == reinforcements) {
                                for (Country country : myCountries) {
                                    rsp.reinforce(country, countryUsedReinforcementCount.get(country.getName()));
                                }
                                exitDecider.setAsNonSystemClose();
                                saveLastKnownWindowLocation(dialog);

                                dialog.close();
                            } else {
                                refreshReinforcementDisplay(true, countryTextCache, countryUsedReinforcementCount, statusText, reinforcements);
                            }
                        }
                    });

                    dialog.setOnHiding(new EventHandler<WindowEvent>() {
                        @Override
                        public void handle(WindowEvent event) {
                            for (Node potentialRemove : mainWindowControlCache) {
                                FXUIPlayer.paneToAddControls.getChildren().remove(potentialRemove);
                            }
                        }
                    });

                    layout.getChildren().addAll(statusText, acceptIt);
                    ScrollPane superSPane = new ScrollPane(layout);

                    //formally add linear layout to scene, and display the dialog
                    dialog.setScene(new Scene(superSPane));

                    FXUIPlayer.crossbar.setCurrentHumanName(getName());
                    FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);

                    registerNodeForBrightnessControl(dialog, superSPane);
                    registerSceneForEyeStrainControl(dialog, dialog.getScene());
                    dialog.show();

                    formatSPaneInlayAfterShow(superSPane);
                }
            });

            /**
             * End mandatory FX thread processing. Immediately following this,
             * pause to wait for FX dialog to be closed!
             */
            waitForDialogToClose(FXUIPlayer.crossbar);
            checkIfCloseMeansMore(exitDecider, FXUIPlayer.crossbar);
            reinforcementsApplied = 0;
            FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
        } while (this.keepRunning);
        return rsp;
    }
    
    /**
     * Specify an allocation of the player's initial reinforcements. RESPONSE
     * REQUIRED
     *
     * @param map
     * @param reinforcements
     * @return initial allocation
     */
    public ReinforcementResponse getInitialAllocationFlat(RiskMap map, int reinforcements) {
        //if the player asked to end the game, don't even display the dialog
        if (crossbar.isHumanEndingGame()) {
            return null;
        }

        //else...make the window and keep displaying until the user has confirmed selection
        final ReinforcementResponse rsp = new ReinforcementResponse();
        do {
            this.keepRunning = false;

            final Set<Country> myCountries = RiskUtils.getPlayerCountries(map, this.name);
            final HashMap<String, Integer> countryUsedReinforcementCount = new HashMap<String, Integer>();
            final HashMap<String, Text> countryTextCache = new HashMap<String, Text>();

            final VBox layout = getVBoxFormattedForLayout();
            final Text guideText = new Text(); //generic instructions for initial allocation
            final Text statusText = new Text(); //status: total reinforcements available, reinf used, reinf available.
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    /**
                     * *********
                     * Begin mandatory processing on FX thread. (Required for
                     * Stage objects.)
                     */

                    final Stage dialog = new Stage();

                    //now let us continue with window/element setup
                    dialog.setTitle("Initial Troop Allocation!");
                    if (FXUIPlayer.owner != null) {
                        dialog.initOwner(FXUIPlayer.owner);
                    }
                    putWindowAtLastKnownLocation(dialog);
                    
                    guideText.setText("You have been assigned starting countries (seen below)\nand " + reinforcements + " initial troops;"
                            + "\nplease allocate those troops now.\nOne troop per country minimum;\nMust use all available troops.");
                    guideText.setTextAlignment(TextAlignment.CENTER);
                    layout.getChildren().add(guideText);

                    statusText.setText("Total: " + reinforcements + "\nUsed: " + reinforcementsApplied + "\nAvailable: " + (reinforcements - reinforcementsApplied));

                    /*
					* Text to indicate country + count being sent to that country for each country you own,
					* plus the buttons to increment/decrement said count. (Target minimum count for each country is 1).
					* Text + buttons are immediately added to the target vertical layout one row/country at a time.
                     */
                    
                    //TODO remove this reference info
                    //Place containing coordinates, and destination for controls:
                    //FXUIPlayer.paneToAddControls = paneForControls;
                    //FXUIPlayer.textNodeMap = controlTextLoc;
                    //FXUIPlayer.controlsInMainWindow = true;
                    
                    final ArrayList<Node> mainWindowControlCache = new ArrayList<Node>();

                    
                    //click targets to display individual controls
                    for (final Country ctIn : myCountries) {
                    	
                    }
                    //controls to actually change allotment
                    for (final Country ctIn : myCountries) {
                        final HBox singleCountryDisp = new HBox(4);
                        singleCountryDisp.setAlignment(Pos.CENTER);
                        map.getCountryArmies(ctIn);
                        countryUsedReinforcementCount.put(ctIn.getName(), 1);
                        reinforcementsApplied++;
                        countryTextCache.put(ctIn.getName(), new Text(ctIn.getName() + " + 1"));
                        //singleCountryDisp.getChildren().add(countryTextCache.get(ctIn.getName()));

                        //button to increment reinforcement count for selected country
                        Button plus = new Button("+");
                        repeatFireOnLongPress(plus);
                        plus.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent event) {
                                final String countryAffected = ctIn.getName();
                                if (reinforcementsApplied + 1 <= reinforcements) {
                                    reinforcementsApplied++;
                                    countryUsedReinforcementCount.put(countryAffected, countryUsedReinforcementCount.get(countryAffected) + 1);
                                }
                                refreshReinforcementDisplay(false, countryTextCache, countryUsedReinforcementCount, statusText, reinforcements);
                            }
                        });
                        //button to decrement reinforcement count for selected country
                        Button minus = new Button("-");
                        repeatFireOnLongPress(minus);
                        minus.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent t) {
                                final String countryAffected = ctIn.getName();
                                if (reinforcementsApplied - 1 >= 0 && countryUsedReinforcementCount.get(countryAffected) - 1 >= 1) {
                                    reinforcementsApplied--;
                                    countryUsedReinforcementCount.put(countryAffected, countryUsedReinforcementCount.get(countryAffected) - 1);
                                }
                                refreshReinforcementDisplay(false, countryTextCache, countryUsedReinforcementCount, statusText, reinforcements);
                            }
                        });
                        singleCountryDisp.getChildren().addAll(minus, countryTextCache.get(ctIn.getName()), plus);
                        //layout.getChildren().add(singleCountryDisp);
                        if (FXUIPlayer.controlsInMainWindow) {
                        	final double baseScaleAmount = 0.7;
                            HBox duplicate = new HBox();
                            duplicate.setStyle("-fx-background-color: white");
                            duplicate.setLayoutX(FXUIPlayer.textNodeMap.get(ctIn.getName()).getX() + mainWindowPosOffset);
                            duplicate.setLayoutY(FXUIPlayer.textNodeMap.get(ctIn.getName()).getY() + mainWindowPosOffset); //TODO refine how you cover the existing text
                            duplicate.getChildren().add(singleCountryDisp);
                            mainWindowControlCache.add(duplicate);
                            duplicate.setScaleX(baseScaleAmount);
                            duplicate.setScaleY(baseScaleAmount);
                            duplicate.hoverProperty().addListener(new ChangeListener<Boolean>() {
                                @Override
                                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
                                        Boolean newValue) {
                                    final double scaleAmount = newValue.booleanValue() ? 2.0 : baseScaleAmount;
                                    duplicate.setScaleX(scaleAmount);
                                    duplicate.setScaleY(scaleAmount);
                                    duplicate.toFront();
                                    //TODO validate shifting values; the threshold may be too low
                                    //or too high, and/or may cause the boxes to shift too much.
                                    if(newValue && (duplicate.getLayoutX() < 200 || duplicate.getLayoutX() > FXUIGameMaster.DEFAULT_CONTENT_WIDTH - 270)){
                                    	duplicate.setTranslateX(duplicate.getLayoutX() < 200 ? 50 : -50);
                                    }
                                    else{
                                    	duplicate.setTranslateX(0);
                                    }

                                }
                            });
                            FXUIPlayer.paneToAddControls.getChildren().add(duplicate);
                        }
                    }

                    refreshReinforcementDisplay(false, countryTextCache, countryUsedReinforcementCount, statusText, reinforcements);

                    //button to attempt to accept final reinforcement allocation
                    Button acceptIt = new Button("Accept/OK");
                    fireButtonAfter3SHover(acceptIt);
                    acceptIt.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent t) {
                            if (reinforcementsApplied == reinforcements) {
                                for (Country country : myCountries) {
                                    rsp.reinforce(country, countryUsedReinforcementCount.get(country.getName()));
                                }
                                exitDecider.setAsNonSystemClose();
                                saveLastKnownWindowLocation(dialog);

                                dialog.close();
                            } else {
                                refreshReinforcementDisplay(true, countryTextCache, countryUsedReinforcementCount, statusText, reinforcements);
                            }
                        }
                    });

                    dialog.setOnHiding(new EventHandler<WindowEvent>() {
                        @Override
                        public void handle(WindowEvent event) {
                            for (Node potentialRemove : mainWindowControlCache) {
                                FXUIPlayer.paneToAddControls.getChildren().remove(potentialRemove);
                            }
                        }
                    });

                    layout.getChildren().addAll(statusText, acceptIt);
                    ScrollPane superSPane = new ScrollPane(layout);

                    //formally add linear layout to scene, and display the dialog
                    dialog.setScene(new Scene(superSPane));

                    FXUIPlayer.crossbar.setCurrentHumanName(getName());
                    FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);

                    registerNodeForBrightnessControl(dialog, superSPane);
                    registerSceneForEyeStrainControl(dialog, dialog.getScene());
                    dialog.show();

                    formatSPaneInlayAfterShow(superSPane);
                }
            });

            /**
             * End mandatory FX thread processing. Immediately following this,
             * pause to wait for FX dialog to be closed!
             */
            waitForDialogToClose(FXUIPlayer.crossbar);
            checkIfCloseMeansMore(exitDecider, FXUIPlayer.crossbar);
            reinforcementsApplied = 0;
            FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
        } while (this.keepRunning);
        return rsp;
    }

    /**
     * Propose a subset of the player's cards that can be redeemed for
     * additional reinforcements. RESPONSE REQUIRED WHEN PLAYER HOLDS AT LEAST 5
     * CARDS, OTHERWISE OPTIONAL Dummy method for this class.
     *
     * @param map
     * @param myCards
     * @param playerCards
     * @param turnInRequired
     * @return subset of the player's cards
     */
    @Override
    public CardTurnInResponse proposeTurnIn(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, boolean turnInRequired) {
        //if the player asked to end the game, don't even display the dialog
        if (crossbar.isHumanEndingGame()) {
            return null;
        }

        //else...make the window and keep displaying until the user has confirmed selection
        final CardTurnInResponse rsp = new CardTurnInResponse();
        final HashMap<Integer, Card> cardsToTurnIn = new HashMap<>();
        AtomicBoolean passTurn = new AtomicBoolean(false);
        do {
            //this.passTurn = true;
            passTurn.set(true);
            this.keepRunning = false;
            final HashMap<Integer, Text> cardStatusMapping = new HashMap<>();
            Platform.runLater(new Runnable() {
                @Override
                public void run() {

                    /**
                     * *********
                     * Begin mandatory processing on FX thread. (Required for
                     * Stage objects.)
                     */
                    final Stage dialog = new Stage();
                    final String selected = "*SELECTED*";
                    final String deselected = "not selected";

                    Text guideText = new Text(); //guide text: generic instructions for turning in cards
                    final String guideTextIfRequired = "As you have " + myCards.size() + " cards,\nplease turn in a selection of 3 cards:\n3x same type\nOR\n3x different type\nOR\nWild+Any combo\n"
                            + "[This action is required for this round]";
                    final String guideTextIfOptional = "Turn In Cards?\nIf you can form a set of cards with...\n3x same type\nOR\n3x different type\nOR\nWild+Any two\n"
                            + "...You are allowed to do so at this point.\nOtherwise, you may review your cards for later use.";

                    final VBox layout = getVBoxFormattedForLayout();
                    final HBox cardArrayDisplayRowA = new HBox(4);
                    final HBox cardArrayDisplayRowB = new HBox(4);

                    final Text statusText = new Text("--\n--"); //status text: used to indicate if an error occurred upon attempted submission
                    Button acceptIt = new Button("Accept/OK");
                    fireButtonAfter3SHover(acceptIt);
                    Button skipIt = new Button("Skip Action");
                    fireButtonAfter3SHover(skipIt);

                    //now...begin handling the layout details and such.
                    dialog.setTitle(turnInRequired ? "Please Turn In Cards (required)" : "Turn In Cards? (optional)");
                    if (FXUIPlayer.owner != null) {
                        dialog.initOwner(FXUIPlayer.owner);
                    }

                    putWindowAtLastKnownLocation(dialog);

                    //further set up the guide text, depending on whether you must turn in cards or not
                    guideText.setText(turnInRequired ? guideTextIfRequired : guideTextIfOptional);
                    guideText.setTextAlignment(TextAlignment.CENTER);

                    //set up the player's Cards for display, if any Cards are available. (each Card is represented as a button)
                    cardArrayDisplayRowA.setAlignment(Pos.CENTER);
                    cardArrayDisplayRowB.setAlignment(Pos.CENTER);
                    int indexInCards = 0;

                    for (final Card cdIn : myCards) {
                        final int indexInCardsUM = indexInCards;
                        final VBox cardWithStatus = new VBox(4);
                        Text subText = new Text(deselected);

                        cardStatusMapping.put(indexInCards, subText);
                        String ctySrced = cdIn.getType().equals(RiskConstants.WILD_CARD) ? "wild" : cdIn.getCountry().getName();
                        Button card = new Button("******\n[type]\n" + cdIn.getType().toLowerCase() + "\n*****\n" + ctySrced.toLowerCase() + "\n[country]\n******");
                        card.setAlignment(Pos.CENTER);
                        card.setTextAlignment(TextAlignment.CENTER);
                        card.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent t) {
                                final Integer cardAffected = (Integer) indexInCardsUM;
                                if (cardsToTurnIn.containsKey(cardAffected)) {
                                    cardsToTurnIn.remove(cardAffected);
                                    cardStatusMapping.get(cardAffected).setText(deselected);
                                    cardStatusMapping.get(cardAffected).setFont(Font.getDefault());
                                } else {
                                    cardsToTurnIn.put(cardAffected, cdIn);
                                    cardStatusMapping.get(cardAffected).setText(selected);
                                    cardStatusMapping.get(cardAffected).setFont(Font.font(null, FontWeight.BOLD, -1));
                                }
                            }
                        });
                        indexInCards++;
                        cardWithStatus.getChildren().addAll(card, subText);
                        if (indexInCards > 2) {
                            cardArrayDisplayRowB.getChildren().add(cardWithStatus);
                        } else {
                            cardArrayDisplayRowA.getChildren().add(cardWithStatus);
                        }
                    }

                    acceptIt.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent t) {
                            if (cardsToTurnIn.size() == RiskConstants.NUM_CARD_TURN_IN) {
                                for (Card cdOut : cardsToTurnIn.values()) {
                                    rsp.addCard(cdOut);
                                }
                                if (CardTurnInResponse.isValidResponse(rsp, myCards)) {
                                    passTurn.set(false);
                                    exitDecider.setAsNonSystemClose();
                                    saveLastKnownWindowLocation(dialog);
                                    dialog.close();
                                } else {
                                    statusText.setText("invalid selection.\n(cards not a valid set)");
                                    rsp.resetCards();
                                }
                            } else if (!turnInRequired) {
                                passTurn.set(true);
                                exitDecider.setAsNonSystemClose();
                                saveLastKnownWindowLocation(dialog);
                                dialog.close();
                            } else {
                                statusText.setText("invalid selection.\n(invalid card count. " + RiskConstants.NUM_CARD_TURN_IN + " required)");
                            }
                        }
                    });

                    skipIt.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent t) {
                            passTurn.set(true);
                            exitDecider.setAsNonSystemClose();
                            saveLastKnownWindowLocation(dialog);
                            dialog.close();
                        }
                    });

                    if (turnInRequired) {
                        skipIt.setDisable(true);
                        skipIt.setText("Skip [unavailable]");
                    }

                    //add status and buttons to layout
                    layout.getChildren().addAll(guideText, cardArrayDisplayRowA, cardArrayDisplayRowB, statusText, acceptIt, skipIt);

                    //formally add linear layout to scene, and display the dialog
                    dialog.setScene(new Scene(layout));

                    FXUIPlayer.crossbar.setCurrentHumanName(getName());
                    FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
                    registerNodeForBrightnessControl(dialog, layout);
                    registerSceneForEyeStrainControl(dialog, dialog.getScene());
                    dialog.show();
                }
            });

            /**
             * End mandatory FX thread processing. Immediately after this, pause
             * the non-UI thread (which you should be back on) and wait for the
             * dialog to close!
             */
            waitForDialogToClose(FXUIPlayer.crossbar);
            checkIfCloseMeansMore(exitDecider, FXUIPlayer.crossbar);
            FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
        } while (this.keepRunning);

        if (passTurn.get()) {
            return null;
        }
        return rsp;
    }

    /**
     * Specify an allocation of the player's reinforcements. RESPONSE REQUIRED
     *
     * @param map
     * @param myCards
     * @param playerCards
     * @param reinforcements
     * @return reinforcement allocation
     */
    public ReinforcementResponse reinforce(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, int reinforcements) {
        //if the player asked to end the game, don't even display the dialog
        if (crossbar.isHumanEndingGame()) {
            return null;
        }

        //else...make the window and keep displaying until the user has confirmed selection
        final ReinforcementResponse rsp = new ReinforcementResponse();
        do {
            this.keepRunning = false;
            final Set<Country> myCountries = RiskUtils.getPlayerCountries(map, this.name);
            final HashMap<String, Integer> countryUsedReinforcementCount = new HashMap<String, Integer>();
            final HashMap<String, Text> countryTextCache = new HashMap<String, Text>();
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    /**
                     * *********
                     * Begin mandatory processing on FX thread. (Required for
                     * Stage objects.)
                     */
                    final Stage dialog = new Stage();
                    final VBox layout = getVBoxFormattedForLayout();
                    
                    final ScrollPane superSPane = new ScrollPane();
                    final Text guideText = new Text();
                    final Text statusText = new Text();
                    final Button acceptIt = new Button("Accept/OK");
                    fireButtonAfter3SHover(acceptIt);

                    //updating the elements with their contents &/or styles...
                    dialog.setTitle("Reinforcement with new troops!");
                    if (FXUIPlayer.owner != null) {
                        dialog.initOwner(FXUIPlayer.owner);
                    }
                    putWindowAtLastKnownLocation(dialog);

                    //Generic instructions for reinforcement
                    guideText.setText("Please place extra reinforcements\nin the countries you own.");
                    guideText.setTextAlignment(TextAlignment.CENTER);
                    layout.getChildren().add(guideText);

                    //status text: total reinforcements available, reinf used, reinf available.
                    statusText.setText("Total: " + reinforcements + "\nUsed: " + reinforcementsApplied + "\nAvailable: " + (reinforcements - reinforcementsApplied));

                    //Meat and potatoes of the dialog, generates the display for each of the countries (along with their current count),
                    //  as well as creates the buttons to increment/decrement the troop count for each associated country.
                    //Each country (& its controls) is given its own row, and is added to the layout as it is generated here.
                    //This button/text generation only happens once and is updated in-place.
                    for (final Country ctIn : myCountries) {
                        final HBox singleCountryDisp = new HBox(4);
                        Button plus = new Button("+");
                        Button minus = new Button("-");
                        repeatFireOnLongPress(plus);
                        repeatFireOnLongPress(minus);
                        singleCountryDisp.setAlignment(Pos.CENTER);
                        map.getCountryArmies(ctIn);
                        countryUsedReinforcementCount.put(ctIn.getName(), 0);
                        countryTextCache.put(ctIn.getName(), new Text(ctIn.getName() + " + 0")); //"place" the country and its current reinf count
                        singleCountryDisp.getChildren().add(countryTextCache.get(ctIn.getName()));

                        //set what "plus" button does (increment reinforcement count for selected country)
                        plus.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent t) {
                                final String countryAffected = ctIn.getName();
                                if (reinforcementsApplied + 1 <= reinforcements) {
                                    reinforcementsApplied++;
                                    countryUsedReinforcementCount.put(countryAffected, countryUsedReinforcementCount.get(countryAffected) + 1);
                                }
                                refreshReinforcementDisplay(false, countryTextCache, countryUsedReinforcementCount, statusText, reinforcements);
                            }
                        });

                        //set what "minus" button does (decrement reinforcement count for selected country)
                        minus.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent t) {
                                final String countryAffected = ctIn.getName();
                                if (reinforcementsApplied - 1 >= 0 && countryUsedReinforcementCount.get(countryAffected) - 1 >= 0) {
                                    reinforcementsApplied--;
                                    countryUsedReinforcementCount.put(countryAffected, countryUsedReinforcementCount.get(countryAffected) - 1);
                                }
                                refreshReinforcementDisplay(false, countryTextCache, countryUsedReinforcementCount, statusText, reinforcements);
                            }
                        });
                        singleCountryDisp.getChildren().addAll(plus, minus);
                        layout.getChildren().add(singleCountryDisp);
                    }

                    //button to attempt to accept final reinforcement allocation
                    acceptIt.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            if (reinforcementsApplied == reinforcements) {
                                for (Country country : myCountries) {
                                    rsp.reinforce(country, countryUsedReinforcementCount.get(country.getName()));
                                }
                                exitDecider.setAsNonSystemClose();
                                saveLastKnownWindowLocation(dialog);
                                dialog.close();
                            } else {
                                refreshReinforcementDisplay(true, countryTextCache, countryUsedReinforcementCount, statusText, reinforcements);
                            }
                        }
                    });//end eventhandler actionevent

                    //add status info Text and acceptance Button to layout. (Note: this method does not ever allow the player to "skip")
                    layout.getChildren().addAll(statusText, acceptIt);

                    //formally add linear layout to scene, and display the dialog
                    superSPane.setContent(layout);
                    dialog.setScene(new Scene(superSPane));
                    FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
                    FXUIPlayer.crossbar.setCurrentHumanName(getName());
                    refreshReinforcementDisplay(false, countryTextCache, countryUsedReinforcementCount, statusText, reinforcements);

                    registerNodeForBrightnessControl(dialog, superSPane);
                    registerSceneForEyeStrainControl(dialog, dialog.getScene());
                    dialog.show();
                    formatSPaneInlayAfterShow(superSPane);
                }
            });

            /**
             * End mandatory FX thread processing. Immediately after this, pause
             * the non-UI thread (which you should be back on) and wait for the
             * dialog to close!
             */
            waitForDialogToClose(FXUIPlayer.crossbar);
            checkIfCloseMeansMore(exitDecider, FXUIPlayer.crossbar);
            FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
            reinforcementsApplied = 0;
        } while (this.keepRunning);
        return rsp;
    }

    /**
     * Helper method to refresh the display of the "Reinforce" dialog. Task
     * includes simple indication of an error state upon improper reinforcement
     * allocation.
     *
     * @param isError set to "True" if you want to indicate that an error state
     * has been triggered, false otherwise
     * @param textElements Primary window content to be updated. Updated
     * in-place. Updates only the affected country.
     * @param dataSource the integers representing the current troop counts, as
     * mapped to each country
     * @param statusText Secondary window content to be updated; the so-called
     * status text created in the "Reinforce" method. Updated in-place.
     * @param reinforcements the total number of reinforcements available to the
     * user during this turn/during this singular call to "Reinforce"
     */
    private void refreshReinforcementDisplay(boolean isError, HashMap<String, Text> textElements, HashMap<String, Integer> dataSource, Text statusText, int reinforcements) {
        statusText.setText("Total: " + reinforcements + "\nUsed: " + reinforcementsApplied + "\nAvailable: " + (reinforcements - reinforcementsApplied));
        for (String countryToUpdate : textElements.keySet()) {
            textElements.get(countryToUpdate).setText(countryToUpdate + " ::: " + dataSource.get(countryToUpdate));
            if (isError) {
                textElements.get(countryToUpdate).setFill(Color.RED);
                statusText.setFill(Color.RED);
            } else {
                textElements.get(countryToUpdate).setFill(Color.BLACK);
                statusText.setFill(Color.BLACK);
            }
        }
    }
    
    //FLAT ATTACK:
    //IF source = destination, deselect (button + country)
    //IF source already selected, deselect & reselect (button + country)
    //IF destination already selected, deselct & reselect (button + country)
    //IF click button once active, deselect (button + country)
    //ELSE, select (button + country)

    /**
     * Specify how and where an attack should be mounted. RESPONSE OPTIONAL
     *
     * @param map
     * @param myCards
     * @param playerCards
     * @return attack choice
     */
    public AttackResponse attackFlat(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards) {
        //if the player asked to end the game, don't even display the dialog
        if (crossbar.isHumanEndingGame()) {
            return null;
        }
        //else...make the window and keep displaying until the user has confirmed selection
        final AttackResponse rsp = new AttackResponse();
        final AtomicBoolean passTurn = new AtomicBoolean(false);
        final String blankText = "-----";
        final AtomicReference<String> attackTarget = new AtomicReference<>(blankText);
        final AtomicReference<String> attackSource = new AtomicReference<>(blankText);
        //Collection<Country> sources = RiskUtils.getPossibleSourceCountries(map, RiskUtils.getPlayerCountries(map, this.getName()));

        //HashMap<String, Button> potentialTargets = new HashMap<>();
        HashMap<String, Button> sourceBtnCache = new HashMap<>(), targetBtnCache = new HashMap<>();
        HashMap<String, ArrayList<String>> destTargetSourceMap = new HashMap<>();
        ArrayList<String> srcCountryNames = new ArrayList<>();
        ArrayList<String> tgtCountryNames = new ArrayList<>();
        RiskUtils.getPossibleSourceCountries(map, RiskUtils.getPlayerCountries(map, this.getName())).forEach((ct) -> {
            srcCountryNames.add(ct.getName());
        });
        
        final int unavailbleSrcCountryCount = RiskUtils.getPlayerCountries(map, this.getName()).size() - srcCountryNames.size();
        /**
        * Map potential targets to player-owned countries
        */
        for (String sourceName : srcCountryNames) {
            Country source = COUNTRIES_BY_NAME.get(sourceName);
            for (Country target : source.getNeighbors()) {
                if (!map.getCountryOwner(target).equals(getName())) {
                   destTargetSourceMap.putIfAbsent(target.getName(), new ArrayList<String>());
                   destTargetSourceMap.get(target.getName()).add(sourceName);
                   Collections.sort(destTargetSourceMap.get(target.getName()));
                   if(!tgtCountryNames.contains(target.getName())){
                       tgtCountryNames.add(target.getName());
                   }
                }
            }
        }
        Collections.sort(srcCountryNames);
        Collections.sort(tgtCountryNames);
        AtomicReference<Button> selectedSourceBtn = new AtomicReference<>(null), selectedTargetBtn = new AtomicReference<>(null);
        do {
            this.keepRunning = false;

            Platform.runLater(new Runnable() {
                @Override
                public void run() {

                    /**
                     * *********
                     * Begin mandatory processing on FX thread. (Required for
                     * Stage objects.)
                     */
                    ScrollPane superSPane = new ScrollPane();
                    final Stage dialog = new Stage();
                    final VBox layout = getVBoxFormattedForLayout();

                    final Text guideText = new Text();
                    final Text statusText = new Text();

                    final VBox sourceCountriesVBox = new VBox(10);
                    final VBox targetCountriesVBox = new VBox(10);
                    final VBox sourceCTopVBox = new VBox(10);
                    final VBox targetCTopVBox = new VBox(10);

                    final Text diceCountStatus = new Text("Dice Count:\n- - -");
                    final Button diceCountDec = new Button("Dice--");
                    repeatFireOnLongPress(diceCountDec);
                    final Button diceCountInc = new Button("Dice++");
                    repeatFireOnLongPress(diceCountInc);
                    final HBox diceDisplay = new HBox(10);

                    final HBox bothCountryGroups = new HBox(10);

                    final Circle selectionIndicSrc = new Circle(4);
                    selectionIndicSrc.setFill(Color.DARKRED);
                    final Circle selectionIndicTgt = new Circle(4);
                    selectionIndicTgt.setFill(Color.DARKRED);

                    final Button acceptIt = new Button("Accept/OK");
                    fireButtonAfter3SHover(acceptIt);
                    acceptIt.setDisable(true);
                    final Button skipIt = new Button("[skip/pass]");
                    fireButtonAfter3SHover(skipIt);
                    
                    final Button clearSelection = new Button("Undo/Clear Selection");
                    fireButtonAfter3SHover(clearSelection);

                    final HBox acceptanceBtns = new HBox(10);
                    final Text buttonDivider = new Text("***********");

                    //now that things have been placed in memory, let's set it all up...
                    dialog.setTitle("Attack? [optional]");
                    if (FXUIPlayer.owner != null) {
                        dialog.initOwner(FXUIPlayer.owner);
                    }

                    putWindowAtLastKnownLocation(dialog);

                    //Generic instructions for attacking (the act of which is always optional, technically)
                    //guideText.setText("Select the country from which you want to attack [left],\nthen select the target of your attack [right].\n[attacking is optional; you may pass]");
                    guideText.setText("Attack? [optional]");
                    guideText.setTextAlignment(TextAlignment.CENTER);

                    //status text: the target of the attack (name of country, when set), and the source of the attacks (name of country, when set)
                    final String baseStatusTextString = "Currently attacking\n%s\nfrom\n%s";
                    final String baseTgtHeaderString = "attack\n%s";
                    final String baseSrcHeaderString = "from\n%s";
                    statusText.setText(String.format(baseStatusTextString, blankText, blankText));
                    statusText.setTextAlignment(TextAlignment.CENTER);

                    sourceCountriesVBox.setAlignment(Pos.CENTER);
                    sourceCountriesVBox.setFillWidth(true);
                    targetCountriesVBox.setAlignment(Pos.CENTER);
                    targetCountriesVBox.setFillWidth(true);
                    
                    sourceCTopVBox.setAlignment(Pos.CENTER);
                    sourceCTopVBox.setFillWidth(true);
                    targetCTopVBox.setAlignment(Pos.CENTER);
                    targetCTopVBox.setFillWidth(true);
                    final Text sourceHeader = new Text(String.format(baseSrcHeaderString, blankText)),
                            targetHeader = new Text(String.format(baseTgtHeaderString, blankText));
                    sourceHeader.setTextAlignment(TextAlignment.CENTER);
                    targetHeader.setTextAlignment(TextAlignment.CENTER);
                    sourceCTopVBox.getChildren().add(sourceHeader);
                    targetCTopVBox.getChildren().add(targetHeader);
                    

                    //pre-setup for dice selection -- position in the dialog box, and disable buttons (you can't immediately change the dice count)
                    diceCountStatus.setTextAlignment(TextAlignment.CENTER);
                    diceCountInc.setDisable(true);
                    diceCountDec.setDisable(true);
                    diceDisplay.getChildren().addAll(diceCountDec, diceCountStatus, diceCountInc);
                    diceDisplay.setAlignment(Pos.CENTER);
                    //the actions for the increment and decrement buttons, when buttons are available
                    diceCountInc.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent t) {
                            if (rsp.getNumDice() < maxAtkDiceAvailable) {
                                rsp.setNumDice(rsp.getNumDice() + 1);
                                updateDiceDisplay(diceCountStatus, rsp.getNumDice(), maxAtkDiceAvailable, diceCountDec, diceCountInc);
                            }
                        }
                    });

                    diceCountDec.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent t) {
                            if (rsp.getNumDice() > 1) {
                                rsp.setNumDice(rsp.getNumDice() - 1);
                                updateDiceDisplay(diceCountStatus, rsp.getNumDice(), maxAtkDiceAvailable, diceCountDec, diceCountInc);
                            }
                        }
                    });

                    //TODO remove this reference info
                    //Place containing coordinates, and destination for controls:
                    //FXUIPlayer.paneToAddControls = paneForControls;
                    //FXUIPlayer.textNodeMap = controlTextLoc;
                    //FXUIPlayer.controlsInMainWindow = true;
                    
                    /* Buttons representing potential conquests (target countries) & home (attacking) countries.
                    * Once a potential target's button is pressed, reveals all possible attacking countries.
                    * Targets always display, so no buffer.
                    * Source contries vary, so are cached and displayed dynamically. */
                    for (String targetName : tgtCountryNames) {
                        Country target = COUNTRIES_BY_NAME.get(targetName);
                        if (!map.getCountryOwner(target).equals(getName())){
                            final Button ctTgtBtn = new Button("tgt: " + targetName);
                            ctTgtBtn.setLayoutX(FXUIPlayer.textNodeMap.get(targetName).getX() + mainWindowPosOffset);
                            ctTgtBtn.setLayoutY(FXUIPlayer.textNodeMap.get(targetName).getY() + mainWindowPosOffset);
                            ctTgtBtn.setOnAction(new EventHandler<ActionEvent>() {
                                @Override
                                public void handle(ActionEvent t) {
                                    if (selectedTargetBtn.get() != null) {
                                        selectedTargetBtn.get().setGraphic(null);
                                    }
                                    acceptIt.setDisable(true);
                                    selectedTargetBtn.set(ctTgtBtn);
                                    ctTgtBtn.setGraphic(selectionIndicTgt);
                                    rsp.setDfdCountry(target);
                                    attackTarget.set(targetName);
                                    attackSource.set(blankText);
                                    targetHeader.setText(String.format(baseTgtHeaderString, targetName));
                                    sourceHeader.setText(String.format(baseSrcHeaderString, blankText));
                                    statusText.setText(String.format(baseStatusTextString, attackTarget.get(), attackSource.get()));
                                    statusText.setFill(Color.BLACK);
                                    for(Entry<String, Button> sourceToDisable : sourceBtnCache.entrySet()){
                                        sourceToDisable.getValue().setDisable(true);
                                    }
                                    for(Entry<String, Button> othertgt : targetBtnCache.entrySet()){
                                        if(othertgt.getKey() != targetName){
                                            othertgt.getValue().setDisable(true);
                                        }
                                    }
                                    for(String sourceName : destTargetSourceMap.get(targetName)/*.stream().sorted().collect(Collectors.toList())*/){
                                        sourceBtnCache.get(sourceName).setDisable(false);
                                    }
                                }
                            });
                            //finally add this target button for this singular country
                            //targetCountriesVBox.getChildren().add(ctTgtBtn);
                            targetBtnCache.put(targetName, ctTgtBtn);
                        }
                    }
                    for (String sourceName : srcCountryNames) {
                        Country source = COUNTRIES_BY_NAME.get(sourceName);
                        final Button ctSrcBtn = new Button("src: " + sourceName);
                        ctSrcBtn.setLayoutX(FXUIPlayer.textNodeMap.get(sourceName).getX() + mainWindowPosOffset);
                        ctSrcBtn.setLayoutY(FXUIPlayer.textNodeMap.get(sourceName).getY() + mainWindowPosOffset);
                        if(map.getCountryArmies(source) < 2){
                            ctSrcBtn.setDisable(true);
                        }
                        else{
                            ctSrcBtn.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent t) {
                                if (selectedSourceBtn.get() != null) {
                                    selectedSourceBtn.get().setGraphic(null);
                                }
                                sourceHeader.setText(String.format(baseSrcHeaderString, sourceName));
                                acceptIt.setDisable(false);
                                selectedSourceBtn.set(ctSrcBtn);
                                ctSrcBtn.setGraphic(selectionIndicSrc);
                                rsp.setAtkCountry(source);
                                attackSource.set(sourceName);
                                maxAtkDiceAvailable = map.getCountryArmies(rsp.getAtkCountry()) > RiskConstants.MAX_ATK_DICE ? RiskConstants.MAX_ATK_DICE : map.getCountryArmies(rsp.getAtkCountry()) - 1;
                                rsp.setNumDice(maxAtkDiceAvailable); //default to the max dice available for an attack
                                updateDiceDisplay(diceCountStatus, rsp.getNumDice(), maxAtkDiceAvailable, diceCountDec, diceCountInc);
                                statusText.setText(String.format(baseStatusTextString, attackTarget, attackSource));
                            }
                            });
                            ctSrcBtn.setDisable(true);
                            sourceBtnCache.put(sourceName, ctSrcBtn);
                        }
                    }
                    FXUIPlayer.paneToAddControls.getChildren().addAll(sourceBtnCache.values());
                    FXUIPlayer.paneToAddControls.getChildren().addAll(targetBtnCache.values());
                    
                    
                    
                    //button to attempt to accept final reinforcement allocation
                    acceptIt.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent t) {
                            if (rsp.getAtkCountry() != null && rsp.getDfdCountry() != null) {
                                if (!AttackResponse.isValidResponse(rsp, map, getName())) {
                                    statusText.setText("Not a valid response; try another combo.");
                                    statusText.setFill(Color.RED);
                                } else {
                                    passTurn.set(false);
                                    exitDecider.setAsNonSystemClose();
                                    saveLastKnownWindowLocation(dialog);
                                    dialog.close();
                                }
                            } else {
                                statusText.setText("Not a valid response; \nmake sure you select a target and source!!");
                                statusText.setFill(Color.RED);
                            }
                        }
                    });

                    //if you want to pass on this action for this turn...
                    skipIt.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent t) {
                            passTurn.set(true);
                            exitDecider.setAsNonSystemClose();
                            saveLastKnownWindowLocation(dialog);
                            dialog.close();
                        }
                    });
                    
                    final double baseScaleAmount = 0.7d;
                    Stream.concat(targetBtnCache.values().stream(), sourceBtnCache.values().stream())
                        .forEach((btnToEdit) -> {
                        btnToEdit.setScaleX(baseScaleAmount);
                        btnToEdit.setScaleY(baseScaleAmount);
                        btnToEdit.hoverProperty().addListener(new ChangeListener<Boolean>() {
                            @Override
                            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
                                    Boolean newValue) {
                                final double scaleAmount = newValue.booleanValue() ? 1.0 : baseScaleAmount;
                                btnToEdit.setScaleX(scaleAmount);
                                btnToEdit.setScaleY(scaleAmount);
                                btnToEdit.toFront();
                                //TODO validate shifting values; the threshold may be too low
                                //or too high, and/or may cause the boxes to shift too much.
                                if(newValue && (btnToEdit.getLayoutX() < 100 || btnToEdit.getLayoutX() > FXUIGameMaster.DEFAULT_CONTENT_WIDTH - 170)){
                                    btnToEdit.setTranslateX(btnToEdit.getLayoutX() < 100 ? 50 : -50);
                                }
                                else{
                                    btnToEdit.setTranslateX(0);
                                }

                            }
                        });
                    });
                    
                    clearSelection.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent t) {
                            for(Button singlesrc : sourceBtnCache.values()){
                                singlesrc.setDisable(true);
                            }
                            for(Button singletgt : targetBtnCache.values()){
                                singletgt.setDisable(false);
                            }
                            rsp.setDfdCountry(null);
                            rsp.setAtkCountry(null);
                            attackSource.set(blankText);
                            attackTarget.set(blankText);
                            if (selectedTargetBtn.get() != null) {
                                selectedTargetBtn.get().setGraphic(null);
                            }
                            if (selectedSourceBtn.get() != null) {
                                selectedSourceBtn.get().setGraphic(null);
                            }
                            statusText.setText(String.format(baseStatusTextString, attackTarget, attackSource));
                            targetHeader.setText(String.format(baseTgtHeaderString, blankText));
                            sourceHeader.setText(String.format(baseSrcHeaderString, blankText));
                        }
                    });

                    //finish setting up rest of layout...
                    //includes double ScrollPane -- one for leftmost (source) contents, one for for rightmost (destination) contents

                    bothCountryGroups.getChildren().addAll(targetCTopVBox, sourceCTopVBox);
                    bothCountryGroups.setAlignment(Pos.CENTER);

                    acceptanceBtns.getChildren().addAll(acceptIt, skipIt);
                    acceptanceBtns.setAlignment(Pos.CENTER);

                    //add status and buttons to layout
                    buttonDivider.setTextAlignment(TextAlignment.CENTER);
                    layout.getChildren().addAll(guideText, /*statusText,*/ clearSelection, /*bothCountryGroups,*/ buttonDivider, diceDisplay, acceptanceBtns);
                    layout.setAlignment(Pos.CENTER);

                    //formally add linear layout to scene through the use of a scroll pane, and display the dialog
                    superSPane.setContent(layout);
                    dialog.setScene(new Scene(superSPane));
                    FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
                    FXUIPlayer.crossbar.setCurrentHumanName(getName());

                    registerNodeForBrightnessControl(dialog, superSPane);
                    registerSceneForEyeStrainControl(dialog, dialog.getScene());
                    dialog.show();

                    formatSPaneInlayAfterShow(superSPane);
                }
            });

            /**
             * End mandatory FX thread processing. Immediately after this, pause
             * the non-UI thread (which you should be back on) and wait for the
             * dialog to close!
             */
            waitForDialogToClose(FXUIPlayer.crossbar);
            checkIfCloseMeansMore(exitDecider, FXUIPlayer.crossbar);
            //if we have completed all business within the dialog, cleanup and return as required.
            FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
            attackSource.set(blankText);
            attackTarget.set(blankText);
        } while (this.keepRunning);
        Platform.runLater(()->{
            FXUIPlayer.paneToAddControls.getChildren().removeAll(sourceBtnCache.values());
            FXUIPlayer.paneToAddControls.getChildren().removeAll(targetBtnCache.values());
        });
        if (passTurn.get()) {
            return null;
        }
        return rsp;
    }

    /**
     * Specify how and where an attack should be mounted. RESPONSE OPTIONAL
     *
     * @param map
     * @param myCards
     * @param playerCards
     * @return attack choice
     */
    public AttackResponse attack(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards) {
        //if the player asked to end the game, don't even display the dialog
        if(FXUIPlayer.controlsInMainWindow){
            return attackFlat(map, myCards, playerCards);
        }
        if (crossbar.isHumanEndingGame()) {
            return null;
        }
        //else...make the window and keep displaying until the user has confirmed selection
        final AttackResponse rsp = new AttackResponse();
        final AtomicBoolean passTurn = new AtomicBoolean(false);
        final String blankText = "-----";
        final AtomicReference<String> attackTarget = new AtomicReference<>(blankText);
        final AtomicReference<String> attackSource = new AtomicReference<>(blankText);
        //Collection<Country> sources = RiskUtils.getPossibleSourceCountries(map, RiskUtils.getPlayerCountries(map, this.getName()));

        //HashMap<String, Button> potentialTargets = new HashMap<>();
        HashMap<String, Button> sourceBtnCache = new HashMap<>();
        HashMap<String, ArrayList<String>> destTargetSourceMap = new HashMap<>();
        ArrayList<String> srcCountryNames = new ArrayList<>();
        ArrayList<String> tgtCountryNames = new ArrayList<>();
        RiskUtils.getPossibleSourceCountries(map, RiskUtils.getPlayerCountries(map, this.getName())).forEach((ct) -> {
            srcCountryNames.add(ct.getName());
        });
        
        final int unavailbleSrcCountryCount = RiskUtils.getPlayerCountries(map, this.getName()).size() - srcCountryNames.size();
        /**
        * Map potential targets to player-owned countries
        */
        for (String sourceName : srcCountryNames) {
            Country source = COUNTRIES_BY_NAME.get(sourceName);
            for (Country target : source.getNeighbors()) {
                if (!map.getCountryOwner(target).equals(getName())) {
                   destTargetSourceMap.putIfAbsent(target.getName(), new ArrayList<String>());
                   destTargetSourceMap.get(target.getName()).add(sourceName);
                   Collections.sort(destTargetSourceMap.get(target.getName()));
                   if(!tgtCountryNames.contains(target.getName())){
                       tgtCountryNames.add(target.getName());
                   }
                }
            }
        }
        Collections.sort(srcCountryNames);
        Collections.sort(tgtCountryNames);
        AtomicReference<Button> selectedSourceBtn = new AtomicReference<>(null), selectedTargetBtn = new AtomicReference<>(null);
        do {
            this.keepRunning = false;

            Platform.runLater(new Runnable() {
                @Override
                public void run() {

                    /**
                     * *********
                     * Begin mandatory processing on FX thread. (Required for
                     * Stage objects.)
                     */
                    ScrollPane superSPane = new ScrollPane();
                    final Stage dialog = new Stage();
                    final VBox layout = getVBoxFormattedForLayout();

                    final Text guideText = new Text();
                    final Text statusText = new Text();

                    final VBox sourceCountriesVBox = new VBox(10);
                    final VBox targetCountriesVBox = new VBox(10);
                    final VBox sourceCTopVBox = new VBox(10);
                    final VBox targetCTopVBox = new VBox(10);

                    final Text diceCountStatus = new Text("Dice Count:\n- - -");
                    final Button diceCountDec = new Button("Dice--");
                    repeatFireOnLongPress(diceCountDec);
                    final Button diceCountInc = new Button("Dice++");
                    repeatFireOnLongPress(diceCountInc);
                    final HBox diceDisplay = new HBox(10);

                    final ScrollPane spaneSrc = new ScrollPane();
                    final ScrollPane spaneTgt = new ScrollPane();
                    final HBox bothCountryGroups = new HBox(10);

                    final Circle selectionIndicSrc = new Circle(4);
                    selectionIndicSrc.setFill(Color.DARKRED);
                    final Circle selectionIndicTgt = new Circle(4);
                    selectionIndicTgt.setFill(Color.DARKRED);

                    final Button acceptIt = new Button("Accept/OK");
                    fireButtonAfter3SHover(acceptIt);
                    acceptIt.setDisable(true);
                    final Button skipIt = new Button("[skip/pass]");
                    fireButtonAfter3SHover(skipIt);

                    final HBox acceptanceBtns = new HBox(10);
                    final Text buttonDivider = new Text("***********");

                    //now that things have been placed in memory, let's set it all up...
                    dialog.setTitle("Attack? [optional]");
                    if (FXUIPlayer.owner != null) {
                        dialog.initOwner(FXUIPlayer.owner);
                    }

                    putWindowAtLastKnownLocation(dialog);

                    //Generic instructions for attacking (the act of which is always optional, technically)
                    //guideText.setText("Select the country from which you want to attack [left],\nthen select the target of your attack [right].\n[attacking is optional; you may pass]");
                    guideText.setText("Attack? [optional]");
                    guideText.setTextAlignment(TextAlignment.CENTER);

                    //status text: the target of the attack (name of country, when set), and the source of the attacks (name of country, when set)
                    final String baseStatusTextString = "Currently attacking\n%s\nfrom\n%s";
                    final String baseTgtHeaderString = "attack\n%s";
                    final String baseSrcHeaderString = "from\n%s";
                    statusText.setText(String.format(baseStatusTextString, blankText, blankText));
                    statusText.setTextAlignment(TextAlignment.CENTER);

                    sourceCountriesVBox.setAlignment(Pos.CENTER);
                    sourceCountriesVBox.setFillWidth(true);
                    targetCountriesVBox.setAlignment(Pos.CENTER);
                    targetCountriesVBox.setFillWidth(true);
                    
                    sourceCTopVBox.setAlignment(Pos.CENTER);
                    sourceCTopVBox.setFillWidth(true);
                    targetCTopVBox.setAlignment(Pos.CENTER);
                    targetCTopVBox.setFillWidth(true);
                    final Text sourceHeader = new Text(String.format(baseSrcHeaderString, blankText)),
                            targetHeader = new Text(String.format(baseTgtHeaderString, blankText));
                    sourceHeader.setTextAlignment(TextAlignment.CENTER);
                    targetHeader.setTextAlignment(TextAlignment.CENTER);
                    sourceCTopVBox.getChildren().add(sourceHeader);
                    targetCTopVBox.getChildren().add(targetHeader);
                    

                    //pre-setup for dice selection -- position in the dialog box, and disable buttons (you can't immediately change the dice count)
                    diceCountStatus.setTextAlignment(TextAlignment.CENTER);
                    diceCountInc.setDisable(true);
                    diceCountDec.setDisable(true);
                    diceDisplay.getChildren().addAll(diceCountDec, diceCountStatus, diceCountInc);
                    diceDisplay.setAlignment(Pos.CENTER);
                    //the actions for the increment and decrement buttons, when buttons are available
                    diceCountInc.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent t) {
                            if (rsp.getNumDice() < maxAtkDiceAvailable) {
                                rsp.setNumDice(rsp.getNumDice() + 1);
                                updateDiceDisplay(diceCountStatus, rsp.getNumDice(), maxAtkDiceAvailable, diceCountDec, diceCountInc);
                            }
                        }
                    });

                    diceCountDec.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent t) {
                            if (rsp.getNumDice() > 1) {
                                rsp.setNumDice(rsp.getNumDice() - 1);
                                updateDiceDisplay(diceCountStatus, rsp.getNumDice(), maxAtkDiceAvailable, diceCountDec, diceCountInc);
                            }
                        }
                    });

                    /* Buttons representing potential conquests (target countries) & home (attacking) countries.
                    * Once a potential target's button is pressed, reveals all possible attacking countries.
                    * Targets always display, so no buffer.
                    * Source contries vary, so are cached and displayed dynamically. */
                    for (String targetName : tgtCountryNames) {
                        Country target = COUNTRIES_BY_NAME.get(targetName);
                        if (!map.getCountryOwner(target).equals(getName())){
                            final Button ctTgtBtn = new Button(targetName);
                            ctTgtBtn.setOnAction(new EventHandler<ActionEvent>() {
                                @Override
                                public void handle(ActionEvent t) {
                                    if (selectedTargetBtn.get() != null) {
                                        selectedTargetBtn.get().setGraphic(null);
                                    }
                                    acceptIt.setDisable(true);
                                    selectedTargetBtn.set(ctTgtBtn);
                                    ctTgtBtn.setGraphic(selectionIndicTgt);
                                    rsp.setDfdCountry(target);
                                    attackTarget.set(targetName);
                                    attackSource.set(blankText);
                                    targetHeader.setText(String.format(baseTgtHeaderString, targetName));
                                    sourceHeader.setText(String.format(baseSrcHeaderString, blankText));
                                    statusText.setText(String.format(baseStatusTextString, attackTarget.get(), attackSource.get()));
                                    statusText.setFill(Color.BLACK);
                                    sourceCountriesVBox.getChildren().clear();
                                    for(String sourceName : destTargetSourceMap.get(targetName)/*.stream().sorted().collect(Collectors.toList())*/){
                                        sourceCountriesVBox.getChildren().add(sourceBtnCache.get(sourceName));
                                    }
                                }
                            });
                            //finally add this target button for this singular country
                            targetCountriesVBox.getChildren().add(ctTgtBtn);
                        }
                    }
                    for (String sourceName : srcCountryNames) {
                        Country source = COUNTRIES_BY_NAME.get(sourceName);
                        final Button ctSrcBtn = new Button(sourceName);
                        if(map.getCountryArmies(source) < 2){
                            ctSrcBtn.setDisable(true);
                        }
                        else{
                            ctSrcBtn.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent t) {
                                if (selectedSourceBtn.get() != null) {
                                    selectedSourceBtn.get().setGraphic(null);
                                }
                                sourceHeader.setText(String.format(baseSrcHeaderString, sourceName));
                                acceptIt.setDisable(false);
                                selectedSourceBtn.set(ctSrcBtn);
                                ctSrcBtn.setGraphic(selectionIndicSrc);
                                rsp.setAtkCountry(source);
                                attackSource.set(sourceName);
                                maxAtkDiceAvailable = map.getCountryArmies(rsp.getAtkCountry()) > RiskConstants.MAX_ATK_DICE ? RiskConstants.MAX_ATK_DICE : map.getCountryArmies(rsp.getAtkCountry()) - 1;
                                rsp.setNumDice(maxAtkDiceAvailable); //default to the max dice available for an attack
                                updateDiceDisplay(diceCountStatus, rsp.getNumDice(), maxAtkDiceAvailable, diceCountDec, diceCountInc);
                                statusText.setText(String.format(baseStatusTextString, attackTarget, attackSource));
                            }
                        });
                        }
                        sourceBtnCache.put(sourceName, ctSrcBtn);
                    }
                    ///////////////////////////////////
                    /*
                    for (String sourceName : countryNames) {
                        Country source = COUNTRIES_BY_NAME.get(sourceName);
                        final Button ctSrcBtn = new Button(source.getName());
                        ctSrcBtn.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent t) {
                                if (selectedSourceBtn.get() != null) {
                                    selectedSourceBtn.get().setGraphic(null);
                                }
                                selectedSourceBtn.set(ctSrcBtn);
                                ctSrcBtn.setGraphic(selectionIndicSrc);
                                rsp.setAtkCountry(source);
                                maxAtkDiceAvailable = map.getCountryArmies(rsp.getAtkCountry()) > RiskConstants.MAX_ATK_DICE ? RiskConstants.MAX_ATK_DICE : map.getCountryArmies(rsp.getAtkCountry()) - 1;
                                rsp.setNumDice(maxAtkDiceAvailable); //default to the max dice available for an attack
                                updateDiceDisplay(diceCountStatus, rsp.getNumDice(), maxAtkDiceAvailable, diceCountDec, diceCountInc);
                                attackSource.set(sourceName);
                                attackTarget.set(blankText);
                                statusText.setText(String.format(baseStatusTextString, attackTarget.get(), attackSource.get()));
                                statusText.setFill(Color.BLACK);
                                targetCountriesVBox.getChildren().clear();
                                for (Country target : source.getNeighbors()) {
                                    if (!map.getCountryOwner(target).equals(getName())) {
                                        final Button ctTgtBtn = new Button(target.getName());
                                        ctTgtBtn.setOnAction(new EventHandler<ActionEvent>() {
                                            @Override
                                            public void handle(ActionEvent t) {
                                                if (selectedTargetBtn.get() != null) {
                                                    selectedTargetBtn.get().setGraphic(null);
                                                }
                                                selectedTargetBtn.set(ctTgtBtn);
                                                ctTgtBtn.setGraphic(selectionIndicTgt);
                                                rsp.setDfdCountry(target);
                                                attackTarget.set(target.getName());
                                                statusText.setText(String.format(baseStatusTextString, attackTarget, attackSource));
                                            }
                                        });
                                        targetCountriesVBox.getChildren().add(ctTgtBtn);
                                    }
                                }
                            }
                        });
                        //finally add this source button for this singular country
                        if(map.getCountryArmies(source) < 2){
                            ctSrcBtn.setDisable(true);
                        }
                        sourceCountriesVBox.getChildren().add(ctSrcBtn);
                    }
                    */

                    //button to attempt to accept final reinforcement allocation
                    acceptIt.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent t) {
                            if (rsp.getAtkCountry() != null && rsp.getDfdCountry() != null) {
                                if (!AttackResponse.isValidResponse(rsp, map, getName())) {
                                    statusText.setText("Not a valid response; try another combo.");
                                    statusText.setFill(Color.RED);
                                } else {
                                    passTurn.set(false);
                                    exitDecider.setAsNonSystemClose();
                                    saveLastKnownWindowLocation(dialog);
                                    dialog.close();
                                }
                            } else {
                                statusText.setText("Not a valid response; \nmake sure you select a target and source!!");
                                statusText.setFill(Color.RED);
                            }
                        }
                    });

                    //if you want to pass on this action for this turn...
                    skipIt.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent t) {
                            passTurn.set(true);
                            exitDecider.setAsNonSystemClose();
                            saveLastKnownWindowLocation(dialog);
                            dialog.close();
                        }
                    });

                    //finish setting up rest of layout...
                    //includes double ScrollPane -- one for leftmost (source) contents, one for for rightmost (destination) contents
                    spaneSrc.setPrefHeight(400);
                    spaneSrc.setPrefWidth(200);
                    spaneTgt.setPrefHeight(400);
                    spaneTgt.setPrefWidth(200);
                    spaneSrc.setFitToHeight(true);
                    spaneSrc.setFitToWidth(true);
                    spaneTgt.setFitToHeight(true);
                    spaneTgt.setFitToWidth(true);

                    spaneSrc.setContent(sourceCountriesVBox);
                    spaneTgt.setContent(targetCountriesVBox);
                    
                    sourceCTopVBox.getChildren().add(spaneSrc);
                    targetCTopVBox.getChildren().add(spaneTgt);

                    bothCountryGroups.getChildren().addAll(targetCTopVBox, sourceCTopVBox);
                    bothCountryGroups.setAlignment(Pos.CENTER);

                    acceptanceBtns.getChildren().addAll(acceptIt, skipIt);
                    acceptanceBtns.setAlignment(Pos.CENTER);

                    //add status and buttons to layout
                    buttonDivider.setTextAlignment(TextAlignment.CENTER);
                    layout.getChildren().addAll(guideText, /*statusText,*/ bothCountryGroups, buttonDivider, diceDisplay, acceptanceBtns);
                    layout.setAlignment(Pos.CENTER);

                    //formally add linear layout to scene through the use of a scroll pane, and display the dialog
                    superSPane.setContent(layout);
                    dialog.setScene(new Scene(superSPane));
                    FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
                    FXUIPlayer.crossbar.setCurrentHumanName(getName());

                    registerNodeForBrightnessControl(dialog, superSPane);
                    registerSceneForEyeStrainControl(dialog, dialog.getScene());
                    dialog.show();

                    formatSPaneInlayAfterShow(superSPane);
                    formatSPaneInlayAfterShow(spaneSrc);
                    formatSPaneInlayAfterShow(spaneTgt);
                }
            });

            /**
             * End mandatory FX thread processing. Immediately after this, pause
             * the non-UI thread (which you should be back on) and wait for the
             * dialog to close!
             */
            waitForDialogToClose(FXUIPlayer.crossbar);
            checkIfCloseMeansMore(exitDecider, FXUIPlayer.crossbar);
            //if we have completed all business within the dialog, cleanup and return as required.
            FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
            attackSource.set(blankText);
            attackTarget.set(blankText);
        } while (this.keepRunning);
        if (passTurn.get()) {
            return null;
        }
        return rsp;
    }

    /**
     * Used with the attack dialog to update the status of the dice -- count
     * used, available buttons, etc
     *
     * @param diceStatusDisplay the Text field indicating the status of the dice
     * @param currentDiceCount the current number of dice to be applied/rolled
     * when the attack commences
     * @param maxDiceCount the max number of dice available to roll given
     * available troops
     * @param decBtn the button to decrement the dice count (disabled under
     * select circumstances, re-enabled otherwise)
     * @param incBtn the button to increment the dice count (disabled under
     * select circumstances, re-enabled otherwise)
     */
    private void updateDiceDisplay(Text diceStatusDisplay, int currentDiceCount, int maxDiceCount, Button decBtn, Button incBtn) {
        final String dieOrDice = currentDiceCount == 1 ? "die" : "dice";
        diceStatusDisplay.setText("Rolling " + currentDiceCount + " "
                + dieOrDice + ".\n(" + maxDiceCount + " allowed)");
        decBtn.setDisable(false);
        if (maxDiceCount > currentDiceCount) {
            incBtn.setDisable(false);
        } else {
            incBtn.setDisable(true);
        }
        if (currentDiceCount == 1) {
            decBtn.setDisable(true);
        } else {
            decBtn.setDisable(false);
        }
    }

    /**
     * Specify the number of armies that should be advanced into a conquered
     * territory. RESPONSE REQUIRED
     *
     * @param map
     * @param myCards
     * @param playerCards
     * @param fromCountry
     * @param toCountry
     * @param minAdv
     * @return advance response containing the number of armies being added to
     * the new conquest.
     */
    public AdvanceResponse advance(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, Country fromCountry, Country toCountry, int minAdv) {
        //if the player asked to end the game, don't even display the dialog
        if (crossbar.isHumanEndingGame()) {
            return null;
        }

        //else...make the window and keep displaying until the user has confirmed selection
        final AdvanceResponse rsp = new AdvanceResponse(minAdv);
        do {
            this.keepRunning = false;
            final int sourceArmies = map.getCountryArmies(fromCountry);
            Platform.runLater(new Runnable() {
                @Override
                public void run() {

                    /**
                     * *********
                     * Begin mandatory processing on FX thread. (Required for
                     * Stage objects.)
                     */
                    final Stage dialog = new Stage();
                    final VBox layout = getVBoxFormattedForLayout();

                    final Text sourceCount = new Text();
                    final Text destCount = new Text();
                    final HBox countryCounts = new HBox(24);

                    final Button plusle = new Button("Add/+");
                    repeatFireOnLongPress(plusle);
                    final Button minun = new Button("Recall/-");
                    repeatFireOnLongPress(minun);
                    final HBox allocationButtons = new HBox(4);

                    final Button acceptance = new Button("Submit/OK");
                    fireButtonAfter3SHover(acceptance);
                    final Text acceptanceStatus = new Text("Minimum to advance: " + minAdv);

                    final Text briefInstructions = new Text("Advance some armies "
                            + "\ninto your new conquest!");

                    dialog.setTitle("Advance!");
                    if (FXUIPlayer.owner != null) {
                        dialog.initOwner(FXUIPlayer.owner);
                    }

                    putWindowAtLastKnownLocation(dialog);
                    sourceCount.setTextAlignment(TextAlignment.CENTER);
                    destCount.setTextAlignment(TextAlignment.CENTER);
                    acceptanceStatus.setTextAlignment(TextAlignment.CENTER);

                    final class UpdateStatus {

                        boolean doubleCheck = false;

                        UpdateStatus() {
                        }

                        private String troopOrTroops(int troopCount) {
                            return (troopCount == 1 ? "troop" : "troops");
                        }

                        public void refreshStatus() {
                            int srcCt = (sourceArmies - rsp.getNumArmies());
                            int dstCt = rsp.getNumArmies();
                            sourceCount.setText("Leave\n" + srcCt + "\n"
                                    + troopOrTroops(srcCt) + "\n"
                                    + "in\n" + fromCountry.getName()
                                    + "\n:::::\n");
                            destCount.setText("Advance\n" + dstCt + "\n"
                                    + troopOrTroops(dstCt) + "\n"
                                    + "into\n" + toCountry.getName()
                                    + "\n:::::\n");
                            doubleCheck = false;
                        }

                        public void resetAcceptance() {
                            acceptanceStatus.setText("Minimum to advance: " + minAdv);
                            doubleCheck = false;
                        }

                        public boolean verifyAcceptance() {
                            if (sourceArmies - rsp.getNumArmies() != 0 && rsp.getNumArmies() != 0) {
                                return true;
                            } else if (!doubleCheck) {
                                acceptanceStatus.setText("You cannot leave 0 army members in " + (rsp.getNumArmies() == 0 ? toCountry.getName() : fromCountry.getName()) + "?");
                                return false;
                            } else {

                                return true;
                            }
                        }
                    }

                    countryCounts.setAlignment(Pos.CENTER);

                    countryCounts.getChildren().addAll(sourceCount, destCount);

                    final UpdateStatus updater = new UpdateStatus();
                    updater.refreshStatus();

                    plusle.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            updater.resetAcceptance();
                            /*if we make sure that we're advancing the minimum 
                             * number of troops, AND make sure that we leave
                             * at least one army member in the source country,
                             * then we can add another army person.*/
                            if (rsp.getNumArmies() + 1 < sourceArmies) {
                                rsp.setNumArmies(rsp.getNumArmies() + 1);
                                updater.resetAcceptance();
                                updater.refreshStatus();
                            }
                        }
                    });
                    minun.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            updater.resetAcceptance();
                            /*if we make sure that we're advancing the minimum 
                             * number of troops, AND make sure that we leave
                             * at least one army member in the source country,
                             * then we are fine.*/
                            if (rsp.getNumArmies() > minAdv) {
                                updater.resetAcceptance();
                                rsp.setNumArmies(rsp.getNumArmies() - 1);
                                updater.refreshStatus();
                            }
                        }
                    });

                    acceptance.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            //we can't verify it with the official function, 
                            //but we can check if we've actually put our soldiers somewhere
                            //and if we so decide, it's possible to just skip proper allocation (wherein either src or dst has 0 troops
                            if (updater.verifyAcceptance()) {
                                exitDecider.setAsNonSystemClose();
                                saveLastKnownWindowLocation(dialog);
                                dialog.close();
                            }
                        }
                    });

                    allocationButtons.setAlignment(Pos.CENTER);
                    allocationButtons.getChildren().addAll(minun, plusle);
                    
                    layout.getChildren().setAll(
                            briefInstructions, countryCounts, allocationButtons,
                            acceptanceStatus, acceptance
                    );

                    //finally place the layout into the new dialog window, and display the dialog.
                    dialog.setScene(new Scene(layout));
                    FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
                    FXUIPlayer.crossbar.setCurrentHumanName(getName());

                    registerNodeForBrightnessControl(dialog, layout);
                    registerSceneForEyeStrainControl(dialog, dialog.getScene());
                    dialog.show();
                }
            });

            /**
             * End mandatory FX thread processing. Immediately after this, pause
             * the non-UI thread (which you should be back on) and wait for the
             * dialog to close!
             */
            waitForDialogToClose(FXUIPlayer.crossbar);
            checkIfCloseMeansMore(exitDecider, FXUIPlayer.crossbar);
            FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
        } while (this.keepRunning);
        return rsp;
    }

    /**
     * Propose a fortification transfer. RESPONSE OPTIONAL
     *
     * @param map
     * @param myCards
     * @param playerCards
     * @return fortification choice
     */
    public FortifyResponse fortify(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards) {
        //if the player asked to end the game, don't even display the dialog
        if (crossbar.isHumanEndingGame()) {
            return null;
        }
        //else...make the window and keep displaying until the user has confirmed selection

        final FortifyResponse rsp = new FortifyResponse();
        AtomicBoolean passTurn = new AtomicBoolean(false);

        do {
            this.keepRunning = false;
            Collection<Country> sources = RiskUtils.getPossibleSourceCountries(map, RiskUtils.getPlayerCountries(map, this.name));
            Collection<Set<Country>> allConnectedSets = RiskUtils.getAllConnectedCountrySets(map, this.name);
            Map<Country, Set<Country>> destMap = new HashMap<Country, Set<Country>>();

            //Create mapping of each possible fortification source to all possible destinations
            for (Country source : sources) {
                if (!destMap.containsKey(source)) {
                    boolean found = false;
                    for (Set<Country> connectedSet : allConnectedSets) {
                        for (Country country : connectedSet) {
                            if (source == country) {
                                destMap.put(source, connectedSet);
                                //destMap.get(source).remove(source);
                                found = true;
                                break;
                            }
                        }
                        if (found) {
                            break;
                        }
                    }
                }
            }

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    /**
                     * *********
                     * Begin mandatory processing on FX thread. (Required for
                     * Stage objects.)
                     */

                    ScrollPane superSPane = new ScrollPane();
                    final Stage dialog = new Stage();
                    dialog.setTitle("Fortify? [optional]");
                    if (FXUIPlayer.owner != null) {
                        dialog.initOwner(FXUIPlayer.owner);
                    }

                    putWindowAtLastKnownLocation(dialog);

                    final VBox layout = getVBoxFormattedForLayout();
                    
                    //Set up instructions for fortification & add it to the layout.
                    Text guideText = new Text();
                    guideText.setText("Select the country from which you want to fortify [left],\nthen select the destination for your troops [right].\n[fortification is optional; you can skip this]");
                    guideText.setTextAlignment(TextAlignment.CENTER);
                    layout.getChildren().add(guideText);

                    //status text to be used for indicating the currently selected countries & the appropriate count
                    //by default, shouldn't display any applicable country.
                    //Set-up here doubles as a placeholder function to assist with proper automatic dialog sizing.
                    final Text statusText = new Text();
                    statusText.setText("Current selection: \n[No Selection]\n----\n----\n----");
                    statusText.setTextAlignment(TextAlignment.CENTER);

                    final VBox sourceCountriesVBox = new VBox(10);
                    final VBox targetCountriesVBox = new VBox(10);
                    sourceCountriesVBox.setAlignment(Pos.CENTER);
                    targetCountriesVBox.setAlignment(Pos.CENTER);
                    sourceCountriesVBox.setFillWidth(true);
                    targetCountriesVBox.setFillWidth(true);
                    sourceCountriesVBox.getChildren().add(new Text("Source:"));

                    for (Country source : sources) {
                        final Button ctSrcBtn = new Button(source.getName());
                        repeatFireOnLongPress(ctSrcBtn);
                        //disable the buttons if there's no adjacent countries
                        ctSrcBtn.setDisable(true);
                        for (Country dest : destMap.get(source)) {
                            if (dest != source) {
                                ctSrcBtn.setDisable(false);
                            }
                        }
                        //what to do when the buttons are pressed:
                        //show the country/countries that can receive troops
                        ctSrcBtn.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent event) {
                                statusText.setText("Current selection:\nFortifying\n????\nusing ??? troops from\n" + source.getName() + ".");

                                //reset the target country list and set it up when a given source button is pressed
                                targetCountriesVBox.getChildren().clear();
                                targetCountriesVBox.getChildren().add(new Text("Target:"));
                                rsp.setFromCountry(source);

                                for (Country dest : destMap.get(source)) {
                                    if (dest != source) {
                                        final Button ctTgtBtn = new Button(dest.getName());
                                        repeatFireOnLongPress(ctTgtBtn);
                                        ctTgtBtn.setOnAction(new EventHandler<ActionEvent>() {
                                            @Override
                                            public void handle(ActionEvent t) {
                                                rsp.setToCountry(dest);
                                                statusText.setFill(Color.BLACK);
                                                statusText.setText("Current selection:\nFortifying\n" + dest.getName() + "\nusing ??? troops from\n" + source.getName() + ".");
                                            }//end of actionevent definition
                                        });
                                        targetCountriesVBox.getChildren().add(ctTgtBtn);
                                    }
                                }
                            }
                        });
                        //button to decrement reinforcement count for selected country
                        sourceCountriesVBox.getChildren().add(ctSrcBtn);
                    }

                    //Leftmost ScrollPane is for the source countries, rightmost ScrollPane is for the destination/target countries.
                    ScrollPane spaneSource = new ScrollPane();
                    spaneSource.setPrefHeight(400);
                    spaneSource.setPrefWidth(200);
                    
                    ScrollPane spaneTarget = new ScrollPane();
                    spaneTarget.setPrefHeight(400);
                    spaneTarget.setPrefWidth(200);
                    
                    spaneSource.setFitToHeight(true);
                    spaneSource.setFitToWidth(true);
                    
                    spaneTarget.setFitToHeight(true);
                    spaneTarget.setFitToWidth(true);

                    spaneSource.setContent(sourceCountriesVBox);
                    spaneTarget.setContent(targetCountriesVBox);

                    //finally add both lists to the layout.
                    final HBox bothCountryGroups = new HBox(10);
                    bothCountryGroups.getChildren().addAll(spaneSource, spaneTarget);
                    bothCountryGroups.setAlignment(Pos.CENTER);

                    final Button plusle = new Button("Troops++");
                    repeatFireOnLongPress(plusle);
                    //plusle.setDefaultButton(true);
                    plusle.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            int curArmies = rsp.getNumArmies();
                            if (rsp.getToCountry() != null && curArmies < map.getCountryArmies(rsp.getFromCountry()) - 1) {
                                rsp.setNumArmies(rsp.getNumArmies() + 1);
                                statusText.setFill(Color.BLACK);
                                statusText.setText("Current selection:\nFortifying\n" + rsp.getToCountry().getName() + "\nusing " + rsp.getNumArmies() + " troops from\n" + rsp.getFromCountry().getName() + ".");
                            }
                        }
                    });

                    final Button minun = new Button("Troops--");
                    repeatFireOnLongPress(minun);
                    //minun.setDefaultButton(true);
                    minun.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            int curArmies = rsp.getNumArmies();
                            if (rsp.getToCountry() != null && curArmies > 0) {
                                rsp.setNumArmies(rsp.getNumArmies() - 1);
                                statusText.setFill(Color.BLACK);
                                statusText.setText("Current selection:\nFortifying\n" + rsp.getToCountry().getName() + "\nusing " + rsp.getNumArmies() + " troops from\n" + rsp.getFromCountry().getName() + ".");
                            }
                        }
                    });

                    HBox plusMinusBtns = new HBox(4);
                    plusMinusBtns.setAlignment(Pos.CENTER);
                    plusMinusBtns.getChildren().addAll(minun, plusle);

                    final String playaName = getName();
                    //button to attempt to accept final reinforcement allocation
                    Button acceptIt = new Button("Accept/OK");
                    fireButtonAfter3SHover(acceptIt);
                    acceptIt.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            if (rsp.getFromCountry() != null
                                    && rsp.getToCountry() != null
                                    && FortifyResponse.isValidResponse(rsp, map, playaName)) {
                                exitDecider.setAsNonSystemClose();
                                passTurn.set(false);
                                saveLastKnownWindowLocation(dialog);
                                dialog.close();
                            } else {
                                statusText.setText("Not a valid response; \nmake sure you select a target and source!!");
                                statusText.setFill(Color.RED);
                            }
                        }

                    });

                    Button skipIt = new Button("[skip/pass]");
                    fireButtonAfter3SHover(skipIt);
                    skipIt.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            passTurn.set(true);
                            exitDecider.setAsNonSystemClose();
                            saveLastKnownWindowLocation(dialog);
                            dialog.close();
                        }
                    });

                    //add status and buttons to layout
                    Text buttonDividerTop = new Text("***********");
                    buttonDividerTop.setTextAlignment(TextAlignment.CENTER);
                    Text buttonDividerBottom = new Text("***********");
                    buttonDividerBottom.setTextAlignment(TextAlignment.CENTER);
                    layout.getChildren().addAll(statusText, bothCountryGroups, buttonDividerTop, plusMinusBtns, buttonDividerBottom, acceptIt, skipIt);
                    layout.setAlignment(Pos.CENTER);

                    //formally add linear layout to scene through use of scrollpane, then display the dialog
                    superSPane.setContent(layout);
                    dialog.setScene(new Scene(superSPane));
                    FXUIPlayer.crossbar.setCurrentHumanName(getName());
                    FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);

                    registerNodeForBrightnessControl(dialog, superSPane);
                    registerSceneForEyeStrainControl(dialog, dialog.getScene());
                    dialog.show();
                    formatSPaneInlayAfterShow(superSPane);
                    formatSPaneInlayAfterShow(spaneSource);
                    formatSPaneInlayAfterShow(spaneTarget);
                }
            });

            /**
             * End mandatory FX thread processing. Immediately following this,
             * pause to wait for FX dialog to be closed!
             */
            waitForDialogToClose(FXUIPlayer.crossbar);
            checkIfCloseMeansMore(exitDecider, FXUIPlayer.crossbar);
            FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
        } while (this.keepRunning);
        if (passTurn.get()) {
            return null;
        }
        return rsp;
    }

    /**
     * Specify how a territory should be defended. RESPONSE REQUIRED
     *
     * @param map
     * @param myCards
     * @param playerCards
     * @param atkCountry
     * @param dfdCountry
     * @param numAtkDice
     * @return defense choice
     */
    public DefendResponse defend(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, Country atkCountry, Country dfdCountry, int numAtkDice) {
        //if the player asked to end the game, don't even display the dialog
        if (crossbar.isHumanEndingGame()) {
            return null;
        }

        DefendResponse rsp = new DefendResponse();
        //else...make the window and keep displaying until the user has confirmed selection

        int numDice = map.getCountryArmies(dfdCountry);
        if (numDice > RiskConstants.MAX_DFD_DICE) {
            numDice = RiskConstants.MAX_DFD_DICE;
        }
        final int maxDfdDiceAvailable = numDice;
        rsp.setNumDice(maxDfdDiceAvailable);

        do {
            this.keepRunning = false;
            Platform.runLater(new Runnable() {
                @Override
                public void run() {

                    /**
                     * *********
                     * Begin mandatory processing on FX thread. (Required for
                     * Stage objects.)
                     */
                    ScrollPane superSPane = new ScrollPane();
                    final Stage dialog = new Stage();
                    final VBox layout = getVBoxFormattedForLayout();

                    final Text guideText = new Text();
                    final Text statusText = new Text();

                    final Text diceCountStatus = new Text("Dice Count: " + rsp.getNumDice() + "\n(" + maxDfdDiceAvailable + " allowed)");
                    final Button diceCountDec = new Button("Dice--");
                    repeatFireOnLongPress(diceCountDec);
                    final Button diceCountInc = new Button("Dice++");
                    repeatFireOnLongPress(diceCountInc);
                    final HBox diceDisplay = new HBox(10);

                    Button acceptIt = new Button("Accept/OK");
                    fireButtonAfter3SHover(acceptIt);

                    final CheckBox autoDefend = new CheckBox("Auto-defend on successive turns");
                    autoDefend.setTooltip(new Tooltip("Automatically roll dice on future turns (dialog still appears)"));
                    autoDefend.setFont(Font.font("Arial", FontWeight.LIGHT, FontPosture.ITALIC, 16));
                    autoDefend.selectedProperty().addListener(new ChangeListener<Boolean>() {
                        @Override
                        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
                                Boolean newValue) {
                            autoProgressDefense.set(newValue);
                        }
                    }
                    );
                    autoDefend.setSelected(autoProgressDefense.get());

                    HBox acceptanceBtns = new HBox(10);
                    Text buttonDivider = new Text("***********");

                    //now that things have been placed in memory, let's set it all up...
                    dialog.setTitle("Defend! (?)");
                    if (FXUIPlayer.owner != null) {
                        dialog.initOwner(FXUIPlayer.owner);
                    }

                    putWindowAtLastKnownLocation(dialog);

                    //Generic instructions for attacking (the act of which is always optional, technically)
                    final String guideTextContents = map.getCountryOwner(dfdCountry)
                            + " of " + dfdCountry.getName() + ","
                            + "\nyou are being attacked by\n"
                            + map.getCountryOwner(atkCountry)
                            + " of " + atkCountry.getName() + "."
                            + "\nDefend yourself!"
                            + "\n"
                            + "\nHow many dice will you roll?"
                            + "\nYour attacker is rolling " + numAtkDice + "."
                            + "\nYOU can roll a maximum of " + maxDfdDiceAvailable + ".";
                    guideText.setText(guideTextContents);
                    guideText.setTextAlignment(TextAlignment.CENTER);

                    //status text: the target of the attack (name of country, when set), and the source of the attacks (name of country, when set)
                    statusText.setText("~~~");
                    statusText.setTextAlignment(TextAlignment.CENTER);

                    //pre-setup for dice selection -- position in the dialog box, and disable buttons (you can't immediately change the dice count)
                    diceCountStatus.setTextAlignment(TextAlignment.CENTER);
                    diceDisplay.getChildren().addAll(diceCountDec, diceCountStatus, diceCountInc);
                    diceDisplay.setAlignment(Pos.CENTER);
                    //the actions for the increment and decrement buttons, when buttons are available
                    diceCountInc.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent t) {
                            if (rsp.getNumDice() < maxDfdDiceAvailable) {
                                rsp.setNumDice(rsp.getNumDice() + 1);
                                updateDiceDisplay(diceCountStatus, rsp.getNumDice(), maxDfdDiceAvailable, diceCountDec, diceCountInc);
                            }
                        }
                    });

                    diceCountDec.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent t) {
                            if (rsp.getNumDice() > 1) {
                                rsp.setNumDice(rsp.getNumDice() - 1);
                                updateDiceDisplay(diceCountStatus, rsp.getNumDice(), maxDfdDiceAvailable, diceCountDec, diceCountInc);
                            }
                        }
                    });

                    //button to attempt to accept final reinforcement allocation
                    acceptIt.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent t) {
                            exitDecider.setAsNonSystemClose();
                            saveLastKnownWindowLocation(dialog);
                            dialog.close();
                        }
                    });

                    final String helpTooltipContents
                            = "\n\nYou roll dice to defend. Roll at least one die."
                            + "\nYou may roll two dice if you own enough countries."
                            + "\nRolling more dice gives you a better chance of having"
                            + "\n a successful defense. However, more dice also"
                            + "\nmeans you lose more troops if your defense fails.";
                    Tooltip helpTooltip = new Tooltip(helpTooltipContents);
                    diceCountInc.setTooltip(helpTooltip);
                    diceCountDec.setTooltip(helpTooltip);

                    if (autoProgressDefense.get()) {
                        fireButtonUponCheckbox(acceptIt, autoDefend);
                    }
                    acceptanceBtns.getChildren().addAll(acceptIt);
                    acceptanceBtns.setAlignment(Pos.CENTER);

                    //add status and buttons to layout
                    buttonDivider.setTextAlignment(TextAlignment.CENTER);
                    layout.getChildren().addAll(guideText, statusText, buttonDivider, diceDisplay, autoDefend, acceptanceBtns);
                    layout.setAlignment(Pos.CENTER);

                    //formally add linear layout to scene through the use of a scroll pane, and display the dialog
                    superSPane.setContent(layout);
                    dialog.setScene(new Scene(superSPane));
                    FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
                    FXUIPlayer.crossbar.setCurrentHumanName(getName());

                    registerNodeForBrightnessControl(dialog, superSPane);
                    registerSceneForEyeStrainControl(dialog, dialog.getScene());
                    dialog.show();

                    formatSPaneInlayAfterShow(superSPane);
                }
            });

            /**
             * End mandatory FX thread processing. Immediately after this, pause
             * the non-UI thread (which you should be back on) and wait for the
             * dialog to close!
             */
            waitForDialogToClose(FXUIPlayer.crossbar);
            checkIfCloseMeansMore(exitDecider, FXUIPlayer.crossbar);
            //if we have completed all business within the dialog, cleanup and return as required.
            FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
        } while (this.keepRunning);
        return rsp;
    }

    /**
     * Takes a button that's been set up with a "setOnAction" command and makes
     * it so that button will fire multiple times on long press.
     *
     * @param btn
     */
    private static void repeatFireOnLongPress(Button btn) {
        if (btn == null) {
            return;
        }
        EventHandler<MouseEvent> mEvent = (new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                //btn.fire();
                try {
                    Thread ctdRun = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final long fireDelta = 350;
                            RiskUtils.sleep(fireDelta);
                            final AtomicInteger fireCountSincePress = new AtomicInteger(0);
                            final short maxFireSlowdownThresh = 5;
                            btn.pressedProperty().addListener(new ChangeListener<Boolean>() {
                                @Override
                                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                                    /* reset the fire count when button is physically pressed
                                     * should restore speed.*/
                                    if (newValue.booleanValue() == true) {
                                        fireCountSincePress.set(0);
                                    }
                                }
                            });
                            btn.hoverProperty().addListener(new ChangeListener<Boolean>() {
                                @Override
                                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                                    /* reset the fire count when button is physically pressed
                                     * should restore speed.*/
                                    if (newValue.booleanValue() == false && !btn.isPressed()) {
                                        btn.setOpacity(1);
                                    }
                                }
                            });
                            while (btn.isPressed()) {
                                try {
                                    Platform.runLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            btn.fire();
                                            btn.setOpacity(0.5d + (double) (fireCountSincePress.get() % 2) / 2);
                                        }
                                    });
                                    RiskUtils.sleep(fireDelta + (275 * fireCountSincePress.get()));
                                    if (fireCountSincePress.get() < maxFireSlowdownThresh) {
                                        fireCountSincePress.incrementAndGet();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                    ctdRun.setDaemon(true);
                    ctdRun.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }

        });
        btn.setOnMousePressed(mEvent);
        //btn.setOnMouseClicked(mEvent);
        //btn.setOnMouseEntered(mEvent);
    }

    /**
     * Takes a button that's been set up with a "setOnAction" command and makes
     * it so that button will fire on hover. Also makes it so that the button
     * will continue to fire with extended hovering.
     *
     * @param btn
     */
    private static void fireButtonOnHover(Button btn) {
        if (btn == null) {
            return;
        }
        btn.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                btn.fire();

                try {
                    Thread ctdRun = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final long refreshDelta = 975;
                            RiskUtils.sleep(refreshDelta);
                            while (btn.isHover()) {
                                try {
                                    Platform.runLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            btn.fire();
                                        }
                                    });
                                    RiskUtils.sleep(refreshDelta);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                    ctdRun.setDaemon(true);
                    ctdRun.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }

        });
    }

    /**
     * Takes a list of buttons which have been set up with a "setOnAction"
     * command and makes it so that buttons will fire on hover. Also makes it so
     * that the buttons will continue to fire with extended hovering.
     *
     * @param btns the list of buttons to configure.
     */
    private static void fireButtonsOnHover(Button[] btns) {
        if (btns == null) {
            return;
        }
        for (Button btn : btns) {
            fireButtonOnHover(btn);
        }
    }

    /**
     * Takes a button that's been set up with a "setOnAction" command and makes
     * it so that button will fire on hover. Also makes it so that the button
     * will continue to fire with extended hovering.
     *
     * @param btn
     */
    private static void fireButtonAfter3SHover(Button btn) {
        if (btn == null) {
            return;
        }
        btn.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                final String origText = btn.getText();

                try {
                    Thread ctdRun = new Thread(new Runnable() {
                        @Override
                        public void run() {

                            final int startingNo = 3;
                            int rept = startingNo;
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    btn.setText(startingNo + "s: " + origText);
                                }
                            });
                            RiskUtils.sleep(975);

                            while (btn.isHover() && rept > 0) {
                                try {
                                    rept--;
                                    final int reptN = rept;
                                    Platform.runLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            btn.setText(reptN + "s: " + origText);
                                        }
                                    });
                                    RiskUtils.sleep(975);
                                    if (rept == 0) {
                                        Platform.runLater(new Runnable() {
                                            @Override
                                            public void run() {
                                                btn.fire();
                                            }
                                        });
                                    }

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            try {
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        btn.setText(origText);
                                    }
                                });
                            } catch (Exception e) {

                            }

                        }
                    });
                    ctdRun.setDaemon(true);
                    ctdRun.start();
                    btn.hoverProperty().addListener(new ChangeListener<Boolean>() {
                        @Override
                        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                            ctdRun.interrupt();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }

        });
    }

    /**
     * Takes a button that's been set up with a "setOnAction" command and makes
     * it so that button will fire on hover. Also makes it so that the button
     * will continue to fire with extended hovering.
     *
     * @param btn
     */
    private static void fireButtonUponCheckbox(Button btn, CheckBox controllerCBox) {
        if (btn == null) {
            return;
        }
        final String origText = btn.getText();

        try {
            Thread ctdRun = new Thread(new Runnable() {
                @Override
                public void run() {

                    final int startingNo = 3;
                    int rept = startingNo;
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            btn.setText(startingNo + "s: " + origText);
                        }
                    });
                    RiskUtils.sleep(975);

                    while (controllerCBox.isSelected() && rept > 0) {
                        try {
                            rept--;
                            final int reptN = rept;
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    btn.setText(reptN + "s: " + origText);
                                }
                            });
                            RiskUtils.sleep(975);
                            if (rept == 0) {
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        btn.fire();
                                    }
                                });
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                btn.setText(origText);
                            }
                        });
                    } catch (Exception e) {

                    }

                }
            });
            ctdRun.setDaemon(true);
            ctdRun.start();

            controllerCBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    ctdRun.interrupt();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

}

/**
 * to determine whether the user is still playing the game, or if the user
 * initiated a normal program exit from the system
 */
class ExitStateSubHelper {

    private boolean systemExitUsed = true;

    /**
     * Get whether the program should attempt to exit back to the OS, or if the
     * app should continue running after "dialog.close()" is called
     *
     * @return "false" ONCE AND ONLY ONCE after "setAsNonSystemClose()" until
     * the next time said method is called again, returns "true" otherwise. (So
     * only ask if we are closing the application once, because it defaults to
     * saying "yes, we want to close the entire application!" every subsequent
     * time until you raise the flag again).
     */
    public boolean isSystemExit() {
        final boolean cExit = systemExitUsed;
        systemExitUsed = true;
        return cExit;
    }

    /**
     * Raises a flag to tell the program to not attempt to exit. Aka "the user
     * is interacting with the program as per normal use, so a dialog closing is
     * OK". Use every time you close a dialog to prevent the app from asking if
     * you're trying to leave/exit.
     *
     * @return "false" to indicate that we have successfully told the app to not
     * fully exit, "true" otherwise. (Should never have "true" as a return!)
     */
    public boolean setAsNonSystemClose() {
        systemExitUsed = false;
        return systemExitUsed;
    }
}
