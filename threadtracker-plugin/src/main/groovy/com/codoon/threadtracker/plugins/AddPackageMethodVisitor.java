package com.codoon.threadtracker.plugins;


import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class AddPackageMethodVisitor extends MethodVisitor {

    AddPackageMethodVisitor(int api, MethodVisitor methodVisitor) {
        super(api, methodVisitor);
    }

    @Override
    public void visitInsn(int opcode) {
        if (opcode == Opcodes.ARETURN || opcode == Opcodes.RETURN) {
            for (String path : PluginUtils.getClassList()) {
                mv.visitFieldInsn(Opcodes.GETSTATIC, "com/codoon/threadtracker/UserPackage", "packageList", "Ljava/util/ArrayList;");
                mv.visitLdcInsn(path);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "add", "(Ljava/lang/Object;)Z", false);
            }
        }
        super.visitInsn(opcode);
    }
}
