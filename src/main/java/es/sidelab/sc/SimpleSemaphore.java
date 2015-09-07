package es.sidelab.sc;

public class SimpleSemaphore {

	/** current number of available permits **/
	protected long permits;

	/**
	 * Create a Semaphore with the given initial number of permits. Using a seed
	 * of one makes the semaphore act as a mutual exclusion lock. Negative seeds
	 * are also allowed, in which case no acquires will proceed until the number
	 * of releases has pushed the number of permits past 0.
	 **/
	public SimpleSemaphore(long initialPermits) {
		permits = initialPermits;
	}

	public synchronized void acquire() {
		try {
			while (permits <= 0){
				wait();
			}
			permits--;
		} catch (InterruptedException ex) {
			notify();
		}
	}
	
	public void acquire(int n){
		for(int i=0; i<n; i++){
			acquire();
		}
	}
	
	public synchronized void release() {
		++permits;
		notifyAll();
	}
	
	public void release(int n) {
		for(int i=0; i<n; i++){
			release();
		}
	}

	/**
	 * Return the current number of available permits. Returns an accurate, but
	 * possibly unstable value, that may change immediately after returning.
	 **/
	public synchronized long permits() {
		return permits;
	}
	
	@Override
	public String toString() {
		return "[permits=" + permits+"]";		
	}
}