package com.codoon.threadtracker.plugins;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static com.codoon.threadtracker.plugins.ClassConstant.S_HandlerThread;
import static com.codoon.threadtracker.plugins.ClassConstant.S_ScheduledThreadPoolExecutor;
import static com.codoon.threadtracker.plugins.ClassConstant.S_TBaseHandlerThread;
import static com.codoon.threadtracker.plugins.ClassConstant.S_TBaseScheduledThreadPoolExecutor;
import static com.codoon.threadtracker.plugins.ClassConstant.S_TBaseThread;
import static com.codoon.threadtracker.plugins.ClassConstant.S_TBaseThreadPoolExecutor;
import static com.codoon.threadtracker.plugins.ClassConstant.S_TBaseTimer;
import static com.codoon.threadtracker.plugins.ClassConstant.S_Thread;
import static com.codoon.threadtracker.plugins.ClassConstant.S_ThreadPoolExecutor;
import static com.codoon.threadtracker.plugins.ClassConstant.S_Timer;

class ThreadTrackerClassVisitor extends ClassVisitor implements Opcodes {

    private String className;
    private boolean changingSuper = false; // 是否处于改继承状态
    private boolean buildingPackage = false; // 是否处于建立用户可达代码包列表中
    private String jarName = null; // 不是jar包则为空

    public ThreadTrackerClassVisitor(ClassVisitor cv, String jarName) {
        super(Opcodes.ASM5, cv);
        this.jarName = jarName;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        changingSuper = false;
        buildingPackage = false;
        className = name;

        if (filterClass(className)) {
            super.visit(version, access, name, signature, superName, interfaces);
            return;
        }

        // 用来获取用户可达代码的包名列表，用于调用栈内高亮显示
        if (jarName != null && className.contains("com/codoon/threadtracker")) {
            if (className.equals("com/codoon/threadtracker/UserPackage")) {
                buildingPackage = true;
            }
        } else {
            if (jarName != null) {
                // 如果项目分module，此时其他module代码可能已经被打成jar
                if (PluginUtils.inProjectList(jarName)) {
                    PluginUtils.addClassPath(className);
                }
            } else {
                PluginUtils.addClassPath(className);
            }
        }

        if (!buildingPackage && (access & Opcodes.ACC_SUPER) > 0) {
            switch (superName) {
                case S_Thread:
                    changingSuper = true;
                    super.visit(version, access, name, signature, S_TBaseThread, interfaces);
                    return;
                case S_ThreadPoolExecutor:
                    changingSuper = true;
                    super.visit(version, access, name, signature, S_TBaseThreadPoolExecutor, interfaces);
                    return;
                case S_ScheduledThreadPoolExecutor:
                    changingSuper = true;
                    super.visit(version, access, name, signature, S_TBaseScheduledThreadPoolExecutor, interfaces);
                    return;
                case S_Timer:
                    changingSuper = true;
                    super.visit(version, access, name, signature, S_TBaseTimer, interfaces);
                    return;
                case S_HandlerThread:
                    changingSuper = true;
                    super.visit(version, access, name, signature, S_TBaseHandlerThread, interfaces);
                    return;
            }
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access0, String name0, String desc0, String signature0, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access0, name0, desc0, signature0, exceptions);
        if (filterClass(className)) {
            return mv;
        }

        if (changingSuper) { // 改继承
            mv = new ChangeSuperMethodVisitor(ASM5, mv, className);
        } else {
            if (buildingPackage && name0.equals("buildPackageList")) {
                mv = new AddPackageMethodVisitor(ASM5, mv);
            } else {
                mv = new ChangeProxyMethodVisitor(ASM5, mv, className);
            }
        }

        return mv;
    }

    private boolean filterClass(String className) {
        return className.contains("com/codoon/threadtracker/proxy");
    }
}
