package com.zbq.function;

import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

/**
 * Created by zhangboqing on 2017/6/7.
 *
 * 并行排序
 *
 * 1.除了并行流外，对于普通数组，Java 8中也提供了简单的并行功能。
 * 比如，对于数组排序，我们有Arrays.sort()方法。当然这是串行排序，
 * 但在Java 8中，我们可以使用新增的Arrays. parallel-Sort()方法直接使用并行排序。
 *
 *
 * 2.除了并行排序外，Arrays中还增加了一些API用于数组中数据的赋值，
 * 比如：public static void setAll(int[] array, IntUnaryOperator generator)
 */
public class Parallel_Sort {



    @Test
    public void run() {
        /**并行排序*/
        int[] arr=new int[10000000];
        Arrays.parallelSort(arr);

    }

    @Test
    public void run2() {
        int[] arr=new int[10000000];
        Random r=new Random();
        /**串行给数组中每一个元素都附上一个随机值*/
        Arrays.setAll(arr, (i)->r.nextInt());

        /**并行给数组中每一个元素都附上一个随机值*/
        Arrays. parallelSetAll (arr, (i)->r.nextInt());
    }

}
