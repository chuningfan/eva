package eva.core.valve;

import java.util.Objects;

public class EvaPipeline<T, R extends Result> {

	private Valve<T, R> firstNode;
	
	private Valve<T, R> lastNode;
	

	public final EvaPipeline<T, R> addLast(Valve<T, R> valve) {
		if (Objects.isNull(firstNode)) {
			this.firstNode = valve;
		} else {
			this.lastNode.setNext(valve);
			valve.setPrevious(this.lastNode);
		}
		this.lastNode = valve;
		return this;
	}

	public final EvaPipeline<T, R> addFirst(Valve<T, R> valve) {
		if (Objects.isNull(lastNode)) {
			this.lastNode = valve;
		} else {
			this.firstNode.setPrevious(valve);
			valve.setNext(this.firstNode);
		}
		this.firstNode = valve;
		return this;
	}

	public final EvaPipeline<T, R> removeLast() {
		if (Objects.isNull(this.lastNode)) {
			this.lastNode = null;
		} else {
			Valve<T, R> previous = this.lastNode.getPrevious();
			if (Objects.isNull(previous)) {
				this.firstNode = null;
				this.lastNode = null;
			} else {
				previous.setNext(null);
				this.lastNode = previous;
			}
		}
		return this;
	}

	public final EvaPipeline<T, R> removeFirst() {
		if (Objects.isNull(this.firstNode)) {
			this.firstNode = null;
		} else {
			Valve<T, R> next = this.firstNode.getNext();
			if (Objects.isNull(next)) {
				this.firstNode = null;
				this.lastNode = null;
			} else {
				next.setPrevious(null);
				this.firstNode = next;
			}
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	public R doProcess(Direction dire, T data, R result) throws Exception {
		switch (dire) {
		case FORWARD:
			if (Objects.isNull(firstNode)) {
				return (R) result.setSuccessful(false).setMessage("Pipeline: No valve was found for FORWARD!");
			}
			return firstNode.process(dire, data, result);
		case BACKWARD:
			if (Objects.isNull(lastNode)) {
				return (R) result.setSuccessful(false).setMessage("Pipeline: No valve was found for BACKWARD!");
			}
			return lastNode.process(dire, data, result);
		default:
			return (R) result.setSuccessful(false).setMessage("Pipeline: Unknown direction!");
		}
	}

	public static enum Direction {
		FORWARD, BACKWARD;
	}

}
