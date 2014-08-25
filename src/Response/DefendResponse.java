package Response;
import Map.Country;
import Util.RiskConstants;

public class DefendResponse {
	private int numDice;
	
	public DefendResponse() {
		this.numDice = 1;
	}
	
	public DefendResponse(int numIn) {
		this.numDice = numIn;
	}
	
	public int getNumDice() {
		return this.numDice;
	}
	
	public void setNumDice(int numIn) {
		this.numDice = numIn;
	}
	
	public static boolean isValidResponse(DefendResponse rsp, Country dfdCountry) {
		if (rsp != null) {
			int n = rsp.getNumDice();
			return (n > 0 && n <= RiskConstants.MAX_DFD_DICE) && (n <= dfdCountry.getNumArmies());
		}
		else {
			return false;
		}
	}
}