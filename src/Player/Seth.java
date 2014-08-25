package Player;

import java.util.Collection;
import java.util.Map;

import Map.Country;
import Map.RiskMap;
import Response.CardTurnInResponse;
import Response.FortifyResponse;
import Util.Card;
import Util.RiskUtils;

public class Seth extends DefaultPlayer {
	private static final String NAME = "Seth";
	
	public Seth() {
		super(NAME);
	}
	
	/**
	 * Always returns a set when it is available.
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
	 * Find the first interior country with available armies and a neighboring exterior country, and transfer the armies to the neighbor.
	 * Interior refers to a country surrounded by only friendly neighbors, and exterior has at least one enemy neighbor.
	 */
	@Override
	public FortifyResponse fortify(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards) {
		FortifyResponse rsp = new FortifyResponse();
		Collection<String> myCountries = RiskUtils.getPlayerCountries(map, this.name);
		for (String countryName : myCountries) {
			Country currentCountry = map.getCountries().get(countryName);
			Country exteriorNeighbor = null;
			if (currentCountry.getNumArmies() > 1) {
				boolean isInterior = true;
				for (Country neighbor : currentCountry.getNeighbors()) {
					if (!neighbor.getOwner().equals(this.name)) {
						isInterior = false;
					}
					else if (exteriorNeighbor == null) {
						for (Country potentialEnemy : neighbor.getNeighbors()) {
							if (!potentialEnemy.getOwner().equals(this.name)) {
								exteriorNeighbor = neighbor;
							}
						}
					}
				}
				if (isInterior && exteriorNeighbor != null) {
					rsp.setFromCountry(currentCountry.getName());
					rsp.setToCountry(exteriorNeighbor.getName());
					rsp.setNumArmies(currentCountry.getNumArmies() - 1);
					return rsp;
				}
			}
		}
		return null;
	}
}
