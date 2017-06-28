package com.zbq.concurrent;

import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.StampedLock;

/**
 * Created by zhangboqing on 2017/6/8.
 *
 * StampedLock内部实现时，使用类似于CAS操作的死循环反复尝试的策略。
 * 在它挂起线程时，使用的是Unsafe.park()函数，
 * 而park()函数在遇到线程中断时，会直接返回（注意，不同于Thread.sleep()，它不会抛出异常）。
 * 而在StampedLock的死循环逻辑中，没有处理有关中断的逻辑。
 * 因此，这就会导致阻塞在park()上的线程被中断后，会再次进入循环。
 * 而当退出条件得不到满足时，就会发生疯狂占用CPU的情况。这一点值得我们注意，
 */
public class StampedLockCPUDemo {

    static Thread[] holdCpuThreads = new Thread[3];
    static final StampedLock lock = new StampedLock();

    public static void main(String[] args) throws InterruptedException {

        //首先开启线程占用写锁（
        new Thread() {

            public void run() {
                long readLong = lock.writeLock();
                LockSupport.parkNanos(600000000000L);
                lock.unlockWrite(readLong);

            }
        }.start();

        //开启3个读线程，让它们请求读锁。此时，由于写锁的存在，所有读线程都会被最终挂起。
        Thread.sleep(100);
        for (int i = 0; i < 3; ++i) {
            holdCpuThreads[i] = new Thread(new HoldCPUReadThread());
            holdCpuThreads[i].start();

        }
        Thread.sleep(10000);
        //线程中断后，会占用CPU
        for (int i = 0; i < 3; ++i) {
            holdCpuThreads[i].interrupt();
        }

    }

    private static class HoldCPUReadThread implements Runnable {

        public void run() {
            long lockr = lock.readLock();
            System.out.println(Thread.currentThread().getName() + " 获得读锁");
            lock.unlockRead(lockr);

        }
    }
}
