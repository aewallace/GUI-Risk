package Util;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import Map.Country;
import Map.RiskMap;
import Player.Player;

public class PersistentSettings {
	private static final String DEFAULT_CHKPNT_FILE_NAME = "fxuigm_save.ser";
	private static String loadfrom_filename = DEFAULT_CHKPNT_FILE_NAME;
	private static String saveto_filename = DEFAULT_CHKPNT_FILE_NAME;
	private static final long AUTO_CLOSE_TIMEOUT = 4500;
	private static final int DEFAULT_APP_WIDTH = 1600;
	private static final int DEFAULT_APP_HEIGHT = 1062;
	public static final int DEFAULT_DIALOG_OFFSET = 300;
	private static final int IDLE_MODE = 0, NEW_GAME_MODE = 1, LOADED_GAME_MODE = 2;
	private static int workingMode = IDLE_MODE;
	protected static final String LOGFILE = "LOG.txt";
	protected static final String STATSFILE = "STATS.txt";
	protected static final String EVENT_DELIM = "...";
	protected static final boolean LOGGING_OFF = false;
	protected static final boolean LOGGING_ON = true;
	protected static boolean forceEnableLogging = false, forceDisableLogging = false;
	protected static boolean loggingEnabled = true; //this is the one that has the final say as to whether the log file is created
	private static FXUI_Crossbar crossbar = new FXUI_Crossbar();
	protected RiskMap map;
	protected Deque<Card> deck;
	protected static String desiredPlayersForGame = null;
	protected List<String> players;
	protected Map<String, Player> playerMap;
	protected Map<String, Collection<Card>> playerCardMap;
	
	protected static RiskMap starterMap = null;
	protected static Random rand;
	protected static int allocationIdx = 0;
	
	protected FileWriter log, stats;
	protected ArrayList<String> internalLogCache = new ArrayList<String>();
	protected List<String> allPlayers;
	protected int round, turnCount;
	
	private int numGames = 1;
	
	//to handle recovering a prior session or help with launching a new game session
	private SavePoint savePoint = new SavePoint();
	private SavePoint loadedSaveIn = null;
	private static String loadFailureReason = "";
	private HashMap<String, Country> stringCountryRepresentation = new HashMap<String, Country>();
	private ArrayList<Node> buttonCache = new ArrayList<Node>();
	private static boolean initiationGood = false;
	private static boolean endGame = false;
	private static Player currentPlayer = null;
	private boolean updateUI = false;
	private static boolean exitDialogIsShowing = true;
	private static boolean skipExitConfirmation = false;
	private static Thread priGameLogicThread = null;
	private static boolean loadTypeState = false;

	public PersistentSettings() {
		// TODO Auto-generated constructor stub
	}

}
