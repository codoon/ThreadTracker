package com.codoon.threadtracker.proxy

import com.codoon.threadtracker.ThreadInfoManager
import com.codoon.threadtracker.TrackerUtils
import com.codoon.threadtracker.bean.ThreadPoolInfo
import java.lang.ref.WeakReference
import java.util.concurrent.*

/**
 * ScheduledThreadPoolExecutor代理类
 * 参见TBaseThreadPoolExecutor注释
 *
 * 因ScheduledThreadPoolExecutor extends ThreadPoolExecutor
 * 但此继承关系在sdk中，asm无法改变，于是ThreadPoolExecutor中方法复制一份过来
 */
open class TBaseScheduledThreadPoolExecutor : ScheduledThreadPoolExecutor {
    private val poolName = TrackerUtils.toObjectString(this)
    private val weakRunnableList = mutableListOf<WeakReference<Runnable>>()

    constructor(corePoolSize: Int) : super(corePoolSize)
    constructor(corePoolSize: Int, threadFactory: ThreadFactory?) : super(
        corePoolSize,
        threadFactory
    ) {
        init()
    }

    constructor(corePoolSize: Int, handler: RejectedExecutionHandler?) : super(
        corePoolSize,
        handler
    ) {
        init()
    }

    constructor(
        corePoolSize: Int,
        threadFactory: ThreadFactory?,
        handler: RejectedExecutionHandler?
    ) : super(corePoolSize, threadFactory, handler) {
        init()
    }

    init {
        val createStack = TrackerUtils.getStackString()
        val poolInfo = ThreadPoolInfo()
        poolInfo.poolName = poolName
        poolInfo.createStack = createStack
        poolInfo.createThreadId = Thread.currentThread().id
        ThreadInfoManager.INSTANCE.putThreadPoolInfo(poolName, poolInfo)
    }

    private fun init() {
        threadFactory = TBaseThreadFactory(threadFactory, poolName)
    }

    override fun <V : Any?> schedule(
        callable: Callable<V>,
        delay: Long,
        unit: TimeUnit
    ): ScheduledFuture<V> {

        var task = callable
        if (task !is PoolRunnableAndOther) {
            val callStack = TrackerUtils.getStackString()
            task = PoolRunnableAndOther(task, callStack, poolName) as Callable<V>
        }
        return super.schedule(task, delay, unit)
    }

    override fun schedule(command: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture<*> {
        var runnable = command
        if (runnable !is PoolRunnableAndOther) {
            val callStack = TrackerUtils.getStackString()
            runnable = PoolRunnableAndOther(command, callStack, poolName)
        }
        return super.schedule(runnable, delay, unit)
    }

    override fun scheduleAtFixedRate(
        command: Runnable,
        initialDelay: Long,
        period: Long,
        unit: TimeUnit
    ): ScheduledFuture<*> {
        var runnable = command
        if (runnable !is PoolRunnableAndOther) {
            val callStack = TrackerUtils.getStackString()
            runnable = PoolRunnableAndOther(command, callStack, poolName)
        }
        return super.scheduleAtFixedRate(runnable, initialDelay, period, unit)
    }

    override fun scheduleWithFixedDelay(
        command: Runnable,
        initialDelay: Long,
        delay: Long,
        unit: TimeUnit
    ): ScheduledFuture<*> {
        var runnable = command
        if (runnable !is PoolRunnableAndOther) {
            val callStack = TrackerUtils.getStackString()
            runnable = PoolRunnableAndOther(command, callStack, poolName)
        }
        return super.scheduleWithFixedDelay(runnable, initialDelay, delay, unit)
    }

    override fun submit(task: Runnable): Future<*> {
        var runnable = task
        if (task !is PoolRunnableAndOther) {
            val callStack = TrackerUtils.getStackString()
            runnable = PoolRunnableAndOther(task, callStack, poolName)
        }
        return super.submit(runnable)
    }

    override fun <T : Any?> submit(task: Callable<T>): Future<T> {
        var callable = task
        if (task !is PoolRunnableAndOther) {
            val callStack = TrackerUtils.getStackString()
            callable = PoolRunnableAndOther(task, callStack, poolName) as Callable<T>
        }
        return super.submit(callable)
    }

    override fun <T : Any?> submit(task: Runnable, result: T): Future<T> {
        var runnable = task
        if (task !is PoolRunnableAndOther) {
            val callStack = TrackerUtils.getStackString()
            runnable = PoolRunnableAndOther(task, callStack, poolName)
        }
        return super.submit(runnable, result)
    }

    private fun <T> proxyInvokeList(tasks: MutableCollection<out Callable<T>>): MutableList<Callable<T>> {
        val callStack = TrackerUtils.getStackString()
        val proxyTasks = mutableListOf<Callable<T>>()
        tasks.forEach { task ->
            if (task !is PoolRunnableAndOther) {
                val callable = PoolRunnableAndOther(task, callStack, poolName)
                proxyTasks.add(callable as Callable<T>)
            } else {
                proxyTasks.add(task)
            }
            Unit
        }
        return proxyTasks
    }

    override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>): T {
        return super.invokeAny(proxyInvokeList(tasks))
    }

    override fun <T : Any?> invokeAny(
        tasks: MutableCollection<out Callable<T>>,
        timeout: Long,
        unit: TimeUnit
    ): T {
        return super.invokeAny(proxyInvokeList(tasks), timeout, unit)
    }

    override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>): MutableList<Future<T>> {
        return super.invokeAll(proxyInvokeList(tasks))
    }

    override fun <T : Any?> invokeAll(
        tasks: MutableCollection<out Callable<T>>,
        timeout: Long,
        unit: TimeUnit
    ): MutableList<Future<T>> {
        return super.invokeAll(proxyInvokeList(tasks), timeout, unit)
    }

    override fun execute(command: Runnable) {
        val callStack = TrackerUtils.getStackString()
        var runnable = command
        if (command !is PoolRunnableAndOther) { // 可能先进入队列后进入这里，此时已经是PoolRunnable
            runnable = PoolRunnableAndOther(command, callStack, poolName)
            weakRunnableList.add(WeakReference(runnable))
        }
        super.execute(runnable)
    }

    override fun setThreadFactory(threadFactory: ThreadFactory?) {
        threadFactory?.apply {
            super.setThreadFactory(TBaseThreadFactory(this, poolName))
        } ?: super.setThreadFactory(threadFactory)
    }

    override fun getThreadFactory(): ThreadFactory {
        if (super.getThreadFactory() is TBaseThreadFactory) {
            return (super.getThreadFactory() as TBaseThreadFactory).getReal()
        } else {
            return super.getThreadFactory()
        }
    }

    override fun remove(task: Runnable?): Boolean {

        for (i in 0 until weakRunnableList.size) {
            val runnable = weakRunnableList[i].get()
            if (runnable is PoolRunnableAndOther && runnable.getReal() == task) {
                weakRunnableList.removeAt(i)
                return super.remove(runnable)
            }
        }
        return super.remove(task)
    }

    override fun shutdown() {
        weakRunnableList.clear()
        super.shutdown()
        ThreadInfoManager.INSTANCE.shutDownPool(poolName)
    }

    override fun shutdownNow(): MutableList<Runnable> {
        val list = super.shutdownNow()
        for (i in 0 until list.size) {
            if (list[i] is PoolRunnableAndOther) {
                val real = (list[i] as PoolRunnableAndOther).getReal()
                if (real is Runnable) {
                    list[i] = real
                }
            }
        }
        weakRunnableList.clear()
        ThreadInfoManager.INSTANCE.shutDownPool(poolName)
        return list
    }
}