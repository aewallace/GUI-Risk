package Player;

import java.util.Collection;
import java.util.Map;

import Map.Country;
import Map.RiskMap;
import Response.AttackResponse;
import Response.CardTurnInResponse;
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
						if (dfdCountry == null) {
							dfdCountry = neighbor;
						}
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
}
