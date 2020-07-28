package com.codoon.threadtracker.proxy

import android.os.SystemClock
import com.codoon.threadtracker.ThreadInfoManager
import com.codoon.threadtracker.TrackerUtils.getStackString
import com.codoon.threadtracker.bean.ThreadInfo

/**
 * 如果有人自定义Thread，理论上一定会调用super，而继承通过字节码改成ProxyThread，因为调用了super所以这里也能调用到
 * start方法也可能是来自线程池，所以putThreadInfo时要先查找，以免用空的poolName覆盖原有poolName
 * 比如以下调用栈
 * com.codoon.threadtracker.proxy.ProxyThread.start(ProxyThread.kt:31)
 * java.util.concurrent.ThreadPoolExecutor.addWorker(ThreadPoolExecutor.java:970)
 * java.util.concurrent.ThreadPoolExecutor.ensurePrestart(ThreadPoolExecutor.java:1611)
 * java.util.concurrent.ScheduledThreadPoolExecutor.delayedExecute(ScheduledThreadPoolExecutor.java:342)
 * java.util.concurrent.ScheduledThreadPoolExecutor.scheduleWithFixedDelay(ScheduledThreadPoolExecutor.java:629)
 */
open class TBaseThread : Thread {
    internal constructor() : super()
    internal constructor(runnable: Runnable?) : super(runnable)
    internal constructor(group: ThreadGroup?, target: Runnable?) : super(group, target)
    internal constructor(group: ThreadGroup?, name: String) : super(group, name)
    internal constructor(target: Runnable?, name: String) : super(target, name)
    internal constructor(group: ThreadGroup?, target: Runnable?, name: String) : super(
        group,
        target,
        name
    )

    internal constructor(
        group: ThreadGroup?,
        target: Runnable?,
        name: String,
        stackSize: Long
    ) : super(group, target, name, stackSize)

    @Synchronized
    override fun start() {
        val callStack = getStackString()
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