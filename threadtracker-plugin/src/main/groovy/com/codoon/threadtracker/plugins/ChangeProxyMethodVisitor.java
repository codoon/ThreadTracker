package com.codoon.threadtracker.plugins;


import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static com.codoon.threadtracker.plugins.ClassConstant.S_Executors;
import static com.codoon.threadtracker.plugins.ClassConstant.S_HandlerThread;
import static com.codoon.threadtracker.plugins.ClassConstant.S_ProxyExecutors;
import static com.codoon.threadtracker.plugins.ClassConstant.S_ScheduledThreadPoolExecutor;
import static com.codoon.threadtracker.plugins.ClassConstant.S_TBaseHandlerThread;
import static com.codoon.threadtracker.plugins.ClassConstant.S_TBaseScheduledThreadPoolExecutor;
import static com.codoon.threadtracker.plugins.ClassConstant.S_TBaseThread;
import static com.codoon.threadtracker.plugins.ClassConstant.S_TBaseThreadPoolExecutor;
import static com.codoon.threadtracker.plugins.ClassConstant.S_TBaseTimer;
import static com.codoon.threadtracker.plugins.ClassConstant.S_Thread;
import static com.codoon.threadtracker.plugins.ClassConstant.S_ThreadPoolExecutor;
import static com.codoon.threadtracker.plugins.ClassConstant.S_Timer;
import static com.codoon.threadtracker.plugins.PluginUtils.log;


public class ChangeProxyMethodVisitor extends MethodVisitor {
    private String className;

    ChangeProxyMethodVisitor(int api, MethodVisitor methodVisitor, String className) {
        super(api, methodVisitor);
        this.className = className;
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if (opcode == Opcodes.NEW) {
            switch (type) {
                case S_Thread:
                    log("new Thread " + className);
                    mv.visitTypeInsn(Opcodes.NEW, S_TBaseThread);
                    return;
                case S_ThreadPoolExecutor:
                    log("new ThreadPoolExecutor " + className);
                    mv.visitTypeInsn(Opcodes.NEW, S_TBaseThreadPoolExecutor);
                    return;
                case S_ScheduledThreadPoolExecutor:
                    log("new ScheduledThreadPoolExecutor " + className);
                    mv.visitTypeInsn(Opcodes.NEW, S_TBaseScheduledThreadPoolExecutor);
                    return;
                case S_Timer:
                    log("new Timer " + className);
                    mv.visitTypeInsn(Opcodes.NEW, S_TBaseTimer);
                    return;
                case S_HandlerThread:
                    log("new HandlerThread " + className);
                    mv.visitTypeInsn(Opcodes.NEW, S_TBaseHandlerThread);
                    return;
            }
        }
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        if (owner.equals(S_Thread) && name.equalsIgnoreCase("<init>")) {
            mv.visitMethodInsn(opcode, S_TBaseThread, name, descriptor, false);
            return;
        } else if (owner.equals(S_ThreadPoolExecutor) && name.equalsIgnoreCase("<init>")) {
            mv.visitMethodInsn(opcode, S_TBaseThreadPoolExecutor, name, descriptor, false);
            return;
        } else if (owner.equals(S_ScheduledThreadPoolExecutor) && name.equalsIgnoreCase("<init>")) {
            mv.visitMethodInsn(opcode, S_TBaseScheduledThreadPoolExecutor, name, descriptor, false);
            return;
        } else if (owner.equals(S_Timer) && name.equalsIgnoreCase("<init>")) {
            mv.visitMethodInsn(opcode, S_TBaseTimer, name, descriptor, false);
            return;
        } else if (owner.equals(S_HandlerThread) && name.equalsIgnoreCase("<init>")) {
            mv.visitMethodInsn(opcode, S_TBaseHandlerThread, name, descriptor, false);
            return;
        } else if (opcode == Opcodes.INVOKESTATIC && owner.equals(S_Executors)) {

            if ((name.equals("newFixedThreadPool") && descriptor.equalsIgnoreCase("(I)Ljava/util/concurrent/ExecutorService;"))

                    || (name.equals("newWorkStealingPool") && descriptor.equalsIgnoreCase("(I)Ljava/util/concurrent/ExecutorService;"))

                    || (name.equals("newWorkStealingPool") && descriptor.equalsIgnoreCase("()Ljava/util/concurrent/ExecutorService;"))

                    || (name.equals("newFixedThreadPool") && descriptor.equalsIgnoreCase("(ILjava/util/concurrent/ThreadFactory;)Ljava/util/concurrent/ExecutorService;"))

                    || (name.equals("newSingleThreadExecutor") && descriptor.equalsIgnoreCase("()Ljava/util/concurrent/ExecutorService;"))

                    || (name.equals("newSingleThreadExecutor") && descriptor.equalsIgnoreCase("(Ljava/util/concurrent/ThreadFactory;)Ljava/util/concurrent/ExecutorService;"))

                    || (name.equals("newCachedThreadPool") && descriptor.equalsIgnoreCase("()Ljava/util/concurrent/ExecutorService;"))

                    || (name.equals("newCachedThreadPool") && descriptor.equalsIgnoreCase("(Ljava/util/concurrent/ThreadFactory;)Ljava/util/concurrent/ExecutorService;"))

                    || (name.equals("newSingleThreadScheduledExecutor") && descriptor.equalsIgnoreCase("()Ljava/util/concurrent/ScheduledExecutorService;"))

                    || (name.equals("newSingleThreadScheduledExecutor") && descriptor.equalsIgnoreCase("(Ljava/util/concurrent/ThreadFactory;)Ljava/util/concurrent/ScheduledExecutorService;"))

                    || (name.equals("newScheduledThreadPool") && descriptor.equalsIgnoreCase("(I)Ljava/util/concurrent/ScheduledExecutorService;"))

                    || (name.equals("newScheduledThreadPool") && descriptor.equalsIgnoreCase("(ILjava/util/concurrent/ThreadFactory;)Ljava/util/concurrent/ScheduledExecutorService;"))

                    || (name.equals("unconfigurableExecutorService") && descriptor.equalsIgnoreCase("(Ljava/util/concurrent/ExecutorService;)Ljava/util/concurrent/ExecutorService;"))

                    || (name.equals("unconfigurableScheduledExecutorService") && descriptor.equalsIgnoreCase("(Ljava/util/concurrent/ScheduledExecutorService;)Ljava/util/concurrent/ScheduledExecutorService;"))

            ) {
                log("Executors.xxx : " + className + " " + name + " " + descriptor);
                mv.visitMethodInsn(opcode, S_ProxyExecutors, name, descriptor, false);
                return;
            }
        }

        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }
}
