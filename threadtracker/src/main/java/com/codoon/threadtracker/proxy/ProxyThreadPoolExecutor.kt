package com.codoon.threadtracker.proxy

import com.codoon.threadtracker.ThreadInfoManager.Companion.INSTANCE
import com.codoon.threadtracker.TrackerUtils.getStackString
import com.codoon.threadtracker.TrackerUtils.toObjectString
import com.codoon.threadtracker.bean.ThreadPoolInfo
import java.util.concurrent.*

/**
 * 目前只是为了代理AsyncTask.THREAD_POOL_EXECUTOR
 * 不用动态代理主要是为了防止上层将AsyncTask.THREAD_POOL_EXECUTOR强转为ThreadPoolExecutor时崩溃
 * 除execute方法外其他比如remove、submit等暂未处理，因AsyncTask.THREAD_POOL_EXECUTOR只是Executor
 * ！！！这个类只是防止crash，并未承担其他ThreadPoolExecutor的责任
 */
open class ProxyThreadPoolExecutor(private val real: ThreadPoolExecutor) :
    ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, LinkedBlockingDeque<Runnable>()) {

    private val poolName = toObjectString(real)

    init {
        val createStack = getStackString(false)
        val poolInfo = ThreadPoolInfo()
        poolInfo.poolName = poolName
        poolInfo.createStack = createStack
        poolInfo.createThreadId = Thread.currentThread().id
        INSTANCE.putThreadPoolInfo(poolName, poolInfo)
    }

    override fun execute(command: Runnable) {
        val callStack = getStackString(true)

        // 处理外部直接使用AsyncTask.THREAD_POOL_EXECUTOR.execute的情况，
        // 因AsyncTask中sDefaultExecutor把PoolRunnableAndOther又包装成Runnable提交到THREAD_POOL_EXECUTOR
        // 所以这里根据调用栈区分，如果是外部直接execute则包装成PoolRunnableAndOther，否则什么都不做
        // Log.d(LOG_TAG,"ProxyAsyncTaskExecutor callStack: "+ callStack);
        //     android.os.AsyncTask$SerialExecutor.scheduleNext(AsyncTask.java:258)
        //     android.os.AsyncTask$SerialExecutor.execute(AsyncTask.java:252)
        val runnable: Runnable
        if (!callStack.contains("AsyncTask\$SerialExecutor.scheduleNext")) {
            runnable = PoolRunnableAndOther(command, callStack, poolName)
        } else {
            runnable = command
        }
        real.execute(runnable)
    }


    override fun submit(task: Runnable): Future<*> {
        return real.submit(task)
    }

    override fun <T : Any?> submit(task: Runnable, result: T): Future<T> {
        return real.submit(task, result)
    }

    override fun <T : Any?> submit(task: Callable<T>): Future<T> {
        return real.submit(task)
    }

    override fun getCorePoolSize(): Int {
        return real.corePoolSize
    }

    override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>): T {
        return real.invokeAny(tasks)
    }

    override fun <T : Any?> invokeAny(
        tasks: MutableCollection<out Callable<T>>,
        timeout: Long,
        unit: TimeUnit
    ): T {
        return real.invokeAny(tasks, timeout, unit)
    }

    override fun prestartAllCoreThreads(): Int {
        return real.prestartAllCoreThreads()
    }

    override fun getCompletedTaskCount(): Long {
        return real.completedTaskCount
    }

    override fun getQueue(): BlockingQueue<Runnable> {
        return real.queue
    }

    override fun getPoolSize(): Int {
        return real.poolSize
    }

    override fun getRejectedExecutionHandler(): RejectedExecutionHandler {
        return real.rejectedExecutionHandler
    }

    override fun getTaskCount(): Long {
        return real.taskCount
    }

    override fun allowCoreThreadTimeOut(value: Boolean) {
        real.allowCoreThreadTimeOut(value)
    }

    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
        return real.awaitTermination(timeout, unit)
    }

    override fun getThreadFactory(): ThreadFactory {
        return real.threadFactory
    }

    override fun setRejectedExecutionHandler(handler: RejectedExecutionHandler?) {
        real.rejectedExecutionHandler = handler
    }

    override fun getLargestPoolSize(): Int {
        return real.largestPoolSize
    }

    override fun setThreadFactory(threadFactory: ThreadFactory?) {
        real.threadFactory = threadFactory
    }

    override fun setCorePoolSize(corePoolSize: Int) {
        real.corePoolSize = corePoolSize
    }

    override fun toString(): String {
        return real.toString()
    }

    override fun remove(task: Runnable?): Boolean {
        return real.remove(task)
    }

    override fun isTerminated(): Boolean {
        return real.isTerminated
    }

    override fun getKeepAliveTime(unit: TimeUnit?): Long {
        return real.getKeepAliveTime(unit)
    }

    override fun setKeepAliveTime(time: Long, unit: TimeUnit?) {
        real.setKeepAliveTime(time, unit)
    }

    override fun prestartCoreThread(): Boolean {
        return real.prestartCoreThread()
    }

    override fun purge() {
        real.purge()
    }

    override fun shutdown() {
        real.shutdown()
    }

    override fun shutdownNow(): MutableList<Runnable> {
        return real.shutdownNow()
    }

    override fun isShutdown(): Boolean {
        return real.isShutdown
    }

    override fun setMaximumPoolSize(maximumPoolSize: Int) {
        real.maximumPoolSize = maximumPoolSize
    }

    override fun getMaximumPoolSize(): Int {
        return real.maximumPoolSize
    }

    override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>): MutableList<Future<T>> {
        return real.invokeAll(tasks)
    }

    override fun <T : Any?> invokeAll(
        tasks: MutableCollection<out Callable<T>>,
        timeout: Long,
        unit: TimeUnit
    ): MutableList<Future<T>> {
        return real.invokeAll(tasks, timeout, unit)
    }

    override fun getActiveCount(): Int {
        return real.activeCount
    }

    override fun isTerminating(): Boolean {
        return real.isTerminating
    }

    override fun allowsCoreThreadTimeOut(): Boolean {
        return real.allowsCoreThreadTimeOut()
    }
}