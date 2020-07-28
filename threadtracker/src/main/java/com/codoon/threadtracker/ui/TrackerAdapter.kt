package com.codoon.threadtracker.ui

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.RelativeLayout
import android.widget.TextView
import com.codoon.threadthracker.R
import com.codoon.threadtracker.bean.ShowInfo
import com.codoon.threadtracker.toPx


class TrackerAdapter(private var context: Context, private var list: List<ShowInfo>) :
    BaseAdapter() {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var holder: ViewHolder? = null
        var returnView = convertView

        returnView?.apply {
            holder = tag as ViewHolder
        } ?: apply {
            returnView =
                LayoutInflater.from(context).inflate(R.layout.threadtracker_thread_item, parent, false)?.also {
                    holder = ViewHolder().apply {
                        threadLayout = it.findViewById(R.id.threadLayout)
                        threadName = it.findViewById(R.id.threadName)
                        threadState = it.findViewById(R.id.threadState)
                    }
                    it.tag = holder
                }
        }
        holder?.also { showItem(it, position) }
        return returnView!!
    }

    private fun showItem(holder: ViewHolder, position: Int) {
        val data = list[position];
        when (data.type) {
            ShowInfo.SINGLE_THREAD -> {
                holder.threadLayout?.setBackgroundColor(Color.argb(0x00, 0x00, 0xbc, 0x71))
                holder.threadName?.text = data.threadName
                holder.threadState?.text = data.threadState.name
                holder.threadName?.setPadding(0, 0, 0, 0)
                holder.threadState?.visibility = View.VISIBLE
            }
            ShowInfo.POOL -> {
                holder.threadLayout?.setBackgroundColor(Color.argb(0x20, 0x00, 0xbc, 0x71))
                holder.threadName?.text = data.poolName
                holder.threadState?.visibility = View.GONE
                holder.threadName?.setPadding(0, 0, 0, 0)
            }
            ShowInfo.POOL_THREAD -> {
                holder.threadLayout?.setBackgroundColor(Color.argb(0x00, 0x00, 0xbc, 0x71))
                holder.threadName?.text = data.threadName
                holder.threadState?.text = data.threadState.name
                holder.threadName?.apply {
                    setPadding(20.toPx(context), paddingTop, paddingRight, paddingBottom)
                }
                holder.threadState?.visibility = View.VISIBLE
            }
        }
    }

    override fun getItem(position: Int): Any {
        return list[position]
    }

    fun getItemList(): List<ShowInfo> {
        return list
    }

    fun setItemList(list: List<ShowInfo>) {
        this.list = list
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return list.size
    }

    internal class ViewHolder {
        var threadLayout: RelativeLayout? = null
        var threadName: TextView? = null
        var threadState: TextView? = null
    }
}

