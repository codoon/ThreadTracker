package com.codoon.threadtracker

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.codoon.threadtracker.proxy.AsyncTaskHook

class ThreadTrackerInitializer : ContentProvider() {
    override fun onCreate(): Boolean {
        AsyncTaskHook.hook()
        Log.d(LOG_TAG, "ThreadTracker Initialize")
        UserPackage.buildPackageList()
        UserPackage.getPackageList().removeAt(0)
        val list = UserPackage.getPackageList()
        Log.d(LOG_TAG, "package list:")
        list.forEach {
            Log.d(LOG_TAG, it)
        }
        return true
    }

    override fun insert(p0: Uri, p1: ContentValues?): Uri? {
        return null
    }

    override fun query(
        p0: Uri,
        p1: Array<out String>?,
        p2: String?,
        p3: Array<out String>?,
        p4: String?
    ): Cursor? {
        return null
    }


    override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int {
        return 0
    }

    override fun delete(p0: Uri, p1: String?, p2: Array<out String>?): Int {
        return 0
    }

    override fun getType(p0: Uri): String? {
        return null
    }
}