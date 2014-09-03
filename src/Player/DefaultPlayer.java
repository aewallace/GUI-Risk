package Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;

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

/**
 * Implements the methods in the Player interface with
 * minimally viable simplicity.
 */
public class DefaultPlayer implements Player {
	protected String name;
	protected String lastCountryReinforced;
	
	public DefaultPlayer(String nameIn) {
		this.name = nameIn;
		this.lastCountryReinforced = null;
	}
	
	public String getName() {
		return this.name;
	}
	
	/**
	 * Allocates armies evenly.
	 */
	public ReinforcementResponse getInitialAllocation(RiskMap map, int reinforcements) {
		ReinforcementResponse rsp = new ReinforcementResponse();
		Collection<String> myCountries = RiskUtils.getPlayerCountries(map, this.name);
		while (reinforcements > 0) {
			for (String countryName : myCountries) {
				if (reinforcements > 0) {
					rsp.reinforce(countryName, 1);
					reinforcements--;
					this.lastCountryReinforced = countryName;
				}
			}
		}
		return rsp;
	}
	
	/**
	 * Always returns a set when it is available.
	 */
	public CardTurnInResponse proposeTurnIn(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, boolean turnInRequired) {
		return turnInCards(myCards);
	}
	
	/**
	 * Finds a set of cards that can be turned in, or null if none exists.
	 * @param myCards
	 * @return turn-in proposal
	 */
	protected CardTurnInResponse turnInCards(Collection<Card> myCards) {
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
	 * @param myCards
	 * @param wildCards
	 * @return
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
	 * @param myCards
	 * @param wildCards
	 * @return card set
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
	 * Reinforces countries evenly, with a circular ordering to ensure complete cross-turn reinforcement.
	 */
	public ReinforcementResponse reinforce(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, int reinforcements) {
		ReinforcementResponse rsp = new ReinforcementResponse();
		Collection<String> myCountries = RiskUtils.getPlayerCountries(map, this.name);
		boolean beginReinforce = !myCountries.contains(this.lastCountryReinforced);
		while (reinforcements > 0) {
			for (String countryName : myCountries) {
				if (beginReinforce && reinforcements > 0) {
					rsp.reinforce(countryName, 1);
					reinforcements--;
					this.lastCountryReinforced = countryName;
				}
				if (this.lastCountryReinforced.equals(countryName)) {
					beginReinforce = true;
				}
			}
		}
		return rsp;
	}
	
	/**
	 * For the first country that can attack a neighbor, attack the weakest neighbor with the maximum number of dice.
	 */
	public AttackResponse attack(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards) {
		AttackResponse rsp = new AttackResponse();
		Collection<String> myCountries = RiskUtils.getPlayerCountries(map, this.name);
		Country atkCountry = null, dfdCountry = null, currentCountry;
		for (String countryName : myCountries) {
			currentCountry = map.getCountries().get(countryName);
			if (currentCountry.getNumArmies() > 1) {
				for (Country neighbor : currentCountry.getNeighbors()) {
					if (!neighbor.getOwner().equals(this.name) && (dfdCountry == null
						|| (dfdCountry != null && dfdCountry.getNumArmies() > neighbor.getNumArmies()))) {
						atkCountry = currentCountry;
						dfdCountry = neighbor;
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
		}
		return rsp;
	}
	
	/**
	 * Advance all available armies.
	 */
	public AdvanceResponse advance(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, String fromCountryName, String toCountryName, int min) {
		AdvanceResponse rsp = new AdvanceResponse();
		rsp.setNumArmies(map.getCountries().get(fromCountryName).getNumArmies() - 1);
		return rsp;
	}
	
	/**
	 * Find the first interior country with available armies and a neighboring exterior country, and transfer the armies to the neighbor.
	 * Interior refers to a country surrounded by only friendly neighbors, and exterior has at least one enemy neighbor.
	 */
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
	
	/**
	 * Defend with as many dice as possible.
	 */
	public DefendResponse defend(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, String atkCountry, String dfdCountry, int numAtkDice) {
		DefendResponse rsp = new DefendResponse();
		int numDice = map.getCountries().get(dfdCountry).getNumArmies();
		if (numDice > RiskConstants.MAX_DFD_DICE) {
			numDice = RiskConstants.MAX_DFD_DICE;
		}
		rsp.setNumDice(numDice);
		return rsp;
	}
}