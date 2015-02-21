/*FXUI GameMaster Class
*Albert Wallace, 2015. Version info now found in class definition.
*for Seth Denney's RISK, JavaFX UI-capable version
*
*Base build from original GameMaster class implementation, by Seth Denney, Sept 10 2014 
*/


package Master;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import Map.Country;
import Map.RiskMap;
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
import Util.FXUI_Crossbar;
import Util.OSExitException;
import Util.PlayerEliminatedException;
import Util.RiskConstants;
import Util.RiskUtils;
import Util.RollOutcome;
import Util.TextNodes;

/**
 * Represents the primary game controller for a game of Risk.
 * 	Asks for necessary responses and makes necessary decisions,
 * 	acting as a trigger for any Player objects added.
 * 
 * Compatible with original CPU opponents (Player), as well as
 * 	a human player through FXUI Player (FXUIPlayer) type.
 * 
 * 
 * UI elements are JavaFX, done with Java JDK 8.
 *
 */
public class FXUIGameMaster extends Application {
	public static final String versionInfo = "FXUI-RISK\nVersion REL00-GH08\nStamp Y2015.M02.D21.HM0000\nType:Alpha(01)";
	private static final int DEFAULT_APP_WIDTH = 1600;
	private static final int DEFAULT_APP_HEIGHT = 1062;
	protected static final String LOGFILE = "LOG.txt";
	protected static final String STATSFILE = "STATS.txt";
	protected static final String EVENT_DELIM = "...";
	protected static final boolean LOGGING_OFF = false;
	protected static final boolean LOGGING_ON = true;
	protected static final String FXUI_PLAYER_NAME = "FXUIPlayer";
	private static FXUI_Crossbar crossbar = new FXUI_Crossbar();
	private static Stage myStage;
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
    private Text errorDisplay;
    private Text currentPlayStatus;
    private String errorText;
    private boolean errorDisplayBit;
    private HashMap<String, Text> textNodeMap;
    private Map<String, Color> playerColorMap;
    private int numGames = 1;
    private boolean proceedWithExit = false;
    private boolean mainWindowExit = false;
    
    // TODO analyze viability of serialization vs using the already-existing log
    
    
    /*
     * If the app detects a call from the system to exit the program, and it's from a dialog window, handle the call by...asking if we really want to exit
     */
    public int doYouWantToMakeAnExit(int currentAttempts) {
		proceedWithExit = false;
		Window owner = pane.getScene().getWindow();
			
		try{
	      final Stage dialog = new Stage();
	      
	      dialog.setTitle("bye bye?");
	      dialog.initOwner(owner);
	      dialog.setX(owner.getX());
	      dialog.setY(owner.getY());
	      
	      final Text queryText = new Text("Did you want to end the game?\n(Your progress will NOT be saved!)");
	      queryText.setTextAlignment(TextAlignment.CENTER);
	      
	      if(mainWindowExit)
	      {
	    	  queryText.setText("Application fully exiting;\nShall we go?");
	      }
	      final Button yeah = new Button("Yes");
	      yeah.setOnAction(new EventHandler<ActionEvent>() {
	        @Override public void handle(ActionEvent t) {
	        	crossbar.signalPlayerEndingGame();
	        	proceedWithExit = true;
	        	if(!mainWindowExit)
	  	      {
	  	    	  currentPlayStatus.setText("I D L E");
	  	      }
	  	      
	          dialog.close();
	        }
	      });
	      final Button nah = new Button("No");
	      nah.setDefaultButton(true);
	      nah.setOnAction(new EventHandler<ActionEvent>() {
	        @Override public void handle(ActionEvent t) {
	        	proceedWithExit = false;
	          dialog.close();
	        }
	      });
	      
	      if(mainWindowExit)
	      {
	    	  nah.setDisable(true);
	      }
	      
	      final VBox layout = new VBox(10);
	      layout.setAlignment(Pos.CENTER);
	      layout.setStyle("-fx-padding: 10;");
	      layout.getChildren().setAll(
	    		  queryText, nah, yeah
	      );
	
	      dialog.setScene(new Scene(layout));
	      dialog.showAndWait();
	      
	      }
		catch(Exception e){System.out.println(e);}
		if(proceedWithExit)
		{
			return RiskConstants.MAX_ATTEMPTS;
		}
		else{
			return currentAttempts - 1;
		}
    }
	

	public void pseudoFXUIGameMaster(String mapFile, String playerFile, boolean logSwitch) throws IOException {
		this.round = 0;
		this.turnCount = 0;
		if (rand == null) {
			rand = new Random(RiskConstants.SEED);
		}
		//System.out.println("E G U 4 6 5 S");
		if (logSwitch == LOGGING_ON) {
			this.log = new FileWriter(LOGFILE);
			this.stats = new FileWriter(STATSFILE);
		}
		
		//System.out.println("E T U 4 6 5 Q");
		writeLogLn("Loading map from " + mapFile + "...");
		if (starterMap == null) {
			starterMap = new RiskMap();
		}
		
		//System.out.println("E M G 4 6 9 M");
		this.map = starterMap.getCopy();
		loadDeck();
		if (!loadPlayers(playerFile)) {
			System.out.println("Invalid number of players. 2-6 Players allowed.");
		}
		
		allocateMap();
		
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
					//System.out.println("G TE M U 4 6 5 M");
					//System.out.println(currentPlayer.getName());
					updateDisplay();
					reinforce(currentPlayer, true);
					//System.out.println("G GE M U 4 6 5 M");
					//System.out.println(currentPlayer.getName());
					updateDisplay();
					attack(currentPlayer);
					//System.out.println("G ME M U 4 6 5 M");
					//System.out.println(currentPlayer.getName());
					updateDisplay();
					fortify(currentPlayer);
					updateDisplay();
					turn = (this.players.indexOf(currentPlayer.getName()) + 1) % this.players.size();
				}
				catch (PlayerEliminatedException e) {
					//If an elimination exception is thrown up to this level,
					//then it was currentPlayer who was eliminated.
					turn %= this.players.size();
				}
			}

			
			if(!mainWindowExit){
				writeStatsLn();
				System.out.println(this.players.get(0) + " is the victor!");
				writeLogLn(this.players.get(0) + " is the victor!");
			}
			else
			{
				System.out.println("Game forced to exit by UI player; sorry 'bout it!");
			}
		}
		try {
			if (this.log != null && this.stats != null) {
				log.close();
				stats.close();
			}
		}
		catch (IOException e) {
		}
		if(this.players.size() > 0)
		{
			return this.players.get(0);
		}
		else
		{
			return null;
		}
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
			while (!valid && attempts < RiskConstants.MAX_ATTEMPTS  && !mainWindowExit) {
				try{
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
				catch(OSExitException e){
					if(crossbar.playerDialogIsActive())
					{
						crossbar.getCurrentPlayerDialog().close();
						crossbar.setCurrentPlayerDialog(null);
					}
					attempts = doYouWantToMakeAnExit(attempts);
				}
			}
			
			if (!valid || (crossbar.isPlayerBowingOut() && player.getName() == crossbar.getPlayerName())) {
				try {
					if(!valid){
					eliminate(player, null, "You failed to provide a valid initial army allocation.");
					}
					else{
						eliminate(player, null, "Player decided the game isn't for them right now. No worries.");
					}
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
		// TODO turn off oppCards IN THIS METHOD; it's unused after you get extra reinforcements manually beforehand
		if (withCountryBonus) {
			reinforcements += RiskUtils.calculateReinforcements(this.map, currentPlayer.getName());
		}
		writeLogLn(currentPlayer.getName() + " reinforcing with " + reinforcements + " armies.");
		while (!valid && attempts < RiskConstants.MAX_ATTEMPTS  && !mainWindowExit) {
			try{
				attempts++;
				ReinforcementResponse rsp = tryReinforce(currentPlayer, oppCards, reinforcements);
				if (valid = ReinforcementResponse.isValidResponse(rsp, this.map, currentPlayer.getName(), reinforcements)) {
					for (Map.Entry<Country, Integer> entry : rsp.getAllocation().entrySet()) {
						this.map.addCountryArmies(entry.getKey(), entry.getValue());
						writeLogLn(entry.getValue() + " " + entry.getKey().getName());
					}
				}
			}
			catch(OSExitException e)
			{
				if(crossbar.playerDialogIsActive())
				{
					crossbar.getCurrentPlayerDialog().close();
					crossbar.setCurrentPlayerDialog(null);
				}
				attempts = doYouWantToMakeAnExit(attempts);
			}
		}
		if (!valid) {
			eliminate(currentPlayer, null, "You failed to provide a valid reinforcement allocation.");
		}
		else if(crossbar.isPlayerBowingOut()) {
			eliminate(currentPlayer, null, "Player decided to leave. Come back any time, friend!");
		}
		writeLogLn(EVENT_DELIM);
	}
	
	protected void attack(Player currentPlayer) throws PlayerEliminatedException {
		int attempts = 0;
		boolean resetTurn;
		boolean hasGottenCard = false;
		while (attempts < RiskConstants.MAX_ATTEMPTS && !mainWindowExit) {
			updateDisplay();
			attempts++;
			resetTurn = false;
			try{
				AttackResponse atkRsp = tryAttack(currentPlayer, createCardSetCopy(currentPlayer.getName()), getPlayerCardCounts());
				if (atkRsp != null) {
					if (AttackResponse.isValidResponse(atkRsp, this.map, currentPlayer.getName())) {
						writeLogLn(currentPlayer.getName() + " is attacking "
								+ atkRsp.getDfdCountry() + "(" + this.map.getCountryArmies(atkRsp.getDfdCountry())
								+ ") from " + atkRsp.getAtkCountry() + "(" + this.map.getCountryArmies(atkRsp.getAtkCountry()) + ")!");
						System.out.println(currentPlayer.getName() + " is attacking "
								+ atkRsp.getDfdCountry() + "(" + this.map.getCountryArmies(atkRsp.getDfdCountry())
								+ ") from " + atkRsp.getAtkCountry() + "(" + this.map.getCountryArmies(atkRsp.getAtkCountry()) + ")!");
						attempts = 0;
						updateDisplay();
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
			catch (OSExitException e)
			{
				attempts = doYouWantToMakeAnExit(attempts);
			}
			
		}
	}
	
	protected DefendResponse defend(Player defender, Map<String, Integer> oppCards, AttackResponse atkRsp) throws PlayerEliminatedException {
		int attempts = 0;
		boolean valid = false;
		DefendResponse rsp = null;
		while (!valid && attempts < RiskConstants.MAX_ATTEMPTS && !mainWindowExit) {
			attempts++;
			rsp = tryDefend(defender, createCardSetCopy(defender.getName()), oppCards, new AttackResponse(atkRsp));
			valid = DefendResponse.isValidResponse(rsp, this.map, atkRsp.getDfdCountry());
		}
		if (!valid) {
			eliminate(defender, null, "You failed to provide a valid defense response.");
		}
		else if(crossbar.isPlayerBowingOut() && defender.getName() == crossbar.getPlayerName())
		{
			eliminate(defender, null, "This defender wants a break. Go ahead, friend. You deserve it.");
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
		while (!valid && attempts < RiskConstants.MAX_ATTEMPTS && !mainWindowExit) {
			attempts++;
			try{
				AdvanceResponse advRsp = tryAdvance(attacker, createCardSetCopy(attacker.getName()), getPlayerCardCounts(), atkRsp);
				if (valid = AdvanceResponse.isValidResponse(advRsp, atkRsp, this.map)) {
					writeLogLn(attacker.getName() + " advanced " + advRsp.getNumArmies() + " into " + atkRsp.getDfdCountry() + " from " + atkRsp.getAtkCountry() + ".");
					this.map.addCountryArmies(atkRsp.getAtkCountry(), -1 * advRsp.getNumArmies());
					this.map.addCountryArmies(atkRsp.getDfdCountry(), advRsp.getNumArmies());
				}
			}
			catch (OSExitException e)
			{
				attempts = doYouWantToMakeAnExit(attempts);
				System.out.println("AA ::: " + e);
			}
		}
		if (!valid) {
			eliminate(attacker, null, "You failed to provide a valid advance response.");
		}
		else if (crossbar.isPlayerBowingOut() && crossbar.getPlayerName() == attacker.getName())
		{
			eliminate(attacker, null, "The advancer decided to take a break. 'S OK. Get some cookies. Or hot cocoa.");
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
		while (!valid && attempts < RiskConstants.MAX_ATTEMPTS && !mainWindowExit) {
			attempts++;
			try{
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
			catch (OSExitException e)
			{
				attempts = doYouWantToMakeAnExit(attempts);
				System.out.println("FF ::: " + e);
			}
		}
	}
	
	protected ReinforcementResponse tryInitialAllocation(Player player, int reinforcements) throws OSExitException {
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
		catch(OSExitException e){
			if(crossbar.playerDialogIsActive())
			{
				crossbar.getCurrentPlayerDialog().close();
				crossbar.setCurrentPlayerDialog(null);
			}
			throw e;
		}
		catch (Exception e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	protected CardTurnInResponse tryTurnIn(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards, boolean turnInRequired) throws OSExitException {
		try {
			if(player.getName() != FXUI_PLAYER_NAME){ //if a CPU player
				CardTurnInResponse rsp = player.proposeTurnIn(this.map.getReadOnlyCopy(), cardSet, oppCards, turnInRequired);
				validatePlayerName(player);
				return rsp;
			}
			else{ //if not a CPU player, aka if a human player
				FXUIPlayer pIn =(FXUIPlayer)player;
				CardTurnInResponse rsp = pIn.proposeTurnIn(this.map.getReadOnlyCopy(), cardSet, oppCards, turnInRequired, pane.getScene().getWindow());
				validatePlayerName(player);
				return rsp;
			}
		}
		catch(OSExitException e){
			if(crossbar.playerDialogIsActive())
			{
				crossbar.getCurrentPlayerDialog().close();
				crossbar.setCurrentPlayerDialog(null);
			}
			throw e;
		}
		catch (Exception e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	protected ReinforcementResponse tryReinforce(Player player, Map<String, Integer> oppCards, int reinforcements) throws OSExitException{
		try {
			/*
			rsp = player.reinforce(this.map.getReadOnlyCopy(), createCardSetCopy(player.getName()), oppCards, reinforcements);
			validatePlayerName(player);
			return rsp;*/
			if(player.getName() != FXUI_PLAYER_NAME){ //if a CPU player
				ReinforcementResponse rsp = player.reinforce(this.map.getReadOnlyCopy(), createCardSetCopy(player.getName()), oppCards, reinforcements);
				validatePlayerName(player);
				return rsp;
			}
			else{ //if not a CPU player, aka if a human player
				FXUIPlayer pIn =(FXUIPlayer)player;
				ReinforcementResponse rsp = pIn.reinforce(this.map.getReadOnlyCopy(), createCardSetCopy(player.getName()), oppCards, reinforcements, pane.getScene().getWindow());
				validatePlayerName(player);
				return rsp;
			}
		}
		catch (OSExitException e)
		{
			throw e;
		}
		catch (Exception e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	protected AttackResponse tryAttack(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards) throws OSExitException{
		try {
			
			if(player.getName() != FXUI_PLAYER_NAME){ //if a CPU player
				AttackResponse rsp = player.attack(this.map.getReadOnlyCopy(), createCardSetCopy(player.getName()), oppCards);
				validatePlayerName(player);
				return rsp;
			}
			else{ //if not a CPU player, aka if a human player
				FXUIPlayer pIn =(FXUIPlayer)player;
				AttackResponse rsp = pIn.attack(this.map.getReadOnlyCopy(), createCardSetCopy(player.getName()), oppCards, pane.getScene().getWindow());
				validatePlayerName(player);
				return rsp;
			}
		}
		catch (OSExitException e)
		{
			throw e;
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
	
	protected AdvanceResponse tryAdvance(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards, AttackResponse atkRsp) throws OSExitException {
		try {
			if(player.getName() != FXUI_PLAYER_NAME) //CPU player
			{
				AdvanceResponse rsp = player.advance(this.map.getReadOnlyCopy(), createCardSetCopy(player.getName()), oppCards, atkRsp.getAtkCountry(), atkRsp.getDfdCountry(), atkRsp.getNumDice());
				validatePlayerName(player);
				return rsp;
			}
			else //human player
			{
				FXUIPlayer fxPlayer = (FXUIPlayer)player;
				AdvanceResponse rsp = fxPlayer.advance(this.map.getReadOnlyCopy(), createCardSetCopy(player.getName()), oppCards, atkRsp.getAtkCountry(), atkRsp.getDfdCountry(), atkRsp.getNumDice(), pane.getScene().getWindow());
				validatePlayerName(player);
				return rsp;
			}
		}
		catch (OSExitException e)
		{
			throw e;
		}
		catch (Exception e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	protected FortifyResponse tryFortify(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards) throws OSExitException {
		try {
			if(player.getName() != FXUI_PLAYER_NAME) //CPU player
			{
				FortifyResponse rsp = player.fortify(this.map.getReadOnlyCopy(), createCardSetCopy(player.getName()), oppCards);
				validatePlayerName(player);
				return rsp;
			}
			else //human player
			{
				FXUIPlayer fxPlayer = (FXUIPlayer)player;
				FortifyResponse rsp = fxPlayer.fortify(this.map.getReadOnlyCopy(), createCardSetCopy(player.getName()), oppCards, pane.getScene().getWindow());
				validatePlayerName(player);
				return rsp;
			}
		}
		catch (OSExitException e)
		{
			throw e;
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
		while (!valid && attempts < RiskConstants.MAX_ATTEMPTS && !mainWindowExit) {
			try{
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
				System.out.println(attempts);
			}
			catch(OSExitException e){
				if(crossbar.playerDialogIsActive())
				{
					crossbar.getCurrentPlayerDialog().close();
					crossbar.setCurrentPlayerDialog(null);
				}
				attempts = doYouWantToMakeAnExit(attempts);
				System.out.println("gCTI ::: " + e);
			}
		}
		if (!valid && turnInRequired) {
			eliminate(currentPlayer, null, "You were required to turn in cards this turn, and you failed to do so.");
		}
		else if(crossbar.isPlayerBowingOut() && currentPlayer.getName() == crossbar.getPlayerName()){
			eliminate(currentPlayer, null, "The player is opting out of the game altogether. Have a good day, buddy.");
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
		FXUIPlayer.setCrossbar(FXUIGameMaster.crossbar);
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
				//System.out.println("E S M - 0 8 P");
				//System.out.println(card.getType());
				//System.out.println(card.getCountry() + " + " + allocationIdx + "+" + this.players.size() + "--" + allocationIdx % this.players.size());
				//if(this.playerMap == null){ System.out.println("E S M - 0 8 P M 1");}
				//if(this.players == null){ System.out.println("E S M - 0 8 P M 3");}
				//if(this.playerMap.get(this.players.get(allocationIdx % this.players.size())) == null){ System.out.println("E S M - 0 8 P X -");}
				if (!card.getType().equals(RiskConstants.WILD_CARD)) {
					try{
					map.setCountryOwner(card.getCountry(), this.playerMap.get(this.players.get(allocationIdx % this.players.size())).getName());
					//System.out.println("E G X N 0 0" + this.playerMap.get(this.players.get(allocationIdx % this.players.size())).getName());
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
			System.out.println(loser.getName() + " Eliminated! " + reason);
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
				Text txt = new Text(200 * (i) + 50, 20, "∎"+playerName.toLowerCase());
				txt.setFont(Font.font("Verdana", FontWeight.THIN, 20));
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
		//System.out.println("E M U 7 6 5 8");
			HashMap<String, Integer> winLog = new HashMap<String, Integer>();
			RiskConstants.SEED = 1;
			for (int i = 0; i < this.numGames; i++) {
				RiskConstants.resetTurnIn();
				//System.out.println("E M U 7 6 5 6");
				pseudoFXUIGameMaster("Countries.txt", null, i == this.numGames - 1 ? LOGGING_ON : LOGGING_OFF);
				//System.out.println("E M U 7 6 5 4");
				System.out.print((i + 1) + " - ");
				String victor = begin();
				if (victor != null)
				{
					if (!winLog.containsKey(victor)) {
						winLog.put(victor, 0);
					}
					winLog.put(victor, winLog.get(victor) + 1);
				}
			}
			if(!mainWindowExit){
				for (Map.Entry<String, Integer> entry : winLog.entrySet()) {
					System.out.println(entry.getKey() + " had a win percentage of " + 100.0 * entry.getValue() / this.numGames + "%");
				}
			}
		}
		catch (Exception e) {
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
					Scanner reader = new Scanner(TextNodes.nodes);
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
	
	public void updateDisplay()
	{
		for (Player player : playerMap.values())
		{
			for (Country country : RiskUtils.getPlayerCountries(this.map, player.getName()))
			{
				this.textNodeMap.get(country.getName()).setFill(this.playerColorMap.get(this.map.getCountryOwner(country)));
				this.textNodeMap.get(country.getName()).setText(country.getName() + "\n" + this.map.getCountryArmies(country));
			}
		}
	}
	
	
	/**
	 * Get your life in the form of a game!
	 * This method is the point at which the JavaFX items are populated on the main map screen.
	 * 
	 * Extra dialogs are further prompted elsewhere, depending on their needs/uses.
	 * (In this file is the method to present an exit confirmation dialog, as is the class representing the About dialog.)
	 */
	@Override
	public void start(Stage primaryStage) throws Exception {
		try{
			About nAbout = new About();
			myStage = primaryStage;
			
			
			double widthOfPriScreen = Screen.getPrimary().getVisualBounds().getWidth() - 5;
			double heightOfPriScreen = Screen.getPrimary().getVisualBounds().getHeight() - 25;
			System.out.println("Width first set: " + widthOfPriScreen + " :: Height first set: " + heightOfPriScreen);
			
			pane = new Pane();
	        pane.setPrefSize(DEFAULT_APP_WIDTH, DEFAULT_APP_HEIGHT);
	        pane.setStyle("-fx-background-color: blue");
	        /*We set the image in the pane based on whether there was an error or not.
	        * If there was an error, it'll be changed later.*/
	       
	        //Facilitate checking for errors...
	        errorDisplayBit = false;
	        errorText = "Status...";
	        
	        //pre-load the error background, just in case...
	        Image imageE = new Image("RiskBoardAE.jpg",true);
            ImageView im0 = new ImageView();
            im0.setImage(imageE);
            pane.getChildren().add(im0);
            
	        //...which will happen here:
	        //populate the countries and players, and find out if there was an error doing either activity
	        pseudoFXUIGameMaster("Countries.txt", null, LOGGING_ON);
	        loadTextNodesForUI("TextNodes.txt");
	        representPlayersOnUI();
	        //now display elements -- status and buttons -- according to whether there was an error!
	        errorDisplay = new Text(errorText);
	        errorDisplay.setFont(Font.font("Verdana", FontWeight.THIN, 20));
	        if(errorDisplayBit)
	        {
	        	errorDisplay.setFill(Color.RED);
	        }
	        else
	        {
	        	errorDisplay.setFill(Color.WHITE);
	        	Image imageOK = new Image("RiskBoard.jpg", true);
                im0.setImage(imageOK);
	        }
	        
	        //The vertical box to contain the major buttons and status.
	        VBox primaryStatusButtonPanel = new VBox(10);
	        primaryStatusButtonPanel.setAlignment(Pos.CENTER_LEFT);
	        primaryStatusButtonPanel.setLayoutX(29);
	        primaryStatusButtonPanel.setLayoutY(525);
	        primaryStatusButtonPanel.getChildren().add(errorDisplay);
	        
        	
	        currentPlayStatus = new Text("H E L L O");
	        currentPlayStatus.setFont(Font.font("Verdana", FontWeight.NORMAL, 24));
	        currentPlayStatus.setFill(Color.WHITE);
	        
	        
	        //End the current game, but don't close the program.
	        Button endGame = new Button("Bow out.\n(End current game)");
	        endGame.setOnAction(event -> Platform.runLater(() -> {
		    	if(crossbar.playerDialogIsActive())
				{
					crossbar.getCurrentPlayerDialog().close();
					crossbar.setCurrentPlayerDialog(null);
				}
			} ));
	        
	        //Button to initiate the game
	        Button startBtn = new Button("Let's go!!\n(Start new game)");
	        startBtn.setOnAction(event -> Platform.runLater(() -> {
			  try
			  {
				  currentPlayStatus.setText("in play.");
				  pseudoMain();
			  }//end try
			  catch(Exception e)
			  {	
				  // TODO: in case any uncaught exceptions occur, catch 'em here.
			  }	
			}));
	        
	        //your standard About buttons...
	        HBox talkToMe = new HBox(15);
	        Button tellMe = new Button("About");
	        tellMe.setOnAction(event -> Platform.runLater(() -> nAbout.launch(pane.getScene().getWindow(), false) ));
	        
	        //...I said "About buttons". Plural. Yep.	
	        Button tellMe2 = new Button("more.");
	        tellMe2.setOnAction(event -> Platform.runLater(() -> nAbout.more(pane.getScene().getWindow()) ));
	        talkToMe.getChildren().addAll(tellMe, tellMe2);
	        
	        //Exit the application entirely
	        Button exitApp = new Button("Lights out!\n(Exit to desktop)");
	        exitApp.setOnAction(event -> Platform.runLater(() -> {
				primaryStage.close();
				mainWindowExit = true;
			} ));
	        
	        
	        
	        //tweaks to perform if there was an error...
	        if(errorDisplayBit){
	        	currentPlayStatus.setText("------");
	        	startBtn.setDisable(true);
	        	endGame.setDisable(true);
	        }
	        else{
				pane.setOnKeyPressed(event -> Platform.runLater( () -> {
				  try
				  {
					  currentPlayStatus.setText("Game started...");
					  pseudoMain();
				  }//end try
				  catch(Exception e)
				  {	
				  } //end catch	
			    }));
	        }
	        
	        primaryStatusButtonPanel.getChildren().addAll(currentPlayStatus,startBtn,endGame,exitApp,talkToMe);
	        //****layout of text & buttons displayed upon launch ends here.***
	        
	        pane.getChildren().add(primaryStatusButtonPanel);
	        

			// DEFAULT_APP_WIDTH, DEFAULT_APP_HEIGHT);
			scene = new Scene(pane,widthOfPriScreen, heightOfPriScreen);
		
			
			//one more tweak to perform if there was -no- error
			scene.widthProperty().addListener(new ChangeListener<Number>() {
                @Override public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneWidth, Number newSceneWidth) {
                    System.out.println("Width: " + newSceneWidth);
                    resize(null);
                }
            });
            scene.heightProperty().addListener(new ChangeListener<Number>() {
                @Override public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneHeight, Number newSceneHeight) {
                    System.out.println("Height: " + newSceneHeight);
                    resize(null);
                }
            });
			
			
            resize(primaryStage);
			primaryStage.setTitle("RISK!");
	        primaryStage.setScene(scene);
	        primaryStage.show();
	        
	        //go ahead and launch the "About" window, and tell it to autohide -- time until autohide set via the "About" class.
	        nAbout.launch(pane.getScene().getWindow(), true);
	        
	        //Help control what happens when the user tries to exit by telling the app...what...to do.
	        //In this case, we're telling it "Yes, we're trying to exit from the main window, so display the appropriate dialog.
	        //That's what this single boolean does.
	        scene.getWindow().setOnCloseRequest(t -> mainWindowExit = true);
		}
		catch (Exception e) {
			// TODO analyze whether this try-catch is required.
		}
		
	}
	
	private void resize(Stage stageIn)
    {
        double currLiveRatio = scene.getWidth()/scene.getHeight();
        double targetRatio = (double)(DEFAULT_APP_WIDTH)/(double)(DEFAULT_APP_HEIGHT);
        double newWidth = 0;
        double newHeight = 0;
        
        
        if (currLiveRatio <= targetRatio) //wider than high; limit by height
        {
            newWidth = scene.getWidth();
            newHeight = scene.getWidth() / targetRatio;
            Scale scale = new Scale(newWidth/(DEFAULT_APP_WIDTH), newWidth/(DEFAULT_APP_WIDTH));
            scale.setPivotX(0);
            scale.setPivotY(0);
            scene.getRoot().getTransforms().setAll(scale);
            if(stageIn != null){
            	stageIn.setHeight(newHeight);
            }

        }
        else //higher than wide; limit by width
        {
            newHeight = scene.getHeight();
            newWidth = scene.getHeight() * targetRatio;
            Scale scale = new Scale(newHeight/(DEFAULT_APP_HEIGHT), newHeight/(DEFAULT_APP_HEIGHT));
            scale.setPivotX(0);
            scale.setPivotY(0);
            scene.getRoot().getTransforms().setAll(scale);
            if(stageIn != null){
            	stageIn.setWidth(newWidth);
            	
            }
            
        }
      
    }
	
	class MissingTextPrompt {

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
	      layout.setStyle("-fx-padding: 10;");
	      //old::: 	      layout.setStyle("-fx-background-color: azure; -fx-padding: 10;");
	      layout.getChildren().setAll(
	        textField, 
	        submitButton
	      );

	      dialog.setScene(new Scene(layout));
	      dialog.showAndWait();
	    }

	}
	    
    class About {

	    About(){
	    }
	    
	    public void launch(Window owner, boolean autoExit) {
	      final Stage dialog = new Stage();

	      dialog.setTitle("Hi, friend. :D");
	      dialog.initOwner(owner);
	      //dialog.initStyle(StageStyle.UTILITY);
	      //dialog.initModality(Modality.WINDOW_MODAL);
	      dialog.setX(owner.getX());
	      dialog.setY(owner.getY() + 300);

	      final Text info1= new Text();
	      info1.setText(":::\n\nRISK!\nor\nconquest of the modern Mercator");
	      info1.setTextAlignment(TextAlignment.CENTER);
	      info1.setFont(Font.font("Arial", FontWeight.THIN, 16));
	      
	      final Hyperlink hlink = new Hyperlink(":::");
	      hlink.setOnAction(new EventHandler<ActionEvent>() {
	    	  @Override public void handle(ActionEvent t){
	    		  try {
	    	            Desktop.getDesktop().browse(new URI("http://xkcd.com/977/"));
	    	        } catch (IOException e1) {
	    	            e1.printStackTrace();
	    	        } catch (URISyntaxException e1) {
	    	            e1.printStackTrace();
	    	        }
	    	  }
	      });
	      
	      final Text info2= new Text();
	      info2.setText("\n\nJava + JavaFX\n\nDenney, Wallace\n\n2015\n\n:D\n\n:::::::");
	      info2.setTextAlignment(TextAlignment.CENTER);
	      info2.setFont(Font.font("Arial", FontWeight.THIN, 12));
	      
	      final Button submitButton = new Button("OK");
	      submitButton.setDefaultButton(true);
	      submitButton.setOnAction(new EventHandler<ActionEvent>() {
	        @Override public void handle(ActionEvent t) {
	          dialog.close();
	        }
	      });
	      //textField.setMinHeight(TextField.USE_PREF_SIZE);

	      final VBox layout = new VBox();
	      layout.setAlignment(Pos.CENTER);
	      layout.setStyle("-fx-padding: 50;");
	      //old::: 	      layout.setStyle("-fx-background-color: azure; -fx-padding: 10;");
	      layout.getChildren().setAll(
	        info1, hlink, info2,
	        submitButton
	      );

	      dialog.setScene(new Scene(layout));
	      dialog.show();
	      if(autoExit)
	      {
	    	  Runnable task = () -> {
        		  try
        		  {
        			  Thread.sleep(5000);
        			  Platform.runLater(new Runnable()
						{
        				  @Override public void run(){
							dialog.close();
						} 
						});
        		  }//end try
        		  catch(Exception e)
        		  {	
        		  } //end catch	
    	     };
	    	Thread th = new Thread(task);
	    	th.setDaemon(true);
	    	th.start();
	      }
	    }
	    public void more(Window owner) {
		      final Stage dialog = new Stage();

		      dialog.setTitle("more.");
		      dialog.initOwner(owner);
		      //dialog.initStyle(StageStyle.UTILITY);
		      //dialog.initModality(Modality.WINDOW_MODAL);
		      dialog.setX(owner.getX());
		      dialog.setY(owner.getY() + 300);

		      
		      final Hyperlink hlinkD = new Hyperlink("denney");
		      hlinkD.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
		      hlinkD.setOnAction(new EventHandler<ActionEvent>() {
		    	  @Override public void handle(ActionEvent t){
		    		  try {
		    	            Desktop.getDesktop().browse(new URI("http://github.com/sethau"));
		    	        } catch (IOException e1) {
		    	            e1.printStackTrace();
		    	        } catch (URISyntaxException e1) {
		    	            e1.printStackTrace();
		    	        }
		    	  }
		      });
		      
		      final Hyperlink hlinkW = new Hyperlink("wallace");
		      hlinkW.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
		      hlinkW.setOnAction(new EventHandler<ActionEvent>() {
		    	  @Override public void handle(ActionEvent t){
		    		  try {
		    	            Desktop.getDesktop().browse(new URI("http://github.com/aewallace"));
		    	        } catch (IOException e1) {
		    	            e1.printStackTrace();
		    	        } catch (URISyntaxException e1) {
		    	            e1.printStackTrace();
		    	        }
		    	  }
		      });
		      
		      final Text bridge2= new Text("\n\n:::::::\n2015\n:::::::\n\n");
		      bridge2.setTextAlignment(TextAlignment.CENTER);
		      bridge2.setFont(Font.font("Arial", FontWeight.THIN, 16));
		      
		      final Text deepVersionInfo= new Text(versionInfo + "\n\n");
		      deepVersionInfo.setTextAlignment(TextAlignment.CENTER);
		      deepVersionInfo.setFont(Font.font("Arial", FontWeight.THIN, 12));
		      
		      final Button submitButton = new Button("OK");
		      submitButton.setDefaultButton(true);
		      submitButton.setOnAction(new EventHandler<ActionEvent>() {
		        @Override public void handle(ActionEvent t) {
		          dialog.close();
		        }
		      });
		      //textField.setMinHeight(TextField.USE_PREF_SIZE);

		      final VBox layout = new VBox(10);
		      layout.setAlignment(Pos.CENTER);
		      layout.setStyle("-fx-padding: 50;");
		      //old::: 	      layout.setStyle("-fx-background-color: azure; -fx-padding: 10;");
		      layout.getChildren().setAll(
		    	deepVersionInfo,
		        hlinkD, bridge2, hlinkW,
		        submitButton
		      );

		      dialog.setScene(new Scene(layout));
		      dialog.showAndWait();
		    }

	  }
	
}

