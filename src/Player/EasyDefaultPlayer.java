package Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

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
public class EasyDefaultPlayer implements Player {
	protected String name;
	protected Country lastCountryReinforced;
	
	public EasyDefaultPlayer(String nameIn) {
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
		return reinforce(map, null, null, reinforcements);
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
		Set<Country> myCountries = RiskUtils.getPlayerCountries(map, this.name);
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
	 */
	public AttackResponse attack(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards) {
		AttackResponse rsp = new AttackResponse();
		Set<Country> myCountries = RiskUtils.getPlayerCountries(map, this.name);
		Country atkCountry = null, dfdCountry = null;
		for (Country currentCountry : myCountries) {
			if (map.getCountryArmies(currentCountry) > 1) {
				for (Country neighbor : currentCountry.getNeighbors()) {
					if (!map.getCountryOwner(neighbor).equals(this.name) && (dfdCountry == null
						|| (dfdCountry != null && map.getCountryArmies(dfdCountry) > map.getCountryArmies(neighbor)))) {
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
		}
		return rsp;
	}
	
	/**
	 * Advance all available armies.
	 */
	public AdvanceResponse advance(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, Country fromCountry, Country toCountry, int min) {
		AdvanceResponse rsp = new AdvanceResponse();
		rsp.setNumArmies(map.getCountryArmies(fromCountry) - 1);
		return rsp;
	}
	
	/**
	 * Don't fortify.
	 */
	public FortifyResponse fortify(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards) {
		return null;
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
}