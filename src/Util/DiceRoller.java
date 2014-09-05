package Util;
import java.util.Random;

public class DiceRoller {
	private static Random rand = new Random(RiskConstants.SEED);
	
	public static RollOutcome roll(int atkDice, int dfdDice) {
		if (atkDice < 1 || atkDice > 3 || dfdDice < 1 || dfdDice > 2) {
			throw new IllegalArgumentException("DiceRoller.roll: Attacker must use [1,3] dice and Defender must use [1,2] dice.");
		}
		else {
			int[] atk = new int[atkDice];
			int[] dfd = new int[dfdDice];
			int i, j, roll;
			//attack rolls
			for (i = 0; i < atkDice; i++) {
				roll = rand.nextInt(6) + 1;
				atk[i] = roll;
				for (j = i - 1; j >= 0; j--) {
					if (roll > atk[j]) {
						atk[j + 1] = atk[j];
						atk[j] = roll;
					}
				}
			}
			//defend rolls
			for (i = 0; i < dfdDice; i++) {
				roll = rand.nextInt(6) + 1;
				dfd[i] = roll;
				for (j = i - 1; j >= 0; j--) {
					if (roll > dfd[j]) {
						dfd[j + 1] = dfd[j];
						dfd[j] = roll;
					}
				}
			}
			//compute result
			RollOutcome result = new RollOutcome();
			for (i = 0; i < atkDice && i < dfdDice; i++) {
				if (atk[i] > dfd[i]) {
					result.addDfdLoss();
				}
				else {
					result.addAtkLoss();
				}
			}
			return result;
		}
	}
}