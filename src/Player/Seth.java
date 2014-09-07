package Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import Map.Continent;
import Map.Country;
import Map.RiskMap;
import Response.AdvanceResponse;
import Response.AttackResponse;
import Response.CardTurnInResponse;
import Response.DefendResponse;
import Response.FortifyResponse;
import Response.ReinforcementResponse;
import Util.Card;
import Util.RiskConstants;
import Util.RiskUtils;

public class Seth implements Player {
	protected final String name = "Seth";
	protected Country lastCountryReinforced;
	protected int lastCardCount;
	
	public Seth() {
		this.lastCountryReinforced = null;
		this.lastCardCount = 0;
	}
	
	/**
	 * Allocates armies as in every other reinforcement stage.
	 */
	public ReinforcementResponse getInitialAllocation(RiskMap map, int reinforcements) {
		ReinforcementResponse rsp = new ReinforcementResponse();
		Collection<Country> myCountries = RiskUtils.getPlayerCountries(map, this.name);
		//For initial allocation, every country must have at least one army
		for (Country country : myCountries) {
			reinforcements -= rsp.reinforce(country, 1);
		}
		//now, add remaining reinforcements as they would be allocated normally
		for (Entry<Country, Integer> entry : reinforce(map, null, null, reinforcements).getAllocation().entrySet()) {
			rsp.reinforce(entry.getKey(), entry.getValue());
		}
		
		return rsp;
	}
	
	/**
	 * Only returns a set when it is required.
	 */
	public CardTurnInResponse proposeTurnIn(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, boolean turnInRequired) {
		this.lastCardCount -= RiskConstants.NUM_CARD_TURN_IN;
		return turnInCards(map, myCards);
	}
	
	/**
	 * Finds a set of cards that can be turned in, or null if none exists.
	 */
	protected CardTurnInResponse turnInCards(RiskMap map, Collection<Card> myCards) {
		Deque<Card> wildCards = new LinkedList<Card>();
		//find all wilds
		for (Card card : myCards) {
			if (card.getType().equals(RiskConstants.WILD_CARD)) {
				wildCards.add(card);
			}
		}
		CardTurnInResponse rsp = new CardTurnInResponse();
		//three of a kind
		Collection<Card> cardSet = threeOfAKind(myCards, wildCards);
		if (cardSet == null) {
			//one of each
			cardSet = oneOfEach(myCards, wildCards);
		}
		if (cardSet != null) {
			for (Card card : cardSet) {
				if (card.getCountry() != null
					&& map.getCountryOwner(card.getCountry()).equals(this.name)) {
					rsp.setBonusCountry(card.getCountry());
				}
				rsp.addCard(card);
			}
			return rsp;
		}
		else {
			return null;
		}
	}
	
	/**
	 * Finds a Three-of-a-Kind set of cards that can be turned in, or null if none exists.
	 */
	protected Collection<Card> threeOfAKind(Collection<Card> myCards, Deque<Card> wildCards) {
		Collection<Card> cardSet = new ArrayList<Card>();
		for (String type : RiskConstants.REG_CARD_TYPES) {
			for (Card card : myCards) {
				if (card.getType().equals(type) && cardSet.size() < RiskConstants.NUM_CARD_TURN_IN) {
					cardSet.add(card);
				}
			}
			if (cardSet.size() == RiskConstants.NUM_CARD_TURN_IN) {
				return cardSet;
			}
			else if (wildCards.size() >= RiskConstants.NUM_CARD_TURN_IN - cardSet.size()) {
				while (cardSet.size() < RiskConstants.NUM_CARD_TURN_IN) {
					cardSet.add(wildCards.remove());
				}
				return cardSet;
			}
			else {
				cardSet.clear();
			}
		}
		return null;
	}
	
	/**
	 * Finds a One-of-Each set of cards that can be turned in, or null if none exists.
	 */
	protected Collection<Card> oneOfEach(Collection<Card> myCards, Deque<Card> wildCards) {
		Collection<Card> cardSet = new ArrayList<Card>();
		boolean found;
		for (String type : RiskConstants.REG_CARD_TYPES) {
			found = false;
			for (Card card : myCards) {
				if (!found && card.getType().equals(type) && cardSet.size() < RiskConstants.NUM_CARD_TURN_IN) {
					cardSet.add(card);
					found = true;
				}
			}
		}
		if (cardSet.size() == RiskConstants.NUM_CARD_TURN_IN) {
			return cardSet;
		}
		else if (wildCards.size() >= RiskConstants.NUM_CARD_TURN_IN - cardSet.size()) {
			while (cardSet.size() < RiskConstants.NUM_CARD_TURN_IN) {
				cardSet.add(wildCards.remove());
			}
			return cardSet;
		}
		else {
			return null;
		}
	}
	
	/**
	 * Only reinforces countries that have enemy neighbors. Reinforce enemy-facing borders of controlled continents first,
	 * such that each of these countries has at least as many armies as there are enemy armies in adjacent countries.
	 * Afterward, for every enemy-facing country, add one reinforcement per neighboring enemy until reinforcements are exhausted.
	 * Still enforces cross-reinforce cyclical country ordering.
	 */
	public ReinforcementResponse reinforce(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, int reinforcements) {
		int temp = reinforcements;
		ReinforcementResponse rsp = new ReinforcementResponse();
		Collection<Country> myCountries = RiskUtils.getPlayerCountries(map, this.name);
		Collection<Continent> myContinents = RiskUtils.getPlayerContinents(map, this.name);
		Map<Continent, Integer> continentAttainability = new HashMap<Continent, Integer>();
		boolean hopeless = true;
		int best = -9999;
		Continent targetContinent = null;
		for (Continent continent : Continent.values()) {
			int score = getContinentAttainability(map, continent, 0);
			continentAttainability.put(continent, score);
			if (score >= 0) {
				hopeless = false;
			}
			if (score > best) {
				best = score;
				targetContinent = continent;
			}
		}
		boolean beginReinforce = !myCountries.contains(this.lastCountryReinforced);
		for (Continent continent : myContinents) {
			for (Country country : continent.getCountries()) {
				if (reinforcements > 0) {
					int adjacentEnemyArmies = 0;
					for (Country neighbor : country.getNeighbors()) {
						if (!map.getCountryOwner(neighbor).equals(this.name)) {
							adjacentEnemyArmies += map.getCountryArmies(neighbor);
						}
					}
					if (adjacentEnemyArmies > 0) {
						int diff = adjacentEnemyArmies - map.getCountryArmies(country);
						if (diff > 0) {
							if (reinforcements >= diff) {
								reinforcements -= rsp.reinforce(country, diff);
							}
							else {
								reinforcements -= rsp.reinforce(country, reinforcements);
							}
							this.lastCountryReinforced = country;
						}
					}
				}
			}
		}
		if (targetContinent != null) {
			//if there is a target continent, double the reinforcements there
			//for (int continentRep = 0; continentRep < 2; continentRep++) {
			boolean found = false;
			do {
				for (Country currentCountry : targetContinent.getCountries()) {
					if (map.getCountryOwner(currentCountry).equals(this.name)) {
						for (Country neighbor : currentCountry.getNeighbors()) {
							if (!map.getCountryOwner(neighbor).equals(this.name)) {
								if (reinforcements > 0) {
									reinforcements -= rsp.reinforce(currentCountry, 1);
									found = true;
								}
							}
							else {
								for (Country nbrNeighbor : neighbor.getNeighbors()) {
									if (nbrNeighbor.getContinent() == targetContinent
										&& !map.getCountryOwner(nbrNeighbor).equals(this.name)) {
										if (reinforcements > 0) {
											reinforcements -= rsp.reinforce(neighbor, 1);
											found = true;
										}
									}
								}
							}
						}
					}
					else {
						for (Country neighbor : currentCountry.getNeighbors()) {
							if (neighbor.getContinent() != targetContinent
								&& map.getCountryOwner(neighbor).equals(this.name)) {
								if (reinforcements > 0) {
									reinforcements -= rsp.reinforce(neighbor, 1);
									found = true;
								}
							}
						}
					}
				}
			} while (reinforcements > 0 && found);
		}
		else {
			Set<Country> reinforceable = new HashSet<Country>();
			boolean useSet = false;
			while (reinforcements > 0) {
				for (Country currentCountry : useSet ? reinforceable : myCountries) {
					if (hopeless || continentAttainability.get(currentCountry.getContinent()) >= 0) {
						for (Country neighbor : currentCountry.getNeighbors()) {
							if (!map.getCountryOwner(neighbor).equals(this.name) && reinforcements > 0) {
								if (beginReinforce) {
									reinforcements -= rsp.reinforce(currentCountry, 1);
									this.lastCountryReinforced = currentCountry;
								}
								if (!useSet) {
									reinforceable.add(currentCountry);
								}
							}
						}
					}
					else if (!hopeless) {
						for (Country neighbor : currentCountry.getNeighbors()) {
							if (!map.getCountryOwner(neighbor).equals(this.name)
								&& continentAttainability.get(neighbor.getContinent()) >= 0
								&& reinforcements > 0) {
								if (beginReinforce) {
									reinforcements -= rsp.reinforce(currentCountry, 1);
									this.lastCountryReinforced = currentCountry;
								}
								if (!useSet) {
									reinforceable.add(currentCountry);
								}
							}
						}
					}
					if (this.lastCountryReinforced == currentCountry) {
						beginReinforce = true;
					}
				}
				useSet = true;
				beginReinforce = true;
			}
		}
		return rsp;
	}
	
	/**
	 * First, goes for a target continent, using a favorable strength diff, favoring shared neighbors.
	 * If no ideal option exists, and no card has been attained, be more aggressive.
	 */
	public AttackResponse attack(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards) {
		boolean hasGottenCard = myCards.size() > this.lastCardCount;
		if (hasGottenCard) {
			this.lastCardCount = myCards.size() - 1;
		}
		Collection<Country> myCountries = RiskUtils.getPlayerCountries(map, this.name);
		AttackDecider decider = new AttackDecider(this.name);
		decider.useLowestStrengthDiff = true;
		decider.useHighestStrengthDiff = false;
		decider.useFirstValidOption = false;
		decider.useStrDiffThreshold = true;
		decider.strDiffThresh = 2;
		decider.targetContinent = getTargetContinent(map, 0);
		decider.useTargetContinent = decider.targetContinent != null && (getContinentAttainability(map, decider.targetContinent, 0) > 0 || !hasGottenCard);
		decider.attackSharedNeighborsFirst = true;
		decider.useSharedAttackStrength = true;
		AttackResponse rsp = decider.determineBattleground(map, myCountries);
		
		if (rsp != null) {
			return rsp;
		}
		else if (!hasGottenCard) {
			decider.useHighestStrengthDiff = true;
			decider.useLowestStrengthDiff = false;
			decider.useTargetContinent = false;
			decider.strDiffThresh = -2;
			rsp = decider.determineBattleground(map, myCountries);
			return rsp;
		}
		else {
			this.lastCardCount++;
			return null;
		}
	}
	
	/**
	 * Advance all available armies, unless the conquered country is on the target continent AND external.
	 * In that case, leave a small portion, determined by the number of armies available.
	 */
	public AdvanceResponse advance(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, Country fromCountry, Country toCountry, int min) {
		AdvanceResponse rsp = new AdvanceResponse();
		Continent targetContinent = getTargetContinent(map, 0);
		int maxAdvance = map.getCountryArmies(fromCountry) - 1;
		int enemyNeighbors = 0;
		int futureEnemyArmies = 0;
		boolean isBorder = false;
		for (Country neighbor : fromCountry.getNeighbors()) {
			if (!map.getCountryOwner(neighbor).equals(this.name)){
				enemyNeighbors++;
				if (RiskUtils.areConnected(map, toCountry, neighbor, this.name, false)) {
					futureEnemyArmies += map.getCountryArmies(neighbor) + 1;
				}
			}
			if (neighbor.getContinent() != neighbor.getContinent()) {
				isBorder = true;
			}
		}
		if (isBorder && RiskUtils.playerControlsContinent(map, fromCountry.getContinent(), this.name)) {
			if (maxAdvance - futureEnemyArmies - enemyNeighbors > min) {
				rsp.setNumArmies(maxAdvance - futureEnemyArmies - enemyNeighbors);
			}
			else {
				rsp.setNumArmies(min);
			}
			return rsp;
		}
		for (Country neighbor : toCountry.getNeighbors()) {
			if (!map.getCountryOwner(neighbor).equals(this.name)
				&& !fromCountry.getNeighbors().contains(neighbor)) {
				futureEnemyArmies += map.getCountryArmies(neighbor) + 1;
			}
		}
		if (enemyNeighbors > 0
			&& futureEnemyArmies > 0
			&& targetContinent != null
			&& fromCountry.getContinent().equals(targetContinent)
			&& maxAdvance < 3 * futureEnemyArmies) {
			if (maxAdvance >= 3 * enemyNeighbors) {
				rsp.setNumArmies(maxAdvance - enemyNeighbors);
			}
			else if (maxAdvance >= 2 * enemyNeighbors) {
				rsp.setNumArmies(maxAdvance - 1);
			}
			else {
				rsp.setNumArmies(maxAdvance);
			}
		}
		else {
			rsp.setNumArmies(maxAdvance);
		}
		if (rsp.getNumArmies() < min) {
			rsp.setNumArmies(min);
		}
		return rsp;
	}
	
	/**
	 * Find the interior country which has the most armies to spare, and transfer all extra armies to
	 * the weakest exterior country that is connected to it.
	 * If no internal-external fortification is possible, search for a favorable external-external fortification.
	 * Interior refers to a country surrounded by only friendly neighbors, and an exterior country
	 * has at least one enemy neighbor.
	 */
	public FortifyResponse fortify(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards) {
		FortifyResponse rsp = new FortifyResponse();
		Collection<Set<Country>> allConnectedSets = RiskUtils.getAllConnectedCountrySets(map, this.name);
		Country strongestFrom = null, weakestTo = null;
		int nearbyEnemyStr = 0;
		for (Set<Country> connectedSet : allConnectedSets) {
			Collection<Country> interiorCountries = RiskUtils.filterCountriesByBorderStatus(map, this.name, connectedSet, true);
			Collection<Country> exteriorCountries = RiskUtils.filterCountriesByBorderStatus(map, this.name, connectedSet, false);
			Country interiorCountry = null, exteriorCountry = null;
			for (Country country : interiorCountries) {
				if (interiorCountry == null && map.getCountryArmies(country) > 1
					|| interiorCountry != null
					&& map.getCountryArmies(country) > map.getCountryArmies(interiorCountry)) {
					interiorCountry = country;
				}
			}
			for (Country currentCountry : exteriorCountries) {
				if (exteriorCountry == null
					|| exteriorCountry != null
					&& map.getCountryArmies(currentCountry) <= map.getCountryArmies(exteriorCountry)) {
					if (exteriorCountry != null && map.getCountryArmies(currentCountry) == map.getCountryArmies(exteriorCountry)) {
						int enemyStr = 0;
						for (Country neighbor : currentCountry.getNeighbors()) {
							if (!map.getCountryOwner(neighbor).equals(this.name)) {
								enemyStr += map.getCountryArmies(neighbor);
							}
						}
						if (enemyStr > nearbyEnemyStr) {
							exteriorCountry = currentCountry;
						}
					}
					else {
						exteriorCountry = currentCountry;
					}
				}
			}
			if (interiorCountry != null && exteriorCountry != null
				&& (strongestFrom == null && weakestTo == null
				|| map.getCountryArmies(interiorCountry) > map.getCountryArmies(strongestFrom))) {
				strongestFrom = interiorCountry;
				weakestTo = exteriorCountry;
			}
		}
		if (strongestFrom != null && weakestTo != null) {
			rsp.setFromCountry(strongestFrom);
			rsp.setToCountry(weakestTo);
			rsp.setNumArmies(map.getCountryArmies(strongestFrom) - 1);
			return rsp;
		}
		else {
			strongestFrom = null;
			weakestTo = null;
			for (Set<Country> connectedSet : allConnectedSets) {
				Country lclStrongest = null, lclWeakest = null;
				Collection<Country> exteriorCountries = RiskUtils.filterCountriesByBorderStatus(map, this.name, connectedSet, false);
				if (exteriorCountries.size() == 0) {
					//the only way this could happen is if the player has already won
					return null;
				}
				for (Country country : exteriorCountries) {
					if (lclStrongest == null || map.getCountryArmies(country) > map.getCountryArmies(lclStrongest)) {
						lclStrongest = country;
					}
					if (lclWeakest == null || map.getCountryArmies(country) < map.getCountryArmies(lclWeakest)) {
						lclWeakest = country;
					}
				}
				if (strongestFrom == null && weakestTo == null
					|| map.getCountryArmies(lclStrongest) - map.getCountryArmies(lclWeakest)
					> map.getCountryArmies(strongestFrom) - map.getCountryArmies(weakestTo)) {
					strongestFrom = lclStrongest;
					weakestTo = lclWeakest;
				}
			}
			rsp.setFromCountry(strongestFrom);
			rsp.setToCountry(weakestTo);
			rsp.setNumArmies(map.getCountryArmies(strongestFrom) / 3);
			return rsp;
		}
	}
	
	/**
	 * Finds the most attainable continent that is not already owned by this player.
	 */
	private Continent getTargetContinent(RiskMap map, int additionalArmies) {
		int bestScore = -9999;
		Continent bestContinent = null;
		for (Continent continent : Continent.values()) {
			if (!RiskUtils.playerControlsContinent(map, continent, this.name)) {
				int score = getContinentAttainability(map, continent, additionalArmies);
				if (score > bestScore) {
					bestContinent = continent;
					bestScore = score;
				}
			}
		}
		return bestContinent;
	}
	
	/**
	 * Calculates the a score for a given continent that represents how attainable that continent is. Higher is more attainable.
	 * Takes into account:
	 *     number of countries owned vs un-owned
	 *     number of armies owned vs un-owned
	 */
	private int getContinentAttainability(RiskMap map, Continent continent, int additionalArmies) {
		int myCountries = 0;
		int enemyCountries = 0;
		int myArmies = additionalArmies;
		int enemyArmies = 0;
		int borderCountries = 0;
		boolean isAlreadyOwned = true;
		Set<Country> checked = new HashSet<Country>();
		
		for (Country country : continent.getCountries()) {
			if (map.getCountryOwner(country).equals(this.name)) {
				myCountries++;
				myArmies += map.getCountryArmies(country);
			}
			else {
				isAlreadyOwned = false;
				enemyCountries++;
				enemyArmies += map.getCountryArmies(country);
			}
			boolean isBorder = false;
			for (Country neighbor : country.getNeighbors()) {
				if (neighbor.getContinent() != continent) {
					isBorder = true;
					if (!checked.contains(neighbor)) {
						checked.add(neighbor);
						//only add armies, not countries, as the countries are not in the target continent, but the armies could eventually be
						if (map.getCountryOwner(neighbor).equals(this.name)) {
							myArmies += map.getCountryArmies(neighbor) - 1;
						}
						else {
							enemyArmies += map.getCountryArmies(neighbor) - 1;
						}
					}
				}
			}
			if (isBorder) {
				borderCountries++;
			}
		}
		
		//if it is already owned, or I cannot yet attack it, then it is worthless as a target
		if (isAlreadyOwned || myArmies == 0) {
			return -9999;
		}
		else {
			return myArmies + myCountries - enemyArmies - enemyCountries - borderCountries;
		}
	}
	
	/**
	 * Defend with as many dice as possible.
	 */
	public DefendResponse defend(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, Country atkCountry, Country dfdCountry, int numAtkDice) {
		DefendResponse rsp = new DefendResponse();
		int numDice = map.getCountryArmies(dfdCountry);
		if (numDice > RiskConstants.MAX_DFD_DICE) {
			numDice = RiskConstants.MAX_DFD_DICE;
		}
		rsp.setNumDice(numDice);
		return rsp;
	}

	public String getName() {
		return this.name;
	}
}

class AttackDecider {
	String playerName;

	boolean useHighestStrengthDiff;//chooses the option with the largest strength mismatch
	boolean useLowestStrengthDiff;//chooses the option with the smallest strength mismatch
	boolean useStrDiffThreshold;//disallows any options in which the player does not have a sufficient advantage
	int strDiffThresh;//the required minimum strength difference in favor of the player
	boolean useTargetContinent;//gives precedence to attacking the target continent
	Continent targetContinent;//a continent that is used as a short-term goal
	boolean useFirstValidOption;//used when an arbitrary decision must be made between > 1 valid options
	boolean attackSharedNeighborsFirst;//gives precedence to breadth-first attack choices, rather than depth-first
	boolean useSharedAttackStrength;//calculate strDiff as all possible friendly armies adjacent to an enemy - enemy armies
	
	public AttackDecider(String playerName) {
		this.playerName = playerName;
		//set default values
		this.useFirstValidOption = true;
		this.useHighestStrengthDiff = true;
		this.useLowestStrengthDiff = false;
		this.useStrDiffThreshold = true;
		this.strDiffThresh = 0;
		this.useTargetContinent = false;
		this.targetContinent = null;
		this.attackSharedNeighborsFirst = true;
		this.useSharedAttackStrength = true;
	}
	
	/**
	 * Given parameters for the desired decision logic, determine where to attack.
	 * NOTE: If this method is not given enough information to determine a battlefield, it will not make any assumptions, and will return null.
	 *     For example, if useHighestStrengthDiff is set to false, and no other metrics are specified.
	 */
	public AttackResponse determineBattleground(RiskMap map, Collection<Country> myCountries) {
		AttackResponse rsp = new AttackResponse();
		Country atkCountry = null, dfdCountry = null, sharedAtk = null, sharedDfd = null;
		int bestStrDiff, bestSharedStrDiff, bestSharedCount = 0, bestSharedEnemyCount = 9999;
		if (this.useLowestStrengthDiff) {
			bestStrDiff = 9999;
			bestSharedStrDiff = 9999;
		}
		else {
			bestStrDiff = -9999;
			bestSharedStrDiff = -9999;
		}
		for (Country currentCountry : myCountries) {
			if (map.getCountryArmies(currentCountry) > 1) {
				//can attack FROM this country
				for (Country neighbor : currentCountry.getNeighbors()) {
					if (!map.getCountryOwner(neighbor).equals(this.playerName)) {
						//is an ENEMY country
						boolean skipThisOption = false;
						boolean needsSharedAttack = true;
						int sharedNeighbors = 0, sharedEnemies = 0;
						int sharedStrDiff = 0;
						for (Country nbrNeighbor : neighbor.getNeighbors()) {
							if (map.getCountryOwner(nbrNeighbor).equals(this.playerName)) {
								if (!this.useStrDiffThreshold
									|| map.getCountryArmies(nbrNeighbor) >= map.getCountryArmies(neighbor) + this.strDiffThresh) {
									needsSharedAttack = false;
								}
								if (this.useStrDiffThreshold && map.getCountryArmies(neighbor) > this.strDiffThresh) {
									sharedStrDiff += map.getCountryArmies(currentCountry) - 2;
								}
								if (!(nbrNeighbor == currentCountry)) {
									sharedNeighbors++;
									sharedEnemies++;
									if (this.useTargetContinent && nbrNeighbor.getContinent().equals(this.targetContinent)) {
										sharedNeighbors++;
									}
								}
								if (map.getCountryArmies(neighbor) <= RiskConstants.MAX_DFD_DICE
									&& map.getCountryArmies(nbrNeighbor) > map.getCountryArmies(currentCountry)) {
									int numOptions = 0;
									for (Country mainAtkPathCountry : nbrNeighbor.getNeighbors()) {
										if (!map.getCountryOwner(mainAtkPathCountry).equals(this.playerName)) {
											if (!this.useTargetContinent
												|| mainAtkPathCountry.getContinent() == this.targetContinent) {
												numOptions++;
											}
										}
									}
									skipThisOption = numOptions == 1;
								}
							}
						}
						if (!skipThisOption) {
							if (!needsSharedAttack) {
								sharedStrDiff = map.getCountryArmies(currentCountry) - map.getCountryArmies(neighbor);
							}
							if (sharedNeighbors > bestSharedCount) {
								bestSharedCount = sharedNeighbors;
								bestSharedEnemyCount = sharedEnemies;
							}
							if (!this.useTargetContinent || neighbor.getContinent().equals(this.targetContinent)) {
								int newStrDiff;
								if (useSharedAttackStrength) {
									newStrDiff = sharedStrDiff;
								}
								else {
									newStrDiff = map.getCountryArmies(currentCountry) - map.getCountryArmies(neighbor);
								}
								if (this.useHighestStrengthDiff) {
									if (newStrDiff > bestStrDiff) {
										if (!this.useStrDiffThreshold || newStrDiff >= this.strDiffThresh) {
											bestStrDiff = newStrDiff;
											atkCountry = currentCountry;
											dfdCountry = neighbor;
										}
									}
									if (sharedNeighbors == bestSharedCount
										&& sharedEnemies <= bestSharedEnemyCount
										&& newStrDiff > bestSharedStrDiff) {
										if (!this.useStrDiffThreshold || newStrDiff >= this.strDiffThresh) {
											bestSharedStrDiff = newStrDiff;
											sharedAtk = currentCountry;
											sharedDfd = neighbor;
										}
									}
								}
								else if (this.useLowestStrengthDiff) {
									if (newStrDiff < bestStrDiff) {
										if (!this.useStrDiffThreshold || newStrDiff >= this.strDiffThresh) {
											bestStrDiff = newStrDiff;
											atkCountry = currentCountry;
											dfdCountry = neighbor;
										}
									}
									if (sharedNeighbors == bestSharedCount
										&& sharedEnemies <= bestSharedEnemyCount
										&& newStrDiff < bestSharedStrDiff) {
										if (!this.useStrDiffThreshold || newStrDiff >= this.strDiffThresh) {
											bestSharedStrDiff = newStrDiff;
											sharedAtk = currentCountry;
											sharedDfd = neighbor;
										}
									}
								}
								else if (useFirstValidOption && atkCountry == null &&  dfdCountry == null
										&& (!this.useStrDiffThreshold || newStrDiff >= this.strDiffThresh)) {
									bestStrDiff = newStrDiff;
									atkCountry = currentCountry;
									dfdCountry = neighbor;
									if (sharedNeighbors == bestSharedCount
										&& sharedEnemies <= bestSharedEnemyCount
										&& newStrDiff > bestSharedStrDiff) {
										if (!this.useStrDiffThreshold || newStrDiff >= this.strDiffThresh) {
											bestSharedStrDiff = newStrDiff;
											sharedAtk = currentCountry;
											sharedDfd = neighbor;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		if (this.attackSharedNeighborsFirst
			&& sharedAtk != null && sharedDfd != null
			&& (!this.useTargetContinent
				|| sharedDfd != null
				&& sharedDfd.getContinent().equals(this.targetContinent))) {
			atkCountry = sharedAtk;
			dfdCountry = sharedDfd;
		}
		if (atkCountry != null && dfdCountry != null) {
			rsp.setAtkCountry(atkCountry);
			rsp.setDfdCountry(dfdCountry);
			int dice = map.getCountryArmies(atkCountry) - 1;
			if (dice > RiskConstants.MAX_ATK_DICE) {
				dice = RiskConstants.MAX_ATK_DICE;
			}
			rsp.setNumDice(dice);
			return rsp;
		}
		else {
			return null;
		}
	}
}
