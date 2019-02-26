package eva.core.valve;

import java.util.Objects;

import eva.core.valve.EvaPipeline.Direction;

public abstract class Valve {

	private Valve previous;

	private Valve next;

	private String name;

	public Valve(String name) {
		this.name = name;
	}

	public Valve getPrevious() {
		return previous;
	}

	public void setPrevious(Valve previous) {
		this.previous = previous;
	}

	public Valve getNext() {
		return next;
	}

	public void setNext(Valve next) {
		this.next = next;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	protected Object process(Object obj, Direction dire) {
		Object object = process0(obj);
		switch (dire) {
		case FORWARD:
			if (Objects.nonNull(next)) {
				return next.process(object, dire);
			}
			break;
		case BACKWARD:
			if (Objects.nonNull(previous)) {
				return previous.process(object, dire);
			}
			break;
		}
		return object;
	}

	protected abstract Object process0(Object obj);

}
