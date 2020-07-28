package com.codoon.threadtracker.proxy

import android.os.SystemClock
import android.util.Log
import com.codoon.threadtracker.LOG_TAG
import com.codoon.threadtracker.ThreadInfoManager
import com.codoon.threadtracker.TrackerUtils
import com.codoon.threadtracker.bean.ThreadInfo
import java.util.*

open class TBaseTimer : Timer {
    constructor() : super() {
        init()
    }

    constructor(isDaemon: Boolean) : super(isDaemon) {
        init()
    }

    constructor(name: String) : super(name) {
        init()
    }

    constructor(name: String, isDaemon: Boolean) : super(name, isDaemon) {
        init()
    }

    private fun init() {
        val callStack = TrackerUtils.getStackString()
        var hasProxy = false
        try {
            val fields = javaClass.superclass?.declaredFields
            fields?.forEach {
                it.isAccessible = true
                val any = it.get(this)
                if (any is Thread && any.isAlive) {
                    hasProxy = true
                    any.apply {
                        val newInfo = ThreadInfo()
                        newInfo.id = id
                        newInfo.name = name
                        newInfo.callStack = callStack
                        newInfo.callThreadId = Thread.currentThread().id
                        newInfo.state = state
                        newInfo.startTime = SystemClock.elapsedRealtime()
                        ThreadInfoManager.INSTANCE.putThreadInfo(id, newInfo)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "ProxyTimer err: ${e.message}")
        }
        if (!hasProxy) {
            Log.e(LOG_TAG, "ProxyTimer fail")
        }
    }
}