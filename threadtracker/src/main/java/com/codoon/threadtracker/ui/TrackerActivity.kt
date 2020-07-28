package com.codoon.threadtracker.ui

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import com.codoon.threadthracker.R
import com.codoon.threadtracker.ThreadInfoManager
import com.codoon.threadtracker.TrackerUtils.setStatusBarColor
import kotlinx.android.synthetic.main.threadtracker_activity_tracker.*

/**
 * 线程/线程池列表
 */
class TrackerActivity : Activity() {

    private val refreshHandlerThread = HandlerThread("ThreadTracker-Refresh").apply {
        start()
    }
    private val refreshHandler = object : Handler(refreshHandlerThread.looper) {
        override fun handleMessage(msg: Message) {
            refreshList(msg.what == 1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.threadtracker_activity_tracker)
        setStatusBarColor(window)

        refreshBtn.setOnClickListener {
            refreshBtn.visibility = View.GONE
            refreshProgress.visibility = View.VISIBLE
            refreshHandler.sendEmptyMessage(1)
        }
        val adapter = TrackerAdapter(this, emptyList())
        listView.adapter = adapter
        listView.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                ThreadDetailsActivity.startDetailsActivity(
                    this@TrackerActivity,
                    (listView.adapter as TrackerAdapter).getItemList()[position]
                )
            }
        }

        refreshHandler.sendEmptyMessage(0)
    }

    private fun refreshList(toast: Boolean) {
        val infoResult = ThreadInfoManager.INSTANCE.buildAllThreadInfo()
        runOnUiThread {
            (listView.adapter as TrackerAdapter).setItemList(infoResult.list)
            refreshBtn.visibility = View.VISIBLE
            refreshProgress.visibility = View.GONE
            if (toast) {
                Toast.makeText(
                    this,
                    "total: ${infoResult.totalNum}  system/unknown: ${infoResult.unknownNum}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroy() {
        refreshHandler.removeCallbacksAndMessages(null)
        refreshHandlerThread.quit()
        super.onDestroy()
    }
}