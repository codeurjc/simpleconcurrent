package es.sidelab.sc;

import java.util.concurrent.Semaphore;

public class SimpleSemaphore {

	private Semaphore semaphore;

	public SimpleSemaphore(int permits) {
		this.semaphore = new Semaphore(permits, false);
	}

	public void release() {
		this.semaphore.release();
	}

	public void acquire() {
		try {
			this.semaphore.acquire();
		} catch (InterruptedException e) {}
	}

	@Override
	public String toString() {
		return "[permits=" + semaphore.availablePermits()
				+ " blocked_processes=" + semaphore.getQueueLength() + "]";
	}

}
