package eva.core.listener;

import java.util.Observable;
import java.util.Observer;
import java.util.logging.Logger;

import eva.common.global.StatusEvent;

public interface StatusListener extends Observer {

	static final Logger LOG = Logger.getLogger("StatusListener");
	
	@Override
	default void update(Observable o, Object arg) {
		StatusEvent event = (StatusEvent) arg;
		short status = event.getStatus();
		switch (status) {
		case 0:
			onSuccess(o, event);
			break;
		case 1:
			onFailure(o, event);
			break;
		case 2: onClose(o, event);
			break;
		default: 
			LOG.info("Unrecognized status: " + status + ", when triggering listener [" + getClass().getName() + "]");
			break;
		}
	}

	void onSuccess(Observable source, StatusEvent event);

	void onFailure(Observable source, StatusEvent event);

	default void onClose(Observable source, StatusEvent event){}
}
