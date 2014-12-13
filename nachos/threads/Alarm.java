package nachos.threads;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import sun.nio.cs.ext.MacIceland;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
		waitingThreads=new HashMap<Long,LinkedList<KThread>>();
		mapLock=new Lock();
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		KThread.currentThread().yield();
		Long time=Machine.timer().getTime();
		mapLock.acquire();
		Iterator<Long> keySet=waitingThreads.keySet().iterator();
		for(int i=0;keySet.hasNext() ;i++){
			long wait=keySet.next();
			if(wait<=time){
				LinkedList<KThread> lst=waitingThreads.get(wait);
				for(int j=0;j<lst.size();j++){
					lst.get(j).ready();
					lst.remove(j);
				}
			}
		}
		mapLock.release();
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x
	 *            the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		long wakeTime = Machine.timer().getTime() + x;
		/*while (wakeTime > Machine.timer().getTime())
			KThread.yield();*/
		
		KThread current=KThread.currentThread();
		mapLock.acquire();
		LinkedList<KThread> lst=new LinkedList<KThread>();
		lst.add(current);
		waitingThreads.put(x, lst);
		mapLock.release();
		boolean stat=Machine.interrupt().disable();
		KThread.sleep();
		Machine.interrupt().setStatus(stat);
	}
	
	private Map<Long, LinkedList<KThread>> waitingThreads;
	private Lock mapLock;
}