package com.codoon.threadtracker.bean

/**
 * 展示线程/线程池列表所用数据
 */
data class ShowInfo(
    var threadId: Long = -1L,
    var threadName: String = "",
    var threadState: Thread.State = Thread.State.TERMINATED,
    var poolName: String? = null,
    var type: Int = SINGLE_THREAD // 0单个线程 1线程池 2线程池中的线程
) {
    companion object {
        const val SINGLE_THREAD = 0
        const val POOL = 1
        const val POOL_THREAD = 2
    }
}

