package com.codoon.threaddemo

import android.util.Log

object CommonUtil {
    fun printAllThread() {
        val threadMap = Thread.getAllStackTraces()
        Log.e("ThreadDebug", "all start==============================================")
        for ((thread, stackElements) in threadMap) {
            Log.e(
                "ThreadDebug",
                "name:${thread.name} id:${thread.id} priority:${thread.priority} state:${thread.state}"
            )
            for (i in stackElements.indices) {
                val stringBuilder = StringBuilder("    ")
                stringBuilder.append(stackElements[i].className + ".")
                    .append(stackElements[i].methodName + "(")
                    .append(stackElements[i].fileName + ":")
                    .append(stackElements[i].lineNumber.toString() + ")")
                Log.e("ThreadDebug", stringBuilder.toString())
            }

            Log.e("ThreadDebug", "\n\n-")
        }
        Log.e("ThreadDebug", "all end==============================================")
    }
}