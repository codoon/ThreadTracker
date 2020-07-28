package com.codoon.threadtracker.proxy

import android.os.HandlerThread
import android.os.SystemClock
import com.codoon.threadtracker.ThreadInfoManager
import com.codoon.threadtracker.TrackerUtils
import com.codoon.threadtracker.bean.ThreadInfo

/**
 * 因HandlerThread继承自Thread，所以复制ProxyThread方法
 */
open class TBaseHandlerThread : HandlerThread {
    constructor(name: String) : super(name)

    constructor(name: String, priority: Int) : super(name, priority)

    @Synchronized
    override fun start() {
        val callStack = TrackerUtils.getStackString()
        super.start()

        // 有则更新没有则新增
        val info = ThreadInfoManager.INSTANCE.getThreadInfoById(id)
        info?.also {
            it.id = id
            it.name = name
            it.state = state
            if (it.callStack.isEmpty()) { // 如果来自线程池，callStack意义为任务添加栈，可能已经有值了，不能更新为start调用栈
                it.callStack = callStack
                it.callThreadId = currentThread().id
            }
        } ?: apply {
            val newInfo = ThreadInfo()
            newInfo.id = id
            newInfo.name = name
            newInfo.callStack = callStack
            newInfo.callThreadId = currentThread().id
            newInfo.state = state
            newInfo.startTime = SystemClock.elapsedRealtime()
            ThreadInfoManager.INSTANCE.putThreadInfo(id, newInfo)
        }
    }

    override fun run() {
        super.run()
        ThreadInfoManager.INSTANCE.removeThreadInfo(id)
    }
}