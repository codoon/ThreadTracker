package com.codoon.threadtracker.proxy

import com.codoon.threadtracker.ThreadInfoManager
import com.codoon.threadtracker.bean.ThreadInfo
import java.util.concurrent.ThreadFactory


open class TBaseThreadFactory(
    private val threadFactory: ThreadFactory,
    private val poolName: String
) : ThreadFactory {
    override fun newThread(runnable: Runnable): Thread {
        // 注意这里面的runnable是被worker包装过的，已经不是用户传来的runnable
        val thread = threadFactory.newThread(runnable)
        addThreadInfo(thread)
        return thread
    }

    private fun addThreadInfo(thread: Thread) {
        var info = ThreadInfoManager.INSTANCE.getThreadInfoById(thread.id)
        info = (info ?: ThreadInfo()).also {
            it.id = thread.id
            it.name = thread.name
            it.state = thread.state
            it.poolName = poolName
        }
        ThreadInfoManager.INSTANCE.putThreadInfo(thread.id, info)
    }

    fun getReal(): ThreadFactory {
        return threadFactory
    }
}