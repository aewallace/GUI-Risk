package LogPlayer;

import Util.TextNodes;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.lang.InterruptedException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class LogPlayer extends Application {

    public static final String versionInfo = "Log-Player\nVersion 00x0Dh,\nStamp 2015.11.10, 18:00,\nStability: Beta(02)";
    private static final int DEFAULT_APP_WIDTH = 1600;
    private static final int DEFAULT_APP_HEIGHT = 1062;
    private static final int RAPID_PLAY_TIME_DELTA = 1170;
    private static final int NORMAL_PLAY_TIME_DELTA = 5650;
    private static final int BUSYROUTINE_THRESH = 1;
    private static final int BUSYROUTINE_RETRY_COUNT = 7;
    private static final int BUSYROUTINE_WAIT_TIME = 350;
    private static final String LOG_FILE = "LOG.txt";
    private static final String EVENT_DELIM = "...";
    private static final int PLAY_FWD = 1;
    private static final int FAST_FWD = -1;
    private static final int REWIND = 2;
    private static final int PAUSE = 0;
    private static final int STEP_FWD = -2;
    private static final int animCycleCountMAX = 21;
    private static boolean launchedFromFXUIGM = false;
    private static double EXPON_SPEED_UP_PCT = 1.0;

    private ScrollPane scrollPane;
    private Scene scene;
    private Pane pane;
    private Text eventTitle;
    private Text round;
    private Text turn;
    private Text nextLogLine;
    private Text errorDisplay;
    private Text currentPlayStatus;
    private String errorText;
    private boolean errorDisplayBit;
    private HashMap<String, Text> textNodeMap;
    private Map<String, Color> playerColorMap;

    private Scanner log;
    private String nextToken;
    private String dlTokenHelper;
    private ArrayList<String> logCache;
    private ArrayList<HashMap<String, Text>> mapStateCache;
    private int positionInCaches;
    private boolean inREWIND;
    private boolean initialPlay = true;
    private boolean cancelActiveActions;
    private int currentButton;
    private String currentSimpleStatus;
    private int animCycleCount;
    private int busyRoutines; //to perform basic resource locks
    private static int routinesRequestingPriority;
    private HashMap<Long, Thread> threadMap;

    /**
     * used to let the logplayer know it didn't start itself.
     *
     * @return returns "true" if successfully set. (Yes, vague, I know.)
     */
    public static boolean setAsLaunchedFromFXUIGM() {
        return LogPlayer.launchedFromFXUIGM = true;
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            this.log = new Scanner(new File(LOG_FILE));
            this.nextToken = null;
            this.currentButton = PAUSE;
            this.currentSimpleStatus = "";
            this.animCycleCount = 0;
            this.busyRoutines = 0;
            LogPlayer.routinesRequestingPriority = 0;
            this.inREWIND = false;
            this.dlTokenHelper = "";
            this.positionInCaches = -1;
            this.logCache = new ArrayList<String>();
            this.mapStateCache = new ArrayList<HashMap<String, Text>>();
            this.cancelActiveActions = false;
            this.threadMap = new HashMap<Long, Thread>();

            pane = new Pane();
            pane.setPrefSize(DEFAULT_APP_WIDTH + 200, DEFAULT_APP_HEIGHT + 30);

            errorDisplayBit = false;
            errorText = "Status...";

            loadTextNodes("TextNodes.txt");
            loadPlayers();

            //if there is an error on loading necessary resources,
            // render the "negated" map image as a visual cue to indicate failure
            pane.setStyle("-fx-background-image: url(\"RiskBoardAE.jpg\")");
            errorDisplay = new Text(29, 560, errorText);
            errorDisplay.setFont(Font.font("Verdana", FontWeight.BOLD, 24));
            if (errorDisplayBit) {
                errorDisplay.setFill(Color.RED);
            } else {
                errorDisplay.setFill(Color.WHITE);
            }

            pane.getChildren().add(errorDisplay);

            if (LogPlayer.launchedFromFXUIGM) {
                Button closeWindow = new Button("close (return to main menu)");
                closeWindow.setLayoutX(29);
                closeWindow.setLayoutY(50);
                closeWindow.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        primaryStage.close();
                    }
                });
                pane.getChildren().add(closeWindow);
            }

            //if there was no error, populate the window with appropriate elements
            if (!errorDisplayBit) {
                pane.setStyle("-fx-background-image: url(\"RiskBoard.jpg\")");
                eventTitle = new Text(1350, 515, "Initial Reinforcement\nStage");
                eventTitle.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
                eventTitle.setFill(Color.LIGHTGRAY);

                round = new Text(1460, 450, "");
                round.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
                round.setFill(Color.LIGHTGRAY);

                turn = new Text(1425, 470, "");
                turn.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
                turn.setFill(Color.LIGHTGRAY);

                nextLogLine = new Text(600, 1030, "");
                nextLogLine.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
                nextLogLine.setFill(Color.LIGHTGRAY);

                currentPlayStatus = new Text(29, 600, "Hello! ^.^");
                currentPlayStatus.setFont(Font.font("Verdana", FontWeight.BOLD, 40));
                currentPlayStatus.setFill(Color.WHITE);
                pane.getChildren().addAll(eventTitle, round, turn, nextLogLine, currentPlayStatus);

                //The original single-seek/step-through "Next Event" button 
                Button nextActionBtn = new Button("Single-Step to Next Event");
                nextActionBtn.setLayoutX(29);
                nextActionBtn.setLayoutY(770);
                nextActionBtn.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        Runnable task = new Runnable() {
                            @Override
                            public void run() {
                                runButtonRunnable(STEP_FWD, cancelActiveActions);
                            }
                        };
                        Thread th = new Thread(task);
                        addThreadToMap(th);
                        th.setDaemon(true);
                        th.start();
                    }
                });

                //The Play-Forward (normal speed) Button
                Button pauseAllBtn = new Button("Pause Event Playback");
                pauseAllBtn.setLayoutX(29);
                pauseAllBtn.setLayoutY(650);

                pauseAllBtn.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        Runnable task = new Runnable() {
                            @Override
                            public void run() {
                                runButtonRunnable(PAUSE, cancelActiveActions);
                            }
                        };
                        Thread th = new Thread(task);
                        addThreadToMap(th);
                        th.setDaemon(true);
                        th.start();
                    }
                });

                //The Play-Forward (normal speed) Button
                Button playFwdBtn = new Button("Auto-play Events");
                playFwdBtn.setLayoutX(29);
                playFwdBtn.setLayoutY(610);

                playFwdBtn.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        Runnable task = new Runnable() {
                            @Override
                            public void run() {
                                runButtonRunnable(PLAY_FWD, cancelActiveActions);
                            }
                        };
                        Thread th = new Thread(task);
                        addThreadToMap(th);
                        th.setDaemon(true);
                        th.start();
                    }
                });

                //The fast forward (rapid-speed forward) button:
                Button fastFwdBtn = new Button("Fast-Forward Events");
                fastFwdBtn.setLayoutX(29);
                fastFwdBtn.setLayoutY(690);

                fastFwdBtn.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        Runnable task = new Runnable() {
                            @Override
                            public void run() {
                                runButtonRunnable(FAST_FWD, cancelActiveActions);
                            }
                        };
                        Thread fth = new Thread(task);
                        addThreadToMap(fth);
                        fth.setDaemon(true);
                        fth.start();

                    }
                });
                //end FFWD button

                //The rewind (dual-speed reverse) button:
                Button dsRewindBtn = new Button("Slow/Fast Rewind Events");
                dsRewindBtn.setLayoutX(29);
                dsRewindBtn.setLayoutY(730);

                dsRewindBtn.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        Runnable task = new Runnable() {
                            @Override
                            public void run() {
                                runButtonRunnable(REWIND, cancelActiveActions);
                            }
                        };
                        Thread fth = new Thread(task);
                        addThreadToMap(fth);
                        fth.setDaemon(true);
                        fth.start();

                    }
                });
                //end RWND button

                pane.getChildren().addAll(nextActionBtn, pauseAllBtn, playFwdBtn, fastFwdBtn, dsRewindBtn);
            } //END: layout of buttons displayed upon successful launch ends here.

            scrollPane = new ScrollPane();
            scrollPane.setContent(pane);
            if (!errorDisplayBit) {
                scrollPane.setOnKeyPressed(new EventHandler<KeyEvent>() {
                    @Override
                    public void handle(KeyEvent event) {
                        Runnable task = new Runnable() {
                            @Override
                            public void run() {
                                runButtonRunnable(STEP_FWD, cancelActiveActions);
                            }
                        };
                        Thread th = new Thread(task);
                        addThreadToMap(th);
                        th.setDaemon(true);
                        th.start();
                    }
                });
            }

            scene = new Scene(scrollPane, DEFAULT_APP_WIDTH, DEFAULT_APP_HEIGHT);
            primaryStage.setTitle("Log Player: RISK Game Review");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (FileNotFoundException e) {
        }
    }

    private void addThreadToMap(Thread threadIn) {
        this.threadMap.put(threadIn.getId(), threadIn);
    }

    private void removeThread(Long idIn) {
        this.threadMap.remove(idIn);
    }

    private void interruptThreadsExceptSelf() {
        if (!threadMap.isEmpty()) {
            for (Long keyC : this.threadMap.keySet()) {
                if (keyC != Thread.currentThread().getId()) {
                    Thread toRemove = this.threadMap.remove(keyC);
                    toRemove.interrupt();
                }
            }

        }

    }

    void runButtonRunnable(int btnTypeIn, boolean cancelIMCurrentAction) {
        int waitTime = 0; //will be set to a certain number of milliseconds to alter rapid-vs-normal FWD/REWIND
        routinesRequestingPriority++;
        animCycleCount = 0;
        switch (btnTypeIn) {
            case PLAY_FWD:
                inREWIND = false;
                this.currentSimpleStatus = ">";
                waitTime = NORMAL_PLAY_TIME_DELTA;
                break;
            case FAST_FWD:
                inREWIND = false;
                if (this.currentSimpleStatus.equals(">>>")) {
                    this.currentSimpleStatus = ">>>>>";
                    waitTime = (int) (0.7 * RAPID_PLAY_TIME_DELTA);
                } else {
                    this.currentSimpleStatus = ">>>";
                    waitTime = RAPID_PLAY_TIME_DELTA;
                }
                break;
            case REWIND:
                if (inREWIND) {
                    waitTime = (int) (0.7 * RAPID_PLAY_TIME_DELTA);
                    this.currentSimpleStatus = "<<<<<";
                }
                if (!inREWIND) {
                    waitTime = RAPID_PLAY_TIME_DELTA;
                    inREWIND = true;
                    this.currentSimpleStatus = "<<<";
                }
                break;
            case STEP_FWD:
                inREWIND = false;
                this.currentSimpleStatus = ">||";
                break;
            case PAUSE:
            default:
                if (this.currentButton == PAUSE || busyRoutines == 0) {
                    routinesRequestingPriority--;
                    return;
                }
                inREWIND = false;
                this.currentSimpleStatus = "||";
                break;
        }
        routinesRequestingPriority--;
        this.busyRoutines++;
        try {
            this.currentButton = btnTypeIn;
            int waitCount = 1;
            while (this.busyRoutines > BUSYROUTINE_THRESH && waitCount < BUSYROUTINE_RETRY_COUNT && !Thread.interrupted()) {
                animateWaitStatus(waitCount);
                java.lang.Thread.sleep(BUSYROUTINE_WAIT_TIME);
                interruptThreadsExceptSelf();
                waitCount++;
            }
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            EXPON_SPEED_UP_PCT = 1.0;
            final int OLDBUTTON = btnTypeIn;
            final String OLDshPLAYSTATE = this.currentSimpleStatus;

            while (routinesRequestingPriority == 0 && !cancelActiveActions && this.currentButton == OLDBUTTON && OLDshPLAYSTATE == this.currentSimpleStatus && !Thread.interrupted()) {
                int q = 3;
                double qRatio = (double) 1 / q;
                if (((inREWIND && positionInCaches < 25) || (!initialPlay && positionInCaches + 25 >= logCache.size())) && 1.25 * EXPON_SPEED_UP_PCT <= 0.95) {
                    EXPON_SPEED_UP_PCT = 1.25 * EXPON_SPEED_UP_PCT;
                } else if (0.95 * EXPON_SPEED_UP_PCT >= 0.05) {
                    EXPON_SPEED_UP_PCT = 0.9 * EXPON_SPEED_UP_PCT;
                }

                for (; q > 0 && this.currentButton == OLDBUTTON && OLDshPLAYSTATE == this.currentSimpleStatus; q--) {
                    if (animCycleCount < animCycleCountMAX) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                animateStatus(false);
                            }
                        });
                    }
                    java.lang.Thread.sleep((int) (qRatio * waitTime * EXPON_SPEED_UP_PCT));
                }

                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                if (this.currentButton == OLDBUTTON && OLDshPLAYSTATE == this.currentSimpleStatus && routinesRequestingPriority == 0) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            stepThroughBtnLogic(LOG_FILE, OLDBUTTON, cancelActiveActions);
                        }
                    });
                }

                if (OLDBUTTON == STEP_FWD) {
                    setStatus(false);
                    cancelActiveActions = true;
                }
            }
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            System.out.println("Log playback action complete.");
            if (OLDshPLAYSTATE == this.currentSimpleStatus && this.currentButton != STEP_FWD) {
                cancelActiveActions = false;
                for (int m = 0; m < 8 && !Thread.interrupted(); m++) {
                    animateStopStatus(m, false);
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    java.lang.Thread.sleep(BUSYROUTINE_WAIT_TIME);
                }
                animateStopStatus(0, true);
            }
        } catch (Exception e) {
            if (this.currentButton == STEP_FWD) {
                setStatus(false);
            } else {
                animateStopStatus(0, false);
            }
        } finally {
            this.busyRoutines--;
            cancelActiveActions = false;
            removeThread(Thread.currentThread().getId());
        }
    }

    // override to change APP WIDTH
    protected double getAppWidth() {
        return DEFAULT_APP_WIDTH;
    }

    // override to change APP HEIGHT
    protected double getAppHeight() {
        return DEFAULT_APP_HEIGHT;
    }

    // override to set App Title
    protected String getAppTitle() {
        return "Risk Log Player";
    }

    private void loadTextNodes(String nodeFile) {
        try {
            if (nodeFile != null) {
                this.textNodeMap = new HashMap<String, Text>();
                File fileRepresentation = new File(nodeFile);
                //basic check for existence of country list file
                if (!fileRepresentation.exists()) {
                    try (Scanner reader = new Scanner(TextNodes.nodes)) {
                        while (reader.hasNext()) {
                            int nextX = reader.nextInt();
                            int nextY = reader.nextInt();
                            String nextCountry = reader.nextLine().trim();
                            Text txt = new Text(nextX, nextY, nextCountry + "\n0");
                            txt.setFill(Color.BLUEVIOLET);
                            txt.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
                            this.textNodeMap.put(nextCountry, txt);
                            this.pane.getChildren().add(txt);
                        }
                        System.out.print("Info: using internal list of countries!");
                        System.out.print("\nExpected: \"" + nodeFile + "\"\n");
                        System.out.print("LogPlayer setup continuing as normal...");
                    }
                } //and basic check for valid file contents
                else if (fileRepresentation.length() < 25) {
                    System.out.print("Warning: malform input file detected!");
                    System.out.print("\nExpected \"" + nodeFile + "\" to be of a certain size.\n");
                    System.out.print("Please check the file and restart the LogyPlayer GUI.\n");
                    errorDisplayBit = true;
                    errorText = "Malformed input file detected;\nsee console for details.";
                } else {
                    Scanner reader = new Scanner(fileRepresentation);
                    while (reader.hasNext()) {
                        int nextX = reader.nextInt();
                        int nextY = reader.nextInt();
                        String nextCountry = reader.nextLine().trim();
                        Text txt = new Text(nextX, nextY, nextCountry + "\n0");
                        txt.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
                        this.textNodeMap.put(nextCountry, txt);
                        this.pane.getChildren().add(txt);
                    }
                    reader.close();
                }
            }
        } catch (Exception e) {
            //errorDisplay.setText(e.getMessage());
        }
    }

    private void loadPlayers() {
        try {
            ArrayList<Color> colors = new ArrayList<Color>();
            colors.add(Color.WHITE);
            colors.add(Color.AQUA);
            colors.add(Color.RED);
            colors.add(Color.GREENYELLOW);
            colors.add(Color.CORAL);
            colors.add(Color.VIOLET);
            this.playerColorMap = new HashMap<String, Color>();
            int i = 0;
            boolean finished = false;
            while (!finished && log.hasNext()) {
                //assign player colors
                if (log.nextLine().equals("Players:")) {
                    if (nextToken == null) {
                        nextToken = log.nextLine();
                    }
                    while (!nextToken.equals(EVENT_DELIM)) {
                        this.playerColorMap.put(nextToken, colors.get(i++ % colors.size()));
                        Text txt = new Text(200 * (i - 1) + 50, 20, nextToken);
                        txt.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
                        txt.setFill(colors.get((i - 1) % colors.size()));
                        this.pane.getChildren().add(txt);
                        nextToken = log.nextLine();
                    }
                    finished = true;
                }
            }
        } catch (Exception e) {
        }
    }

    private void setStatus(boolean isEndOfTask) {
        if (isEndOfTask) {
            currentPlayStatus.setText("||");
        } else {
            currentPlayStatus.setText(this.currentSimpleStatus);
        }
    }

    private void animateStatus(boolean isEndOfTask) {
        if (isEndOfTask) {
            currentPlayStatus.setText("||");
        } else if (animCycleCount % 2 == 0) {
            currentPlayStatus.setText(this.currentSimpleStatus);
        } else if (animCycleCount + 2 >= animCycleCountMAX) {
            currentPlayStatus.setText(this.currentSimpleStatus);
        } else {
            currentPlayStatus.setText("- - -");
        }
        animCycleCount++;
    }

    private void animateStopStatus(int clkIn, final boolean setFinalStatus) {
        final int clk = clkIn % 3;
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (setFinalStatus) {
                    currentPlayStatus.setText("Idle.");
                    return;
                }
                switch (clk) {
                    case 0:
                        currentPlayStatus.setText("STOP");
                        break;
                    case 1:
                        currentPlayStatus.setText("STOP");
                        ;
                        break;
                    case 2:
                    case 3:
                        currentPlayStatus.setText("- - -");
                        break;
                }
            }
        });
    }

    private void animateWaitStatus(int clkIn) {
        final int clk = clkIn % 4;
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                switch (clk) {
                    case 0:
                        currentPlayStatus.setText("Wait");
                        break;
                    case 1:
                        currentPlayStatus.setText("Busy");
                        break;
                    case 2:
                        currentPlayStatus.setText("Wait");
                        break;
                    case 3:
                        currentPlayStatus.setText("....");
                        break;
                }
            }
        });
    }

    private void stepThroughBtnLogic(String logFile, int btnTypeIn, boolean cancelIMCurrentAction) {
        if (cancelActiveActions) {
            return;
        }
        try {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            if (this.initialPlay) { //if we haven't filled the cache, we step through, or we make use of the little that IS in the cache up to this point
                switch (btnTypeIn) {
                    case PLAY_FWD:
                    case STEP_FWD:
                    case FAST_FWD:
                        //if we have cached up to this point (i.e., we played forward, rewound, and played forward again)
                        if (!logCache.isEmpty() && positionInCaches < logCache.size() - 1 && nextToken != null) {
                            positionInCaches++;
                            processCaptiveToken(logCache.get(positionInCaches), false, cancelActiveActions);
                        } //else if there is still more in the log, but we haven't cached up to the current point
                        else if (nextToken != null) {
                            readNextLogEvent(logFile, cancelActiveActions);
                        } //else, we have reached the end of the log for the initial playthrough (and everything is cached by now)
                        else {
                            this.currentButton = PAUSE;
                        }
                        break;
                    case REWIND:
                        System.out.println("PerformAutoPlayback genericE: Single rewind step started");
                        if (!logCache.isEmpty() && positionInCaches >= 0 && inREWIND) {
                            /*System.out.println("PerformAutoPlayback genericE: Rewind middle1");*/
                            if (logCache.get(positionInCaches) != null) { //more error handling; todo: remove in final version
                                processCaptiveToken(logCache.get(positionInCaches), (positionInCaches == 0), cancelActiveActions);
                                if (this.textNodeMap == null || this.textNodeMap.isEmpty()) {
                                    System.out.println("PerformAutoPlayback genericE: textNodeMap reported as empty");
                                }
                            } else {
                                System.out.println("genericE: null entry found in token collection");
                            }
                            positionInCaches--;
                        } else {
                            inREWIND = false;
                            this.currentButton = PAUSE;
                        }
                        if (positionInCaches < 0) {
                            positionInCaches = 0;
                            this.currentButton = PAUSE;
                        }
                        break;
                    case PAUSE:
                        inREWIND = false;
                        cancelActiveActions = true;
                        break;
                    default:
                        break;
                }
            } else { //else, everything has been cached, and we make use of said cache for all actions
                if (cancelActiveActions) {
                    return;
                }
                switch (btnTypeIn) {
                    case PLAY_FWD:
                    case STEP_FWD:
                    case FAST_FWD:
                        if (!logCache.isEmpty() && positionInCaches < logCache.size() - 1) {
                            positionInCaches++;
                            processCaptiveToken(logCache.get(positionInCaches), (positionInCaches == logCache.size() - 1), cancelActiveActions);
                        }
                        if (positionInCaches >= logCache.size()) {
                            positionInCaches = logCache.size() - 1;
                            this.currentButton = PAUSE;
                        }
                        break;

                    case REWIND:
                        if (!logCache.isEmpty() && positionInCaches >= 0 && inREWIND) {
                            if (logCache.get(positionInCaches) != null) { //more error handling; todo: remove in final version
                                processCaptiveToken(logCache.get(positionInCaches), (positionInCaches == 0), cancelActiveActions);
                                if (this.textNodeMap == null || this.textNodeMap.isEmpty()) {
                                    System.out.println("PerformAutoPlayback genericE: textNodeMap reported as empty");
                                }
                            } else {
                                System.out.println("PerformAutoPlayback genericE: null entry found in token collection");
                            }
                            positionInCaches--;
                        } else {
                            inREWIND = false;
                            this.currentButton = PAUSE;/* cancelActiveActions = true;*/
                        }
                        if (positionInCaches < 0) {
                            positionInCaches = 0;
                            this.currentButton = PAUSE;
                        }
                        break;
                    case PAUSE:
                        inREWIND = false;
                        cancelActiveActions = true;
                        break;
                    default:
                        break;
                }
            }
        } catch (IndexOutOfBoundsException e) {
            if (positionInCaches > 0) {
                positionInCaches--;
                System.out.println("Spilled over index; position auto-reset enabled; programmer, please check!");
                cancelActiveActions = true;
            } else if (positionInCaches < 0) {
                positionInCaches--;
                System.out.println("Seeked under index; position auto-reset to zero; programmer, please check!");
                cancelActiveActions = true;
            }
        } catch (Exception e) {
            System.out.println("PerformAutoPlayback exception:" + e);
        }
    }

    private void processCaptiveToken(String currentTokenIn, boolean isLastToken, boolean cancelIMCurrentAction) {
        if (cancelActiveActions || routinesRequestingPriority != 0 || Thread.interrupted()) {
            return;
        }
        updateMapFromCache();
        try {
            if (currentTokenIn.matches(".* reinforcing with .* armies.")) {
                String playerName = parsePlayerName(currentTokenIn, " reinforcing ");
                eventTitle.setText(playerName + " reinforcing.");
                turn.setText(playerName + "'s Turn");
            } else if (currentTokenIn.matches("Beginning Round .*!")) {
                round.setText(currentTokenIn.substring(10, currentTokenIn.length() - 1));
                eventTitle.setText("New Round.");
            } else if (currentTokenIn.matches(".* is attacking .* from .*!")) {
                if (!inREWIND) { //if going forward, store our info for later parsing
                    dlTokenHelper = currentTokenIn;
                } else { //if going in reverse, parse current info + old info
                    String playerName = parsePlayerName(currentTokenIn, " is attacking ");
                    String atkCountry = parseAtkCountry(currentTokenIn);
                    String dfdCountry = parseDfdCountry(currentTokenIn);
                    eventTitle.setText(playerName + " attacked\n" + dfdCountry + " from " + atkCountry);
                }
            } else if (currentTokenIn.matches("Attacker lost: .*; Defender lost: .*")) {
                if (inREWIND) { //if rewinding, store our info for parsing up the chain
                    dlTokenHelper = currentTokenIn;

                } else { //if forwarding, parse old info + current info
                    String playerName = parsePlayerName(dlTokenHelper, " is attacking ");
                    String atkCountry = parseAtkCountry(dlTokenHelper);
                    String dfdCountry = parseDfdCountry(dlTokenHelper);
                    eventTitle.setText(playerName + " attacked\n" + dfdCountry + " from " + atkCountry);

                }
            } else if (currentTokenIn.matches(".* has taken .* from .*!")) {
                String playerName = parsePlayerName(currentTokenIn, " has taken ");
                eventTitle.setText(playerName + " has taken\n" + parseTakenCountry(currentTokenIn));
            } else if (currentTokenIn.matches(".* advanced .* into .*")) {
                String[] line = currentTokenIn.split(" advanced ");
                eventTitle.setText(line[0] + " advanced\n" + line[1]);
            } else if (currentTokenIn.matches(".* is transferring .* from .* to .*")) {
                String[] line = currentTokenIn.split(" transferring ");
                eventTitle.setText(line[0] + " transferring\n" + line[1]);
            }

            nextLogLine.setText("Next event: " + currentTokenIn);

            if (isLastToken /*&& this.initialPlay == false*/) {
                if (inREWIND) {
                    nextLogLine.setText("[Beginning of game.]\n" + currentTokenIn);
                } else {
                    nextLogLine.setText("Game over!\n" + currentTokenIn);
                }
            }
            /*}*/ //end while
        } //end try
        catch (Exception e) {
            System.out.println("processCaptiveToken:::" + e.getMessage());
        }
    }

    private void readNextLogEvent(String logFile, boolean cancelIMCurrentAction) {
        if (cancelActiveActions || routinesRequestingPriority != 0 || Thread.interrupted()) {
            return;
        }
        try {
            boolean nextLineFound = nextLogLine.getText().equals("Next event: " + nextToken);
            while (nextToken != null) {
                if (nextToken.matches(".* reinforcing with .* armies.")) {
                    if (!nextLineFound) {
                        nextLineFound = true;
                    } else {
                        nextLineFound = false;
                        String playerName = parsePlayerName(nextToken, " reinforcing ");
                        eventTitle.setText(playerName + " reinforcing.");
                        nextToken = log.nextLine();
                        while (!nextToken.equals(EVENT_DELIM)) {
                            int armies = parseReinforceAmt(nextToken);
                            String countryName = parseReinforceCountry(nextToken);
                            setCountryOwnership(countryName, playerName);
                            addArmiesToCountry(countryName, armies);
                            turn.setText(playerName + "'s Turn");
                            nextToken = log.nextLine();
                        }
                        nextToken = log.nextLine();
                    }
                } else if (nextToken.matches("Beginning Round .*!")) {
                    if (!nextLineFound) {
                        nextLineFound = true;
                    } else {
                        nextLineFound = false;
                        round.setText(nextToken.substring(10, nextToken.length() - 1));
                        eventTitle.setText("New Round.");
                        nextToken = log.nextLine();
                    }
                } else if (nextToken.matches(".* is attacking .* from .*!")) {
                    if (!nextLineFound) {
                        nextLineFound = true;
                    } else {
                        nextLineFound = false;
                        String playerName = parsePlayerName(nextToken, " is attacking ");
                        String atkCountry = parseAtkCountry(nextToken);
                        String dfdCountry = parseDfdCountry(nextToken);
                        eventTitle.setText(playerName + " attacked\n" + dfdCountry + " from " + atkCountry);
                        nextToken = log.nextLine();
                        int atkLosses = parseAtkLosses(nextToken);
                        int dfdLosses = parseDfdLosses(nextToken);
                        addArmiesToCountry(atkCountry, -1 * atkLosses);
                        addArmiesToCountry(dfdCountry, -1 * dfdLosses);
                        nextToken = log.nextLine();
                    }
                } else if (nextToken.matches(".* has taken .* from .*!")) {
                    if (!nextLineFound) {
                        nextLineFound = true;
                    } else {
                        nextLineFound = false;
                        String playerName = parsePlayerName(nextToken, " has taken ");
                        eventTitle.setText(playerName + " has taken\n" + parseTakenCountry(nextToken));
                        setCountryOwnership(parseTakenCountry(nextToken), playerName);
                        nextToken = log.nextLine();
                    }
                } else if (nextToken.matches(".* advanced .* into .*")) {
                    if (!nextLineFound) {
                        nextLineFound = true;
                    } else {
                        nextLineFound = false;
                        String[] line = nextToken.split(" advanced ");
                        eventTitle.setText(line[0] + " advanced\n" + line[1]);
                        int armies = parseAdvanceArmies(nextToken);
                        addArmiesToCountry(parseAdvanceSourceCountry(nextToken), -1 * armies);
                        addArmiesToCountry(parseAdvanceDestinationCountry(nextToken), armies);
                        nextToken = log.nextLine();
                    }
                } else if (nextToken.matches(".* is transferring .* from .* to .*")) {
                    if (!nextLineFound) {
                        nextLineFound = true;
                    } else {
                        nextLineFound = false;
                        String[] line = nextToken.split(" transferring ");
                        eventTitle.setText(line[0] + " transferring\n" + line[1]);
                        int armies = parseFortifyArmies(nextToken);
                        String source = parseFortifySourceCountry(nextToken);
                        String dst = parseFortifyDestinationCountry(nextToken);
                        addArmiesToCountry(source, -1 * armies);
                        addArmiesToCountry(dst, armies);
                        nextToken = log.nextLine();
                    }
                } else {
                    nextToken = log.nextLine();
                }
                if (nextLineFound) {
                    nextLogLine.setText("Next event: " + nextToken);
                    return;
                }
                if (!log.hasNext()) {
                    eventTitle.setText("Game over!");
                    nextToken = null;
                    log.close();
                    this.initialPlay = false;
                    this.currentButton = PAUSE;

                    logCache.add("[End of game log]");
                    this.mapStateCache.add(duplicateTextNodeMap(this.textNodeMap));
                    positionInCaches++;
                } else {
                    logCache.add(nextToken);
                    this.mapStateCache.add(duplicateTextNodeMap(this.textNodeMap));
                    positionInCaches++;
                }
            }
        } catch (Exception e) {
            System.out.println("readNextLine::: " + e.getMessage());
            errorDisplay.setText(e.getMessage());
        }
    }

    private String parsePlayerName(String line, String afterName) {
        return line.split(afterName)[0];
    }

    private int getPrevArmies(String countryName) {
        Text txt = this.textNodeMap.get(countryName);
        return Integer.parseInt(txt.getText().split("\n")[1]);
    }

    private int parseReinforceAmt(String reinforceLine) {
        return Integer.parseInt(reinforceLine.split(" ")[0]);
    }

    private String parseReinforceCountry(String reinforceLine) {
        String[] split = reinforceLine.split(" ");
        String countryName = split[1];
        if (split.length == 3) {
            countryName += " " + split[2];
        }
        return countryName;
    }

    private void addArmiesToCountry(String countryName, int armies) {
        Text txt = this.textNodeMap.get(countryName);
        int oldArmies = getPrevArmies(countryName);
        txt.setText(countryName + "\n" + (oldArmies + armies));
    }

    private void setCountryOwnership(String countryName, String owner) {
        this.textNodeMap.get(countryName).setFill(this.playerColorMap.get(owner));
    }

    private void updateMapFromCache() {
        if (positionInCaches < 0) {
            System.out.println("updateMapFromCaches: No value at this pstn");
            return;
        }
        for (String keyC : this.textNodeMap.keySet()) {
            Text dstTxt = this.textNodeMap.get(keyC);
            Text srcTxt = this.mapStateCache.get(positionInCaches).get(keyC);
            dstTxt.setFill(srcTxt.getFill());
            dstTxt.setText(srcTxt.getText());
        }
    }

    private HashMap<String, Text> duplicateTextNodeMap(HashMap<String, Text> incomingTextNodeMap) {
        HashMap<String, Text> outgoingTextNodeMap = new HashMap<String, Text>();
        for (String keyC : incomingTextNodeMap.keySet()) {
            double nextX = incomingTextNodeMap.get(keyC).getLayoutX();
            double nextY = incomingTextNodeMap.get(keyC).getLayoutY();
            Text txt = new Text(nextX, nextY, incomingTextNodeMap.get(keyC).getText());
            txt.setFont(incomingTextNodeMap.get(keyC).getFont());
            txt.setFill(incomingTextNodeMap.get(keyC).getFill());
            outgoingTextNodeMap.put(keyC, txt);
        }
        return outgoingTextNodeMap;
    }

    private String parseAtkCountry(String atkLine) {
        return atkLine.split("\\) from ")[1].split("\\(")[0];
    }

    private String parseDfdCountry(String atkLine) {
        return atkLine.split(" is attacking ")[1].split("\\(")[0];
    }

    private int parseAtkLosses(String lossLine) {
        String temp = lossLine.split("Attacker lost: ")[1];
        return Integer.parseInt(temp.substring(0, temp.indexOf(';')));
    }

    private int parseDfdLosses(String lossLine) {
        String temp = lossLine.split("Defender lost: ")[1];
        return Integer.parseInt(temp.substring(0));
    }

    private String parseTakenCountry(String takenLine) {
        return takenLine.split(" has taken ")[1].split(" from")[0];
    }

    //for string similar to...
    //Hard 3 advanced 3 into Northern Europe from Southern Europe.
    private String parseAdvanceSourceCountry(String advLine) {
        String temp = advLine.split(" from ")[1];
        return temp.substring(0, temp.length() - 1);
    }

    private String parseAdvanceDestinationCountry(String advLine) {
        String temp = advLine.split(" into ")[1];
        return temp.split(" from ")[0];
    }

    private int parseAdvanceArmies(String advLine) {
        String temp = advLine.split(" advanced ")[1];
        return Integer.parseInt(temp.substring(0, temp.indexOf(' ')));
    }

    private int parseFortifyArmies(String fortifyLine) {
        return Integer.parseInt(fortifyLine.split(" is transferring ")[1].split(" from ")[0]);
    }

    private String parseFortifySourceCountry(String fortifyLine) {
        return fortifyLine.split(" from ")[1].split(" to ")[0];
    }

    private String parseFortifyDestinationCountry(String fortifyLine) {
        String temp = fortifyLine.split(" to ")[1];
        return temp.substring(0, temp.length() - 1);
    }

    public static void main(String[] args) {
        launch(LogPlayer.class, args);
    }
}
