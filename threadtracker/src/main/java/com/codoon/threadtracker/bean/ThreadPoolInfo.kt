package com.codoon.threadtracker.bean

/**
 * 线程池信息
 * 在获取当前所有线程时会根据SingleThreadInfo#poolName填写threadIds
 * 填写后如果threadIds为空并且已经被调用过shutDown，则清除此对象
 */
data class ThreadPoolInfo(
    var poolName: String = "",
    var createStack: String = "", // 线程池对象创建栈
    var createThreadId: Long = -1L, // 被创建时所处线程id
    var threadIds: MutableList<Long> = mutableListOf(), // 包含的线程，在获取当前所有线程信息时填写
    var shutDown: Boolean = false // 是否已被调用shutDown或shutDownNow
)