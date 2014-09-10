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
	protected static final int MIN_SCORE = -9999;
	protected String name;
	protected Country lastCountryReinforced;
	protected int lastCardCount;
	protected boolean hasGottenCard;
	protected boolean conqueringRun;
	
	public Seth() {
		this.name = "Seth";
		this.lastCountryReinforced = null;
		this.lastCardCount = 0;
		this.hasGottenCard = false;
		this.conqueringRun = false;
	}
	
	public Seth(String nameIn) {
		this.name = nameIn;
		this.lastCountryReinforced = null;
		this.lastCardCount = 0;
		this.hasGottenCard = false;
		this.conqueringRun = false;
	}
	
	/**
	 * Allocates armies as in every other reinforcement stage.
	 */
	public ReinforcementResponse getInitialAllocation(RiskMap map, int reinforcements) {
		ReinforcementResponse rsp = new ReinforcementResponse();
		Set<Country> myCountries = RiskUtils.getPlayerCountries(map, this.name);
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
		CardTurnInResponse rsp = turnInCards(map, myCards);
		if (rsp != null) {
			this.lastCardCount -= RiskConstants.NUM_CARD_TURN_IN;
		}
		return rsp;
	}
	
	/**
	 * Finds a set of cards that can be turned in, or null if none exists.
	 */
	private CardTurnInResponse turnInCards(RiskMap map, Collection<Card> myCards) {
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
	private Collection<Card> threeOfAKind(Collection<Card> myCards, Deque<Card> wildCards) {
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
	private Collection<Card> oneOfEach(Collection<Card> myCards, Deque<Card> wildCards) {
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
		ReinforcementResponse rsp = new ReinforcementResponse();
		Map<Continent, Integer> continentAttainability = getallAttainabilities(map, 0);
		Continent targetContinent = maxScoreContinent(getallAttainabilities(map, 0));
		
		reinforcements -= reinforceOwnedContinents(map, rsp, reinforcements);
		
		if (targetContinent != null) {
			reinforcements -= reinforceTargetContinent(map, rsp, targetContinent, reinforcements);
		}
		else {
			reinforcements -= reinforceAll(map, rsp, reinforcements, continentAttainability);
		}
		return rsp;
	}
	
	/**
	 * Reinforces the external boundary countries of all owned continents.
	 */
	private int reinforceOwnedContinents(RiskMap map, ReinforcementResponse rsp, int reinforcements) {
		int remaining = reinforcements;
		for (Continent continent : RiskUtils.getPlayerContinents(map, this.name)) {
			for (Country country : continent.getCountries()) {
				if (remaining > 0) {
					int adjacentEnemyArmies = 0;
					for (Country neighbor : country.getNeighbors()) {
						if (!map.getCountryOwner(neighbor).equals(this.name)) {
							adjacentEnemyArmies += map.getCountryArmies(neighbor);
						}
					}
					if (adjacentEnemyArmies > 0) {
						int diff = adjacentEnemyArmies - map.getCountryArmies(country);
						if (diff > 0) {
							if (remaining >= diff) {
								remaining -= rsp.reinforce(country, diff);
							}
							else {
								remaining -= rsp.reinforce(country, remaining);
							}
							this.lastCountryReinforced = country;
						}
					}
				}
			}
		}
		return reinforcements - remaining;
	}
	
	/**
	 * Reinforces countries in or around the target continent, if any.
	 */
	private int reinforceTargetContinent(RiskMap map, ReinforcementResponse rsp, Continent targetContinent, int reinforcements) {
		int remaining = reinforcements;
		boolean found = false;
		do {
			for (Country currentCountry : targetContinent.getCountries()) {
				if (map.getCountryOwner(currentCountry).equals(this.name)) {
					for (Country neighbor : currentCountry.getNeighbors()) {
						if (!map.getCountryOwner(neighbor).equals(this.name)) {
							if (remaining > 0) {
								remaining -= rsp.reinforce(currentCountry, 1);
								found = true;
							}
						}
						else {
							for (Country nbrNeighbor : neighbor.getNeighbors()) {
								if (nbrNeighbor.getContinent() == targetContinent
									&& !map.getCountryOwner(nbrNeighbor).equals(this.name)) {
									if (remaining > 0) {
										remaining -= rsp.reinforce(neighbor, 1);
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
							if (remaining > 0) {
								remaining -= rsp.reinforce(neighbor, 1);
								found = true;
							}
						}
					}
				}
			}
		} while (remaining > 0 && found);
		return reinforcements - remaining;
	}
	
	/**
	 * Reinforces throughout the entire set of owned countries, giving precedence to attainable continents.
	 */
	private int reinforceAll(RiskMap map, ReinforcementResponse rsp, int reinforcements, Map<Continent, Integer> continentAttainability) {
		int remaining = reinforcements;
		
		boolean hopeless = maxScoreContinent(continentAttainability) == null;
		Set<Country> myCountries = RiskUtils.getPlayerCountries(map, this.name);
		boolean beginReinforce = !myCountries.contains(this.lastCountryReinforced);
		
		Set<Country> reinforceable = new HashSet<Country>();
		boolean useSet = false;
		while (remaining > 0) {
			for (Country currentCountry : useSet ? reinforceable : myCountries) {
				if (hopeless || continentAttainability.get(currentCountry.getContinent()) >= 0) {
					for (Country neighbor : currentCountry.getNeighbors()) {
						if (!map.getCountryOwner(neighbor).equals(this.name) && remaining > 0) {
							if (beginReinforce) {
								remaining -= rsp.reinforce(currentCountry, 1);
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
							&& remaining > 0) {
							if (beginReinforce) {
								remaining -= rsp.reinforce(currentCountry, 1);
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
		return reinforcements - remaining;
	}
	
	/**
	 * First, goes for a target continent, using a favorable strength diff, favoring shared neighbors.
	 * If no ideal option exists, and no card has been attained, be more aggressive.
	 */
	public AttackResponse attack(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards) {
		this.hasGottenCard = this.hasGottenCard || myCards.size() > this.lastCardCount;
		AttackResponse rsp = decide(map, true);
		
		if (rsp == null && !this.hasGottenCard) {
			rsp = decide(map, false);
		}
		return rsp;
	}
	
	private AttackResponse decide(RiskMap map, boolean tryIdealAttack) {
		AttackDecider decider = new AttackDecider(this.name);
		decider.useCombinedAttackStrength = true;
		decider.useFirstValidOption = false;
		decider.useStrDiffThreshold = true;
		
		if (tryIdealAttack) {
			decider.attackSharedNeighborsFirst = true;
			decider.useLowestStrengthDiff = true;
			decider.useHighestStrengthDiff = false;
			decider.strDiffThresh = 2;
			decider.targetContinent = getTargetContinent(map, 0);
			decider.useTargetContinent = decider.targetContinent != null;
			if (getContinentAttainability(map, decider.targetContinent, 0) < AttackDecider.MIN_SCORE) {
				this.conqueringRun = false;
				return null;
			}
			else {
				this.conqueringRun = true;
			}
		}
		else {
			this.conqueringRun = false;
			decider.attackSharedNeighborsFirst = false;
			decider.useLowestStrengthDiff = false;
			decider.useHighestStrengthDiff = true;
			decider.useTargetContinent = false;
			decider.strDiffThresh = -2;
		}
		return decider.determineBattleground(map);
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
		int pastEnemyArmies = 0, futureEnemyArmies = 0;
		boolean fromBorder = false;
		for (Country neighbor : fromCountry.getNeighbors()) {
			if (!map.getCountryOwner(neighbor).equals(this.name)){
				enemyNeighbors++;
				pastEnemyArmies += map.getCountryArmies(neighbor);
				if (RiskUtils.areConnected(map, toCountry, neighbor, this.name, false)) {
					futureEnemyArmies += map.getCountryArmies(neighbor) + 1;
				}
				if (neighbor.getContinent() != fromCountry.getContinent()) {
					fromBorder = true;
				}
			}
		}
		if (this.conqueringRun
			&& !fromBorder
			&& fromCountry.getContinent() == toCountry.getContinent()) {
			rsp.setNumArmies(maxAdvance);
			return rsp;
		}
		boolean toInternal = true;
		for (Country neighbor : toCountry.getNeighbors()) {
			if (!map.getCountryOwner(neighbor).equals(this.name)) {
				toInternal = false;
				if (!fromCountry.getNeighbors().contains(neighbor)) {
					futureEnemyArmies += map.getCountryArmies(neighbor) + 1;
				}
			}
		}
		if (toInternal) {
			rsp.setNumArmies(enemyNeighbors == 0 ? maxAdvance : min);
			return rsp;
		}
		//if leaving a border country of a controlled continent, leave enough to match enemy neighbors
		if (fromBorder && RiskUtils.playerControlsContinent(map, fromCountry.getContinent(), this.name)) {
			if (maxAdvance - pastEnemyArmies > min) {
				rsp.setNumArmies(maxAdvance - pastEnemyArmies);
			}
			else {
				rsp.setNumArmies(min);
			}
			return rsp;
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
		this.lastCardCount = myCards.size();
		//reset card flag for next turn
		this.hasGottenCard = false;
		FortifyResponse rsp = new FortifyResponse();
		Collection<Set<Country>> allConnectedSets = RiskUtils.getAllConnectedCountrySets(map, this.name);
		Map<Continent, Integer> continentBaseScores = getallAttainabilities(map, 0);
		Continent targetContinent = maxScoreContinent(continentBaseScores);
		
		rsp = fortifyInternalExternal(map, allConnectedSets, continentBaseScores, targetContinent);
		if (rsp != null) {
			return rsp;
		}
		else {
			return fortifyExteriorExterior(map, allConnectedSets, targetContinent);
		}
	}
	
	/**
	 * Searches all connected country sets for the most effective interior-exterior fortification move.
	 */
	private FortifyResponse fortifyInternalExternal(RiskMap map, Collection<Set<Country>> allConnectedSets, Map<Continent, Integer> continentBaseScores, Continent targetContinent) {
		Country strongestFrom = null, weakestTo = null;
		int bestEnemyStr = 0, bestTargetEnemyStr = 0;
		Country borderFortifyFrom = null, borderFortifyTo = null;
		int bestBorderStrDiff = 1;
		for (Set<Country> connectedSet : allConnectedSets) {
			Set<Country> interiorCountries = RiskUtils.filterCountriesByBorderStatus(map, this.name, connectedSet, true);
			Set<Country> exteriorCountries = RiskUtils.filterCountriesByBorderStatus(map, this.name, connectedSet, false);
			Country interiorCountry = null, exteriorCountry = null;
			for (Country currentCountry : interiorCountries) {
				if (interiorCountry == null && map.getCountryArmies(currentCountry) > 1
					|| interiorCountry != null
					&& map.getCountryArmies(currentCountry) > map.getCountryArmies(interiorCountry)) {
					interiorCountry = currentCountry;
				}
			}
			for (Country currentCountry : exteriorCountries) {
				int enemyForcesInTarget = 0;
				int enemyStr = 0;
				//this country might be the external border of an owned continent
				boolean isOwnedBorder = continentBaseScores.get(currentCountry.getContinent()) == MIN_SCORE;
				for (Country neighbor : currentCountry.getNeighbors()) {
					if (!map.getCountryOwner(neighbor).equals(this.name)) {
						enemyStr += map.getCountryArmies(neighbor);
						if (neighbor.getContinent() == targetContinent) {
							enemyForcesInTarget += map.getCountryArmies(neighbor);
						}
					}
					else if (neighbor.getContinent() != currentCountry.getContinent()
							&& interiorCountries.contains(neighbor)
							&& continentBaseScores.get(neighbor.getContinent()) == MIN_SCORE) {
						isOwnedBorder = true;
					}
				}
				int strDiff = map.getCountryArmies(currentCountry) - enemyStr;
				if (isOwnedBorder
					&& strDiff <= 0
					&& (borderFortifyTo == null
						|| strDiff < bestBorderStrDiff)) {
					borderFortifyTo = currentCountry;
					borderFortifyFrom = interiorCountry;
				}
				if (exteriorCountry == null
					|| enemyForcesInTarget > bestTargetEnemyStr) {
					exteriorCountry = currentCountry;
					bestTargetEnemyStr = enemyForcesInTarget;
				}
				else if (enemyForcesInTarget == bestTargetEnemyStr
						&& map.getCountryArmies(currentCountry) <= map.getCountryArmies(exteriorCountry)) {
					if (exteriorCountry != null && map.getCountryArmies(currentCountry) == map.getCountryArmies(exteriorCountry)) {
						if (enemyStr > bestEnemyStr) {
							exteriorCountry = currentCountry;
							bestEnemyStr = enemyStr;
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
		if (borderFortifyFrom != null && borderFortifyTo != null) {
			strongestFrom = borderFortifyFrom;
			weakestTo = borderFortifyTo;
		}
		if (strongestFrom != null && weakestTo != null) {
			FortifyResponse rsp = new FortifyResponse();
			rsp.setFromCountry(strongestFrom);
			rsp.setToCountry(weakestTo);
			rsp.setNumArmies(map.getCountryArmies(strongestFrom) - 1);
			return rsp;
		}
		else {
			return null;
		}
	}
	
	/**
	 * Searches all connected country sets for the most effective exterior-exterior fortification move.
	 * This is (generally) less desirable than an interior-exterior fortification, as no trapped armies
	 * are being brought back into immediate play.
	 */
	private FortifyResponse fortifyExteriorExterior(RiskMap map, Collection<Set<Country>> allConnectedSets, Continent targetContinent) {
		Country strongestFrom = null, weakestTo = null;
		int bestEnemyStr = 0, bestTargetEnemyStr = 0;
		for (Set<Country> connectedSet : allConnectedSets) {
			Country lclStrongest = null, lclWeakest = null;
			Set<Country> exteriorCountries = RiskUtils.filterCountriesByBorderStatus(map, this.name, connectedSet, false);
			if (exteriorCountries.size() == 0) {
				//the only way this could happen is if the player has already won, so decline
				return null;
			}
			for (Country currentCountry : exteriorCountries) {
				int enemyForcesInTarget = 0;
				int enemyStr = 0;
				for (Country neighbor : currentCountry.getNeighbors()) {
					if (!map.getCountryOwner(neighbor).equals(this.name)) {
						enemyStr += map.getCountryArmies(neighbor);
						if (neighbor.getContinent() == targetContinent) {
							enemyForcesInTarget += map.getCountryArmies(neighbor);
						}
					}
				}
				if (lclWeakest == null
					|| enemyForcesInTarget > bestTargetEnemyStr) {
					lclWeakest = currentCountry;
					bestTargetEnemyStr = enemyForcesInTarget;
				}
				else if (enemyForcesInTarget == bestTargetEnemyStr
						&& map.getCountryArmies(currentCountry) <= map.getCountryArmies(lclWeakest)) {
					if (lclWeakest != null && map.getCountryArmies(currentCountry) == map.getCountryArmies(lclWeakest)) {
						if (enemyStr > bestEnemyStr) {
							lclWeakest = currentCountry;
							bestEnemyStr = enemyStr;
						}
					}
					else {
						lclWeakest = currentCountry;
					}
				}
				if (lclStrongest == null || map.getCountryArmies(currentCountry) > map.getCountryArmies(lclStrongest)) {
					lclStrongest = currentCountry;
				}
				if (lclWeakest == null || map.getCountryArmies(currentCountry) < map.getCountryArmies(lclWeakest)) {
					lclWeakest = currentCountry;
				}
			}
			if (strongestFrom == null && weakestTo == null
				|| map.getCountryArmies(lclStrongest) - map.getCountryArmies(lclWeakest)
				> map.getCountryArmies(strongestFrom) - map.getCountryArmies(weakestTo)) {
				strongestFrom = lclStrongest;
				weakestTo = lclWeakest;
			}
		}
		FortifyResponse rsp = new FortifyResponse();
		rsp.setFromCountry(strongestFrom);
		rsp.setToCountry(weakestTo);
		rsp.setNumArmies(map.getCountryArmies(strongestFrom) / 3);
		return rsp;
	}
	
	/**
	 * Finds the most attainable continent that is not already owned by this player.
	 */
	private Continent getTargetContinent(RiskMap map, int additionalArmies) {
		int bestScore = MIN_SCORE;
		Continent bestContinent = null;
		for (Entry<Continent, Integer> entry : getallAttainabilities(map, additionalArmies).entrySet()) {
			if (entry.getValue() > bestScore) {
				bestContinent = entry.getKey();
				bestScore = entry.getValue();
			}
		}
		return bestContinent;
	}
	
	/**
	 * Finds the continent with the highest score from an existing map.
	 */
	private Continent maxScoreContinent(Map<Continent, Integer> scores) {
		int best = MIN_SCORE;
		Continent bestContinent = null;
		for (Entry<Continent, Integer> entry : scores.entrySet()) {
			if (entry.getValue() > best) {
				best = entry.getValue();
				bestContinent = entry.getKey();
			}
		}
		return bestContinent;
	}
	
	/**
	 * Get attainability scores for all continents.
	 */
	private Map<Continent, Integer> getallAttainabilities(RiskMap map, int additionalArmies) {
		Map<Continent, Integer> scores = new HashMap<Continent, Integer>();
		for (Continent continent : Continent.values()) {
			scores.put(continent, getContinentAttainability(map, continent, additionalArmies));
		}
		return scores;
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
			return MIN_SCORE;
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
	public static final int MIN_SCORE = 5;
	
	String playerName;

	boolean useHighestStrengthDiff;//chooses the option with the largest strength mismatch
	boolean useLowestStrengthDiff;//chooses the option with the smallest strength mismatch
	boolean useStrDiffThreshold;//disallows any options in which the player does not have a sufficient advantage
	int strDiffThresh;//the required minimum strength difference in favor of the player
	boolean useTargetContinent;//gives precedence to attacking the target continent
	Continent targetContinent;//a continent that is used as a short-term goal
	boolean useFirstValidOption;//used when an arbitrary decision must be made between > 1 valid options
	boolean attackSharedNeighborsFirst;//gives precedence to breadth-first attack choices, rather than depth-first
	boolean useCombinedAttackStrength;//calculate strDiff as all possible friendly armies adjacent to an enemy - enemy armies
	
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
		this.useCombinedAttackStrength = true;
	}
	
	/**
	 * Given parameters for the desired decision logic, determine where to attack.
	 * NOTE: If this method is not given enough information to determine a battlefield, it will not make any assumptions, and will return null.
	 *     For example, if useHighestStrengthDiff is set to false, and no other metrics are specified.
	 */
	public AttackResponse determineBattleground(RiskMap map) {
		Set<Country> myCountries = RiskUtils.getPlayerCountries(map, this.playerName);
		AttackResponse rsp = new AttackResponse();
		Country atkCountry = null, dfdCountry = null, sharedAtk = null, sharedDfd = null;
		int bestStrDiff, bestSharedStrDiff, bestSharedCount = 0;
		if (this.useLowestStrengthDiff) {
			bestStrDiff = 9999;
			bestSharedStrDiff = 9999;
		}
		else {
			bestStrDiff = MIN_SCORE;
			bestSharedStrDiff = MIN_SCORE;
		}
		for (Country currentCountry : myCountries) {
			if (map.getCountryArmies(currentCountry) > 1) {
				//can attack FROM this country
				for (Country neighbor : currentCountry.getNeighbors()) {
					if (!map.getCountryOwner(neighbor).equals(this.playerName)) {
						//is an ENEMY country
						boolean skipThisOption = false;
						boolean needsDistributedAttack = true;
						int sharedNeighbors = 0;
						int combinedStrDiff = 0;
						for (Country nbrNeighbor : neighbor.getNeighbors()) {
							if (map.getCountryOwner(nbrNeighbor).equals(this.playerName)) {
								if (!this.useStrDiffThreshold
									|| map.getCountryArmies(nbrNeighbor) >= map.getCountryArmies(neighbor) + this.strDiffThresh) {
									needsDistributedAttack = false;
								}
								if (this.useStrDiffThreshold && map.getCountryArmies(nbrNeighbor) > this.strDiffThresh) {
									combinedStrDiff += map.getCountryArmies(nbrNeighbor) - 1;
								}
								if (!(nbrNeighbor == currentCountry)) {
									sharedNeighbors++;
									if (this.useTargetContinent && nbrNeighbor.getContinent().equals(this.targetContinent)) {
										sharedNeighbors++;
									}
								}
								if (map.getCountryArmies(neighbor) <= RiskConstants.MAX_DFD_DICE
									&& map.getCountryArmies(nbrNeighbor) > map.getCountryArmies(currentCountry)) {
									//if this country is the main attack force's only way into the target continent, defer
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
									if (skipThisOption) {
										//only defer to the main attack force if you can't handle all of these countries yourself
										int totalEnemiesLeft = map.getCountryArmies(currentCountry);
										Set<Country> enemyCountriesLeft = RiskUtils.getConnectedCountries(map, neighbor, this.playerName, false, true);
										for (Country remainingEnemy : enemyCountriesLeft) {
											totalEnemiesLeft -= map.getCountryArmies(remainingEnemy) - 1;
											if (this.useStrDiffThreshold) {
												totalEnemiesLeft -= this.strDiffThresh;
											}
										}
										skipThisOption = totalEnemiesLeft < 0;
									}
								}
							}
						}
						if (!skipThisOption) {
							if (!needsDistributedAttack) {
								combinedStrDiff = map.getCountryArmies(currentCountry);
							}
							if (sharedNeighbors > bestSharedCount) {
								bestSharedCount = sharedNeighbors;
							}
							if (!this.useTargetContinent || neighbor.getContinent().equals(this.targetContinent)) {
								int newStrDiff;
								if (useCombinedAttackStrength) {
									newStrDiff = combinedStrDiff - map.getCountryArmies(neighbor);
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
