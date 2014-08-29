package Player;

import java.util.Collection;
import java.util.Map;
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
	 * Only returns a set when it is required.
	 */
	@Override
	public CardTurnInResponse proposeTurnIn(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, boolean turnInRequired) {
		if (turnInRequired) {
			return turnInCards(myCards);
		}
		else {
			return null;
		}
	}
	
	/**
	 * Only reinforces countries that have enemy neighbors. For each enemy-facing country, add one reinforcement per neighboring enemy.
	 * Still enforces cyclical country ordering.
	 */
	@Override
	public ReinforcementResponse reinforce(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, int reinforcements) {
		ReinforcementResponse rsp = new ReinforcementResponse();
		Collection<String> myCountries = RiskUtils.getPlayerCountries(map, this.name);
		boolean beginReinforce = !myCountries.contains(this.lastCountryReinforced);
		while (reinforcements > 0) {
			for (String countryName : myCountries) {
				if (beginReinforce && reinforcements > 0) {
					for (Country neighbor : map.getCountry(countryName).getNeighbors()) {
						if (!neighbor.getOwner().equals(this.name) && reinforcements > 0) {
							rsp.reinforce(countryName, 1);
							reinforcements--;
							this.lastCountryReinforced = countryName;
						}
					}
				}
				if (this.lastCountryReinforced.equals(countryName)) {
					beginReinforce = true;
				}
			}
			beginReinforce = true;
		}
		return rsp;
	}
	
	class AtkParams {
		boolean useHighestStrengthDiff;
		boolean requirePositiveStrengthDiff;
	}
	
	/**
	 * Only attacks opponents when the attacking country is stronger than the defending  country.
	 */
	@Override
	public AttackResponse attack(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards) {
		boolean hasGottenCard = myCards.size() > this.lastCardCount;
		if (hasGottenCard) {
			this.lastCardCount = myCards.size();
		}
		Collection<String> myCountries = RiskUtils.getPlayerCountries(map, this.name);
		AtkParams atkParams = new AtkParams();
		atkParams.useHighestStrengthDiff = true;
		atkParams.requirePositiveStrengthDiff = true;
		AttackResponse rsp = determineBattleground(map, myCountries, atkParams);
		
		if (rsp != null) {
			return rsp;
		}
		else if (!hasGottenCard) {
			atkParams.requirePositiveStrengthDiff = false;
			rsp = determineBattleground(map, myCountries, atkParams);
			return rsp;
		}
		else {
			return null;
		}
	}
	
	private AttackResponse determineBattleground(RiskMap map, Collection<String> myCountries, AtkParams params) {
		AttackResponse rsp = new AttackResponse();
		Country atkCountry = null, dfdCountry = null, currentCountry;
		int bestStrDiff = -9999;
		for (String countryName : myCountries) {
			currentCountry = map.getCountries().get(countryName);
			if (currentCountry.getNumArmies() > 1) {
				for (Country neighbor : currentCountry.getNeighbors()) {
					if (!neighbor.getOwner().equals(this.name)) {
						if (params.useHighestStrengthDiff) {
							int newStrDiff = currentCountry.getNumArmies() - neighbor.getNumArmies();
							if (newStrDiff > bestStrDiff) {
								if (!params.requirePositiveStrengthDiff || newStrDiff > 0) {
									bestStrDiff = newStrDiff;
									atkCountry = currentCountry;
									dfdCountry = neighbor;
								}
							}
						}
					}
				}
			}
		}
		if (atkCountry != null && dfdCountry != null) {
			rsp.setAtkCountry(atkCountry.getName());
			rsp.setDfdCountry(dfdCountry.getName());
			int dice = atkCountry.getNumArmies() - 1;
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
	

	
	/**
	 * Find the interior country which has the most armies to spare, and transfer all extra armies to
	 * the weakest exterior country that is connected to it.
	 * Interior refers to a country surrounded by only friendly neighbors, and an exterior country
	 * has at least one enemy neighbor.
	 */
	public FortifyResponse fortify(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards) {
		FortifyResponse rsp = new FortifyResponse();
		Collection<Set<String>> allConnectedSets = RiskUtils.getAllConnectedCountrySets(map, this.name);
		Country strongestFrom = null, weakestTo = null;
		for (Set<String> connectedSet : allConnectedSets) {
			Collection<String> interiorCountries = RiskUtils.selectCountriesByBorderStatus(map, this.name, connectedSet, true);
			Collection<String> exteriorCountries = RiskUtils.selectCountriesByBorderStatus(map, this.name, connectedSet, false);
			Country interiorCountry = null, exteriorCountry = null;
			for (String countryName : interiorCountries) {
				if (interiorCountry == null && map.getCountry(countryName).getNumArmies() > 1
					|| map.getCountry(countryName).getNumArmies() > interiorCountry.getNumArmies()) {
					interiorCountry = map.getCountry(countryName);
				}
			}
			for (String countryName : exteriorCountries) {
				if (exteriorCountry == null
					|| map.getCountry(countryName).getNumArmies() > exteriorCountry.getNumArmies()) {
					exteriorCountry = map.getCountry(countryName);
				}
			}
			if (strongestFrom == null && weakestTo == null
				|| (interiorCountry != null && exteriorCountry != null
				&& interiorCountry.getNumArmies() - exteriorCountry.getNumArmies()
				> strongestFrom.getNumArmies() - weakestTo.getNumArmies())) {
				strongestFrom = interiorCountry;
				weakestTo = exteriorCountry;
			}
		}
		if (strongestFrom != null && weakestTo != null) {
			rsp.setFromCountry(strongestFrom.getName());
			rsp.setToCountry(weakestTo.getName());
			rsp.setNumArmies(strongestFrom.getNumArmies());
			return rsp;
		}
		return null;
	}
}
