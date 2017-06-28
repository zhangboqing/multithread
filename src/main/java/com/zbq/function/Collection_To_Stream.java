package com.zbq.function;

import com.zbq.function.bean.Student;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangboqing on 2017/6/7.
 *
 * 从集合到并行流
 */
public class Collection_To_Stream {


    /**
     * 从集合对象List中，我们使用stream()方法可以得到一个串行流
     */
    @Test
    public void run() {
        List<Student> ss=new ArrayList<Student>();
        double ave=ss.stream().mapToInt(s->s.score).average().getAsDouble();
    }

     /**
     * 使用parallelStream()函数来变成并行流
     */
    @Test
    public void run2() {
        List<Student> ss=new ArrayList<Student>();
        double ave=ss.parallelStream().mapToInt(s->s.score).average().getAsDouble();
    }


}

