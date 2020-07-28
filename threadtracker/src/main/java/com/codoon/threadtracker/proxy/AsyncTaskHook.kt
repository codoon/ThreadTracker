package com.codoon.threadtracker.proxy

import android.os.AsyncTask
import android.os.Build
import android.util.Log
import com.codoon.threadtracker.LOG_TAG
import com.codoon.threadtracker.TrackerUtils.toObjectString
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadPoolExecutor

/**
 * 主要是替换AsyncTask中的THREAD_POOL_EXECUTOR和SERIAL_EXECUTOR(sDefaultExecutor)
 * SERIAL_EXECUTOR用来记录调用栈，THREAD_POOL_EXECUTOR用来获取线程池信息并且和线程建立联系
 * sBackupExecutor暂未考虑
 *
 * android5.0以下hook会失败，因为暂时未找到针对android5.0以下的通过反射修改final字段的方法
 */
object AsyncTaskHook {

    fun hook() {
        try {
            val t = AsyncTask.THREAD_POOL_EXECUTOR
            val s = AsyncTask.SERIAL_EXECUTOR
            val fieldT = AsyncTask::class.java.getField("THREAD_POOL_EXECUTOR")
            val fieldS = AsyncTask::class.java.getField("SERIAL_EXECUTOR")
            val fieldsDE = AsyncTask::class.java.getDeclaredField("sDefaultExecutor")
            fieldsDE.isAccessible = true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.M
            ) {
                val artField = Field::class.java.getDeclaredField("artField")
                artField.isAccessible = true
                val artT = artField.get(fieldT)
                val artS = artField.get(fieldS)

                val modifiersField =
                    Class.forName("java.lang.reflect.ArtField").getDeclaredField("accessFlags")
                modifiersField.isAccessible = true
                modifiersField.setInt(artT, fieldT.modifiers and Modifier.FINAL.inv())
                modifiersField.setInt(artS, fieldS.modifiers and Modifier.FINAL.inv())
            } else {

                // java使用"modifiers"，但android中Field类已被改变
                // Field modifiersField = Field.class.getDeclaredField("modifiers");
                val modifiersField = Field::class.java.getDeclaredField("accessFlags")
                modifiersField.isAccessible = true
                modifiersField.setInt(fieldT, fieldT.modifiers and Modifier.FINAL.inv())
                modifiersField.setInt(fieldS, fieldS.modifiers and Modifier.FINAL.inv())
            }
            fieldT[null] = proxyT(t)
            val sExecutor = proxyS(s)
            fieldS[null] = sExecutor
            fieldsDE[null] = sExecutor
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(LOG_TAG, "AsyncTaskHook fail ${e.message}")
        }
    }

    private fun proxyT(t: Executor): Executor {
        if (t is ThreadPoolExecutor) {
            t.threadFactory = TBaseThreadFactory(t.threadFactory, toObjectString(t))
            // 防止上层强转为ThreadPoolExecutor
            return ProxyThreadPoolExecutor(t)
        } else { // 万一不是ThreadPoolExecutor则采用通用方式
            val handler =
                ProxyAsyncTaskExecutor(t, ProxyAsyncTaskExecutor.TYPE_THREAD_POOL_EXECUTOR)
            return Proxy.newProxyInstance(
                t.javaClass.classLoader,
                ExecutorService::class.java.interfaces,
                handler
            ) as Executor
        }
    }

    private fun proxyS(s: Executor): Executor {
        val handler = ProxyAsyncTaskExecutor(s, ProxyAsyncTaskExecutor.TYPE_SERIAL_EXECUTOR)
        return Proxy.newProxyInstance(
            s.javaClass.classLoader,
            ExecutorService::class.java.interfaces,
            handler
        ) as Executor
    }
}