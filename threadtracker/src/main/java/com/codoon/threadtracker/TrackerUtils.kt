package com.codoon.threadtracker

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Window
import android.view.WindowManager

object TrackerUtils {

    // 获取线程池名称时使用
    @JvmStatic
    fun toObjectString(any: Any): String {
        // 防止线程池名称出现TBase类
        var className = any.javaClass.name
        val simpleName = any.javaClass.simpleName
        if (simpleName == "TBaseThreadPoolExecutor") {
            className = any.javaClass.superclass?.name ?: "ThreadPoolExecutor"
        } else if (simpleName == "TBaseScheduledThreadPoolExecutor") {
            className = any.javaClass.superclass?.name ?: "ProxyScheduledThreadPoolExecutor"
        }
        return className + "@" + Integer.toHexString(any.hashCode())
    }

    @JvmStatic
    fun logStack() {
        Log.d(LOG_TAG, "logStack :\n")
        val stackElements =
            Throwable().stackTrace
        for (stackElement in stackElements) Log.d(
            LOG_TAG,
            "" + stackElement
        )
    }

    @JvmStatic
    fun getStackString(deleteProxy: Boolean = false): String {
        var str = ""
        val stackElements = Throwable().stackTrace
        var i = 0
        for (stackElement in stackElements) {
            i++
            if (deleteProxy && i <= 4 && stackElement.toString().contains("Proxy")) {
                // 前几行是动态代理的栈
                continue
            }
            if (stackElement.toString().contains("com.codoon.threadtracker")) {
                continue
            }
            str += stackElement.toString() + "\n"
        }
        return str
    }

    fun getThreadRunningStack(stacks: Array<StackTraceElement>): String {
        var str = ""
        for (stackElement in stacks) {
            if (stackElement.toString().contains("com.codoon.threadtracker"))
                continue
            str += stackElement.toString() + "\n"
        }
        return str
    }

    fun setStatusBarColor(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.argb(0xff, 0x00, 0xac, 0x61)
        }
    }
}

fun Number.toPx(context: Context) = toPxF(context).toInt()

fun Number.toPxF(context: Context) = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this.toFloat(),
    context.resources.displayMetrics
)

fun Number.toDp(context: Context) = toDpF(context).toInt()

fun Number.toDpF(context: Context) =
    this.toFloat() * DisplayMetrics.DENSITY_DEFAULT / context.resources.displayMetrics.densityDpi

@JvmField
val LOG_TAG = "ThreadTracker"