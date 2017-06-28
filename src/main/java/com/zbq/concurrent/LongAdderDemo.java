package com.zbq.concurrent;

import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by zhangboqing on 2017/6/9.
 * <p>
 * 指定了测试线程数量、目标总数以及3个初始值为0的整型变量acount、lacount和count。
 * 它们分别表示使用AtomicLong、Lon-gAdder和锁进行同步时的操作对象
 */
public class LongAdderDemo {

    private static final int MAX_THREADS = 3;   //线程数
    private static final int TASK_COUNT = 3;    //任务数
    private static final int TARGET_COUNT = 10000000;       //目标总数
    private AtomicLong acount = new AtomicLong(0L);       //无锁的原子操作

    private LongAdder lacount = new LongAdder();

    private long count = 0;
    static CountDownLatch cdlsync = new CountDownLatch(TASK_COUNT);
    static CountDownLatch cdlatomic = new CountDownLatch(TASK_COUNT);
    static CountDownLatch cdladdr = new CountDownLatch(TASK_COUNT);


    /**
     * 1.使用同步锁的方式测试
     */
    protected synchronized long inc() {                     //有锁的加法
        return ++count;
    }

    protected synchronized long getCount() {                      //有锁的操作
        return count;

    }

    /**
     * 使用加锁方式增加count的值
     */
    public class SyncThread implements Runnable {
        protected String name;
        protected long starttime;
        LongAdderDemo out;

        public SyncThread(LongAdderDemo o, long starttime) {
            out = o;
            this.starttime = starttime;

        }

        @Override
        public void run() {
            long v = out.getCount();
            while (v < TARGET_COUNT) {                        //在到达目标值前，不停循环
                v = out.inc();

            }
            long endtime = System.currentTimeMillis();
            System.out.println("SyncThread spend:" + (endtime - starttime) + "ms" + " v=" + v);
            cdlsync.countDown();

        }
    }

    /**
     * 使用线程池控制多线程进行累加操。
     */
    @Test
    public void testSync() throws InterruptedException {
        ExecutorService exe = Executors.newFixedThreadPool(MAX_THREADS);
        long starttime = System.currentTimeMillis();
        SyncThread sync = new SyncThread(this, starttime);
        for (int i = 0; i < TASK_COUNT; i++) {
            exe.submit(sync);                                 //提交线程开始计算
        }
        cdlsync.await();
        exe.shutdown();

    }

    /**
     * 2.实现原子类累加计时统计
     */
    public class AtomicThread implements Runnable {
        protected String name;
        protected long starttime;

        public AtomicThread(long starttime) {
            this.starttime = starttime;

        }

        @Override
        public void run() {                                      //在到达目标值前，不停循环
            long v = acount.get();
            while (v < TARGET_COUNT) {

                v = acount.incrementAndGet();                      //无锁的加法
            }
            long endtime = System.currentTimeMillis();
            System.out.println("AtomicThread spend:" + (endtime - starttime) + "ms" + " v=" + v);
            cdlatomic.countDown();

        }
    }

    @Test
    public void testAtomic() throws InterruptedException {
        ExecutorService exe = Executors.newFixedThreadPool(MAX_THREADS);
        long starttime = System.currentTimeMillis();
        AtomicThread atomic = new AtomicThread(starttime);
        for (int i = 0; i < TASK_COUNT; i++) {

            exe.submit(atomic);                                  //提交线程开始计算
        }
        cdlatomic.await();
        exe.shutdown();

    }


    /**
     * 使用LongAddr实现类似的功能
     */
    public class LongAddrThread implements Runnable {
        protected String name;
        protected long starttime;

        public LongAddrThread(long starttime) {
            this.starttime = starttime;

        }

        @Override
        public void run() {
            long v = lacount.sum();
            while (v < TARGET_COUNT) {
                lacount.increment();
                v = lacount.sum();

            }
            long endtime = System.currentTimeMillis();
            System.out.println("LongAdder spend:" + (endtime - starttime) + "ms" + " v=" + v);
            cdladdr.countDown();

        }
    }

    /**
     * 3.
     */
    @Test
    public void testAtomicLong() throws InterruptedException {
        ExecutorService exe = Executors.newFixedThreadPool(MAX_THREADS);
        long starttime = System.currentTimeMillis();
        LongAddrThread atomic = new LongAddrThread(starttime);
        for (int i = 0; i < TASK_COUNT; i++) {
            exe.submit(atomic);                                //提交线程开始计算
        }
        cdladdr.await();
        exe.shutdown();

    }

    public static void main(String[] args) throws Exception {
        LongAccumulator accumulator = new LongAccumulator(Long::max, Long.MIN_VALUE);
        Thread[] ts = new Thread[1000];
        for (int i = 0; i < 1000; i++) {
            ts[i] = new Thread(() -> {
                Random random = new Random();
                long value = random.nextLong();
                accumulator.accumulate(value);
            });
            ts[i].start();

        }
        for (int i = 0; i < 1000; i++) {
            ts[i].join();

        }
        System.out.println(accumulator.longValue());

    }
}
