package eva.balance.strategies;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;

import eva.core.base.config.ClientConfig;

public class BalanceStrategyFactory {

	public enum Strategy {
		RANDOM("random") {
			@Override
			public int selectIndex(Collection<String> collection, Object...args) {
				int nodeSize = collection.size();
				Random ran = new Random();
				int index = ran.nextInt(nodeSize);
				return index;
			}
		}, HASH("hash") {
			@Override
			public int selectIndex(Collection<String> collection, Object...args) {
				int nodeSize = collection.size();
				String fromIp = args[0].toString();
				int index = fromIp.hashCode() % nodeSize;
				return index;
			}
		}, LOOP("loop") {
			@Override
			public int selectIndex(Collection<String> collection, Object...args) {
				// TODO Auto-generated method stub
				return 0;
			}
		};
		
		private String strategy;
		
		private Strategy(String strategy) {
			this.strategy = strategy;
		}

		public abstract int selectIndex(Collection<String> collection, Object...args);
		
		public String getStrategy() {
			return strategy;
		}

		public void setStrategy(String strategy) {
			this.strategy = strategy;
		}
		
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

	private static final List<String> setOrdered(Collection<String> collection) {
		int nodeSize = collection.size();
		List<String> list = Lists.newArrayListWithExpectedSize(nodeSize);
		list.addAll(collection);
		return list;
	}
	
	public static final String getApplicableAddress(Collection<String> collection, Strategy balanceStrategy, Object...args) {
		List<String> list = setOrdered(collection);
		return list.get(balanceStrategy.selectIndex(collection, args));
	}
	
}
