package Master;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import Map.Country;
import Map.RiskMap;
import Player.EasyDefaultPlayer;
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

public class GameMaster {
	private static final String LOGFILE = "LOG.txt";
	private static final String STATSFILE = "STATS.txt";
	private static final String EVENT_DELIM = "...";
	private static final boolean LOGGING_OFF = false;
	private static final boolean LOGGING_ON = true;
	private RiskMap map;
	private Deque<Card> deck;
	private List<String> players;
	private Map<String, Player> playerMap;
	private Map<String, Collection<Card>> playerCardMap;
	
	private static RiskMap starterMap = null;
	private static Random rand;
	private static int allocationIdx = 0;
	
	private FileWriter log, stats;
	private List<String> allPlayers;
	private int round, turnCount;
	
	public GameMaster(String mapFile, String playerFile, boolean logSwitch) throws IOException {
		this.round = 0;
		this.turnCount = 0;
		if (rand == null) {
			rand = new Random(RiskConstants.SEED);
		}
		if (logSwitch == LOGGING_ON) {
			this.log = new FileWriter(LOGFILE);
			this.stats = new FileWriter(STATSFILE);
		}
		writeLogLn("Loading map from " + mapFile + "...");
		if (starterMap == null) {
			starterMap = new RiskMap();
		}
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
	
	private boolean initializeForces() {
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
	
	private void validatePlayerName(Player player) throws PlayerEliminatedException {
		if (!(this.playerMap.containsKey(player.getName()) && this.playerMap.get(player.getName()) == player)) {
			eliminate(player, null, "Players who hide their true identity are not welcome here. BEGONE!");
		}
	}
	
	private void reinforce(Player currentPlayer, boolean withCountryBonus) throws PlayerEliminatedException {
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
	
	private void attack(Player currentPlayer) throws PlayerEliminatedException {
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
	
	private DefendResponse defend(Player defender, Map<String, Integer> oppCards, AttackResponse atkRsp) throws PlayerEliminatedException {
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
	
	private void carryOutAttack(AttackResponse atk, DefendResponse dfd) {
		RollOutcome result = DiceRoller.roll(atk.getNumDice(), dfd.getNumDice());
		this.map.addCountryArmies(atk.getAtkCountry(), -1 * result.getAtkLosses());
		this.map.addCountryArmies(atk.getDfdCountry(), -1 * result.getDfdLosses());
		writeLogLn("\tAttacker lost: " + result.getAtkLosses() + "; Defender lost: " + result.getDfdLosses());
	}
	
	private boolean checkForTakeover(Player attacker, AttackResponse atkRsp, boolean hasGottenCard) throws PlayerEliminatedException {
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
	
	private void advanceArmies(Player attacker, AttackResponse atkRsp) throws PlayerEliminatedException {
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
	
	private void awardCard(String playerName) {
		writeLogLn("Awarding " + playerName + " one card.");
		if (this.deck.size() > 0) {
			this.playerCardMap.get(playerName).add(this.deck.removeFirst());
		}
	}
	
	private void checkForElimination(Player attacker, String loserName, Country takenCountry, boolean allowReinforce) throws PlayerEliminatedException {
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
	
	private void fortify(Player currentPlayer) {
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
	
	private ReinforcementResponse tryInitialAllocation(Player player, int reinforcements) {
		try {
			ReinforcementResponse rsp = player.getInitialAllocation(this.map.getReadOnlyCopy(), reinforcements);
			validatePlayerName(player);
			return rsp;
		}
		catch (Exception e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	private CardTurnInResponse tryTurnIn(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards, boolean turnInRequired) {
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
	
	private ReinforcementResponse tryReinforce(Player player, Map<String, Integer> oppCards, int reinforcements) {
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
	
	private AttackResponse tryAttack(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards) {
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
	
	private DefendResponse tryDefend(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards, AttackResponse atkRsp) {
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
	
	private AdvanceResponse tryAdvance(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards, AttackResponse atkRsp) {
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
	
	private FortifyResponse tryFortify(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards) {
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
	
	private boolean validateInitialAllocation(Map<Country, Integer> allocation, String playerName, int armies) {
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
	
	private void allocateArmies(String playerName, Map<Country, Integer> allocation, int reinforcements) {
		writeLogLn(playerName + " reinforcing with " + reinforcements + " armies.");
		for (Map.Entry<Country, Integer> entry : allocation.entrySet()) {
			this.map.setCountryArmies(entry.getKey(), entry.getValue());
			writeLogLn(entry.getValue() + " " + entry.getKey().getName());
		}
		writeLogLn(EVENT_DELIM);
	}
	
	private int getCardTurnIn(Player currentPlayer, Map<String, Integer> oppCards) throws PlayerEliminatedException {
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
	
	private Collection<Card> createCardSetCopy(String playerName) {
		Collection<Card> copy = new ArrayList<Card>();
		for (Card card : this.playerCardMap.get(playerName)) {
			copy.add(new Card(card.getType(), card.getCountry()));
		}
		return copy;
	}
	
	private Player getOwnerObject(Country country) {
		String playerName = this.map.getCountryOwner(country);
		return getPlayerObject(playerName);
	}
	
	private Player getPlayerObject(String playerName) {
		for (Player player : this.playerMap.values()) {
			if (player.getName().equals(playerName)) {
				return player;
			}
		}
		return null;
	}
	
	private Map<String, Integer> getPlayerCardCounts() {
		Map<String, Integer> playerCardCounts = new HashMap<String, Integer>();
		for (String playerName : this.playerMap.keySet()) {
			playerCardCounts.put(playerName, this.playerCardMap.get(playerName).size());
		}
		return playerCardCounts;
	}
	
	private void loadDeck() {
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
	
	private void shuffleCards(List<Card> cardList) {
		int j;
		Card temp;
		for (int i = 0; i < 2 * cardList.size(); i++) {
			j = rand.nextInt(cardList.size());
			temp = cardList.get(i % cardList.size());
			cardList.set(i % cardList.size(), cardList.get(j));
			cardList.set(j, temp);
		}
	}
	
	private boolean loadPlayers(String playerFile) {
		writeLogLn("Loading players...");
		this.playerMap = new HashMap<String, Player>();
		this.allPlayers = new ArrayList<String>();
		
		this.playerMap.put("Easy 1", new EasyDefaultPlayer("Easy 1"));
		this.allPlayers.add("Easy 1");
		
		this.playerMap.put("Normal 2", new NormalDefaultPlayer("Normal 2"));
		this.allPlayers.add("Normal 2");
		
		this.playerMap.put("Hard 3", new HardDefaultPlayer("Hard 3"));
		this.allPlayers.add("Hard 3");
		
		this.playerMap.put("Seth 1", new Seth("Seth 1"));
		this.allPlayers.add("Seth 1");
		
		//this.playerMap.put("Seth 2", new Seth("Seth 2"));
		//this.allPlayers.add("Seth 2");
		
		//this.playerMap.put("Seth 3", new Seth("Seth 3"));
		//this.allPlayers.add("Seth 3");
		
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
	
	private void shufflePlayers(List<String> playerList) {
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
	private void allocateUnownedCountries() {
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
	private void allocateMap() {
		if (this.players.size() > 0) {
			writeLogLn("Allocating countries...");
			for (Card card : this.deck) {
				if (!card.getType().equals(RiskConstants.WILD_CARD)) {
					map.setCountryOwner(card.getCountry(), this.playerMap.get(this.players.get(allocationIdx % this.players.size())).getName());
					allocationIdx++;
				}
			}
		}
	}
	
	private void eliminate(Player loser, Player eliminator, String reason) throws PlayerEliminatedException {
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
	
	private void writeLogLn(String line) {
		if (this.log != null) {
			try {
				this.log.write(line + "\r\n");
				this.log.flush();
			}
			catch (IOException e) {
			}
		}
	}
	
	private void writeStatsLn() {
		if (this.stats != null) {
			try {
				stats.write(this.turnCount + " " + this.round + " ");
				for (String playerName : this.allPlayers) {
					//count player's countries
					stats.write(RiskUtils.getPlayerCountries(this.map, playerName).size() + " ");
					//count player's armies
					stats.write(RiskUtils.countPlayerArmies(this.map, playerName) + " ");
				}
				stats.write("\r\n");
				this.stats.flush();
			}
			catch (IOException e) {
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		try {
			HashMap<String, Integer> winLog = new HashMap<String, Integer>();
			int numGames = 1;
			RiskConstants.SEED = 1;
			for (int i = 0; i < numGames; i++) {
				RiskConstants.resetTurnIn();
				GameMaster game = new GameMaster("Countries.txt", null, i == numGames - 1 ? LOGGING_ON : LOGGING_OFF);
				System.out.print((i + 1) + " - ");
				String victor = game.begin();
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
			throw e;
		}
	}
}