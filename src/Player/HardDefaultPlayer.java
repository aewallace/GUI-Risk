package Player;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import Map.Continent;
import Map.Country;
import Map.RiskMap;
import Response.AdvanceResponse;
import Response.AttackResponse;
import Response.FortifyResponse;
import Response.ReinforcementResponse;
import Util.Card;
import Util.RiskConstants;
import Util.RiskUtils;

public class HardDefaultPlayer extends NormalDefaultPlayer {

	public HardDefaultPlayer(String nameIn) {
		super(nameIn);
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
	 * Reinforces countries evenly, with a circular ordering to ensure complete cross-turn reinforcement.
	 * Only reinforces external countries, and assigns more armies to countries with more enemy neighbors.
	 */
	@Override
	public ReinforcementResponse reinforce(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, int reinforcements) {
		ReinforcementResponse rsp = new ReinforcementResponse();
		Collection<Country> myCountries = RiskUtils.filterCountriesByBorderStatus(map, this.name, RiskUtils.getPlayerCountries(map, this.name), false);
		boolean beginReinforce = this.lastCountryReinforced == null || !myCountries.contains(this.lastCountryReinforced);
		while (reinforcements > 0) {
			for (Country country : myCountries) {
				for (Country neighbor : country.getNeighbors()) {
					if (!map.getCountryOwner(neighbor).equals(this.name) && reinforcements > 0) {
						if (beginReinforce) {
							reinforcements -= rsp.reinforce(country, 1);
							this.lastCountryReinforced = country;
						}
					}
				}
				if (this.lastCountryReinforced == country) {
					beginReinforce = true;
				}
			}
		}
		return rsp;
	}
	
	/**
	 * For the first country that can attack a neighbor, attack the weakest neighbor with the maximum number of dice.
	 * Only attacks countries that are weaker than the attacker by at least a certain threshold.
	 */
	@Override
	public AttackResponse attack(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards) {
		AttackResponse rsp = new AttackResponse();
		Collection<Country> myCountries = RiskUtils.getPlayerCountries(map, this.name);
		Country atkCountry = null, dfdCountry = null;
		int strDiffThresh = 2;
		for (Country currentCountry : myCountries) {
			if (map.getCountryArmies(currentCountry) > 1) {
				for (Country neighbor : currentCountry.getNeighbors()) {
					if (!map.getCountryOwner(neighbor).equals(this.name)
						&& (dfdCountry == null
						|| map.getCountryArmies(neighbor) < map.getCountryArmies(dfdCountry))
						&& map.getCountryArmies(currentCountry) > map.getCountryArmies(neighbor) + strDiffThresh) {
						atkCountry = currentCountry;
						dfdCountry = neighbor;
					}
				}
			}
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
	
	/**
	 * Advance all available armies, unless the conquered country is still external.
	 * In that case, leave an army behind.
	 */
	@Override
	public AdvanceResponse advance(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, Country fromCountry, Country toCountry, int min) {
		AdvanceResponse rsp = new AdvanceResponse();
		int maxAdvance = map.getCountryArmies(fromCountry) - 1;
		boolean external = false;
		for (Country neighbor : fromCountry.getNeighbors()) {
			if (!map.getCountryOwner(neighbor).equals(this.name)){
				external = true;
			}
		}
		if (external) {
			rsp.setNumArmies(maxAdvance - 1);
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
					|| map.getCountryArmies(currentCountry) <= map.getCountryArmies(exteriorCountry)) {
					exteriorCountry = currentCountry;
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
			return null;
		}
	}
}
