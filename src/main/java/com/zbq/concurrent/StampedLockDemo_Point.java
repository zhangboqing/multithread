package com.zbq.concurrent;

import java.util.concurrent.locks.StampedLock;

/**
 * Created by zhangboqing on 2017/6/8.
 *
 * StampedLock通过引入乐观读来增加系统的并行度
 *
 * 内部有两个元素x和y，表示点的坐标
 */
public class StampedLockDemo_Point {

    private double x, y;
    /**
     * Stamped类似一个时间戳的作用，每次写的时候对其+1来改变被操作对象的Stamped值
     * 这样其它线程读的时候发现目标对象的Stamped改变，则执行重读
     */
    private final StampedLock sl = new StampedLock();

    /**
     * 一个写操作
     * @param deltaX
     * @param deltaY
     */
    void move(double deltaX, double deltaY) {    // 这是一个排它锁
        /**
         * stampedLock调用writeLock和unlockWrite时候都会导致stampedLock的stamp值的变化
         * 即每次+1，直到加到最大值，然后从0重新开始
         */
        long stamp = sl.writeLock();//写锁
        try {
            x += deltaX;
            y += deltaY;

        } finally {
            sl.unlockWrite(stamp);//释放写锁

        }

    }

    /**
     * 一个只读方法，读取x，y坐标
     *
     */
    double distanceFromOrigin() {          // 只读方法
        /**
         * tryOptimisticRead是一个乐观的读，使用这种锁的读不阻塞写
         * 每次读的时候得到一个当前的stamp值（类似时间戳的作用）
         */
        long stamp = sl.tryOptimisticRead();
        //这里就是读操作，读取x和y，因为读取x时，y可能被写了新的值，所以下面需要判断
        double currentX = x, currentY = y;
        /**
         * 如果读取的时候发生了写，则stampedLock的stamp属性值会变化，此时需要重读，
         * 再重读的时候需要加读锁（并且重读时使用的应当是悲观的读锁，即阻塞写的读锁）。
         * 如果当前对象正在被修改，则读锁的申请可能导致线程挂起。
         * 当然重读的时候还可以使用tryOptimisticRead，此时需要结合循环了，即类似CAS方式
         * 读锁又重新返回一个stampe值
         */
        if (!sl.validate(stamp)) {
            stamp = sl.readLock(); //读锁
            try {
                currentX = x;
                currentY = y;

            } finally {
                sl.unlockRead(stamp); //释放读锁

            }
        }
        //读锁验证成功后才执行计算，即读的时候没有发生写
        return Math.sqrt(currentX * currentX + currentY * currentY);

    }
}
