package com.codoon.threadtracker.bean

data class ThreadInfoResult(
    var list: List<ShowInfo> = emptyList(),
    var totalNum: Int = 0, // 总线程数
    var unknownNum: Int = 0 // 未知调用栈的线程数
)