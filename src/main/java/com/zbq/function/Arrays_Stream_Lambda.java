package com.zbq.function;

import org.junit.Test;

import java.util.Arrays;
import java.util.function.IntConsumer;

/**
 * Created by zhangboqing on 2017/6/7.
 *
 * 1.lambda方式简化代码
 * 2.函数引用
 *
 * Arrays.stream()方法返回了一个流对象。类似于集合或者数组，流对象也是一个对象的集合，它将给予我们遍历处理流内元素的功能。
 */
public class Arrays_Stream_Lambda {

    static int[] arr = {1, 3, 4, 5, 6, 7, 8, 9, 10};

    @Test
    public void run() {
        Arrays.stream(arr).forEach(new IntConsumer() {
            @Override
            public void accept(int value) {
                System.out.println(value);
            }
        });
    }


    /**
     * 采用lambda方式简化上面的书写
     * lambda表达式。表达式由“->”分割，左半部分表示参数，右半部分表示实现体
     * 使用虚拟机参数-Djdk.in-ternal.lambda.dumpProxyClasses启动带有lambda表达式的Java小程序，
     * 该参数会将lambda表达式相关的中间类型进行输出，方便调试和学习。
     */
    @Test
    public void run2() {

        //1.省略IntStream接口名称
        Arrays.stream(arr).forEach((final int x) -> {
            System.out.println(x);
        });

        //2.省略参数类型
        Arrays.stream(arr).forEach((x) -> {
            System.out.println(x);
        });

        //3.去掉花括号
        Arrays.stream(arr).forEach((x) -> System.out.println(x));

        //4.Java 8还支持了方法引用，通过方法引用的推导，你甚至连参数申明和传递都可以省略。
        Arrays.stream(arr).forEach(System.out::println);
    }


    /**
     * 这里首先使用函数引用，直接定义了两个Int-Consumer接口实例，一个指向标准输出，另一个指向标准错误。
     * 使用接口默认函数IntConsumer.addThen()，将两个IntConsumer进行组合，得到一个新的IntConsumer，
     * 这个新的Int-Consumer会依次调用outprintln和errprintln，完成对数组中元素的处理。
     *
     * 这个新的IntConsumer会先调用第1个IntConsumer进行处理，接着调用第2个Int-Consumer处理，从而实现多个处理器的整合
     */
    @Test
    public void run3() {

        IntConsumer outprintln = System.out::println;
        IntConsumer errprintln = System.err::println;
        Arrays.stream(arr).forEach(outprintln.andThen(errprintln));
    }

}
