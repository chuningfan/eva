package eva.core.listener;

import java.util.Observable;
import java.util.Observer;

import eva.common.global.NodeChangeEvent;
import eva.common.global.NodeChangeEvent.Action;

public interface NodeListener extends Observer {
	
	@Override
	default void update(Observable o, Object arg) {
		NodeChangeEvent event = (NodeChangeEvent) arg;
		Action action = event.getAction();
		switch (action) {
		case ADD:
			onAdd(o, event);
			break;
		case DELETE:
			onDelete(o, event);
			break;
		default: 
			break;
		}
	}
	
	void onAdd(Observable o, NodeChangeEvent event);
	void onDelete(Observable o, NodeChangeEvent event);
	
}
