package com.dian.util

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

class ClassInject implements Opcodes {
    static final byte[] inject(byte[] classBytes) {
        ClassReader classReader = new ClassReader(classBytes)
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
        ClassVisitor classVisitor = new MyClassVisitor(classWriter)
        classReader.accept(classVisitor, Opcodes.ASM5)

        return classWriter.toByteArray()
    }

    private static class MyClassVisitor extends ClassVisitor {
        MyClassVisitor(ClassVisitor classVisitor) {
            super(Opcodes.ASM5, classVisitor)
        }

        @Override
        MethodVisitor visitMethod(int access,
                                  String name,
                                  String desc,
                                  String signature,
                                  String[] exceptions) {
            if ("<init>" == name) {
                //get origin method
                MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions)
                MethodVisitor newMethod = new AsmMethodVisit(mv)
                return newMethod
            } else {
                return super.visitMethod(access, name, desc, signature, exceptions)
            }
        }
    }

    static class AsmMethodVisit extends MethodVisitor {
        AsmMethodVisit(MethodVisitor mv) {
            super(Opcodes.ASM5, mv)
        }

        @Override
        void visitInsn(int opcode) {
            if (opcode == Opcodes.RETURN) {
//                super.visitFieldInsn(GETSTATIC, "java/lang/Boolean", "FALSE", "Ljava/lang/Boolean;")
//                super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false)
//                Label l0 = new Label()
//                super.visitJumpInsn(IFEQ, l0)
//                mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
//                mv.visitFieldInsn(GETSTATIC, "dianDex.runtime.AntilazyLoad", "str", "Ljava/lang/String;")
//                mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false)
//                super.visitLabel(l0)

//                mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
//                mv.visitLdcInsn("Hello World");
//                mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

                mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                mv.visitLdcInsn(Type.getType("LdianDex/runtime/AntilazyLoad;"));
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V", false);

            }
            super.visitInsn(opcode)
        }
    }
}