package Response;
import java.util.ArrayList;
import java.util.Collection;

import Map.Country;
import Util.Card;
import Util.RiskConstants;

public class CardTurnInResponse {
	private Collection<Card> cards;
	private Country bonusCountry;
	
	public CardTurnInResponse() {
		this.cards = new ArrayList<Card>();
	}
	
	public boolean addCard(Card card) {
		if (card != null) {
			this.cards.add(card);
			return true;
		}
		else {
			return false;
		}
	}
	
	public boolean setBonusCountry(Country country) {
		if (country != null) {
			for (Card card : this.cards) {
				if (card.getCountry() == country) {
					this.bonusCountry = country;
					return true;
				}
			}
		}
		return false;
	}
	
	public Collection<Card> getCards() {
		return this.cards;
	}
	
	public Country getBonusCountry() {
		return this.bonusCountry;
	}
	
	public static boolean isValidResponse(CardTurnInResponse rsp, Collection<Card> playerCards) {
		if (rsp != null) {
			Collection<Card> cards = rsp.getCards();
			if (cards != null && cards.size() == 3) {
				String[] types = new String[3];
				int i = 0;
				for (Card card : cards) {
					if (card == null) {
						return false;
					}
					else {
						types[i] = card.getType();
						//any single wild card forces valididty of the set
						if (types[i].equals(RiskConstants.WILD_CARD)) {
							return true;
						}
					}
					i++;
				}
				//ensure that all proposed cards are actually owned by the player
				for (Card card : cards) {
					if (!playerCards.contains(card)) {
						return false;
					}
				}
				//three of a kind, or three distinct types
				return (types[0].equals(types[1]) && types[1].equals(types[2]))
				|| (!types[0].equals(types[1]) && !types[1].equals(types[2]) && !types[0].equals(types[2]));
			}
			else {
				return false;
			}
		}
		else {
			return false;
		}
	}
}