package com.zbq.concurrent;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Created by zhangboqing on 2017/6/7.
 * <p>
 * <p>
 * CompletableFuture相对Future的特点：
 * <p>
 * 1.手动设置CompletableFuture的完成状态
 * 2.通过CompletableFuture提供的进一步封装，我们很容易实现Future模式那样的异步调用
 * 3.流式调用
 * 4.CompletableFuture中的异常处理
 * 5.组合多个CompletableFuture
 * <p>
 * <p>
 * 在CompletableFuture中，类似的工厂方法有以下几个：
 * static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier);
 * <p>
 * static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor);
 * <p>
 * static CompletableFuture<Void> runAsync(Runnable runnable);
 * <p>
 * static CompletableFuture<Void> runAsync(Runnable runnable, Executor executor);
 * <p>
 * 其中supplyAsync()方法用于那些需要有返回值的场景，比如计算某个数据等。而runAsync()方法用于没有返回值的场景，比如，仅仅是简单地执行某一个异步动作。
 * 在这两对方法中，都有一个方法可以接收一个Executor参数。这就使我们可以让Supplier<U>或者Runnable在指定的线程池中工作。如果不指定，则在默认的系统公共的ForkJoinPool.common线程池中执行。
 * <p>
 * 注意：在Java 8中，新增了ForkJoin-Pool.commonPool()方法。它可以获得一个公共的ForkJoin线程池。这个公共线程池中的所有线程都是Daemon线程。这意味着如果主线程退出，这些线程无论是否执行完毕，都会退出系统。
 */
public class CompletableFutureDemo {


    /**
     * 计算CompletableFuture表示的数字的平方，并将其打印
     */
    public static class AskThread implements Runnable {
        CompletableFuture<Integer> re = null;

        public AskThread(CompletableFuture<Integer> re) {
            this.re = re;

        }

        @Override
        public void run() {
            int myRe = 0;
            try {
                /**
                 * 当re中没有数据时，CompletableFuture处于未完成状态，会进行阻塞
                 * 等待完成
                 */
                myRe = re.get() * re.get();

            } catch (Exception e) {

            }
            System.out.println(myRe);

        }
    }

    /**
     * 1.
     */
    @Test
    public void run1() throws InterruptedException {
        final CompletableFuture<Integer> future = new CompletableFuture<>();
        new Thread(new AskThread(future)).start();
        // 模拟长时间的计算过程
        Thread.sleep(1000);
        // 将最终数据载入CompletableFuture，并标记为完成状态
        future.complete(60);

    }


    public static Integer calc(Integer para) {
        try {
            // 模拟一个长时间的执行
            Thread.sleep(1000);

        } catch (InterruptedException e) {

        }
        return para * para;

    }

    /**
     * 2.
     */
    public void run2() throws InterruptedException, ExecutionException {
        /**
         * 使用Completable-Future.supplyAsync()方法构造一个Completable-Future实例，
         * 在supplyAsync()函数中，它会在一个新的线程中，执行传入的参数。在这里，它会执行calc()方法
         * supplyAsync()会立即返回
         */
        final CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> calc(50));

        /**当计算还没有完成时，future.get()将会进行等待*/
        System.out.println(future.get());
    }

    /**
     * 3.
     */
    public void run3() throws InterruptedException, ExecutionException {
        /**使用supplyAsync()函数执行一个异步任务。接着连续使用流式调用对任务的处理结果进行再加工，直到最后的结果输出。*/
        CompletableFuture<Void> fu = CompletableFuture
                .supplyAsync(() -> calc(50))
                .thenApply((i) -> Integer.toString(i))
                .thenApply((str) -> "\"" + str + "\"")
                .thenAccept(System.out::println);
        /**等待计算完成*/
        fu.get();

    }

    public static Integer calc2(Integer para) {
        return para / 0;

    }

    /**
     * 4.如果CompletableFuture在执行过程中遇到异常，我们可以用函数式编程的风格来优雅地处理这些异常。
     * CompletableFuture提供了一个异常处理方法exceptionally()
     */
    public void run4() throws InterruptedException, ExecutionException {

        CompletableFuture<Void> fu = CompletableFuture
                .supplyAsync(() -> calc2(50))
                /**
                 * 对当前的Completable-Future进行异常处理。如果没有异常发生，则CompletableFuture就会返回原有的结果。
                 * 如果遇到了异常，就可以在exceptionally()中处理异常，并返回一个默认的值
                 */
                .exceptionally(ex -> {
                    System.out.println(ex.toString());
                    return 0;

                })
                .thenApply((i) -> Integer.toString(i)).thenApply((str) -> "\"" + str + "\"")
                .thenAccept(System.out::println);

        fu.get();
    }

    public static Integer calc3(Integer para) {
        return para / 2;

    }

    /**
     * 5.CompletableFuture还允许你将多个CompletableFuture进行组合。
     * 1)一种方法是使用thenCompose()，
     * 它的签名如下：public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn)
     * <p>
     * 一个CompletableFuture可以在执行完成后，将执行结果通过Function传递给下一个Comple-tionStage进行处理（Function接口返回新的Com-pletionStage实例）
     * <p>
     * 2)另外一种组合多个CompletableFuture的方法是thenCombine()，它的签名如下：public <U,V> CompletableFuture<V> thenCombine
     * (CompletionStage<? extends U> other,BiFunction<? super T,? super U,? extends V> fn)
     * <p>
     * 方法thenCombine()首先完成当前Com-pletableFuture和other的执行。
     * 接着，将这两者的执行结果传递给BiFunction（该接口接收两个参数，并有一个返回值），
     * 并返回代表BiFunction实例的CompletableFuture对象
     */
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        /**1*/
        CompletableFuture<Void> fu = CompletableFuture
                .supplyAsync(() -> calc(50))
                .thenCompose((i) -> CompletableFuture.supplyAsync(() -> calc(i)))
                .thenApply((str) -> "\"" + str + "\"")
                .thenAccept(System.out::println);
        fu.get();

        /**2
         * 首先生成两个CompletableFu-ture实例，
         * 接着使用thenCombine()组合这两个CompletableFuture，
         * 将两者的执行结果进行累加（由(i,j)->(i+j)实现），并将其累加结果转为字符串，并输出
         */
        CompletableFuture<Integer> intFuture = CompletableFuture.supplyAsync(() -> calc(50));

        CompletableFuture<Integer> intFuture2 = CompletableFuture
                .supplyAsync(() -> calc(25));

        CompletableFuture<Void> fu2 = intFuture
                .thenCombine(intFuture2, (i, j) -> (i + j))
                .thenApply((str) -> "\"" + str + "\"")
                .thenAccept(System.out::println);

        fu.get();

    }
}

