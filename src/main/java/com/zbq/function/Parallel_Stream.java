package com.zbq.function;

import org.junit.Test;

import java.util.stream.IntStream;

/**
 * Created by zhangboqing on 2017/6/7.
 *
 * 使用并行流过滤数据
 */
public class Parallel_Stream {

    /**判断是否为质数*/
    public static class PrimeUtil {
        public static boolean isPrime(int number) {
            int tmp = number;
            if (tmp < 2) {
                return false;
            }
            for (int i = 2; Math.sqrt(tmp) >= i; i++) {
                if (tmp % i == 0) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * 串行流
     * 首先生成一个1到1000000的数字流。接着使用过滤函数，只选择所有的质数，最后进行数量统计
     */
    @Test
    public void run() {
        IntStream.range(1, 1000000).filter(PrimeUtil::isPrime).count();
    }

    /**
     * 并行流
     *
     * 上述代码是串行的，将它改造成并行计算非常简单，只需要将流并行化即可
     *
     * 首先parallel()方法得到一个并行流，接着，在并行流上进行过滤，
     * 此时，PrimeU-til.isPrime()函数会被多线程并发调用，应用于流中的所有元素。
     */
    @Test
    public void run2() {
        IntStream.range(1, 1000000).parallel().filter(PrimeUtil::isPrime).count();
    }


}

