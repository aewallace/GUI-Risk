package Player;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import Map.Country;
import Map.RiskMap;
import Response.AttackResponse;
import Response.FortifyResponse;
import Response.ReinforcementResponse;
import Util.Card;
import Util.RiskConstants;
import Util.RiskUtils;

public class NormalDefaultPlayer extends EasyDefaultPlayer {

	public NormalDefaultPlayer(String nameIn) {
		super(nameIn);
	}
	
	/**
	 * Allocates armies as in every other reinforcement stage.
	 */
	@Override
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
	 * Reinforces countries evenly, with a circular ordering to ensure complete cross-turn reinforcement.
	 * Only reinforces external countries.
	 */
	@Override
	public ReinforcementResponse reinforce(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, int reinforcements) {
		ReinforcementResponse rsp = new ReinforcementResponse();
		Set<Country> myCountries = RiskUtils.filterCountriesByBorderStatus(map, this.name, RiskUtils.getPlayerCountries(map, this.name), false);
		boolean beginReinforce = this.lastCountryReinforced == null || !myCountries.contains(this.lastCountryReinforced);
		while (reinforcements > 0) {
			for (Country country : myCountries) {
				if (beginReinforce && reinforcements > 0) {
					rsp.reinforce(country, 1);
					reinforcements--;
					this.lastCountryReinforced = country;
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
	 * Only attacks countries that are weaker than the attacker.
	 */
	@Override
	public AttackResponse attack(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards) {
		AttackResponse rsp = new AttackResponse();
		Set<Country> myCountries = RiskUtils.getPlayerCountries(map, this.name);
		Country atkCountry = null, dfdCountry = null;
		for (Country currentCountry : myCountries) {
			if (map.getCountryArmies(currentCountry) > 1) {
				for (Country neighbor : currentCountry.getNeighbors()) {
					if (!map.getCountryOwner(neighbor).equals(this.name)
						&& (dfdCountry == null
						|| map.getCountryArmies(neighbor) < map.getCountryArmies(dfdCountry))
						&& map.getCountryArmies(currentCountry) > map.getCountryArmies(neighbor)) {
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
	 * Find the first interior country with available armies and a neighboring exterior country, and transfer the armies to the neighbor.
	 * Interior refers to a country surrounded by only friendly neighbors, and exterior has at least one enemy neighbor.
	 */
	@Override
	public FortifyResponse fortify(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards) {
		FortifyResponse rsp = new FortifyResponse();
		Set<Country> myCountries = RiskUtils.getPlayerCountries(map, this.name);
		for (Country currentCountry : myCountries) {
			Country exteriorNeighbor = null;
			if (map.getCountryArmies(currentCountry) > 1) {
				boolean isInterior = true;
				for (Country neighbor : currentCountry.getNeighbors()) {
					if (!map.getCountryOwner(neighbor).equals(this.name)) {
						isInterior = false;
					}
					else if (exteriorNeighbor == null) {
						for (Country potentialEnemy : neighbor.getNeighbors()) {
							if (!map.getCountryOwner(potentialEnemy).equals(this.name)) {
								exteriorNeighbor = neighbor;
							}
						}
					}
				}
				if (isInterior && exteriorNeighbor != null) {
					rsp.setFromCountry(currentCountry);
					rsp.setToCountry(exteriorNeighbor);
					rsp.setNumArmies(map.getCountryArmies(currentCountry) - 1);
					return rsp;
				}
			}
		}
		return null;
	}
}
