package com.codoon.threadtracker.bean

/**
 * 线程信息
 * todo 考虑新增类型字段，比如属于Timer还是HandlerThread还是其他什么的
 */
data class ThreadInfo(
    var id: Long = -1L,
    var name: String = "",
    var state: Thread.State = Thread.State.TERMINATED,
    var callStack: String = "", // 如果是单个线程，则是start被调用堆栈，如果是线程池中线程，此字段意义为当前正在执行的task被添加的栈。因task执行完马上被置空，后续可以考虑记录最近一次任务的添加栈信息
    var callThreadId: Long = -1L, // 被调用/添加时所处线程id，方便查看调用链
    var runningStack: String = "", // 运行时栈，由Thread.getAllStackTraces获取
    var poolName: String? = null, // 此线程所属线程池，没有就是null
    var startTime: Long = -1L, // 线程start的cpu时间，用于计算线程运行时间。线程池中的线程无此信息
    var hit: Int = HIT_NEW // 在跟获取当前所有进程后对比时用，0新添加 1未命中 2命中
) {
    companion object {
        const val HIT_NEW = 0
        const val HIT_NO = 1
        const val HIT_YES = 2
    }
}