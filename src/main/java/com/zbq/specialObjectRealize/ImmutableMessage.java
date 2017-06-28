package com.zbq.specialObjectRealize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by zhangboqing on 2017/6/28.
 * 典型不变对象的实现
 * 代码中对final的使用，它申明了当前消息中的几个字段都是常量，在消息构造完成后，就不能再发生改变了。
 * 更加需要注意的是，对于values字段，final关键字只能保证values引用的不可变性，并无法保证values对象的不可变性。
 * 为了实现彻底的不可变性，代码构造了一个不可变的List对象。
 */
public final class ImmutableMessage {
    private final int sequenceNumber;
    private final List<String> values;

    public ImmutableMessage(int sequenceNumber, List<String> values) {
        this.sequenceNumber = sequenceNumber;
        this.values = Collections.unmodifiableList(new ArrayList<String>(values));

    }

    public int getSequenceNumber() {
        return sequenceNumber;

    }

    public List<String> getValues() {
        return values;

    }
}
