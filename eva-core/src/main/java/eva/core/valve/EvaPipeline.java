package eva.core.valve;

import java.util.Objects;

public class EvaPipeline {

	private Valve firstNode;
	
	private Valve lastNode;
	

	public final void addLast(Valve valve) {
		if (Objects.isNull(firstNode)) {
			this.firstNode = valve;
		} else {
			this.firstNode.setNext(valve);
			valve.setPrevious(this.firstNode);
		}
		this.lastNode = valve;
	}

	public final void addFirst(Valve valve) {
		if (Objects.isNull(lastNode)) {
			this.lastNode = valve;
		} else {
			this.firstNode.setPrevious(valve);
			valve.setNext(this.firstNode);
		}
		this.firstNode = valve;
	}

	public final void removeLast() {
		if (Objects.isNull(this.lastNode)) {
			this.lastNode = null;
		} else {
			Valve previous = this.lastNode.getPrevious();
			if (Objects.isNull(previous)) {
				this.firstNode = null;
				this.lastNode = null;
			} else {
				previous.setNext(null);
				this.lastNode = previous;
			}
		}
	}

	public final void removeFirst() {
		if (Objects.isNull(this.firstNode)) {
			this.firstNode = null;
		} else {
			Valve next = this.firstNode.getNext();
			if (Objects.isNull(next)) {
				this.firstNode = null;
				this.lastNode = null;
			} else {
				next.setPrevious(null);
				this.firstNode = next;
			}
		}
	}

	public Object doProcess(Direction dire, Object data) {
		switch (dire) {
		case FORWARD:
			if (Objects.isNull(firstNode)) {
				return null;
			}
			return firstNode.process(data, dire);
		case BACKWARD:
			if (Objects.isNull(lastNode)) {
				return null;
			}
			return lastNode.process(data, dire);
		default:
			return null;
		}
	}

	public static enum Direction {
		FORWARD, BACKWARD;
	}

}
