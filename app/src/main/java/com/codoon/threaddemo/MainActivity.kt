package com.codoon.threaddemo

import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import androidx.appcompat.app.AppCompatActivity
import com.codoon.threaddemo.test.MyAsyncTask
import com.codoon.threaddemo.test.MyRunnable
import com.codoon.threaddemo.test.MyThread
import com.codoon.threadlib.MyJar
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import java.util.concurrent.*

/**
 * 使用各种方式创建各种线程、线程池、以及线程中嵌套启动新线程，用来查看线程溯源的效果。
 */
class MainActivity : AppCompatActivity() {
    private val wait0 = Object()

    // rx线程
    private val observable = Observable.just("").map {
        Thread.sleep(5000)
    }.subscribeOn(Schedulers.io())

    // 自定义线程池
    private val threadPool = MyThreadPool(
        3,
        3,
        960,
        TimeUnit.SECONDS,
        LinkedBlockingQueue(2)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // 外层执行完后即销毁，里层查看调用栈时能查到外层threadId
        Thread {
            Thread {
                Thread.sleep(10000000)
            }.start()
        }.start()


        // 按钮触发的各种线程
        button.setOnClickListener {
            button.isEnabled = false

            // 多重嵌套
            observable.observeOn(Schedulers.computation()).subscribe {
                // 自定义线程
                val myThread = MyThread(Runnable {
                    startDiffTimeThread()
                    alwaysRunning()
                })
                myThread.name = "MY 666 THREAD"
                myThread.start()

                Thread.sleep(500000000)
            }

            // threadPool以两种不同方式添加任务
            threadPool.execute(MyRunnable())
            threadPool.submit(Callable<String> {
                startAsync()
                synchronized(wait0) {
                    wait0.wait()
                }
                ""
            })
        }

        // 调用系统函数打印出当前所有线程以及运行中堆栈
        // rootLayout.postDelayed({
        //     CommonUtil.printAllThread()
        // }, 5000)
    }

    // 主要用来测试刷新功能
    private fun startDiffTimeThread() {
        for (i in 0..6) {
            Thread {
                Thread.sleep((i + 5) * 1000L)
            }.apply {
                name = "SLEEP-$i"
                start()
            }
        }
    }

    // 各种方式使用AsyncTask
    private fun startAsync() {

        AsyncTask.execute {
            Thread.sleep(30000)
        }
        AsyncTask.THREAD_POOL_EXECUTOR.execute {
            alwaysRunning()
        }
        val myAsyncTask1 = MyAsyncTask()
        myAsyncTask1.execute("1")
        val myAsyncTask2 = MyAsyncTask()
        myAsyncTask2.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "2")
        val myAsyncTask3 = MyAsyncTask()
        myAsyncTask3.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "3")
        val myAsyncTask4 = MyAsyncTask()
        myAsyncTask4.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "4")
        val myAsyncTask5 = MyAsyncTask()
        myAsyncTask5.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "5")

        // 类型强转，不应引起crash
        val tf = (AsyncTask.THREAD_POOL_EXECUTOR as ThreadPoolExecutor).threadFactory
    }

    private fun alwaysRunning() {
        var i = 0
        while (true) {
            i++
            i--
        }
    }

    // 其他生命周期中启动线程

    override fun onResume() {
        super.onResume()

        val jarThread = MyJar()
        jarThread.startJarThread()
    }

    override fun onStart() {
        super.onStart()

        Thread {
            // 测试HandlerThread和Timer追踪情况
            val pool = Executors.newFixedThreadPool(5)
            pool.execute {
                val ht = HandlerThread("My HandlerThread")
                ht.start()
                val h = Handler(ht.looper)
                h.sendEmptyMessageDelayed(123, 3333333)
                Thread.sleep(10000000)
            }
            pool.execute {
                Thread.sleep(10000000)
            }
            pool.execute {
                val timer = Timer()
                timer.schedule(object : TimerTask() {
                    override fun run() {

                    }
                }, 0, 1000)
                Thread.sleep(10)
            }

            Thread.sleep(10000000)
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        System.exit(0)
    }


    // 自定义线程池
    private inner class MyThreadPool(
        corePoolSize: Int,
        maximumPoolSize: Int,
        keepAliveTime: Long,
        unit: TimeUnit?,
        workQueue: BlockingQueue<Runnable>?
    ) : ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue) {
    }
}
