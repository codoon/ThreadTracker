package com.codoon.threadtracker.proxy

import android.os.Build
import androidx.annotation.RequiresApi
import com.codoon.threadtracker.TrackerUtils.toObjectString
import java.lang.reflect.Proxy
import java.util.concurrent.*

/**
 * 针对Executors.xxx创建线程池的方式进行字节码替换，替换为ProxyExecutors.xxx
 * 注意要加 @JvmStatic 以便字节码替换成ProxyExecutors后调用正常（否则java层需要ProxyExecutors.INSTANCE.newXXX 的调用方式）
 */
object ProxyExecutors {

    @JvmStatic
    fun newFixedThreadPool(nThreads: Int): ExecutorService {
        // 这里使用TBaseThreadPoolExecutor主要是为了避免上层代码把ExecutorService转型成ThreadPoolExecutor的问题，如果使用proxy方法动态代理，上层这么做会crash
        // 本类中其他不使用动态代理的原因也类似
        return TBaseThreadPoolExecutor(
            nThreads, nThreads,
            0L, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue()
        )
        // return proxy(Executors.newFixedThreadPool(nThreads))
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @JvmStatic
    fun newWorkStealingPool(parallelism: Int): ExecutorService {
        // todo ForkJoinPool 暂时没有TBaseForkJoinPool
        return proxy(Executors.newWorkStealingPool(parallelism))
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @JvmStatic
    fun newWorkStealingPool(): ExecutorService {
        return proxy(Executors.newWorkStealingPool())
    }

    @JvmStatic
    fun newFixedThreadPool(nThreads: Int, threadFactory: ThreadFactory?): ExecutorService {
        return TBaseThreadPoolExecutor(
            nThreads, nThreads,
            0L, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(),
            threadFactory
        )
    }

    @JvmStatic
    fun newSingleThreadExecutor(): ExecutorService {
        return proxy(Executors.newSingleThreadExecutor())
    }

    @JvmStatic
    fun newSingleThreadExecutor(threadFactory: ThreadFactory?): ExecutorService {
        return proxy(Executors.newSingleThreadExecutor(threadFactory))
    }

    @JvmStatic
    fun newCachedThreadPool(): ExecutorService {
        return TBaseThreadPoolExecutor(
            0, Int.MAX_VALUE,
            60L, TimeUnit.SECONDS,
            SynchronousQueue()
        )
    }

    @JvmStatic
    fun newCachedThreadPool(threadFactory: ThreadFactory?): ExecutorService {
        return TBaseThreadPoolExecutor(
            0, Int.MAX_VALUE,
            60L, TimeUnit.SECONDS,
            SynchronousQueue(),
            threadFactory
        )
    }

    @JvmStatic
    fun newSingleThreadScheduledExecutor(): ScheduledExecutorService {
        return proxy(Executors.newSingleThreadScheduledExecutor())
    }

    @JvmStatic
    fun newSingleThreadScheduledExecutor(threadFactory: ThreadFactory?): ScheduledExecutorService {
        return proxy(Executors.newSingleThreadScheduledExecutor(threadFactory))
    }

    @JvmStatic
    fun newScheduledThreadPool(corePoolSize: Int): ScheduledExecutorService {
        return TBaseScheduledThreadPoolExecutor(corePoolSize)
    }

    @JvmStatic
    fun newScheduledThreadPool(
        corePoolSize: Int,
        threadFactory: ThreadFactory?
    ): ScheduledExecutorService {
        return TBaseScheduledThreadPoolExecutor(corePoolSize, threadFactory)
    }

    @JvmStatic
    fun unconfigurableExecutorService(executor: ExecutorService): ExecutorService {
        return proxy(Executors.unconfigurableExecutorService(executor))
    }

    @JvmStatic
    fun unconfigurableScheduledExecutorService(executor: ScheduledExecutorService): ScheduledExecutorService {
        return proxy(Executors.unconfigurableScheduledExecutorService(executor))
    }

    private fun proxy(executorService: ExecutorService): ExecutorService {
        if (executorService is ThreadPoolExecutor) {
            // 这里和TBaseThreadPoolExecutor一样，设置ThreadFactory为了尽早获取线程信息和线程池建立联系，而不用等到run时
            executorService.threadFactory = TBaseThreadFactory(
                executorService.threadFactory,
                toObjectString(executorService)
            )
        }
        val handler = ProxyExecutorService(executorService)
        return Proxy.newProxyInstance(
            executorService.javaClass.classLoader,
            AbstractExecutorService::class.java.interfaces,
            handler
        ) as ExecutorService
    }

    private fun proxy(executorService: ScheduledExecutorService): ScheduledExecutorService {
        if (executorService is ScheduledThreadPoolExecutor) {
            executorService.threadFactory = TBaseThreadFactory(
                executorService.threadFactory,
                toObjectString(executorService)
            )
        }
        val handler = ProxyExecutorService(executorService)
        return Proxy.newProxyInstance(
            executorService.javaClass.classLoader,
            ScheduledThreadPoolExecutor::class.java.interfaces,
            handler
        ) as ScheduledExecutorService
    }
}
