package com.codoon.threaddemo.test

import android.os.AsyncTask

class MyAsyncTask : AsyncTask<String?, Void?, String?>() {
    override fun doInBackground(vararg params: String?): String? {
        synchronized(MyAsyncTask::class.java) {
            Thread.sleep(1000000)
        }
        return ""
    }
}
