package com.codoon.threadtracker.proxy

import com.codoon.threadtracker.ThreadInfoManager
import com.codoon.threadtracker.bean.ThreadInfo
import java.util.concurrent.Callable

/**
 * 静态代理线程池中的Runnable、Callable，时机一般是调用添加Runnable、Callable任务的方法时。这样PoolRunnableAndOther就可以获得调用栈与线程池名，并且可以在call或run时与当前线程建立联系
 * 有些类会同时继承Runnable、Callable，比如rxjava2，或同时继承Runnable、Comparable，比如使用PriorityBlockingQueue的线程池。为了避免类型转换失败，这里继承了已知所有接口，如有相关crash，可继续在这里添加接口
 */
class PoolRunnableAndOther constructor(
    private val any: Any,
    private val callStack: String,
    private val poolName: String? = null
) : Runnable, Callable<Any>, Comparable<Any> {
    private val callThreadId = Thread.currentThread().id

    override fun run() {
        val info = updateThreadInfo()
        (any as Runnable).run()
        // 任务已执行结束，callStack表示任务添加栈，此时应为空代表线程当前无任务在运行
        info.callStack = ""
    }

    override fun call(): Any {
        val info = updateThreadInfo()
        val v = (any as Callable<Any>).call()
        info.callStack = ""
        return v
    }

    private fun updateThreadInfo(): ThreadInfo {
        val thread = Thread.currentThread()
        var info = ThreadInfoManager.INSTANCE.getThreadInfoById(thread.id)

        // 有就更新没有就新建
        info = (info ?: ThreadInfo()).apply {
            id = thread.id
            name = thread.name
            state = thread.state
            // 对应info不为空情况，可能事先已在threadFactory中建立过线程-线程池关联，以此为准
            // 比如AsyncTask场景，具体参见ProxyAsyncTaskExecutor
            poolName = poolName ?: this@PoolRunnableAndOther.poolName
            callStack = this@PoolRunnableAndOther.callStack
            callThreadId = this@PoolRunnableAndOther.callThreadId
        }
        // 更新或添加
        ThreadInfoManager.INSTANCE.putThreadInfo(thread.id, info)
        return info
    }

    override fun compareTo(other: Any): Int {
        if (any is Comparable<*> && other is PoolRunnableAndOther) {
            val c = (any as Comparable<Any>)
            // return c.compareTo(other)
            return c.compareTo(other.getReal())
        }
        return 0
    }

    fun getReal(): Any {
        return any
    }
}
