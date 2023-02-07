package com.promc.payapi.util;

/**
 * 雪花算法
 * 参考: https://gitee.com/yu120/sequence
 */
public final class Sequence {

    /**
     * 起始时间戳
     * 2021年10月1日
     */
    private static final long START_TIME = 1633046400000L;

    /**
     * dataCenterId占用的位数：2
     */
    private static final long DATA_CENTER_ID_BITS = 2L;
    /**
     * workerId占用的位数：8
     */
    private static final long WORKER_ID_BITS = 8L;
    /**
     * 序列号占用的位数：12（表示只允许workId的范围为：0-4095）
     */
    private static final long SEQUENCE_BITS = 12L;

    /**
     * workerId可以使用范围：0-255
     */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    /**
     * dataCenterId可以使用范围：0-3
     */
    private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;

    /**
     * 用mask防止溢出:位与运算保证计算的结果范围始终是 0-4095
     */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private final long workerId;
    private final long dataCenterId;
    private long autoIncrementCode = 0L;
    private long lastTimestamp = -1L;

    private final long timeOffset;

    public Sequence(long dataCenterId, long workerId) {
        this(dataCenterId, workerId, 5L);
    }

    /**
     * 基于Snowflake创建分布式ID生成器
     *
     * @param dataCenterId 数据中心ID,数据范围为0~255
     * @param workerId     工作机器ID,数据范围为0~3
     * @param timeOffset   允许时间回拨的毫秒量,建议5ms
     */
    public Sequence(long dataCenterId, long workerId, long timeOffset) {
        if (dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0) {
            throw new IllegalArgumentException("Data Center Id can't be greater than " + MAX_DATA_CENTER_ID + " or less than 0");
        }
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("Worker Id can't be greater than " + MAX_WORKER_ID + " or less than 0");
        }

        this.workerId = workerId;
        this.dataCenterId = dataCenterId;
        this.timeOffset = timeOffset;
    }

    /**
     * 获取ID
     *
     * @return long
     */
    public synchronized Long nextId() {
        long currentTimestamp = this.timeGen();

        // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过，这个时候应当抛出异常
        while (currentTimestamp < lastTimestamp) {
            // 校验时间偏移回拨量
            long offset = lastTimestamp - currentTimestamp;
            if (offset > timeOffset) {
                throw new IllegalStateException("Clock moved backwards, refusing to generate id for [" + offset + "ms]");
            }

            try {
                // 时间回退timeOffset毫秒内，则允许等待2倍的偏移量后重新获取，解决小范围的时间回拨问题
                wait(offset << 1);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            // 再次获取
            currentTimestamp = this.timeGen();
        }

        // 同一毫秒内序列直接自增
        if (lastTimestamp == currentTimestamp) {
            long temp = autoIncrementCode + 1;

            // 通过位与运算保证计算的结果范围始终是 0-4095
            autoIncrementCode = temp & SEQUENCE_MASK;
            if (autoIncrementCode == 0) {
                currentTimestamp = this.tilNextMillis(lastTimestamp);
            }
        } else {
            // 起始值为0L开始自增
            autoIncrementCode = 0L;
        }

        lastTimestamp = currentTimestamp;
        long currentOffsetTime = currentTimestamp - START_TIME;

        /*
         * 1.左移运算是为了将数值移动到对应的段(41、5、5，12那段因为本来就在最右，因此不用左移)
         * 2.然后对每个左移后的值(la、lb、lc、sequence)做位或运算，是为了把各个短的数据合并起来，合并成一个二进制数
         * 3.最后转换成10进制，就是最终生成的id
         */
        return (currentOffsetTime << TIMESTAMP_LEFT_SHIFT) |
                // 数据中心位
                (dataCenterId << DATA_CENTER_ID_SHIFT) |
                // 工作ID位
                (workerId << WORKER_ID_SHIFT) |
                // 毫秒序列化位
                autoIncrementCode;
    }

    /**
     * 保证返回的毫秒数在参数之后(阻塞到下一个毫秒，直到获得新的时间戳)——CAS
     *
     * @param lastTimestamp last timestamp
     * @return next millis
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = this.timeGen();
        while (timestamp <= lastTimestamp) {
            // 如果发现时间回拨，则自动重新获取（可能会处于无限循环中）
            timestamp = this.timeGen();
        }

        return timestamp;
    }

    /**
     * 获得系统当前毫秒时间戳
     *
     * @return timestamp 毫秒时间戳
     */
    private long timeGen() {
        return System.currentTimeMillis();
    }

}