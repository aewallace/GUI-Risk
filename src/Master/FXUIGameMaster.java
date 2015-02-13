//Current build Albert Wallace, Version 001, Stamp y2015.mdB12.hm2300.sMNT
//Base build by Seth Denney, Sept 10 2014 

package Master;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import LogPlayer.LogPlayer;
import Map.Continent;
import Map.Country;
import Map.RiskMap;
import Player.EasyDefaultPlayer;
import Player.FXUIPlayer;
import Player.HardDefaultPlayer;
import Player.NormalDefaultPlayer;
import Player.Player;
import Player.Seth;
import Response.AdvanceResponse;
import Response.AttackResponse;
import Response.CardTurnInResponse;
import Response.DefendResponse;
import Response.FortifyResponse;
import Response.ReinforcementResponse;
import Util.Card;
import Util.DiceRoller;
import Util.PlayerEliminatedException;
import Util.RiskConstants;
import Util.RiskUtils;
import Util.RollOutcome;

public class FXUIGameMaster extends Application {
	private static final int DEFAULT_APP_WIDTH = 1600;
	private static final int DEFAULT_APP_HEIGHT = 1062;
	protected static final String LOGFILE = "LOG.txt";
	protected static final String STATSFILE = "STATS.txt";
	protected static final String EVENT_DELIM = "...";
	protected static final boolean LOGGING_OFF = false;
	protected static final boolean LOGGING_ON = true;
	protected static final String FXUI_PLAYER_NAME = "FXUIPlayer";
	protected RiskMap map;
	protected Deque<Card> deck;
	protected List<String> players;
	protected Map<String, Player> playerMap;
	protected Map<String, Collection<Card>> playerCardMap;
	
	protected static RiskMap starterMap = null;
	protected static Random rand;
	protected static int allocationIdx = 0;
	
	protected FileWriter log, stats;
	protected List<String> allPlayers;
	protected int round, turnCount;
	
	private ScrollPane scrollPane;
    private Scene scene;
    private Pane pane;
    private Text eventTitle;
    private Text roundText;
    private Text turn;
    private Text nextLogLine;
    private Text errorDisplay;
    private Text currentPlayStatus;
    private String errorText;
    private boolean errorDisplayBit;
    private HashMap<String, Text> textNodeMap;
    private Map<String, Color> playerColorMap;
    
    private Scanner logScanner;
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
    private int iRoN; //todo: fix bad name
    private int busyRoutines; //to perform basic resource locks
    private static int routinesRequestingPriority;
    private HashMap<Long,Thread> threadMap;
	

	public void pseudoFXUIGameMaster(String mapFile, String playerFile, boolean logSwitch) throws IOException {
		System.out.println("E G U 4 1 8 S");
		this.round = 0;
		this.turnCount = 0;
		if (rand == null) {
			rand = new Random(RiskConstants.SEED);
		}
		System.out.println("E G U 4 6 5 S");
		if (logSwitch == LOGGING_ON) {
			this.log = new FileWriter(LOGFILE);
			this.stats = new FileWriter(STATSFILE);
		}
		
		System.out.println("E T U 4 6 5 Q");
		writeLogLn("Loading map from " + mapFile + "...");
		if (starterMap == null) {
			starterMap = new RiskMap();
		}
		
		System.out.println("E M G 4 6 9 M");
		this.map = starterMap.getCopy();
		loadDeck();
		if (!loadPlayers(playerFile)) {
			System.out.println("Invalid number of players. 2-6 Players allowed.");
		}
		
		System.out.println("E M N 4 8 5 M");
		allocateMap();
		
		System.out.println("E M U 4 6 5 M");
	}
	
	public String begin() {
		if (initializeForces()) {
			//play round-robin until there is only one player left
			int turn = 0;
			while (this.players.size() > 1) {
				if (turn == 0) {
					this.round++;
					writeLogLn("Beginning Round " + round + "!");
					if (this.round > RiskConstants.MAX_ROUNDS) {
						return "Stalemate!";
					}
				}
				Player currentPlayer = this.playerMap.get(this.players.get(turn));
				writeLogLn(currentPlayer.getName() + " is starting their turn.");
				writeStatsLn();
				this.turnCount++;
				try {
					reinforce(currentPlayer, true);
					attack(currentPlayer);
					fortify(currentPlayer);
					turn = (this.players.indexOf(currentPlayer.getName()) + 1) % this.players.size();
				}
				catch (PlayerEliminatedException e) {
					//If an elimination exception is thrown up to this level,
					//then it was currentPlayer who was eliminated.
					turn %= this.players.size();
				}
			}

			writeStatsLn();
			System.out.println(this.players.get(0) + " is the victor!");
			writeLogLn(this.players.get(0) + " is the victor!");
		}
		try {
			if (this.log != null && this.stats != null) {
				log.close();
				stats.close();
			}
		}
		catch (IOException e) {
		}
		return this.players.get(0);
	}
	
	protected boolean initializeForces() {
		boolean valid;
		int attempts;
		int playerIndex = 0;
		//get initial troop allocation
		while (playerIndex < this.players.size()) {
			Player player = this.playerMap.get(this.players.get(playerIndex));
			writeLogLn("Getting initial troop allocation from " + player.getName() + "...");
			int reinforcements;
			valid = false;
			attempts = 0;
			while (!valid && attempts < RiskConstants.MAX_ATTEMPTS) {
				attempts++;
				reinforcements = RiskConstants.INIT_ARMIES / this.players.size();
				ReinforcementResponse rsp = tryInitialAllocation(player, reinforcements);
				if (valid = ReinforcementResponse.isValidResponse(rsp, this.map, player.getName(), reinforcements)
						&& validateInitialAllocation(rsp.getAllocation(), player.getName(), reinforcements)) {
					allocateArmies(player.getName(), rsp.getAllocation(), reinforcements);
					playerIndex++;
					writeLogLn("Troops successfully allocated for " + player.getName() + "...");
				}
			}
			
			if (!valid) {
				try {
					eliminate(player, null, "You failed to provide a valid initial army allocation.");
				}
				catch (PlayerEliminatedException e) {
					playerIndex = 0;
				}
			}
		}
		
		return this.players.size() > 0;
	}
	
	protected void validatePlayerName(Player player) throws PlayerEliminatedException {
		if (!(this.playerMap.containsKey(player.getName()) && this.playerMap.get(player.getName()) == player)) {
			eliminate(player, null, "Players who hide their true identity are not welcome here. BEGONE!");
		}
	}
	
	protected void reinforce(Player currentPlayer, boolean withCountryBonus) throws PlayerEliminatedException {
		int reinforcements = 0;
		int attempts = 0;
		boolean valid = false;
		reinforcements += getCardTurnIn(currentPlayer, getPlayerCardCounts());
		Map<String, Integer> oppCards = getPlayerCardCounts();
		if (withCountryBonus) {
			reinforcements += RiskUtils.calculateReinforcements(this.map, currentPlayer.getName());
		}
		writeLogLn(currentPlayer.getName() + " reinforcing with " + reinforcements + " armies.");
		while (!valid && attempts < RiskConstants.MAX_ATTEMPTS) {
			attempts++;
			ReinforcementResponse rsp = tryReinforce(currentPlayer, oppCards, reinforcements);
			if (valid = ReinforcementResponse.isValidResponse(rsp, this.map, currentPlayer.getName(), reinforcements)) {
				for (Map.Entry<Country, Integer> entry : rsp.getAllocation().entrySet()) {
					this.map.addCountryArmies(entry.getKey(), entry.getValue());
					writeLogLn(entry.getValue() + " " + entry.getKey().getName());
				}
			}
		}
		if (!valid) {
			eliminate(currentPlayer, null, "You failed to provide a valid reinforcement allocation.");
		}
		writeLogLn(EVENT_DELIM);
	}
	
	protected void attack(Player currentPlayer) throws PlayerEliminatedException {
		int attempts = 0;
		boolean resetTurn;
		boolean hasGottenCard = false;
		while (attempts < RiskConstants.MAX_ATTEMPTS) {
			attempts++;
			resetTurn = false;
			AttackResponse atkRsp = tryAttack(currentPlayer, createCardSetCopy(currentPlayer.getName()), getPlayerCardCounts());
			if (atkRsp != null) {
				if (AttackResponse.isValidResponse(atkRsp, this.map, currentPlayer.getName())) {
					writeLogLn(currentPlayer.getName() + " is attacking "
							+ atkRsp.getDfdCountry() + "(" + this.map.getCountryArmies(atkRsp.getDfdCountry())
							+ ") from " + atkRsp.getAtkCountry() + "(" + this.map.getCountryArmies(atkRsp.getAtkCountry()) + ")!");
					attempts = 0;
					Player defender = getOwnerObject(atkRsp.getDfdCountry());
					DefendResponse dfdRsp = null;
					try {
						//this is guaranteed to either be valid or throw a PlayerEliminatedException
						dfdRsp = defend(defender, getPlayerCardCounts(), atkRsp);
					}
					catch (PlayerEliminatedException e) {
						//defender messed up and was auto-eliminated
						resetTurn = true;
					}
					if (!resetTurn) {
						carryOutAttack(atkRsp, dfdRsp);
						hasGottenCard = checkForTakeover(currentPlayer, atkRsp, hasGottenCard) || hasGottenCard;
					}
				}
			}
			else {
				//because an attack is not required, a null response is taken to mean that the player declines the opportunity
				attempts = RiskConstants.MAX_ATTEMPTS;
			}
		}
	}
	
	protected DefendResponse defend(Player defender, Map<String, Integer> oppCards, AttackResponse atkRsp) throws PlayerEliminatedException {
		int attempts = 0;
		boolean valid = false;
		DefendResponse rsp = null;
		while (!valid && attempts < RiskConstants.MAX_ATTEMPTS) {
			attempts++;
			rsp = tryDefend(defender, createCardSetCopy(defender.getName()), oppCards, new AttackResponse(atkRsp));
			valid = DefendResponse.isValidResponse(rsp, this.map, atkRsp.getDfdCountry());
		}
		if (!valid) {
			eliminate(defender, null, "You failed to provide a valid defense response.");
		}
		return rsp;
	}
	
	protected void carryOutAttack(AttackResponse atk, DefendResponse dfd) {
		RollOutcome result = DiceRoller.roll(atk.getNumDice(), dfd.getNumDice());
		this.map.addCountryArmies(atk.getAtkCountry(), -1 * result.getAtkLosses());
		this.map.addCountryArmies(atk.getDfdCountry(), -1 * result.getDfdLosses());
		writeLogLn("\tAttacker lost: " + result.getAtkLosses() + "; Defender lost: " + result.getDfdLosses());
	}
	
	protected boolean checkForTakeover(Player attacker, AttackResponse atkRsp, boolean hasGottenCard) throws PlayerEliminatedException {
		if (this.map.getCountryArmies(atkRsp.getDfdCountry()) == 0) {
			String loserName = this.map.getCountryOwner(atkRsp.getDfdCountry());
			writeLogLn(attacker.getName() + " has taken " + atkRsp.getDfdCountry() + " from " + loserName + "!");
			this.map.setCountryOwner(atkRsp.getDfdCountry(), attacker.getName());
			if (!hasGottenCard) {
				awardCard(attacker.getName());
			}
			boolean allowReinforce = false;
			try {
				advanceArmies(attacker, atkRsp);
				allowReinforce = true;
			}
			catch (PlayerEliminatedException attackerException) {
				throw attackerException;
			}
			finally {
				checkForElimination(attacker, loserName, atkRsp.getDfdCountry(), allowReinforce);
			}
			return true;
		}
		else {
			return false;
		}
	}
	
	protected void advanceArmies(Player attacker, AttackResponse atkRsp) throws PlayerEliminatedException {
		int attempts = 0;
		boolean valid = false;
		while (!valid && attempts < RiskConstants.MAX_ATTEMPTS) {
			attempts++;
			AdvanceResponse advRsp = tryAdvance(attacker, createCardSetCopy(attacker.getName()), getPlayerCardCounts(), atkRsp);
			if (valid = AdvanceResponse.isValidResponse(advRsp, atkRsp, this.map)) {
				writeLogLn(attacker.getName() + " advanced " + advRsp.getNumArmies() + " into " + atkRsp.getDfdCountry() + " from " + atkRsp.getAtkCountry() + ".");
				this.map.addCountryArmies(atkRsp.getAtkCountry(), -1 * advRsp.getNumArmies());
				this.map.addCountryArmies(atkRsp.getDfdCountry(), advRsp.getNumArmies());
			}
		}
		if (!valid) {
			eliminate(attacker, null, "You failed to provide a valid advance response.");
		}
	}
	
	protected void awardCard(String playerName) {
		writeLogLn("Awarding " + playerName + " one card.");
		if (this.deck.size() > 0) {
			this.playerCardMap.get(playerName).add(this.deck.removeFirst());
		}
	}
	
	protected void checkForElimination(Player attacker, String loserName, Country takenCountry, boolean allowReinforce) throws PlayerEliminatedException {
		try {
			if (RiskUtils.getPlayerCountries(this.map, loserName).size() == 0) {
				eliminate(getPlayerObject(loserName), attacker, "You were eliminated by " + attacker.getName() + " at " + takenCountry.getName() + ".");
			}
		}
		catch (PlayerEliminatedException defenderException) {
			//this ensures that attacker will not be allowed to reinforce if (s)he was auto-eliminated during the advanceArmies() call.
			if (allowReinforce && this.players.size() > 1) {
				reinforce(attacker, false);//note that if the current player fails to reinforce, the player can be eliminated here and an exception thrown back up to begin()
			}
		}
	}
	
	protected void fortify(Player currentPlayer) {
		int attempts = 0;
		boolean valid = false;
		while (!valid && attempts < RiskConstants.MAX_ATTEMPTS) {
			attempts++;
			FortifyResponse rsp = tryFortify(currentPlayer, createCardSetCopy(currentPlayer.getName()), getPlayerCardCounts());
			if (rsp != null) {
				if (valid = FortifyResponse.isValidResponse(rsp, this.map, currentPlayer.getName())) {
					writeLogLn(currentPlayer.getName() + " is transferring " + rsp.getNumArmies() + " from " + rsp.getFromCountry() + " to " + rsp.getToCountry() + ".");
					this.map.addCountryArmies(rsp.getFromCountry(), -1 * rsp.getNumArmies());
					this.map.addCountryArmies(rsp.getToCountry(), rsp.getNumArmies());
				}
			}
			else {
				//because fortification is not required, a null response is taken to mean that the player declines the opportunity
				attempts = RiskConstants.MAX_ATTEMPTS;
			}
		}
	}
	
	protected ReinforcementResponse tryInitialAllocation(Player player, int reinforcements) {
		try {
			if(player.getName() != FXUI_PLAYER_NAME){ //if a CPU player
				ReinforcementResponse rsp = player.getInitialAllocation(this.map.getReadOnlyCopy(), reinforcements);
				validatePlayerName(player);
				return rsp;
			}
			else{ //if not a CPU player, aka if a human player
				FXUIPlayer pIn =(FXUIPlayer)player;
				ReinforcementResponse rsp = pIn.getInitialAllocation(this.map.getReadOnlyCopy(), reinforcements, pane.getScene().getWindow());
				validatePlayerName(player);
				return rsp;
			}
			
		}
		catch (Exception e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	protected CardTurnInResponse tryTurnIn(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards, boolean turnInRequired) {
		try {
			CardTurnInResponse rsp = player.proposeTurnIn(this.map.getReadOnlyCopy(), cardSet, oppCards, turnInRequired);
			validatePlayerName(player);
			return rsp;
		}
		catch (Exception e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	protected ReinforcementResponse tryReinforce(Player player, Map<String, Integer> oppCards, int reinforcements) {
		ReinforcementResponse rsp;
		try {
			rsp = player.reinforce(this.map.getReadOnlyCopy(), createCardSetCopy(player.getName()), oppCards, reinforcements);
			validatePlayerName(player);
			return rsp;
		}
		catch (Exception e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	protected AttackResponse tryAttack(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards) {
		try {
			AttackResponse rsp = player.attack(this.map.getReadOnlyCopy(), createCardSetCopy(player.getName()), oppCards);
			validatePlayerName(player);
			return rsp;
		}
		catch (Exception e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	protected DefendResponse tryDefend(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards, AttackResponse atkRsp) {
		try {
			DefendResponse rsp = player.defend(this.map.getReadOnlyCopy(), createCardSetCopy(player.getName()), oppCards, atkRsp.getAtkCountry(), atkRsp.getDfdCountry(), atkRsp.getNumDice());
			validatePlayerName(player);
			return rsp;
		}
		catch (Exception e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	protected AdvanceResponse tryAdvance(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards, AttackResponse atkRsp) {
		try {
			AdvanceResponse rsp = player.advance(this.map.getReadOnlyCopy(), createCardSetCopy(player.getName()), oppCards, atkRsp.getAtkCountry(), atkRsp.getDfdCountry(), atkRsp.getNumDice());
			validatePlayerName(player);
			return rsp;
		}
		catch (Exception e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	protected FortifyResponse tryFortify(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards) {
		try {
			FortifyResponse rsp = player.fortify(this.map.getReadOnlyCopy(), createCardSetCopy(player.getName()), oppCards);
			validatePlayerName(player);
			return rsp;
		}
		catch (Exception e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	protected boolean validateInitialAllocation(Map<Country, Integer> allocation, String playerName, int armies) {
		Map<Country, Boolean> allocatedCheck = new HashMap<Country, Boolean>();
		for (Country country : RiskUtils.getPlayerCountries(this.map, playerName)) {
			allocatedCheck.put(country, false);
		}
		for (Country country : allocation.keySet()) {
			if (!allocatedCheck.containsKey(country)) {
				return false;
			}
			else if (allocation.get(country) < 1) {
				return false;
			}
			else {
				allocatedCheck.put(country, true);
				armies -= allocation.get(country);
			}
		}
		for (Boolean check : allocatedCheck.values()) {
			if (!check) {
				return false;
			}
		}
		return armies == 0;
	}
	
	protected void allocateArmies(String playerName, Map<Country, Integer> allocation, int reinforcements) {
		writeLogLn(playerName + " reinforcing with " + reinforcements + " armies.");
		for (Map.Entry<Country, Integer> entry : allocation.entrySet()) {
			this.map.setCountryArmies(entry.getKey(), entry.getValue());
			writeLogLn(entry.getValue() + " " + entry.getKey().getName());
		}
		writeLogLn(EVENT_DELIM);
	}
	
	protected int getCardTurnIn(Player currentPlayer, Map<String, Integer> oppCards) throws PlayerEliminatedException {
		int cardBonus = 0;
		int attempts = 0;
		boolean valid = false;
		boolean turnInRequired = oppCards.get(currentPlayer.getName()) >= RiskConstants.FORCE_TURN_IN;
		while (!valid && attempts < RiskConstants.MAX_ATTEMPTS) {
			CardTurnInResponse rsp = tryTurnIn(currentPlayer, createCardSetCopy(currentPlayer.getName()), oppCards, turnInRequired);
			if (rsp != null) {
				if (valid = CardTurnInResponse.isValidResponse(rsp, this.playerCardMap.get(currentPlayer.getName()))) {
					cardBonus = RiskConstants.advanceTurnIn();
					writeLogLn(currentPlayer.getName() + " turned in cards for " + cardBonus + " additional reinforcements!");
					if (rsp.getBonusCountry() != null) {
						if (this.map.getCountryOwner(rsp.getBonusCountry()).equals(currentPlayer.getName())) {
							this.map.addCountryArmies(rsp.getBonusCountry(), RiskConstants.BONUS_COUNTRY_ARMIES);
						}
					}
					for (Card card : rsp.getCards()) {
						this.playerCardMap.get(currentPlayer.getName()).remove(card);
						this.deck.addLast(card);
					}
				}
			}
			else {
				//if a turn-in is not required, a null response is taken as the player declining
				valid = !turnInRequired;
			}
			attempts++;
		}
		if (!valid && turnInRequired) {
			eliminate(currentPlayer, null, "You were required to turn in cards this turn, and you failed to do so.");
		}
		return cardBonus;
	}
	
	protected Collection<Card> createCardSetCopy(String playerName) {
		Collection<Card> copy = new ArrayList<Card>();
		for (Card card : this.playerCardMap.get(playerName)) {
			copy.add(new Card(card.getType(), card.getCountry()));
		}
		return copy;
	}
	
	protected Player getOwnerObject(Country country) {
		String playerName = this.map.getCountryOwner(country);
		return getPlayerObject(playerName);
	}
	
	protected Player getPlayerObject(String playerName) {
		for (Player player : this.playerMap.values()) {
			if (player.getName().equals(playerName)) {
				return player;
			}
		}
		return null;
	}
	
	protected Map<String, Integer> getPlayerCardCounts() {
		Map<String, Integer> playerCardCounts = new HashMap<String, Integer>();
		for (String playerName : this.playerMap.keySet()) {
			playerCardCounts.put(playerName, this.playerCardMap.get(playerName).size());
		}
		return playerCardCounts;
	}
	
	protected void loadDeck() {
		writeLogLn("Building deck...");
		List<Card> newDeck = new ArrayList<Card>();
		int i = 0;
		for (Country country : Country.values()) {
			newDeck.add(new Card(RiskConstants.REG_CARD_TYPES[i % RiskConstants.REG_CARD_TYPES.length], country));
			i++;
		}
		for (i = 0; i < RiskConstants.NUM_WILD_CARDS; i++) {
			newDeck.add(new Card(RiskConstants.WILD_CARD, null));
		}
		shuffleCards(newDeck);
		this.deck = new LinkedList<Card>(newDeck);
	}
	
	protected void shuffleCards(List<Card> cardList) {
		int j;
		Card temp;
		for (int i = 0; i < 2 * cardList.size(); i++) {
			j = rand.nextInt(cardList.size());
			temp = cardList.get(i % cardList.size());
			cardList.set(i % cardList.size(), cardList.get(j));
			cardList.set(j, temp);
		}
	}
	
	protected boolean loadPlayers(String playerFile) {
		writeLogLn("Loading players...");
		this.playerMap = new HashMap<String, Player>();
		this.allPlayers = new ArrayList<String>();
		
		//this.playerMap.put("Easy 1", new EasyDefaultPlayer("Easy 1"));
		//this.allPlayers.add("Easy 1");
		
		this.playerMap.put("Normal 2", new NormalDefaultPlayer("Normal 2"));
		this.allPlayers.add("Normal 2");
		
		this.playerMap.put("Hard 3", new HardDefaultPlayer("Hard 3"));
		this.allPlayers.add("Hard 3");
		
		this.playerMap.put("Hard 4", new HardDefaultPlayer("Hard 4"));
		this.allPlayers.add("Hard 4");
		
		this.playerMap.put("Hard 5", new HardDefaultPlayer("Hard 5"));
		this.allPlayers.add("Hard 5");
		
		this.playerMap.put("Seth 1", new Seth("Seth 1"));
		this.allPlayers.add("Seth 1");
		
		this.playerMap.put("FXUIPlayer", new FXUIPlayer("FXUIPlayer"));
		this.allPlayers.add("FXUIPlayer");
//		
//		this.playerMap.put("Seth 3", new Seth("Seth 3"));
//		this.allPlayers.add("Seth 3");
//		
//		this.playerMap.put("Seth 4", new Seth("Seth 4"));
//		this.allPlayers.add("Seth 4");
//		
//		this.playerMap.put("Seth 5", new Seth("Seth 5"));
//		this.allPlayers.add("Seth 5");
//		
//		this.playerMap.put("Seth 6", new Seth("Seth 6"));
//		this.allPlayers.add("Seth 6");
		
		this.players = new ArrayList<String>(this.allPlayers);
		shufflePlayers(this.players);//choose a random turn order
		this.playerCardMap = new HashMap<String, Collection<Card>>();
		for (Player player : this.playerMap.values()) {
			this.playerCardMap.put(player.getName(), new ArrayList<Card>());
		}
		if (this.players.size() < RiskConstants.MIN_PLAYERS || this.players.size() > RiskConstants.MAX_PLAYERS) {
			return false;
		}
		else {
			writeLogLn("Players:");
			for (String playerName : this.players) {
				writeLogLn(playerName);
			}
			writeLogLn(EVENT_DELIM);
			return true;
		}
	}
	
	protected void shufflePlayers(List<String> playerList) {
		int j;
		String temp;
		for (int i = 0; i < 2 * playerList.size(); i++) {
			j = rand.nextInt(playerList.size());
			temp = playerList.get(i % playerList.size());
			playerList.set(i % playerList.size(), playerList.get(j));
			playerList.set(j, temp);
		}
	}
	
	//only allocates unowned countries
	protected void allocateUnownedCountries() {
		if (this.players.size() > 0) {
			writeLogLn("Re-allocating eliminated player's countries...");
			for (Country country : Country.values()) {
				if (map.getCountryOwner(country) == null) {
					map.setCountryOwner(country, this.playerMap.get(this.players.get(allocationIdx % this.players.size())).getName());
					if (this.round > 0) {
						//If these countries are being eliminated during a game,
						//it is due to a player being eliminated by the Master,
						//and so the re-allocated countries must be occupied.
						map.setCountryArmies(country, 1);
					}
					allocationIdx++;
				}
			}
		}
	}
	
	//allocates ALL countries on map
	protected void allocateMap() {
		if (this.players.size() > 0) {
			writeLogLn("Allocating countries...");
			for (Card card : this.deck) {
				System.out.println("E S M - 0 8 P");
				System.out.println(card.getType());
				System.out.println(card.getCountry() + " + " + allocationIdx + "+" + this.players.size() + "--" + allocationIdx % this.players.size());
				if(this.playerMap == null){ System.out.println("E S M - 0 8 P M 1");}
				if(this.players == null){ System.out.println("E S M - 0 8 P M 3");}
				if(this.playerMap.get(this.players.get(allocationIdx % this.players.size())) == null){ System.out.println("E S M - 0 8 P X -");}
				if (!card.getType().equals(RiskConstants.WILD_CARD)) {
					try{
					map.setCountryOwner(card.getCountry(), this.playerMap.get(this.players.get(allocationIdx % this.players.size())).getName());
					System.out.println("E G X N 0 0" + this.playerMap.get(this.players.get(allocationIdx % this.players.size())).getName());
					allocationIdx++;
					}
					catch(Exception e)
					{
						System.out.println(e);
					}
				}
			}
		}
	}
	
	protected void eliminate(Player loser, Player eliminator, String reason) throws PlayerEliminatedException {
		if (this.playerMap.containsKey(loser.getName())) {
			writeLogLn(loser.getName() + " Eliminated! " + reason);
			for (Country country : Country.values()) {
				if (map.getCountryOwner(country).equals(loser.getName())) {
					if (eliminator != null) {
						map.setCountryOwner(country, eliminator.getName());
					}
					else {
						map.setCountryOwner(country, null);
					}
				}
			}
			if (eliminator != null) {
				for (Card card : this.playerCardMap.get(loser.getName())) {
					this.playerCardMap.get(eliminator.getName()).add(card);
				}
				this.playerCardMap.get(loser.getName()).clear();
			}
			this.players.remove(loser.getName());
			this.playerMap.remove(loser.getName());
			allocateUnownedCountries();
			throw new PlayerEliminatedException(loser.getName() + " Eliminated! " + reason);
		}
	}
	
	protected void writeLogLn(String line) {
		if (this.log != null) {
			try {
				this.log.write(line + "\r\n");
				this.log.flush();
			}
			catch (IOException e) {
			}
		}
	}
	
	protected void writeStatsLn() {
		if (this.stats != null) {
			try {
				stats.write(this.turnCount + " " + this.round + " ");
				for (String playerName : this.allPlayers) {
					//count player's countries
					stats.write(RiskUtils.getPlayerCountries(this.map, playerName).size() + " ");
					//count player's armies
					stats.write(RiskUtils.countPlayerArmies(this.map, playerName, null) + " ");
				}
				stats.write("\r\n");
				this.stats.flush();
			}
			catch (IOException e) {
			}
		}
	}
	
	private void representPlayersOnUI() {
		//requires loadPlayers to have been run
    	try {
			ArrayList<Color> colors = new ArrayList<Color>();
			colors.add(Color.WHITE);
			colors.add(Color.AQUA);
			colors.add(Color.RED);
			colors.add(Color.GREENYELLOW);
			colors.add(Color.CORAL);
			colors.add(Color.VIOLET);
			this.playerColorMap = new HashMap<String, Color>();
			int i = -1;
			//this.allPlayers
			for (String playerName : this.allPlayers)
			{
				this.playerColorMap.put(playerName, colors.get(++i % colors.size()));
				Text txt = new Text(200 * (i) + 50, 20, playerName);
				txt.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
				txt.setFill(colors.get((i) % colors.size()));
				this.pane.getChildren().add(txt);
			}
		}
		catch (Exception e) {
		}
    }
	
	
	public static void main(String[] args) throws IOException {
		launch(FXUIGameMaster.class, args);
	}
	
	public void pseudoMain(){
	try {
		System.out.println("E M U 7 6 5 8");
			HashMap<String, Integer> winLog = new HashMap<String, Integer>();
			int numGames = 1;
			RiskConstants.SEED = 1;
			for (int i = 0; i < numGames; i++) {
				RiskConstants.resetTurnIn();
				System.out.println("E M U 7 6 5 6");
				pseudoFXUIGameMaster("Countries.txt", null, i == numGames - 1 ? LOGGING_ON : LOGGING_OFF);
				System.out.println("E M U 7 6 5 4");
				System.out.print((i + 1) + " - ");
				String victor = begin();
				if (!winLog.containsKey(victor)) {
					winLog.put(victor, 0);
				}
				winLog.put(victor, winLog.get(victor) + 1);
			}
			for (Map.Entry<String, Integer> entry : winLog.entrySet()) {
				System.out.println(entry.getKey() + " had a win percentage of " + 100.0 * entry.getValue() / numGames + "%");
			}
		}
		catch (IOException e) {
			System.out.println(e);
		}
	}
	
	private void loadTextNodesForUI(String nodeFile) {
		try {
			if (nodeFile != null) {
				this.textNodeMap = new HashMap<String, Text>();
				File fileRepresentation = new File(nodeFile);
						//basic check for existence of country list file
				if (!fileRepresentation.exists()){
					System.out.print("Warning: no known list of countries found!");
					System.out.print("\nExpected: \"" + nodeFile + "\"\n");
					System.out.print("Undefined behavior WILL occur!");
					errorDisplayBit = true;
					errorText = "File not found in working directory;\nsee console for details.";
				}
						//and basic check for valid file contents
				else if (fileRepresentation.length() < 25){
					System.out.print("Warning: malform input file detected!");
					System.out.print("\nExpected \"" + nodeFile + "\" to be of a certain size.\n");
					System.out.print("Please check the file and restart the LogyPlayer GUI.\n");
					errorDisplayBit = true;
					errorText = "Malformed input file detected;\nsee console for details.";
				}
				else{
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
		}
		catch (Exception e) {
			//errorDisplay.setText(e.getMessage());
		}
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		// TODO fix copy/pasted stub for FXUIGM use
		//pseudoMain();
		
		try{
			pane = new Pane();
	        pane.setPrefSize(DEFAULT_APP_WIDTH + 200, DEFAULT_APP_HEIGHT + 30);
	        /*pane.setStyle....
	        * we set the image in the pane based on whether there was an error or not.
	        *  for reference, please see later in the start() method
	        * it will be similar to...
	        * pane.setStyle("-fx-background-image: url(\"RiskBoard.jpg\")");*/
	       
	        errorDisplayBit = false;
	        errorText = "Status...";
	        
	        loadTextNodesForUI("TextNodes.txt");
	        representPlayersOnUI();
	        
	        
	        //if there is an error on loading necessary resources,
	        // render the "negated" map image as a visual cue to indicate failure
	    	pane.setStyle("-fx-background-image: url(\"RiskBoardAE.jpg\")");
	        errorDisplay = new Text(29, 560, errorText);
	        errorDisplay.setFont(Font.font("Verdana", FontWeight.BOLD, 24));
	        if(errorDisplayBit){errorDisplay.setFill(Color.RED);}
	        else{errorDisplay.setFill(Color.WHITE);}
	        	
	        pane.getChildren().add(errorDisplay);
	        
	        //if there was no error, populate the window with appropriate elements
	        if(!errorDisplayBit){ 
	        	pane.setStyle("-fx-background-image: url(\"RiskBoard.jpg\")");
	        	eventTitle = new Text(1350, 515, "Initial Reinforcement\nStage");
		        eventTitle.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
		        eventTitle.setFill(Color.LIGHTGRAY);
		        pane.getChildren().add(eventTitle);
		        
		        roundText = new Text(1460, 450, "");
		        roundText.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
		        roundText.setFill(Color.LIGHTGRAY);
		        pane.getChildren().add(roundText);
		        
		        turn = new Text(1425, 470, "");
		        turn.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
		        turn.setFill(Color.LIGHTGRAY);
		        pane.getChildren().add(turn);
		        
		        nextLogLine = new Text(600, 1030, "");
		        nextLogLine.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
		        nextLogLine.setFill(Color.LIGHTGRAY);
		        pane.getChildren().add(nextLogLine);
		        
		        currentPlayStatus = new Text(29, 600, "Hello! ^.^");
		        currentPlayStatus.setFont(Font.font("Verdana", FontWeight.BOLD, 40));
		        currentPlayStatus.setFill(Color.WHITE);
		        pane.getChildren().add(currentPlayStatus);
		        
		        
	        	
		       //The original single-seek/step-through "Next Event" button 
		        Button nextActionBtn = new Button("Trigger sample event window");
		        nextActionBtn.setLayoutX(29);
		        nextActionBtn.setLayoutY(770);
		        nextActionBtn.setOnAction(new EventHandler<ActionEvent>() {
		        	@Override
		        	public void handle(ActionEvent event) {
		        		Platform.runLater(new Runnable() {
		                    @Override public void run() {
					        			  FXUIPlayer testFXUIPlayer = new FXUIPlayer();
					        			  testFXUIPlayer.testRequiredInputPrompt(pane.getScene().getWindow());
					        			  //java.lang.Thread.sleep(1000);
					        			  //runButtonRunnable(STEP_FWD, cancelActiveActions;
		                    	}
		        		} );
		        	}
		        });
		        pane.getChildren().add(nextActionBtn);
		        
		        
		      //The Play-Forward (normal speed) Button
		        Button pauseAllBtn = new Button("Pause Event Playback");
		        pauseAllBtn.setLayoutX(29);
		        pauseAllBtn.setLayoutY(650);
		        
		        pauseAllBtn.setOnAction(new EventHandler<ActionEvent>() {
		        	@Override
		        	public void handle(ActionEvent event) {
				        Runnable task = new Runnable() {
				        	  @Override public void run() {
				        		  try
				        		  {
				        			  //java.lang.Thread.sleep(1000);
				        			  //runButtonRunnable(PAUSE, cancelActiveActions);
				        			 
				        		  }//end try
				        		  catch(Exception e)
				        		  {	
				        		  } //end catch	
				        	      
				        	      }
				        	  };
				        	Thread th = new Thread(task);
				        	th.setDaemon(true);
				        	th.start();
				        	
		        	}
		        });
		        
		        pane.getChildren().add(pauseAllBtn);
		        
		        
		        //The Play-Forward (normal speed) Button
		        Button playFwdBtn = new Button("Auto-play Events");
		        playFwdBtn.setLayoutX(29);
		        playFwdBtn.setLayoutY(610);
		        
		        playFwdBtn.setOnAction(new EventHandler<ActionEvent>() {
		        	@Override
		        	public void handle(ActionEvent event) {
				        Runnable task = new Runnable() {
				        	  @Override public void run() {
				        		  try
				        		  {
				        			  //java.lang.Thread.sleep(1000);
				        			  //runButtonRunnable(PLAY_FWD, cancelActiveActions);
				        			 
				        		  }//end try
				        		  catch(Exception e)
				        		  {	
				        		  } //end catch	
				        	      
				        	      }
				        	  };
				        	  
				        	
				        	Thread th = new Thread(task);
				        	th.setDaemon(true);
				        	th.start();
				        	
		        	}
		        });
		        
		        pane.getChildren().add(playFwdBtn);
		        
		        //The fast forward (rapid-speed forward) button:
		        Button fastFwdBtn = new Button("Fast-Forward Events");
		        fastFwdBtn.setLayoutX(29);
		        fastFwdBtn.setLayoutY(690);
		        
		        fastFwdBtn.setOnAction(new EventHandler<ActionEvent>() {
		        	@Override
		        	public void handle(ActionEvent event) {
				        Runnable task = new Runnable() {
				        	  @Override public void run() {
				        		  try
				        		  {
				        			  //java.lang.Thread.sleep(1000);
				        			  //runButtonRunnable(FAST_FWD, cancelActiveActions);
				 
				        		  }//end try
				        		  catch(Exception e)
				        		  {	
				        			  //todo: in case any uncaught exceptions occur, catch 'em here.
				        		  } //end catch	
				        	      
				        	      }
				        	  };
				        	  
				        	
				        	Thread fth = new Thread(task);
				        	fth.setDaemon(true);
				        	fth.start();
				        	
		        	}
		        });
		        pane.getChildren().add(fastFwdBtn);
		        //end FFWD button
		        
		      //The rewind (dual-speed reverse) button:
		        Button dsRewindBtn = new Button("Start Game");
		        dsRewindBtn.setLayoutX(29);
		        dsRewindBtn.setLayoutY(730);
		        
		        dsRewindBtn.setOnAction(new EventHandler<ActionEvent>() {
		        	@Override
		        	public void handle(ActionEvent event) {
		        		Platform.runLater(new Runnable() {
		                    @Override public void run() {
				        		  try
				        		  {
				        			  //java.lang.Thread.sleep(1000);
				        			  System.out.println("E M U 8 6 3 2");
				        			  pseudoMain();
				        		  }//end try
				        		  catch(Exception e)
				        		  {	
				        			  //todo: in case any uncaught exceptions occur, catch 'em here.
				        		  } //end catch	
		                    } //end run
		                } ); //end and close new Runnable
		        	}  //end handle 	
		        } ); //end and close new EventHandler
		        pane.getChildren().add(dsRewindBtn);
		        //end RWND button
		        
		        
	        } //END: layout of buttons displayed upon successful launch ends here.
	        
	       
			scrollPane = new ScrollPane();
			scrollPane.setContent(pane);
			if (!errorDisplayBit){
				scrollPane.setOnKeyPressed(new EventHandler<KeyEvent>() {
					@Override
		        	public void handle(KeyEvent event) {
				        Runnable task = new Runnable() {
				        	  @Override public void run() {
					        		  try
					        		  {
					        			  //java.lang.Thread.sleep(1000);
					        			  pseudoMain();
					        		  }//end try
					        		  catch(Exception e)
					        		  {	
					        		  } //end catch	
				        	      }
				        	  };
				        	Thread th = new Thread(task);
				        	th.setDaemon(true);
				        	th.start();
		        	}
		        });
			}
			// TODO make popout pane
			//Pane extraPane = new Pane();
			//extraPane.setPrefSize(someWidth, someHeight);
			//extraPane.getChildren.add(Button singleButtonAtATime);
			//or extraPane.getChildren.addAll(Collection<Button> buttonCollection);
	        //Scene extraScene = new Scene(extraPane, someOtherWidth, someOtherHeight);
			//Stage secondaryDialog = new Stage();
			//secondaryDialog.setOwner(originalPane.getScene().getWindow());
			//secondaryDialog.setTitle("Choose 3 cards, or pass");
			//...eventually secondaryDialog.close() should be a confirmation
			
			scene = new Scene(scrollPane, DEFAULT_APP_WIDTH, DEFAULT_APP_HEIGHT);
	        primaryStage.setScene(scene);
	        primaryStage.show();
		}
		catch (Exception e) {
			// TODO analyze whether this try-catch is required.
		}
		
	}
	
	class MissingTextPrompt {
	    private final String result;

	    MissingTextPrompt(Window owner) {
	      final Stage dialog = new Stage();

	      dialog.setTitle("Enter Missing Text");
	      dialog.initOwner(owner);
	      dialog.initStyle(StageStyle.UTILITY);
	      dialog.initModality(Modality.WINDOW_MODAL);
	      dialog.setX(owner.getX() + owner.getWidth());
	      dialog.setY(owner.getY());

	      final TextField textField = new TextField();
	      final Button submitButton = new Button("Submit");
	      submitButton.setDefaultButton(true);
	      submitButton.setOnAction(new EventHandler<ActionEvent>() {
	        @Override public void handle(ActionEvent t) {
	          dialog.close();
	        }
	      });
	      textField.setMinHeight(TextField.USE_PREF_SIZE);

	      final VBox layout = new VBox(10);
	      layout.setAlignment(Pos.CENTER_RIGHT);
	      layout.setStyle("-fx-background-color: azure; -fx-padding: 10;");
	      layout.getChildren().setAll(
	        textField, 
	        submitButton
	      );

	      dialog.setScene(new Scene(layout));
	      dialog.showAndWait();

	      result = textField.getText();
	    }

	    private String getResult() {
	      return result;
	    }
	  }
	
}

