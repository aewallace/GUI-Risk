package Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import Map.Country;
import Map.RiskMap;
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
		Collection<String> myCountries = RiskUtils.getPlayerCountries(map, this.name);
		//For initial allocation, every country must have at least one army
		for (String countryName : myCountries) {
			reinforcements -= rsp.reinforce(countryName, 1);
		}
		//now, add remaining reinforcements as they would be allocated normally
		for (Entry<String, Integer> entry : reinforce(map, null, null, reinforcements).getAllocation().entrySet()) {
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
		ReinforcementResponse rsp = new ReinforcementResponse();
		Collection<String> myCountries = RiskUtils.getPlayerCountries(map, this.name);
		Collection<String> myContinents = RiskUtils.getPlayerContinents(map, this.name);
		Map<String, Integer> continentAttainability = new HashMap<String, Integer>();
		boolean hopeless = true;
		int best = -9999;
		String targetContinent = null;
		for (String continentName : map.getContinents().keySet()) {
			int score = getContinentAttainability(map, continentName, 0);
			continentAttainability.put(continentName, score);
			if (score >= 0) {
				hopeless = false;
			}
			if (score > best) {
				best = score;
				targetContinent = continentName;
			}
		}
		boolean beginReinforce = !myCountries.contains(this.lastCountryReinforced);
		for (String continentName : myContinents) {
			for (Country country : map.getContinent(continentName).getCountries()) {
				if (reinforcements > 0) {
					int adjacentEnemyArmies = 0;
					for (Country neighbor : country.getNeighbors()) {
						if (!neighbor.getOwner().equals(this.name)) {
							adjacentEnemyArmies += neighbor.getNumArmies();
						}
					}
					if (adjacentEnemyArmies > 0) {
						int diff = adjacentEnemyArmies - country.getNumArmies();
						if (diff > 0) {
							if (reinforcements >= diff) {
								reinforcements -= rsp.reinforce(country.getName(), diff);
							}
							else {
								reinforcements -= rsp.reinforce(country.getName(), reinforcements);
							}
							this.lastCountryReinforced = country.getName();
						}
					}
				}
			}
		}
		if (targetContinent != null) {
			//if there is a target continent, double the reinforcements there
			for (int continentRep = 0; continentRep < 2; continentRep++) {
				for (Country currentCountry : map.getContinent(targetContinent).getCountries()) {
					if (currentCountry.getOwner().equals(this.name)) {
						for (Country neighbor : currentCountry.getNeighbors()) {
							if (!neighbor.getOwner().equals(this.name) && reinforcements > 0) {
								reinforcements -= rsp.reinforce(currentCountry.getName(), 1);
							}
						}
					}
				}
			}
		}
		while (reinforcements > 0) {
			for (String countryName : myCountries) {
				Country currentCountry = map.getCountry(countryName);
				if (hopeless || continentAttainability.get(currentCountry.getContinent()) >= 0) {
					if (beginReinforce && reinforcements > 0) {
						for (Country neighbor : currentCountry.getNeighbors()) {
							if (!neighbor.getOwner().equals(this.name) && reinforcements > 0) {
								reinforcements -= rsp.reinforce(countryName, 1);
								this.lastCountryReinforced = countryName;
							}
						}
					}
					if (this.lastCountryReinforced.equals(countryName)) {
						beginReinforce = true;
					}
				}
			}
			beginReinforce = true;
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
		Collection<String> myCountries = RiskUtils.getPlayerCountries(map, this.name);
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
	 * Find the interior country which has the most armies to spare, and transfer all extra armies to
	 * the weakest exterior country that is connected to it.
	 * Interior refers to a country surrounded by only friendly neighbors, and an exterior country
	 * has at least one enemy neighbor.
	 */
	@Override
	public FortifyResponse fortify(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards) {
		FortifyResponse rsp = new FortifyResponse();
		Collection<Set<String>> allConnectedSets = RiskUtils.getAllConnectedCountrySets(map, this.name);
		Country strongestFrom = null, weakestTo = null;
		for (Set<String> connectedSet : allConnectedSets) {
			Collection<String> interiorCountries = RiskUtils.filterCountriesByBorderStatus(map, this.name, connectedSet, true);
			Collection<String> exteriorCountries = RiskUtils.filterCountriesByBorderStatus(map, this.name, connectedSet, false);
			Country interiorCountry = null, exteriorCountry = null;
			for (String countryName : interiorCountries) {
				if (interiorCountry == null && map.getCountry(countryName).getNumArmies() > 1
					|| interiorCountry != null
					&& map.getCountry(countryName).getNumArmies() > interiorCountry.getNumArmies()) {
					interiorCountry = map.getCountry(countryName);
				}
			}
			for (String countryName : exteriorCountries) {
				if (exteriorCountry == null
					|| exteriorCountry != null
					&& map.getCountry(countryName).getNumArmies() < exteriorCountry.getNumArmies()) {
					exteriorCountry = map.getCountry(countryName);
				}
			}
			if (interiorCountry != null && exteriorCountry != null
				&& (strongestFrom == null && weakestTo == null
				|| interiorCountry.getNumArmies() > strongestFrom.getNumArmies())) {
				strongestFrom = interiorCountry;
				weakestTo = exteriorCountry;
			}
		}
		if (strongestFrom != null && weakestTo != null) {
			rsp.setFromCountry(strongestFrom.getName());
			rsp.setToCountry(weakestTo.getName());
			rsp.setNumArmies(strongestFrom.getNumArmies() - 1);
			return rsp;
		}
		return null;
	}
	
	/**
	 * Finds the most attainable continent that is not already owned by this player.
	 */
	private String getTargetContinent(RiskMap map, int additionalArmies) {
		int bestScore = -9999;
		String bestContinent = null;
		for (String continentName : map.getContinents().keySet()) {
			if (!RiskUtils.playerControlsContinent(map.getContinent(continentName), this.name)) {
				int score = getContinentAttainability(map, continentName, additionalArmies);
				if (bestContinent == null || score > bestScore) {
					bestContinent = continentName;
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
	private int getContinentAttainability(RiskMap map, String continentName, int additionalArmies) {
		int myCountries = 0;
		int enemyCountries = 0;
		int myArmies = additionalArmies;
		int enemyArmies = 0;
		boolean isAlreadyOwned = true;
		
		for (Country country : map.getContinent(continentName).getCountries()) {
			if (country.getOwner().equals(this.name)) {
				myCountries++;
				myArmies += country.getNumArmies();
			}
			else {
				isAlreadyOwned = false;
				enemyCountries++;
				enemyArmies += country.getNumArmies();
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
	String targetContinent;//a continent that is used as a short-term goal
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
	public AttackResponse determineBattleground(RiskMap map, Collection<String> myCountries) {
		AttackResponse rsp = new AttackResponse();
		Country atkCountry = null, dfdCountry = null, sharedAtk = null, sharedDfd = null;
		int bestStrDiff = -9999, bestSharedStrDiff = -9999, bestSharedCount = 0, bestSharedEnemyCount = 9999;
		for (String countryName : myCountries) {
			Country currentCountry = map.getCountries().get(countryName);
			if (currentCountry.getNumArmies() > 1) {
				//can attack FROM this country
				for (Country neighbor : currentCountry.getNeighbors()) {
					if (!neighbor.getOwner().equals(this.playerName)) {
						//is an ENEMY country
						int sharedNeighbors = 0, sharedEnemies = 0;;
						if (this.attackSharedNeighborsFirst) {
							for (Country nbrNeighbor : neighbor.getNeighbors()) {
								if (nbrNeighbor.getOwner().equals(this.playerName) && !nbrNeighbor.equals(currentCountry)) {
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
							int newStrDiff = currentCountry.getNumArmies() - neighbor.getNumArmies();
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
			rsp.setAtkCountry(atkCountry.getName());
			rsp.setDfdCountry(dfdCountry.getName());
			int dice = atkCountry.getNumArmies() - 1;
			if (dice > RiskConstants.MAX_ATK_DICE) {
				dice = RiskConstants.MAX_ATK_DICE;
			}
			rsp.setNumDice(dice);
			if (rsp.getAtkCountry().equals("Ukraine") && rsp.getDfdCountry().equals("Northern Europe")) {
				rsp = rsp;
			}
			return rsp;
		}
		else {
			return null;
		}
	}
}
