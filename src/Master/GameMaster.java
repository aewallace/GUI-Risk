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
import Player.DefaultPlayer;
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

//TODO: Implement Master Credentials to prevent improper alteration of objects.
public class GameMaster {
	private static final String LOGFILE = "LOG.txt";
	private static final String STATSFILE = "STATS.txt";
	private RiskMap map;
	private Deque<Card> deck;
	private List<String> players;
	private Map<String, Player> playerMap;
	private Map<String, Collection<Card>> playerCardMap;
	
	private FileWriter log, stats;
	private List<String> allPlayers;
	private int round, turnCount;
	
	public GameMaster(String mapFile, String playerFile) throws IOException {
		this.round = 0;
		this.turnCount = 0;
		this.log = new FileWriter(LOGFILE);
		this.stats = new FileWriter(STATSFILE);
		writeLogLn("Loading map from " + mapFile + "...");
		this.map = new RiskMap(mapFile);
		loadDeck();
		loadPlayers(playerFile);
		allocateCountries();
	}
	
	public void begin() {
		if (initializeForces()) {
			//play round-robin until there is only one player left
			int turn = 0;
			while (this.players.size() > 1) {
				if (turn == 0) {
					this.round++;
					writeLogLn("Beginning Round " + round + "!");
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
					//TODO: write message and reason to logfile
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
			log.close();
			stats.close();
		}
		catch (IOException e) {
			return;
		}
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
				reinforcements = RiskConstants.INIT_ARMIES;//should eventually be calculated from this.players.size()
				ReinforcementResponse rsp = tryInitialAllocation(player, reinforcements);
				if (valid = ReinforcementResponse.isValidResponse(rsp, this.map, player.getName(), reinforcements)) {
					Collection<String> playerCountries = RiskUtils.getPlayerCountries(this.map, player.getName());
					validateInitialAllocation(rsp.getAllocation(), playerCountries, reinforcements);
					allocateArmies(rsp.getAllocation());
					playerIndex++;
					writeLogLn("Troops successfully allocated for " + player.getName() + "...");
				}
			}
			
			if (!valid) {
				try {
					eliminate(player, null, "You failed to provide a valid initial army allocation.");
				}
				catch (PlayerEliminatedException e) {
					//TODO: write message and reason to logfile
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
			ReinforcementResponse rsp = tryReinforce(currentPlayer, createCardSetCopy(currentPlayer.getName()), oppCards, reinforcements);
			if (valid = ReinforcementResponse.isValidResponse(rsp, this.map, currentPlayer.getName(), reinforcements)) {
				for (Map.Entry<String, Integer> entry : rsp.getAllocation().entrySet()) {
					this.map.getCountries().get(entry.getKey()).addArmies(entry.getValue());
				}
			}
		}
		if (!valid) {
			eliminate(currentPlayer, null, "You failed to provide a valid reinforcement allocation.");
		}
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
					writeLogLn(currentPlayer.getName() + " is attacking " + atkRsp.getDfdCountry() + " from " + atkRsp.getAtkCountry() + "!");
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
			valid = DefendResponse.isValidResponse(rsp, this.map.getCountries().get(atkRsp.getDfdCountry()));
		}
		if (!valid) {
			eliminate(defender, null, "You failed to provide a valid defense response.");
		}
		return rsp;
	}
	
	private void carryOutAttack(AttackResponse atk, DefendResponse dfd) {
		RollOutcome result = DiceRoller.roll(atk.getNumDice(), dfd.getNumDice());
		this.map.getCountries().get(atk.getAtkCountry()).addArmies(-1 * result.getAtkLosses());
		this.map.getCountries().get(atk.getDfdCountry()).addArmies(-1 * result.getDfdLosses());
		
	}
	
	private boolean checkForTakeover(Player attacker, AttackResponse atkRsp, boolean hasGottenCard) throws PlayerEliminatedException {
		if (this.map.getCountries().get(atkRsp.getDfdCountry()).getNumArmies() == 0) {
			String loserName = this.map.getCountries().get(atkRsp.getDfdCountry()).getOwner();
			writeLogLn(attacker.getName() + " has taken " + atkRsp.getDfdCountry() + " from " + loserName + "!");
			this.map.getCountries().get(atkRsp.getDfdCountry()).setOwner(attacker.getName());
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
			if (valid = AdvanceResponse.isValidResponse(advRsp, atkRsp, this.map.getCountries().get(atkRsp.getAtkCountry()).getNumArmies())) {
				writeLogLn(attacker.getName() + " advanced " + advRsp.getNumArmies() + " into " + atkRsp.getDfdCountry() + ".");
				this.map.getCountries().get(atkRsp.getAtkCountry()).addArmies(-1 * advRsp.getNumArmies());
				this.map.getCountries().get(atkRsp.getDfdCountry()).addArmies(advRsp.getNumArmies());
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
	
	private void checkForElimination(Player attacker, String loserName, String takenCountryName, boolean allowReinforce) throws PlayerEliminatedException {
		try {
			if (RiskUtils.getPlayerCountries(this.map, loserName).size() == 0) {
				eliminate(getPlayerObject(loserName), attacker, "You were eliminated by " + attacker.getName() + " at " + takenCountryName + ".");
			}
		}
		catch (PlayerEliminatedException defenderException) {
			//this ensures that attacker will not be allowed to reinforce if (s)he was auto-eliminated during the advanceArmies() call.
			if (allowReinforce) {
				reinforce(attacker, false);//note that if the current player fails to reinforce, the player can be eliminated here and an exception thrown back up to begin()
			}
		}
	}
	
	private void fortify(Player currentPlayer) {
		int attempts = 0;
		while (attempts < RiskConstants.MAX_ATTEMPTS) {
			attempts++;
			FortifyResponse rsp = tryFortify(currentPlayer, createCardSetCopy(currentPlayer.getName()), getPlayerCardCounts());
			if (rsp != null) {
				if (FortifyResponse.isValidResponse(rsp, this.map, currentPlayer.getName())) {
					writeLogLn(currentPlayer.getName() + " is transferring " + rsp.getNumArmies() + " from " + rsp.getFromCountry() + " to " + rsp.getToCountry() + ".");
					this.map.getCountries().get(rsp.getFromCountry()).addArmies(-1 * rsp.getNumArmies());
					this.map.getCountries().get(rsp.getToCountry()).addArmies(rsp.getNumArmies());
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
			ReinforcementResponse rsp = player.getInitialAllocation(this.map.getCopy(), reinforcements);
			validatePlayerName(player);
			return rsp;
		}
		catch (Exception e) {
			return null;
		}
	}
	
	private CardTurnInResponse tryTurnIn(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards, boolean turnInRequired) {
		try {
			CardTurnInResponse rsp = player.proposeTurnIn(this.map.getCopy(), cardSet, oppCards, turnInRequired);
			validatePlayerName(player);
			return rsp;
		}
		catch (Exception e) {
			return null;
		}
	}
	
	private ReinforcementResponse tryReinforce(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards, int reinforcements) {
		try {
			ReinforcementResponse rsp = player.reinforce(this.map.getCopy(), createCardSetCopy(player.getName()), oppCards, reinforcements);
			validatePlayerName(player);
			return rsp;
		}
		catch (Exception e) {
			return null;
		}
	}
	
	private AttackResponse tryAttack(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards) {
		try {
			AttackResponse rsp = player.attack(this.map.getCopy(), createCardSetCopy(player.getName()), oppCards);
			validatePlayerName(player);
			return rsp;
		}
		catch (Exception e) {
			return null;
		}
	}
	
	private DefendResponse tryDefend(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards, AttackResponse atkRsp) {
		try {
			DefendResponse rsp = player.defend(this.map.getCopy(), createCardSetCopy(player.getName()), oppCards, atkRsp.getAtkCountry(), atkRsp.getDfdCountry(), atkRsp.getNumDice());
			validatePlayerName(player);
			return rsp;
		}
		catch (Exception e) {
			return null;
		}
	}
	
	private AdvanceResponse tryAdvance(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards, AttackResponse atkRsp) {
		try {
			AdvanceResponse rsp = player.advance(this.map.getCopy(), createCardSetCopy(player.getName()), oppCards, atkRsp.getAtkCountry(), atkRsp.getDfdCountry(), atkRsp.getNumDice());
			validatePlayerName(player);
			return rsp;
		}
		catch (Exception e) {
			return null;
		}
	}
	
	private FortifyResponse tryFortify(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards) {
		try {
			FortifyResponse rsp = player.fortify(this.map.getCopy(), createCardSetCopy(player.getName()), oppCards);
			validatePlayerName(player);
			return rsp;
		}
		catch (Exception e) {
			return null;
		}
	}
	
	private boolean validateInitialAllocation(Map<String, Integer> allocation, Collection<String> playerCountries, int armies) {
		Map<String, Boolean> allocatedCheck = new HashMap<String, Boolean>();
		for (String country : playerCountries) {
			allocatedCheck.put(country, false);
		}
		for (String country : allocation.keySet()) {
			if (!playerCountries.contains(country)) {
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
	
	private void allocateArmies(Map<String, Integer> allocation) {
		for (Map.Entry<String, Integer> entry : allocation.entrySet()) {
			this.map.getCountries().get(entry.getKey()).setArmies(entry.getValue());
		}
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
						if (this.map.getCountries().containsKey(rsp.getBonusCountry()) && this.map.getCountries().get(rsp.getBonusCountry()).getOwner().equals(currentPlayer.getName())) {
							this.map.getCountries().get(rsp.getBonusCountry()).addArmies(RiskConstants.BONUS_COUNTRY_ARMIES);
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
			if (!valid && attempts == 0) {
				System.out.println("Player Cards:");
				for (Card card : this.playerCardMap.get(currentPlayer.getName())) {
					System.out.println("\t" + card);
				}
				System.out.println("Player Proposed Turn-In:");
				for (Card card : this.playerCardMap.get(currentPlayer.getName())) {
					System.out.println("\t" + card);
				}
				attempts = attempts + 0;
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
	
	private Player getOwnerObject(String countryName) {
		String playerName = this.map.getCountries().get(countryName).getOwner();
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
		for (Country country : this.map.getCountries().values()) {
			newDeck.add(new Card(RiskConstants.REG_CARD_TYPES[i % RiskConstants.REG_CARD_TYPES.length], country.getName()));
			i++;
		}
		for (i = 0; i < RiskConstants.NUM_WILD_CARDS; i++) {
			newDeck.add(new Card(RiskConstants.WILD_CARD, null));
		}
		shuffleCards(newDeck);
		this.deck = new LinkedList<Card>(newDeck);
	}
	
	public static void shuffleCards(List<Card> cardList) {
		Random rand = new Random();
		int j;
		Card temp;
		for (int i = 0; i < cardList.size(); i++) {
			j = rand.nextInt(cardList.size());
			temp = cardList.get(i);
			cardList.set(i, cardList.get(j));
			cardList.set(j, temp);
		}
	}
	
	private void loadPlayers(String playerFile) {
		writeLogLn("Loading players...");
		this.playerMap = new HashMap<String, Player>();
		/*loop {
			if this.playerMap.containsKey(player.getName()) {
				throw new IllegalArgumentException("No two players can )have the same name: " + player.getName());
			}
			else {
				this.players.add(player);
			}
		}*/
		this.allPlayers = new ArrayList<String>();
		this.playerMap.put("Player 1", new DefaultPlayer("Player 1"));
		this.allPlayers.add("Player 1");
		this.playerMap.put("Player 2", new DefaultPlayer("Player 2"));
		this.allPlayers.add("Player 2");
		this.playerMap.put("Player 3", new DefaultPlayer("Player 3"));
		this.allPlayers.add("Player 3");
		this.playerMap.put("Seth", new Seth());
		this.allPlayers.add("Seth");
		this.players = new ArrayList<String>(this.allPlayers);
		shufflePlayers(this.players);//choose a random turn order
		this.playerCardMap = new HashMap<String, Collection<Card>>();
		for (Player player : this.playerMap.values()) {
			this.playerCardMap.put(player.getName(), new ArrayList<Card>());
		}
	}
	
	public static void shufflePlayers(List<String> playerList) {
		Random rand = new Random();
		int j;
		String temp;
		for (int i = 0; i < playerList.size(); i++) {
			j = rand.nextInt(playerList.size());
			temp = playerList.get(i);
			playerList.set(i, playerList.get(j));
			playerList.set(j, temp);
		}
	}
	
	//only allocates unowned countries
	private void allocateCountries() {
		if (this.players.size() > 0) {
			writeLogLn("Allocating countries...");
			Collection<Country> countries = this.map.getCountries().values();
			int i = 0;
			for (Country country : countries) {
				if (country.getOwner() == null) {
					country.setOwner(this.playerMap.get(this.players.get(i % this.players.size())).getName());
					if (this.round > 0) {
						//If these countries are being eliminated during a game,
						//it is due to a player being eliminated by the Master,
						//and so the re-allocated countries must be occupied.
						country.setArmies(1);
					}
					i++;
				}
			}
		}
	}
	
	private void eliminate(Player loser, Player eliminator, String reason) throws PlayerEliminatedException {
		if (this.playerMap.containsKey(loser.getName())) {
			writeLogLn(loser.getName() + " Eliminated! " + reason);
			for (Country country : this.map.getCountries().values()) {
				if (country.getOwner().equals(loser.getName())) {
					if (eliminator != null) {
						country.setOwner(eliminator.getName());
					}
					else {
						country.setOwner(null);
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
			allocateCountries();
			throw new PlayerEliminatedException(loser.getName() + " Eliminated! " + reason);
		}
	}
	
	private void writeLogLn(String line) {
		try {
			this.log.write(line + "\r\n");
			this.log.flush();
		}
		catch (IOException e) {
			return;
		}
	}
	
	private void writeStatsLn() {
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
			return;
		}
	}
	
	public static void main(String[] args) throws IOException {
		try {
			GameMaster game = new GameMaster("Countries.txt", null);
			game.begin();
		}
		catch (IOException e) {
			throw e;
		}
	}
}