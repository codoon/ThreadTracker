package com.codoon.threadtracker.proxy;

import com.codoon.threadtracker.ThreadInfoManager;
import com.codoon.threadtracker.TrackerUtils;
import com.codoon.threadtracker.bean.ThreadPoolInfo;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * 对ExecutorService、ScheduledExecutorService接口进行代理
 * 不使用kotlin是因为以下方法第二个变长参数，如果java传null方法接收到的也是null，
 * 但如果kotlin传null，方法接收后参数会变成["null"]，即有一个字符串的数组，对于不需要参数的方法来说就会报错
 * {@link java.lang.reflect.Method#invoke(java.lang.Object, java.lang.Object...)}
 */
public class ProxyExecutorService implements InvocationHandler {
    private ExecutorService executor;
    private String poolName = null;

    ProxyExecutorService(ExecutorService executor) {
        this.executor = executor;
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

        // 因方法数众多，并且被代理的各类中方法也不一致
        // 所以被调用方法中只要含有Runnable、Callable类型的参数，都替换成PoolRunnableAndOther代理
        if (args != null) {
            String callStack = TrackerUtils.getStackString(true);
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if ((arg instanceof Runnable || arg instanceof Callable) && !(arg instanceof PoolRunnableAndOther)) {
                    PoolRunnableAndOther any = new PoolRunnableAndOther(arg, callStack, poolName);
                    args[i] = any;
                } else if (arg instanceof Collection && !((Collection) arg).isEmpty()) {
                    // invokeAny invokeAll 等情况
                    Iterator iter = ((Collection) arg).iterator();
                    ArrayList<PoolRunnableAndOther> taskList = new ArrayList<>();
                    boolean allOk = iter.hasNext();
                    while (iter.hasNext()) {
                        Object it = iter.next();
                        if (it instanceof Runnable || it instanceof Callable) {
                            if (it instanceof PoolRunnableAndOther) {
                                taskList.add((PoolRunnableAndOther) it);
                            } else {
                                taskList.add(new PoolRunnableAndOther(it, callStack, poolName));
                            }
                        } else {
                            allOk = false;
                            break;
                        }
                    }
                    if (allOk) {
                        args[i] = taskList;
                    }
                }
            }
        }

        if (method.getName().equals("shutdown") || method.getName().equals("shutdownNow")) {
            ThreadInfoManager.getINSTANCE().shutDownPool(poolName);
        }
        return method.invoke(executor, args);
    }
}