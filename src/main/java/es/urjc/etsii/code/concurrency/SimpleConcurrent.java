package es.urjc.etsii.code.concurrency;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class contains static methods to facilitate the teaching of concurrent
 * programming concepts. Specifically, it has some methods to control the thread
 * life cycle and other methods to use several concurrent programming primitives
 * and facilities.
 * 
 * @author Micael Gallego
 * @version 0.6
 * 
 */
public class SimpleConcurrent {

	private static class ThreadInfo {

		private int threadNum;
		private Thread thread;

		public ThreadInfo(String threadName, int threadNum, Thread thread) {

			this.threadNum = threadNum;
			this.thread = thread;
		}

		public int getThreadNum() {
			return threadNum;
		}

		public Thread getThread() {
			return thread;
		}
	}

	private static final CountDownLatch startGate = new CountDownLatch(1);
	private static final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<String, ReentrantLock>();
	private static final ConcurrentMap<String, ThreadInfo> threads = new ConcurrentHashMap<String, ThreadInfo>();
	private static final Map<String, Integer> numThreadsPerMethod = new HashMap<String, Integer>();
	private static final ArrayList<String> spaces = new ArrayList<String>();

	public static void createThread(final String methodName, final Object... args) {
		createThread(methodName, args, 2);
	}

	private static void createThread(final String methodName, final Object[] args, int stackLevel) {

		Class<?> clazz = getCallingClass(stackLevel+1);

		Method method = getMethod(methodName, clazz);

		String threadName;
		Integer num = numThreadsPerMethod.get(methodName);
		if (num == null){
			
			numThreadsPerMethod.put(methodName, 0);
			threadName = methodName;
			num = 0;
			
		} else {
			
			if (num == 0) {
				ThreadInfo ti = threads.remove(methodName);
				String firstThreadName = methodName + "_0";
				ti.getThread().setName(firstThreadName);
				threads.put(firstThreadName, ti);
			}
			num = num + 1;
			numThreadsPerMethod.put(methodName, num);
			threadName = methodName + "_" + num;
		}

		final Thread t = createThread(threadName, method, args);

		threads.put(threadName, new ThreadInfo(threadName, threads.size(), t));

		if (spaces.size() == 0) {
			spaces.add("");
		} else {
			spaces.add(spaces.get(spaces.size() - 1) + "        ");
		}

	}

	private static Method getMethod(final String methodName, Class<?> clazz) {
		Method method = null;
		for (Method auxMethod : clazz.getMethods()) {
			if (auxMethod.getName().equals(methodName)) {
				method = auxMethod;
				break;
			}
		}

		if (method == null) {
			throw new RuntimeException(
					"Method \"" + methodName + "\" not found in class " + clazz.getName());
		}
		return method;
	}

	private static Class<?> getCallingClass(int stackLevel) {
		Class<?> clazz = null;
		try {
			clazz = Class
					.forName(new RuntimeException().getStackTrace()[stackLevel].getClassName());
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		return clazz;
	}

	public static void createThreads(int numThreads, String methodName, Object... args) {
		for (int i = 0; i < numThreads; i++) {
			createThread(methodName, args, 2);
		}
	}

	private static Thread createThread(String threadName, final Method execMethod,
			final Object... args) {

		final Thread t = new Thread(threadName) {
			@SuppressWarnings("deprecation")
			public void run() {
				try {
					startGate.await();
					sleepRandom(25);
					execMethod.invoke(null, args);
				} catch (Exception e) {

					synchronized (threads) {

						for (ThreadInfo ti : threads.values()) {
							Thread thread = ti.getThread();
							if (thread != Thread.currentThread() && thread.isAlive()) {
								thread.stop();
							}
						}

						if (e instanceof InvocationTargetException) {
							Throwable originalException = e.getCause();
							System.out.format("Exception in Thread [%s]\n",
									Thread.currentThread().getName());

							originalException.printStackTrace();
						} else {
							e.printStackTrace();
						}
						System.exit(1);

					}
				}
			}
		};
		return t;
	}

	public static void enterMutex() {
		enterMutex("default");
	}

	public static void exitMutex() {
		exitMutex("default");
	}

	public static void enterMutex(String mutexId) {

		ReentrantLock lock;
		synchronized (locks) {
			lock = locks.get(mutexId);
			if (lock == null) {
				lock = new ReentrantLock();
				locks.put(mutexId, lock);
			}
		}
		lock.lock();
	}

	public static void exitMutex(String mutexId) {

		ReentrantLock lock = locks.get(mutexId);
		if (lock == null) {
			throw new RuntimeException(String.format("MutexId: \"%s\" does not exist.", mutexId));
		} else {
			if (!lock.isHeldByCurrentThread()) {
				throw new RuntimeException(
						"The thread \"" + getThreadName() + "\" is trying to exit of mutex \""
								+ mutexId + "\" but other thread is in the critical section");
			}
			lock.unlock();
		}
	}

	public static void startThreadsAndWait() {

		long startTime = System.currentTimeMillis();

		for (ThreadInfo ti : threads.values()) {
			ti.getThread().start();
		}

		startGate.countDown();

		for (ThreadInfo ti : threads.values()) {
			try {
				ti.getThread().join();
			} catch (InterruptedException e) {
			}
		}

		long duration = System.currentTimeMillis() - startTime;
		System.out.println("\nConcurrent code spend " + duration + " millis");
	}

	public static void println(String text) {
		sleepRandom(10);
		System.out.println(text);
		sleepRandom(10);
	}

	public static void print(String text) {
		sleepRandom(10);
		System.out.print(text);
		sleepRandom(10);
	}

	public static String getThreadName() {
		return Thread.currentThread().getName();
	}

	public static void printlnI(String text) {
		int threadNum = threads.get(Thread.currentThread().getName()).getThreadNum();
		println(spaces.get(threadNum) + text);
	}

	public static void sleepRandom(long millis) {
		sleep((long) (Math.random() * millis));
	}

	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
