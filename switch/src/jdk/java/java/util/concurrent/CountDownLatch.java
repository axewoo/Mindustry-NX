package java.util.concurrent;

/**
 * Minimal CountDownLatch for the Clearwing runtime, which does not ship one.
 * Clearwing's Object.wait() releases the monitor before it starts waiting on the condition, so a
 * notify in between is lost: waits are bounded and the count is rechecked.
 */
public class CountDownLatch{
    private long count;

    public CountDownLatch(int count){
        if(count < 0) throw new IllegalArgumentException("count < 0");
        this.count = count;
    }

    public synchronized void await() throws InterruptedException{
        while(count > 0) wait(5);
    }

    public synchronized boolean await(long timeout, TimeUnit unit) throws InterruptedException{
        long end = System.currentTimeMillis() + unit.toMillis(timeout);
        while(count > 0){
            long left = end - System.currentTimeMillis();
            if(left <= 0) return false;
            wait(Math.min(left, 5));
        }
        return true;
    }

    public synchronized void countDown(){
        if(count > 0 && --count == 0) notifyAll();
    }

    public synchronized long getCount(){
        return count;
    }

    @Override
    public String toString(){
        return super.toString() + "[Count = " + getCount() + "]";
    }
}
