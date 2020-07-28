package com.codoon.threadtracker

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import com.codoon.threadtracker.proxy.AsyncTaskHook

class ThreadTrackerInitializer : Initializer<Any> {
    override fun create(context: Context): Any {
        AsyncTaskHook.hook()
        Log.d(LOG_TAG, "ThreadTracker Initialize")
        UserPackage.buildPackageList()
        UserPackage.getPackageList().removeAt(0)
        val list = UserPackage.getPackageList()
        Log.d(LOG_TAG, "package list:")
        list.forEach {
            Log.d(LOG_TAG, it)
        }
        return Unit
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf()
    }
}