package eva.balance.strategies;

import eva.common.base.config.ClientConfig;

public class BalanceStrategyFactory {

	public enum Strategy {
		RANDOM, HASH, LOOP;
	}

	public static final Strategy getStrategy(ClientConfig config) {
		String strategy = config.getStrategy();
		switch (strategy) {
		case "random":
			return Strategy.RANDOM;
		case "hash":
			return Strategy.HASH;
		case "loop":
			return Strategy.LOOP;
		default: 
			return Strategy.RANDOM;
		}
	}

}
