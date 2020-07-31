## ThreadTracker
Android 线程追踪工具

## 开发背景
随着项目规模越来越大，线程数量暴涨问题很难避免。在 Android 中每个线程都在自己独立的栈中运行，我们虽可以用 Thread.getAllStackTraces() 获取到运行栈信息，但却无法得知线程被启动原因。当做性能优化打开 Profiler 工具时，可能瞬间映入眼帘几百个线程，甚至很多都是无名线程，以致于想去优化，都无从下手。

ThreadTracker 致力于解决这样的问题；它可以方便的查看当前时刻线程**被启动堆栈**、线程池**被创建堆栈**、线程池中的某线程当前正在执行的**task添加栈**（对于线程池来说，其中线程被启动的堆栈并不重要，这取决于线程池策略，重要的是线程正在执行的任务是在哪里被添加的）、以及线程-线程池之间的树状关系图，并**自动高亮**堆栈中用户代码，让您一眼看出问题根源。

## 效果演示
![主界面.jpg](https://upload-images.jianshu.io/upload_images/24284143-f057ebed55bcf008.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
![详情.jpg](https://upload-images.jianshu.io/upload_images/24284143-a9624ebbcb6bccfa.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

另外，当一个线程/线程池是被另一个子线程启动/创建，可以通过点击 show detail 追溯调用链，直到到达主线程为止。

\* 注：如果您需要追踪 ASyncTask 相关信息，请在 Android 5.0 及以上手机运行，4.x 系统上对 ASyncTask 的 Hook 暂无法成功。

## 实现原理
https://juejin.im/post/6855586076132655118/

## 使用方式

首先需要 jcenter:

    repositories {
        jcenter()
    }
    
然后在您的 project gradle 的 dependencies 中添加：

    classpath "com.codoon.threadtracker:threadtracker-plugin:1.1.0"
        
在您的 application gradle 中添加：

    apply plugin: 'com.codoon.threadtracker'

    debugImplementation 'com.codoon.threadtracker:threadtracker:1.1.0'   

打包后会多生成一个 TreadTracker 图标，打开即可使用。

\* 如果您的项目尚不支持 androidx，可把以上 version 全部改成 1.0.0；版本 1.1.0 及以后均需 androidx 支持。

## Coming soon...
* 统计三方 jar 包线程状态，看是谁在为所欲为
* 按线程状态/类型筛选，按运行时间排序
* 记录/恢复当前线程信息快照

## 项目成员
#### 发起者 / 负责人
[zhanyongsheng](https://github.com/zhanyongsheng)

## 协议
<img alt="Apache-2.0 license" src="https://www.apache.org/img/ASF20thAnniversary.jpg" width="128">
ThreadTracker 基于 Apache-2.0 协议进行分发和使用。
