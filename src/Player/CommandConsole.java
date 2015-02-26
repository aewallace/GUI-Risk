package Player;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import Map.Continent;
import Map.RiskMap;
import Response.CardTurnInResponse;
import Response.ReinforcementResponse;
import Util.Card;
import Util.RiskConstants;
import Util.RiskUtils;

public class CommandConsole extends Seth {
	protected static final String reportTemplate =
								"================ Stage: %s ================"
								+ "\r\nOwned Continents:%s"
								+ "\r\nEnemy-Controlled Continents:%s"
								+ "\r\nContested Continents:%s"
								+ "\r\nCards:%s";
	
	protected Scanner in;
	
	public CommandConsole() {
		super();
		this.in = new Scanner(System.in);
	}
	
	public CommandConsole(String nameIn) {
		super(nameIn);
		this.in = new Scanner(System.in);
	}
	
	public CardTurnInResponse proposeTurnIn(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, boolean turnInRequired) {
		System.out.println("\r\n" + report("New Turn", map, myCards, playerCards));
		this.in.nextLine();
		return super.proposeTurnIn(map, myCards, playerCards, turnInRequired);
	}
	
	protected String report(String stage, RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards) {
		StringBuilder report = new StringBuilder(String.format(reportTemplate,
												stage,
												getOwnedContinentsReport(map),
												getEnemyContinentsReport(map),
												getContestedContinentsReport(map),
												getCardsReport(myCards, playerCards)));
		return report.toString();
	}
	
	protected String getOwnedContinentsReport(RiskMap map) {
		StringBuilder report = new StringBuilder();
		for (Continent ownedContinent : RiskUtils.getPlayerContinents(map, this.name)) {
			report.append("\r\n\t" + ownedContinent.getName() + ": Strength: " + getTrueContinentAttainability(map, ownedContinent, 0));
		}
		return report.toString();
	}
	
	protected String getEnemyContinentsReport(RiskMap map) {
		StringBuilder report = new StringBuilder();
		for (Continent continent : Continent.values()) {
			String owner = RiskUtils.getContinentOwner(map, continent);
			if (owner != null && !owner.equals(this.name)) {
				report.append("\r\n\t" + continent.getName() + ": Owner: " + owner + " Attainability: " + getTrueContinentAttainability(map, continent, 0));
			}
		}
		return report.toString();
	}
	
	protected String getContestedContinentsReport(RiskMap map) {
		StringBuilder report = new StringBuilder();
		for (Continent continent : Continent.values()) {
			if (RiskUtils.getContinentOwner(map, continent) == null) {
				report.append("\r\n\t" + continent.getName() + " Attainability: " + getTrueContinentAttainability(map, continent, 0));
			}
		}
		return report.toString();
	}
	
	protected String getCardsReport(Collection<Card> myCards, Map<String, Integer> playerCards) {
		StringBuilder report = new StringBuilder(" Next Turn-In Worth " + RiskConstants.peekNextTurnIn());
		if (myCards.size() > 0) {
			report.append("\r\n\tMine:");
			for (Card card : myCards) {
				report.append(" " + card.getType());
			}
		}
		for (Entry<String, Integer> playerCardCount : playerCards.entrySet()) {
			report.append("\r\n\t" + playerCardCount.getKey() + ": " + playerCardCount.getValue());
		}
		return report.toString();
	}
}
