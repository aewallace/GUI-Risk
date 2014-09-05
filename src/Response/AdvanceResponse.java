package Response;

import Map.RiskMap;

public class AdvanceResponse {
	private int numArmies;
	
	public AdvanceResponse() {
		this.numArmies = 1;
	}
	
	public AdvanceResponse(int numIn) {
		this.numArmies = numIn;
	}
	
	public int getNumArmies() {
		return this.numArmies;
	}
	
	public void setNumArmies(int numIn) {
		this.numArmies = numIn;
	}
	
	public static boolean isValidResponse(AdvanceResponse advRsp, AttackResponse atkRsp, RiskMap map) {
		if (advRsp != null) {
			int numAttackingArmies = map.getCountryArmies(atkRsp.getAtkCountry());
			int n = advRsp.getNumArmies();
			return (n >= atkRsp.getNumDice() || numAttackingArmies <= atkRsp.getNumDice()) && n < numAttackingArmies;
		}
		else {
			return false;
		}
	}
}