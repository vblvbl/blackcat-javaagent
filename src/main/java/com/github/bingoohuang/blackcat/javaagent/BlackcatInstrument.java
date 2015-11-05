package com.github.bingoohuang.blackcat.javaagent;

import com.github.bingoohuang.blackcat.javaagent.utils.Debugs;
import com.github.bingoohuang.blackcat.javaagent.utils.Helper;
import com.github.bingoohuang.blackcat.javaagent.utils.TreeAsms;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Iterator;
import java.util.List;

import static com.github.bingoohuang.blackcat.javaagent.utils.Asms.p;
import static com.github.bingoohuang.blackcat.javaagent.utils.Asms.sig;

public class BlackcatInstrument {
    protected final String callbackId;
    protected final BlackcatJavaAgentInterceptor interceptor;
    protected final String className;
    protected final byte[] originalClassFileBuffer;

    protected ClassNode cn;
    protected Type classType;

    protected MethodNode mn;
    protected Type[] methodArguments;
    protected Type methodReturnType;
    protected int methodOffset;

    /*
     Callback arguments: Method scope variables
     */
    protected int methodVarIndex;
    protected int executionIdIndex;

    protected LabelNode startNode;

    public BlackcatInstrument(String className, byte[] classfileBuffer,
                              BlackcatJavaAgentInterceptor interceptor, String callbackId) {
        this.className = className;
        this.originalClassFileBuffer = classfileBuffer;
        this.callbackId = callbackId;
        this.interceptor = interceptor;
    }

    public byte[] modifyClass() {
        boolean ok = interceptor.interceptClass(className, originalClassFileBuffer);
        if (!ok) return originalClassFileBuffer;

        this.cn = new ClassNode();
        ClassReader cr = new ClassReader(originalClassFileBuffer);
        cr.accept(cn, 0);
        this.classType = Type.getType("L" + cn.name + ";");

        if (!isTransformed(cn.methods)) return originalClassFileBuffer;

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);

        byte[] bytes = cw.toByteArray();

        Debugs.writeClassFile(cn, className, bytes);

        return bytes;

    }

    private boolean isTransformed(List<MethodNode> methods) {
        int transformedCount = 0;
        for (MethodNode node : methods) {
            if (modifyMethod(node)) ++transformedCount;
        }

        return transformedCount > 0;
    }

    private boolean modifyMethod(MethodNode mn) {
        if (Helper.isAbstract(mn)) return false;
        if (!interceptor.interceptMethod(this.cn, mn)) return false;

        this.mn = mn;
        this.methodArguments = Type.getArgumentTypes(this.mn.desc);
        this.methodReturnType = Type.getReturnType(this.mn.desc);
        this.methodOffset = isStatic() ? 0 : 1;

        addTraceStart();
        addTraceReturn();
        addTraceThrow();
        addTraceThrowablePassed();

        return true;
    }

    private int addMethodParametersVariable(InsnList il) {
        il.add(TreeAsms.getPushInstruction(this.methodArguments.length));
        il.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
        int methodParametersIndex = getFistAvailablePosition();
        il.add(new VarInsnNode(Opcodes.ASTORE, methodParametersIndex));
        this.mn.maxLocals++;
        for (int i = 0; i < this.methodArguments.length; i++) {
            il.add(new VarInsnNode(Opcodes.ALOAD, methodParametersIndex));
            il.add(TreeAsms.getPushInstruction(i));
            il.add(TreeAsms.getLoadInst(methodArguments[i],
                    getArgumentPosition(i)));
            MethodInsnNode mNode = TreeAsms
                    .getWrapperContructionInst(methodArguments[i]);
            if (mNode != null) {
                il.add(mNode);
            }
            il.add(new InsnNode(Opcodes.AASTORE));
        }
        return methodParametersIndex;
    }

    private void addGetMethodInvocation(InsnList il) {
        il.add(TreeAsms.getPushInstruction(this.methodArguments.length));
        il.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Class"));
        int parameterClassesIndex = getFistAvailablePosition();
        il.add(new VarInsnNode(Opcodes.ASTORE, parameterClassesIndex));
        this.mn.maxLocals++;
        for (int i = 0; i < this.methodArguments.length; i++) {
            il.add(new VarInsnNode(Opcodes.ALOAD, parameterClassesIndex));
            il.add(TreeAsms.getPushInstruction(i));
            il.add(TreeAsms.getClassReferenceInstruction(methodArguments[i], cn.version & 0xFFFF));
            il.add(new InsnNode(Opcodes.AASTORE));
        }
        il.add(TreeAsms.getClassConstantReference(this.classType, cn.version & 0xFFFF));
        il.add(new LdcInsnNode(this.mn.name));
        il.add(new VarInsnNode(Opcodes.ALOAD, parameterClassesIndex));
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, p(Helper.class), "getSource",
                sig(Object.class, Class.class, String.class, Class[].class), false));
    }

    private void addStoreMethod(InsnList il) {
        this.methodVarIndex = getFistAvailablePosition();
        il.add(new VarInsnNode(Opcodes.ASTORE, this.methodVarIndex));
        this.mn.maxLocals++;
    }

    private void addGetCallback(InsnList il) {
        il.add(new LdcInsnNode(this.callbackId));
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, p(BlackcatJavaAgentCallback.class), "getInstance",
                sig(BlackcatJavaAgentCallback.class, String.class), false));
    }

    private void addTraceStart() {
        InsnList il = new InsnList();
        int methodParametersIndex = addMethodParametersVariable(il);
        addGetMethodInvocation(il);
        addStoreMethod(il);
        addGetCallback(il);

        il.add(new VarInsnNode(Opcodes.ALOAD, this.methodVarIndex));
        il.add(new VarInsnNode(Opcodes.ALOAD, methodParametersIndex));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, p(BlackcatJavaAgentCallback.class), "onStart",
                sig(String.class, Object.class, Object[].class), false));

        this.executionIdIndex = getFistAvailablePosition();
        il.add(new VarInsnNode(Opcodes.ASTORE, this.executionIdIndex));
        this.mn.maxLocals++;
        this.startNode = new LabelNode();
        this.mn.instructions.insert(startNode);
        this.mn.instructions.insert(il);
    }

    private void addTraceReturn() {
        InsnList il = this.mn.instructions;

        Iterator<AbstractInsnNode> it = il.iterator();
        while (it.hasNext()) {
            AbstractInsnNode abstractInsnNode = it.next();

            switch (abstractInsnNode.getOpcode()) {
                case Opcodes.RETURN:
                    il.insertBefore(abstractInsnNode, getVoidReturnTraceInstructions());
                    break;
                case Opcodes.IRETURN:
                case Opcodes.LRETURN:
                case Opcodes.FRETURN:
                case Opcodes.ARETURN:
                case Opcodes.DRETURN:
                    il.insertBefore(abstractInsnNode, getReturnTraceInstructions());
            }
        }
    }

    private void addTraceThrow() {
        InsnList il = this.mn.instructions;

        Iterator<AbstractInsnNode> it = il.iterator();
        while (it.hasNext()) {
            AbstractInsnNode abstractInsnNode = it.next();

            switch (abstractInsnNode.getOpcode()) {
                case Opcodes.ATHROW:
                    il.insertBefore(abstractInsnNode, getThrowTraceInstructions());
                    break;
            }
        }
    }

    private void addTraceThrowablePassed() {
        InsnList il = this.mn.instructions;

        LabelNode endNode = new LabelNode();
        il.add(endNode);

        addCatchBlock(this.startNode, endNode);

    }

    private void addCatchBlock(LabelNode startNode, LabelNode endNode) {
        InsnList il = new InsnList();
        LabelNode handlerNode = new LabelNode();
        il.add(handlerNode);

        int exceptionVariablePosition = getFistAvailablePosition();
        il.add(new VarInsnNode(Opcodes.ASTORE, exceptionVariablePosition));
        this.methodOffset++; // Actualizamos el offset
        addGetCallback(il);
        il.add(new VarInsnNode(Opcodes.ALOAD, this.methodVarIndex));
        il.add(new VarInsnNode(Opcodes.ALOAD, exceptionVariablePosition));
        il.add(new VarInsnNode(Opcodes.ALOAD, this.executionIdIndex));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                p(BlackcatJavaAgentCallback.class), "onThrowableUncaught",
                sig(void.class, Object.class, Throwable.class, String.class), false));

        il.add(new VarInsnNode(Opcodes.ALOAD, exceptionVariablePosition));
        il.add(new InsnNode(Opcodes.ATHROW));

        TryCatchBlockNode blockNode = new TryCatchBlockNode(startNode, endNode, handlerNode, null);

        this.mn.tryCatchBlocks.add(blockNode);
        this.mn.instructions.add(il);
    }

    private InsnList getVoidReturnTraceInstructions() {
        InsnList il = new InsnList();
        addGetCallback(il);
        il.add(new VarInsnNode(Opcodes.ALOAD, this.methodVarIndex));
        il.add(new VarInsnNode(Opcodes.ALOAD, this.executionIdIndex));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                p(BlackcatJavaAgentCallback.class), "onVoidFinish",
                sig(void.class, Object.class, String.class), false));

        return il;
    }

    private InsnList getThrowTraceInstructions() {
        InsnList il = new InsnList();

        int exceptionVariablePosition = getFistAvailablePosition();
        il.add(new VarInsnNode(Opcodes.ASTORE, exceptionVariablePosition));

        this.methodOffset++; // Actualizamos el offset
        addGetCallback(il);
        il.add(new VarInsnNode(Opcodes.ALOAD, this.methodVarIndex));
        il.add(new VarInsnNode(Opcodes.ALOAD, exceptionVariablePosition));
        il.add(new VarInsnNode(Opcodes.ALOAD, this.executionIdIndex));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                p(BlackcatJavaAgentCallback.class), "onThrowableThrown",
                sig(void.class, Object.class, Throwable.class, String.class), false));

        il.add(new VarInsnNode(Opcodes.ALOAD, exceptionVariablePosition));

        return il;
    }

    private InsnList getReturnTraceInstructions() {
        InsnList il = new InsnList();

        int retunedVariablePosition = getFistAvailablePosition();
        il.add(TreeAsms.getStoreInst(this.methodReturnType, retunedVariablePosition));

        this.variableCreated(this.methodReturnType); // Actualizamos el offset
        addGetCallback(il);
        il.add(new VarInsnNode(Opcodes.ALOAD, this.methodVarIndex));
        il.add(TreeAsms.getLoadInst(this.methodReturnType, retunedVariablePosition));
        MethodInsnNode mNode = TreeAsms.getWrapperContructionInst(this.methodReturnType);
        if (mNode != null) {
            il.add(mNode);
        }
        il.add(new VarInsnNode(Opcodes.ALOAD, this.executionIdIndex));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                p(BlackcatJavaAgentCallback.class), "onFinish",
                sig(void.class, Object.class, Object.class, String.class), false));

        il.add(TreeAsms.getLoadInst(this.methodReturnType, retunedVariablePosition));

        return il;

    }

    private int getFistAvailablePosition() {
        return this.mn.maxLocals + this.methodOffset;
    }

    protected void variableCreated(Type type) {
        char charType = type.getDescriptor().charAt(0);
        this.methodOffset += (charType == 'J' || charType == 'D') ? 2 : 1;
    }

    public int getArgumentPosition(int argNo) {
        return Helper.getArgumentPosition(methodOffset, methodArguments, argNo);
    }

    public boolean isAbstract() {
        return Helper.isAbstract(this.mn);
    }

    public boolean isStatic() {
        return Helper.isStatic(this.mn);
    }

    public boolean isPublic() {
        return Helper.isPublic(this.mn);
    }
}
