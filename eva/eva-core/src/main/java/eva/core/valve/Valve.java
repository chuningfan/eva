package eva.core.valve;

import java.util.Objects;

import eva.core.valve.EvaPipeline.Direction;

public abstract class Valve<T, R extends Result> {

	private Valve<T, R> previous;

	private Valve<T, R> next;

	private String name;

	public Valve<T, R> getPrevious() {
		return previous;
	}

	public void setPrevious(Valve<T, R> previous) {
		this.previous = previous;
	}

	public Valve<T, R> getNext() {
		return next;
	}

	public void setNext(Valve<T, R> next) {
		this.next = next;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@SuppressWarnings("unchecked")
	protected R process(Direction dire, T data, R result) throws Exception {
		Result res = process0(data, result);
		if (res.isSuccessful()) {
			switch (dire) {
			case FORWARD:
				if (Objects.nonNull(next)) {
					return next.process(dire, data, result);
				}
				break;
			case BACKWARD:
				if (Objects.nonNull(previous)) {
					return previous.process(dire, data, result);
				}
				break;
			}
		}
		return (R) res;
	}

	protected abstract R process0(T data, R result) throws Exception;

}
