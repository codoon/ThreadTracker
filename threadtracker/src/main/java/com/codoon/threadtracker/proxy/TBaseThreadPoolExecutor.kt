package com.codoon.threadtracker.proxy

import com.codoon.threadtracker.ThreadInfoManager
import com.codoon.threadtracker.TrackerUtils
import com.codoon.threadtracker.TrackerUtils.toObjectString
import com.codoon.threadtracker.bean.ThreadPoolInfo
import java.lang.ref.WeakReference
import java.util.concurrent.*

/**
 * ThreadPoolExecutor代理类
 * 修改后记得同步到TBaseScheduledThreadPoolExecutor
 *
 * 注意类似的base类要加open关键字！
 */
open class TBaseThreadPoolExecutor : ThreadPoolExecutor {
    private val poolName = toObjectString(this)

    // 用于remove操作时，根据原始runnable寻找被代理包装后的runnable并移除
    private val weakRunnableList = mutableListOf<WeakReference<Runnable>>()

    init {
        val createStack = TrackerUtils.getStackString()
        val poolInfo = ThreadPoolInfo()
        poolInfo.poolName = poolName
        poolInfo.createStack = createStack
        poolInfo.createThreadId = Thread.currentThread().id
        ThreadInfoManager.INSTANCE.putThreadPoolInfo(poolName, poolInfo)
    }

    constructor(
        corePoolSize: Int,
        maximumPoolSize: Int,
        keepAliveTime: Long,
        unit: TimeUnit?,
        workQueue: BlockingQueue<Runnable>?
    ) : super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue) {
        init()
    }

    constructor(
        corePoolSize: Int,
        maximumPoolSize: Int,
        keepAliveTime: Long,
        unit: TimeUnit?,
        workQueue: BlockingQueue<Runnable>?,
        threadFactory: ThreadFactory?
    ) : super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory) {
        init()
    }

    constructor(
        corePoolSize: Int,
        maximumPoolSize: Int,
        keepAliveTime: Long,
        unit: TimeUnit?,
        workQueue: BlockingQueue<Runnable>?,
        handler: RejectedExecutionHandler?
    ) : super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler) {
        init()
    }

    constructor(
        corePoolSize: Int,
        maximumPoolSize: Int,
        keepAliveTime: Long,
        unit: TimeUnit?,
        workQueue: BlockingQueue<Runnable>?,
        threadFactory: ThreadFactory?,
        handler: RejectedExecutionHandler?
    ) : super(
        corePoolSize,
        maximumPoolSize,
        keepAliveTime,
        unit,
        workQueue,
        threadFactory,
        handler
    ) {
        init()
    }

    private fun init() {
        // 这样可以尽早将线程和线程池建立联系，而不用等到run时
        threadFactory = TBaseThreadFactory(threadFactory, poolName)
    }

    override fun setThreadFactory(threadFactory: ThreadFactory?) {
        threadFactory?.apply {
            super.setThreadFactory(TBaseThreadFactory(this, poolName))
        } ?: super.setThreadFactory(threadFactory)
    }

    // submit使得task被包了一层，无法直接remove了，不用存到weakRunnableList中
    override fun submit(task: Runnable): Future<*> {
        var runnable = task
        if (task !is PoolRunnableAndOther) {
            val callStack = TrackerUtils.getStackString()
            runnable = PoolRunnableAndOther(task, callStack, poolName)
        }
        return super.submit(runnable)
    }

    // callable不能通过remove移除，不用存到weakRunnableList中
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

    override fun getThreadFactory(): ThreadFactory {
        if (super.getThreadFactory() is TBaseThreadFactory) {
            // 防止上层自定义ThreadFactory，get后向下转型失败
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

    override fun getQueue(): BlockingQueue<Runnable> {
        // 这里如果上层强转runnable会出错，暂未处理
        return super.getQueue()
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

    private fun <T> proxyInvokeList(tasks: MutableCollection<out Callable<T>>): MutableList<Callable<T>> {
        val callStack = TrackerUtils.getStackString()
        val proxyTasks = mutableListOf<Callable<T>>()
        tasks.forEach { task ->
            if (task !is PoolRunnableAndOther) {
                val callable = PoolRunnableAndOther(task, callStack, poolName) as Callable<T>
                proxyTasks.add(callable)
            } else {
                proxyTasks.add(task)
            }
            Unit
        }
        return proxyTasks
    }

    // 复写这个方法有可能导致本该马上回收的被延迟回收
    // override fun finalize() {
    //     ThreadInfoManager.INSTANCE.removePool(poolName)
    //     super.finalize()
    // }
}