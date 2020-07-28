package com.codoon.threaddemo.test

class MyRunnable : Runnable {
    override fun run() {
        Thread.sleep(10000000)
    }
}