package com.codoon.threadtracker.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codoon.threadthracker.R
import com.codoon.threadtracker.bean.ShowInfo
import com.codoon.threadtracker.toPx

class TrackerAdapter(private var list: List<ShowInfo>, private val listener: OnItemClickListener) :
    RecyclerView.Adapter<TrackerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.threadtracker_thread_item, parent, false)
        view.setOnClickListener {
            listener.onItemClick(it)
        }
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun getItemList(): List<ShowInfo> {
        return list
    }

    fun setItemList(list: List<ShowInfo>) {
        this.list = list
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = list[position]
        when (data.type) {
            ShowInfo.SINGLE_THREAD -> {
                holder.threadLayout.setBackgroundColor(Color.argb(0x00, 0x00, 0xbc, 0x71))
                holder.threadName.text = data.threadName
                holder.threadState.text = data.threadState.name
                holder.threadName.setPadding(0, 0, 0, 0)
                holder.threadState.visibility = View.VISIBLE
            }
            ShowInfo.POOL -> {
                holder.threadLayout.setBackgroundColor(Color.argb(0x20, 0x00, 0xbc, 0x71))
                holder.threadName.text = data.poolName
                holder.threadState.visibility = View.GONE
                holder.threadName.setPadding(0, 0, 0, 0)
            }
            ShowInfo.POOL_THREAD -> {
                holder.threadLayout.setBackgroundColor(Color.argb(0x00, 0x00, 0xbc, 0x71))
                holder.threadName.text = data.threadName
                holder.threadState.text = data.threadState.name
                holder.threadName.apply {
                    setPadding(20.toPx(context), paddingTop, paddingRight, paddingBottom)
                }
                holder.threadState.visibility = View.VISIBLE
            }
        }
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val threadLayout: RelativeLayout = itemView.findViewById(R.id.threadLayout)
        val threadName: TextView = itemView.findViewById(R.id.threadName)
        val threadState: TextView = itemView.findViewById(R.id.threadState)
    }
}

interface OnItemClickListener {
    fun onItemClick(view: View)
}
