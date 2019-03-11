package eva.server.core.valve.invoker.async;

import java.util.concurrent.LinkedBlockingQueue;

import com.google.common.collect.Queues;

import eva.server.core.server.NioServer;

public class Queue {

	private static final LinkedBlockingQueue<Task> queue = Queues.newLinkedBlockingQueue(NioServer.QUEUE_CAPACITY);

	private Queue() {
	}

	private static final class QueueHolder {
		private static final Queue INSTANCE = new Queue();
	}

	public static final Queue getInstance() {
		return QueueHolder.INSTANCE;
	}

	public void addToQueue(Task task) {
		queue.offer(task);
	}

	public Task getTask() throws InterruptedException {
		return queue.take();
	}

}
