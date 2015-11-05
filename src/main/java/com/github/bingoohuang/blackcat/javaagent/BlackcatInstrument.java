package com.github.bingoohuang.blackcat.javaagent;

import com.github.bingoohuang.blackcat.javaagent.utils.Debugs;
import com.github.bingoohuang.blackcat.javaagent.utils.Helper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Iterator;
import java.util.List;

import static com.github.bingoohuang.blackcat.javaagent.utils.Asms.p;
import static com.github.bingoohuang.blackcat.javaagent.utils.Asms.sig;
import static com.github.bingoohuang.blackcat.javaagent.utils.TreeAsms.*;
import static org.objectweb.asm.Opcodes.*;

public class BlackcatInstrument {
    protected final String callbackId;
    protected final BlackcatJavaAgentInterceptor interceptor;
    protected final String className;
    protected final byte[] classFileBuffer;

    protected ClassNode classNode;
    protected Type classType;

    protected MethodNode methodNode;
    protected Type[] methodArgs;
    protected Type methodReturnType;
    protected int methodOffset;

    /*
     Callback arguments: Method scope variables
     */
    protected int sourceVarIndex;
    protected int callbackVarIndex;
    protected int executionIdIndex;

    protected LabelNode startNode;

    public BlackcatInstrument(String className, byte[] classfileBuffer,
                              BlackcatJavaAgentInterceptor interceptor,
                              String callbackId) {
        this.className = className;
        this.classFileBuffer = classfileBuffer;
        this.callbackId = callbackId;
        this.interceptor = interceptor;
    }

    public byte[] modifyClass() {
        boolean ok = interceptor.interceptClass(className, classFileBuffer);
        if (!ok) return classFileBuffer;

        classNode = new ClassNode();
        ClassReader cr = new ClassReader(classFileBuffer);
        cr.accept(classNode, 0);
        classType = Type.getType("L" + classNode.name + ";");

        if (!isTransformed(classNode.methods)) return classFileBuffer;

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(cw);

        byte[] bytes = cw.toByteArray();

        Debugs.writeClassFile(classNode, className, bytes);

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
        if (!interceptor.interceptMethod(classNode, mn)) return false;

        methodNode = mn;
        methodArgs = Type.getArgumentTypes(methodNode.desc);
        methodReturnType = Type.getReturnType(methodNode.desc);
        methodOffset = Helper.isStatic(methodNode) ? 0 : 1;

        addTraceStart();
        addTraceReturn();
        addTraceThrow();
        addTraceThrowableUncaught();

        return true;
    }

    private int addMethodParametersVariable(InsnList il) {
        il.add(getPushInst(methodArgs.length));
        il.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
        int methodParametersIndex = getFistAvailablePosition();
        il.add(new VarInsnNode(Opcodes.ASTORE, methodParametersIndex));
        methodNode.maxLocals++;
        for (int i = 0; i < methodArgs.length; i++) {
            il.add(new VarInsnNode(ALOAD, methodParametersIndex));
            il.add(getPushInst(i));
            il.add(getLoadInst(methodArgs[i],
                    getArgumentPosition(i)));
            MethodInsnNode mNode = getWrapperCtorInst(methodArgs[i]);
            if (mNode != null) {
                il.add(mNode);
            }
            il.add(new InsnNode(Opcodes.AASTORE));
        }
        return methodParametersIndex;
    }

    private void addGetMethodInvocation(InsnList il) {
        il.add(getPushInst(methodArgs.length));
        il.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Class"));
        int parameterClassesIndex = getFistAvailablePosition();
        il.add(new VarInsnNode(Opcodes.ASTORE, parameterClassesIndex));
        methodNode.maxLocals++;
        for (int i = 0; i < methodArgs.length; i++) {
            il.add(new VarInsnNode(ALOAD, parameterClassesIndex));
            il.add(getPushInst(i));
            il.add(getClassRefInst(methodArgs[i], classNode.version & 0xFFFF));
            il.add(new InsnNode(Opcodes.AASTORE));
        }
        il.add(getClassConstantRef(classType, classNode.version & 0xFFFF));
        il.add(new LdcInsnNode(methodNode.name));
        il.add(new VarInsnNode(ALOAD, parameterClassesIndex));
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, p(Helper.class), "getSource",
                sig(Object.class, Class.class, String.class, Class[].class), false));
    }

    private void addSourceStore(InsnList il) {
        sourceVarIndex = getFistAvailablePosition();
        il.add(new VarInsnNode(Opcodes.ASTORE, sourceVarIndex));
        methodNode.maxLocals++;
    }

    private void addCallbackStore(InsnList il) {
        callbackVarIndex = getFistAvailablePosition();
        il.add(new VarInsnNode(Opcodes.ASTORE, callbackVarIndex));
        methodNode.maxLocals++;
    }


    private void addGetCallback(InsnList il) {
        il.add(new LdcInsnNode(callbackId));
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                p(BlackcatJavaAgentCallback.class), "getInstance",
                sig(BlackcatJavaAgentCallback.class, String.class), false));
    }

    private void addTraceStart() {
        InsnList il = new InsnList();
        int methodParametersIndex = addMethodParametersVariable(il);
        addGetMethodInvocation(il);
        addSourceStore(il);
        addGetCallback(il);
        addCallbackStore(il);

        il.add(new VarInsnNode(ALOAD, callbackVarIndex));
        il.add(new VarInsnNode(ALOAD, sourceVarIndex));
        il.add(new VarInsnNode(ALOAD, methodParametersIndex));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                p(BlackcatJavaAgentCallback.class), "onStart",
                sig(String.class, Object.class, Object[].class), false));

        executionIdIndex = getFistAvailablePosition();
        il.add(new VarInsnNode(Opcodes.ASTORE, executionIdIndex));
        methodNode.maxLocals++;
        startNode = new LabelNode();
        methodNode.instructions.insert(startNode);
        methodNode.instructions.insert(il);
    }

    private void addTraceReturn() {
        InsnList il = methodNode.instructions;

        Iterator<AbstractInsnNode> it = il.iterator();
        while (it.hasNext()) {
            AbstractInsnNode abstractInsnNode = it.next();

            switch (abstractInsnNode.getOpcode()) {
                case RETURN:
                    il.insertBefore(abstractInsnNode, getVoidReturnTraceInsts());
                    break;
                case IRETURN:
                case LRETURN:
                case FRETURN:
                case ARETURN:
                case DRETURN:
                    il.insertBefore(abstractInsnNode, getReturnTraceInsts());
            }
        }
    }

    private void addTraceThrow() {
        InsnList il = methodNode.instructions;

        Iterator<AbstractInsnNode> it = il.iterator();
        while (it.hasNext()) {
            AbstractInsnNode abstractInsnNode = it.next();

            switch (abstractInsnNode.getOpcode()) {
                case ATHROW:
                    il.insertBefore(abstractInsnNode, getThrowTraceInsts());
                    break;
            }
        }
    }

    private void addTraceThrowableUncaught() {
        InsnList il = methodNode.instructions;

        LabelNode endNode = new LabelNode();
        il.add(endNode);

        addCatchBlock(startNode, endNode);

    }

    private void addCatchBlock(LabelNode startNode, LabelNode endNode) {
        InsnList il = new InsnList();
        LabelNode handlerNode = new LabelNode();
        il.add(handlerNode);

        int exceptionVariablePosition = getFistAvailablePosition();
        il.add(new VarInsnNode(ASTORE, exceptionVariablePosition));
        methodOffset++; // Actualizamos el offset
        il.add(new VarInsnNode(ALOAD, callbackVarIndex));
        il.add(new VarInsnNode(ALOAD, sourceVarIndex));
        il.add(new VarInsnNode(ALOAD, exceptionVariablePosition));
        il.add(new VarInsnNode(ALOAD, executionIdIndex));
        il.add(new MethodInsnNode(INVOKEVIRTUAL,
                p(BlackcatJavaAgentCallback.class), "onThrowableUncaught",
                sig(void.class, Object.class, Throwable.class, String.class), false));

        il.add(new VarInsnNode(ALOAD, exceptionVariablePosition));
        il.add(new InsnNode(ATHROW));

        TryCatchBlockNode blockNode;
        blockNode = new TryCatchBlockNode(startNode, endNode, handlerNode, null);

        methodNode.tryCatchBlocks.add(blockNode);
        methodNode.instructions.add(il);
    }

    private InsnList getVoidReturnTraceInsts() {
        InsnList il = new InsnList();
        il.add(new VarInsnNode(ALOAD, callbackVarIndex));
        il.add(new VarInsnNode(ALOAD, sourceVarIndex));
        il.add(new VarInsnNode(ALOAD, executionIdIndex));
        il.add(new MethodInsnNode(INVOKEVIRTUAL,
                p(BlackcatJavaAgentCallback.class), "onVoidFinish",
                sig(void.class, Object.class, String.class), false));

        return il;
    }

    private InsnList getThrowTraceInsts() {
        InsnList il = new InsnList();

        int exceptionVariablePosition = getFistAvailablePosition();
        il.add(new VarInsnNode(ASTORE, exceptionVariablePosition));

        methodOffset++; // Actualizamos el offset
        il.add(new VarInsnNode(ALOAD, callbackVarIndex));
        il.add(new VarInsnNode(ALOAD, sourceVarIndex));
        il.add(new VarInsnNode(ALOAD, exceptionVariablePosition));
        il.add(new VarInsnNode(ALOAD, executionIdIndex));
        il.add(new MethodInsnNode(INVOKEVIRTUAL,
                p(BlackcatJavaAgentCallback.class), "onThrowableThrown",
                sig(void.class, Object.class, Throwable.class, String.class), false));

        il.add(new VarInsnNode(ALOAD, exceptionVariablePosition));

        return il;
    }

    private InsnList getReturnTraceInsts() {
        InsnList il = new InsnList();

        int retunedVariablePosition = getFistAvailablePosition();
        il.add(getStoreInst(methodReturnType, retunedVariablePosition));

        variableCreated(methodReturnType); // Actualizamos el offset
        il.add(new VarInsnNode(ALOAD, callbackVarIndex));
        il.add(new VarInsnNode(ALOAD, sourceVarIndex));
        il.add(getLoadInst(methodReturnType, retunedVariablePosition));
        MethodInsnNode mNode = getWrapperCtorInst(methodReturnType);
        if (mNode != null) {
            il.add(mNode);
        }
        il.add(new VarInsnNode(ALOAD, executionIdIndex));
        il.add(new MethodInsnNode(INVOKEVIRTUAL,
                p(BlackcatJavaAgentCallback.class), "onFinish",
                sig(void.class, Object.class, Object.class, String.class), false));

        il.add(getLoadInst(methodReturnType, retunedVariablePosition));

        return il;

    }

    private int getFistAvailablePosition() {
        return methodNode.maxLocals + methodOffset;
    }

    protected void variableCreated(Type type) {
        char charType = type.getDescriptor().charAt(0);
        methodOffset += (charType == 'J' || charType == 'D') ? 2 : 1;
    }

    public int getArgumentPosition(int argNo) {
        return Helper.getArgumentPosition(methodOffset, methodArgs, argNo);
    }
}
