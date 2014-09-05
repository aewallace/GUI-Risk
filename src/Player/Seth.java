package Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import Map.Continent;
import Map.Country;
import Map.RiskMap;
import Response.AdvanceResponse;
import Response.AttackResponse;
import Response.CardTurnInResponse;
import Response.FortifyResponse;
import Response.ReinforcementResponse;
import Util.Card;
import Util.RiskConstants;
import Util.RiskUtils;

public class Seth extends DefaultPlayer {
	private static final String NAME = "Seth";
	private int lastCardCount;
	
	public Seth() {
		super(NAME);
		this.lastCardCount = 0;
	}
	
	/**
	 * Allocates armies as in every other reinforcement stage.
	 */
	@Override
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
	@Override
	public CardTurnInResponse proposeTurnIn(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, boolean turnInRequired) {
		if (turnInRequired) {
			this.lastCardCount -= RiskConstants.NUM_CARD_TURN_IN;
			return turnInCards(myCards);
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
	@Override
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
			for (int continentRep = 0; continentRep < 2; continentRep++) {
				for (Country currentCountry : targetContinent.getCountries()) {
					if (map.getCountryOwner(currentCountry).equals(this.name)) {
						for (Country neighbor : currentCountry.getNeighbors()) {
							if (!map.getCountryOwner(neighbor).equals(this.name) && reinforcements > 0) {
								reinforcements -= rsp.reinforce(currentCountry, 1);
							}
						}
					}
				}
			}
		}
		while (reinforcements > 0) {
			for (Country currentCountry : myCountries) {
				if (hopeless || continentAttainability.get(currentCountry.getContinent()) >= 0) {
					if (beginReinforce && reinforcements > 0) {
						for (Country neighbor : currentCountry.getNeighbors()) {
							if (!map.getCountryOwner(neighbor).equals(this.name) && reinforcements > 0) {
								reinforcements -= rsp.reinforce(currentCountry, 1);
								this.lastCountryReinforced = currentCountry;
							}
						}
					}
					if (this.lastCountryReinforced == currentCountry) {
						beginReinforce = true;
					}
				}
			}
			beginReinforce = true;
		}
		if (!ReinforcementResponse.isValidResponse(rsp, map, this.name, temp)) {
			ReinforcementResponse.isValidResponse(rsp, map, this.name, temp);
			return rsp;
		}
		return rsp;
	}
	
	/**
	 * Only attacks opponents when the attacking country is stronger than the defending country, unless a card has not yet been awarded.
	 */
	@Override
	public AttackResponse attack(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards) {
		boolean hasGottenCard = myCards.size() > this.lastCardCount;
		if (hasGottenCard) {
			this.lastCardCount = myCards.size() - 1;
		}
		Collection<Country> myCountries = RiskUtils.getPlayerCountries(map, this.name);
		AttackDecider decider = new AttackDecider(this.name);
		decider.useHighestStrengthDiff = true;
		decider.requirePositiveStrengthDiff = true;
		decider.useTargetContinent = true;
		decider.targetContinent = getTargetContinent(map, 0);
		decider.attackSharedNeighborsFirst = true;
		AttackResponse rsp = decider.determineBattleground(map, myCountries);
		
		if (rsp != null) {
			return rsp;
		}
		else if (!hasGottenCard) {
			decider.useTargetContinent = false;
			decider.requirePositiveStrengthDiff = false;
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
	@Override
	public AdvanceResponse advance(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, Country fromCountry, Country toCountry, int min) {
		AdvanceResponse rsp = new AdvanceResponse();
		Continent targetContinent = getTargetContinent(map, 0);
		int maxAdvance = map.getCountryArmies(fromCountry) - 1;
		int enemyNeighbors = 0;
		for (Country neighbor : fromCountry.getNeighbors()) {
			if (!map.getCountryOwner(neighbor).equals(this.name)){
				enemyNeighbors++;
			}
		}
		if (enemyNeighbors > 0 && fromCountry.getContinent().equals(targetContinent)) {
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
	 * Interior refers to a country surrounded by only friendly neighbors, and an exterior country
	 * has at least one enemy neighbor.
	 */
	@Override
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
				for (Country country : exteriorCountries) {
					if (strongestFrom == null || map.getCountryArmies(country) > map.getCountryArmies(strongestFrom)) {
						lclStrongest = country;
					}
					if (weakestTo == null || map.getCountryArmies(country) < map.getCountryArmies(weakestTo)) {
						lclWeakest = country;
					}
				}
				if (strongestFrom == null && weakestTo == null
					|| map.getCountryArmies(lclStrongest) > map.getCountryArmies(strongestFrom)
					&& map.getCountryArmies(lclWeakest) < map.getCountryArmies(weakestTo)) {
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
				if (bestContinent == null || score > bestScore) {
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
		boolean isAlreadyOwned = true;
		
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
		}
		
		if (isAlreadyOwned) {
			return -9999;
		}
		else {
			return myArmies + myCountries - enemyArmies - enemyCountries;
		}
	}
}

class AttackDecider {
	String playerName;
	
	boolean useHighestStrengthDiff;//chooses the option with the highest strength mismatch
	boolean requirePositiveStrengthDiff;//disallows any options in which the player is the underdog
	boolean useTargetContinent;//gives precedence to attacking the target continent
	boolean useFirstValidOption;//used when an arbitrary decision must be made between > 1 valid options
	Continent targetContinent;//a continent that is used as a short-term goal
	boolean attackSharedNeighborsFirst;//gives precedence to breadth-first attack choices, rather than depth-first
	
	public AttackDecider(String playerName) {
		this.playerName = playerName;
		//set default values
		this.useFirstValidOption = true;
		this.useHighestStrengthDiff = true;
		this.requirePositiveStrengthDiff = true;
		this.useTargetContinent = false;
		this.targetContinent = null;
		this.attackSharedNeighborsFirst = true;
	}
	
	/**
	 * Given parameters for the desired decision logic, determine where to attack.
	 * NOTE: If this method is not given enough information to determine a battlefield, it will not make any assumptions, and will return null.
	 *     For example, if useHighestStrengthDiff is set to false, and no other metrics are specified.
	 */
	public AttackResponse determineBattleground(RiskMap map, Collection<Country> myCountries) {
		AttackResponse rsp = new AttackResponse();
		Country atkCountry = null, dfdCountry = null, sharedAtk = null, sharedDfd = null;
		int bestStrDiff = -9999, bestSharedStrDiff = -9999, bestSharedCount = 0, bestSharedEnemyCount = 9999;
		for (Country currentCountry : myCountries) {
			if (map.getCountryArmies(currentCountry) > 1) {
				//can attack FROM this country
				for (Country neighbor : currentCountry.getNeighbors()) {
					if (!map.getCountryOwner(neighbor).equals(this.playerName)) {
						//is an ENEMY country
						int sharedNeighbors = 0, sharedEnemies = 0;;
						if (this.attackSharedNeighborsFirst) {
							for (Country nbrNeighbor : neighbor.getNeighbors()) {
								if (map.getCountryOwner(nbrNeighbor).equals(this.playerName) && !(nbrNeighbor == currentCountry)) {
									sharedNeighbors++;
									sharedEnemies++;
									if (this.useTargetContinent && nbrNeighbor.getContinent().equals(this.targetContinent)) {
										sharedNeighbors++;
									}
								}
							}
							if (sharedNeighbors > bestSharedCount) {
								bestSharedCount = sharedNeighbors;
								bestSharedEnemyCount = sharedEnemies;
							}
						}
						if (!this.useTargetContinent || neighbor.getContinent().equals(this.targetContinent)) {
							int newStrDiff = map.getCountryArmies(currentCountry) - map.getCountryArmies(neighbor);
							if (this.useHighestStrengthDiff) {
								if (newStrDiff > bestStrDiff) {
									if (!this.requirePositiveStrengthDiff || newStrDiff > 0) {
										bestStrDiff = newStrDiff;
										atkCountry = currentCountry;
										dfdCountry = neighbor;
									}
								}
								if (sharedNeighbors == bestSharedCount
									&& sharedEnemies <= bestSharedEnemyCount
									&& newStrDiff > bestSharedStrDiff) {
									if (!this.requirePositiveStrengthDiff || newStrDiff > 0) {
										bestSharedStrDiff = newStrDiff;
										sharedAtk = currentCountry;
										sharedDfd = neighbor;
									}
								}
							}
							else if (useFirstValidOption && atkCountry == null &&  dfdCountry == null
									&& (!this.requirePositiveStrengthDiff || newStrDiff > 0)) {
								bestStrDiff = newStrDiff;
								atkCountry = currentCountry;
								dfdCountry = neighbor;
								if (sharedNeighbors == bestSharedCount
									&& sharedEnemies <= bestSharedEnemyCount
									&& newStrDiff > bestSharedStrDiff) {
									if (!this.requirePositiveStrengthDiff || newStrDiff > 0) {
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
