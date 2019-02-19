package eva.common.base;

import java.util.Observable;
import java.util.Observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eva.common.dto.Event;

public interface Listener extends Observer {

	static final Logger LOG = LoggerFactory.getLogger(Listener.class);
	
	@Override
	default void update(Observable o, Object arg) {
		Event event = (Event) arg;
		short status = event.getStatus();
		switch (status) {
		case 0:
			onSuccess(o, event);
			break;
		case 1:
			onFailure(o, event);
			break;
		default: 
			LOG.error("Unrecognized status: " + status + ", when triggering listener [" + getClass().getName() + "]");
			break;
		}
	}

	void onSuccess(Observable source, Event event);

	void onFailure(Observable source, Event event);

}
