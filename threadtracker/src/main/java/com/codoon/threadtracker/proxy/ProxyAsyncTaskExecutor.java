package com.codoon.threadtracker.proxy;

import com.codoon.threadtracker.ThreadInfoManager;
import com.codoon.threadtracker.TrackerUtils;
import com.codoon.threadtracker.bean.ThreadPoolInfo;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * 动态代理AsyncTask中THREAD_POOL_EXECUTOR、SERIAL_EXECUTOR
 * <p>
 * AsyncTask中sDefaultExecutor并不是真正的线程池，
 * 任务是由sDefaultExecutor通过队列提交给THREAD_POOL_EXECUTOR
 * 所以poolName应该是THREAD_POOL_EXECUTOR的，但stack应该是sDefaultExecutor的
 * 于是sDefaultExecutor在execute时包装成poolRunnable转入stack，但poolName为空
 * 修改THREAD_POOL_EXECUTOR的threadFactory，使线程和poolName建立关系
 * 最后PoolRunnableAndOther把空的poolName更新成THREAD_POOL_EXECUTOR的poolName
 * <p>
 * 使用java避免kotlin调用method.invoke args传null引起的问题
 */
public class ProxyAsyncTaskExecutor implements InvocationHandler {
    final static int TYPE_THREAD_POOL_EXECUTOR = 0;
    final static int TYPE_SERIAL_EXECUTOR = 1;

    private Executor executor;
    private String poolName;
    private int type;

    ProxyAsyncTaskExecutor(Executor executor, int type) {
        this.executor = executor;
        this.type = type;
        poolName = TrackerUtils.toObjectString(executor);

        String createStack = TrackerUtils.getStackString(false);
        ThreadPoolInfo poolInfo = new ThreadPoolInfo();
        poolInfo.setPoolName(poolName);
        poolInfo.setCreateStack(createStack);
        poolInfo.setCreateThreadId(Thread.currentThread().getId());
        ThreadInfoManager.getINSTANCE().putThreadPoolInfo(poolName, poolInfo);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Log.d(LOG_TAG, "ProxyAsyncTaskExecutor type: " + type + " method:" + method.getName());

        if (method.getName().equals("execute") && args != null && args.length == 1 && args[0] instanceof Runnable) {
            String callStack = TrackerUtils.getStackString(true);
            if (type == TYPE_THREAD_POOL_EXECUTOR) {
                // 处理外部直接使用AsyncTask.THREAD_POOL_EXECUTOR.execute的情况，
                // 因AsyncTask中sDefaultExecutor把PoolRunnableAndOther又包装成Runnable提交到THREAD_POOL_EXECUTOR
                // 所以这里根据调用栈区分，如果是外部直接execute则包装成PoolRunnableAndOther，否则什么都不做
                // Log.d(LOG_TAG,"ProxyAsyncTaskExecutor callStack: "+ callStack);
                //     android.os.AsyncTask$SerialExecutor.scheduleNext(AsyncTask.java:258)
                //     android.os.AsyncTask$SerialExecutor.execute(AsyncTask.java:252)
                if (!callStack.contains("AsyncTask$SerialExecutor.scheduleNext")) {
                    Runnable runnable = new PoolRunnableAndOther(args[0], callStack, poolName);
                    args[0] = runnable;
                }
            } else if (type == TYPE_SERIAL_EXECUTOR) {
                // 需要callStack，不需要poolName，因为SERIAL_EXECUTOR并不是一个真正的线程池
                if (!(args[0] instanceof PoolRunnableAndOther)) {
                    Runnable runnable = new PoolRunnableAndOther(args[0], callStack, null);
                    args[0] = runnable;
                }
            }
        }
        return method.invoke(executor, args);
    }
}
