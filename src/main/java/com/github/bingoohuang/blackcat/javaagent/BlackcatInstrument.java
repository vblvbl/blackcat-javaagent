package com.github.bingoohuang.blackcat.javaagent;

import com.github.bingoohuang.blackcat.javaagent.utils.Debugs;
import com.github.bingoohuang.blackcat.javaagent.utils.Helper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
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
    protected int callbackVarIndex;
    protected int rtIndex;

    protected LabelNode startNode;

    public BlackcatInstrument(
            String className, byte[] classfileBuffer,
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

        int count = modifyMethodCount(classNode.methods);
        if (count == 0) return classFileBuffer;

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(cw);

        byte[] bytes = cw.toByteArray();

        Debugs.writeClassFile(classNode, className, bytes);

        return bytes;

    }

    private int modifyMethodCount(List<MethodNode> methods) {
        int transformedCount = 0;
        for (MethodNode node : methods) {
            if (modifyMethod(node)) ++transformedCount;
        }

        return transformedCount;
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
        il.add(new TypeInsnNode(ANEWARRAY, p(Object.class)));
        int methodParametersIndex = getFistAvailablePosition();
        il.add(new VarInsnNode(ASTORE, methodParametersIndex));
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
            il.add(new InsnNode(AASTORE));
        }
        return methodParametersIndex;
    }

    private void addGetMethodInvocation(InsnList il) {
        il.add(getPushInst(methodArgs.length));
        il.add(new TypeInsnNode(ANEWARRAY, p(Class.class)));
        int parameterClassesIndex = getFistAvailablePosition();
        il.add(new VarInsnNode(ASTORE, parameterClassesIndex));
        methodNode.maxLocals++;
        for (int i = 0; i < methodArgs.length; i++) {
            il.add(new VarInsnNode(ALOAD, parameterClassesIndex));
            il.add(getPushInst(i));
            il.add(getClassRefInst(methodArgs[i], classNode.version & 0xFFFF));
            il.add(new InsnNode(AASTORE));
        }
        il.add(getClassConstantRef(classType, classNode.version & 0xFFFF));
        il.add(new LdcInsnNode(methodNode.name));
        il.add(new VarInsnNode(ALOAD, parameterClassesIndex));
        il.add(new MethodInsnNode(INVOKESTATIC, p(Helper.class), "getSource",
                sig(Object.class, Class.class, String.class, Class[].class), false));
    }

    private int addVarStore(InsnList insnList) {
        int varIndex = getFistAvailablePosition();
        insnList.add(new VarInsnNode(ASTORE, varIndex));
        methodNode.maxLocals++;

        return varIndex;
    }

    private void addGetCallback(InsnList insnList) {
        insnList.add(new LdcInsnNode(callbackId));
        insnList.add(new MethodInsnNode(INVOKESTATIC,
                p(BlackcatJavaAgentCallback.class), "getInstance",
                sig(BlackcatJavaAgentCallback.class, String.class), false));
    }

    private void addTraceStart() {
        InsnList insnList = new InsnList();
        int methodParametersIndex = addMethodParametersVariable(insnList);
        addGetMethodInvocation(insnList);
        int sourceVarIndex = addVarStore(insnList);
        addGetCallback(insnList);
        callbackVarIndex = addVarStore(insnList);

        insnList.add(new VarInsnNode(ALOAD, callbackVarIndex));
        insnList.add(new VarInsnNode(ALOAD, sourceVarIndex));
        insnList.add(new VarInsnNode(ALOAD, methodParametersIndex));
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL,
                p(BlackcatJavaAgentCallback.class), "doStart",
                sig(BlackcatMethodRt.class, Object.class, Object[].class), false));

        rtIndex = getFistAvailablePosition();
        insnList.add(new VarInsnNode(ASTORE, rtIndex));
        methodNode.maxLocals++;
        startNode = new LabelNode();
        methodNode.instructions.insert(startNode);
        methodNode.instructions.insert(insnList);
    }

    private void addTraceReturn() {
        InsnList insnList = methodNode.instructions;

        Iterator<AbstractInsnNode> it = insnList.iterator();
        while (it.hasNext()) {
            AbstractInsnNode abstractInsnNode = it.next();

            switch (abstractInsnNode.getOpcode()) {
                case RETURN:
                    insnList.insertBefore(abstractInsnNode, getVoidReturnTraceInsts());
                    break;
                case IRETURN:
                case LRETURN:
                case FRETURN:
                case ARETURN:
                case DRETURN:
                    insnList.insertBefore(abstractInsnNode, getReturnTraceInsts());
            }
        }
    }

    private void addTraceThrow() {
        InsnList insnList = methodNode.instructions;

        Iterator<AbstractInsnNode> it = insnList.iterator();
        while (it.hasNext()) {
            AbstractInsnNode abstractInsnNode = it.next();

            switch (abstractInsnNode.getOpcode()) {
                case ATHROW:
                    insnList.insertBefore(abstractInsnNode, getThrowTraceInsts());
                    break;
            }
        }
    }

    private void addTraceThrowableUncaught() {
        InsnList insnList = methodNode.instructions;

        LabelNode endNode = new LabelNode();
        insnList.add(endNode);

        addCatchBlock(startNode, endNode);

    }

    private void addCatchBlock(LabelNode startNode, LabelNode endNode) {
        InsnList insnList = new InsnList();
        LabelNode handlerNode = new LabelNode();
        insnList.add(handlerNode);

        int exceptionVariablePosition = getFistAvailablePosition();
        insnList.add(new VarInsnNode(ASTORE, exceptionVariablePosition));
        methodOffset++;
        insnList.add(new VarInsnNode(ALOAD, callbackVarIndex));
        insnList.add(new VarInsnNode(ALOAD, rtIndex));
        insnList.add(new VarInsnNode(ALOAD, exceptionVariablePosition));
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL,
                p(BlackcatJavaAgentCallback.class), "doThrowableUncaught",
                sig(void.class, BlackcatMethodRt.class, Throwable.class), false));

        insnList.add(new VarInsnNode(ALOAD, exceptionVariablePosition));
        insnList.add(new InsnNode(ATHROW));

        TryCatchBlockNode blockNode;
        blockNode = new TryCatchBlockNode(startNode, endNode, handlerNode, null);

        methodNode.tryCatchBlocks.add(blockNode);
        methodNode.instructions.add(insnList);
    }

    private InsnList getVoidReturnTraceInsts() {
        InsnList insnList = new InsnList();
        insnList.add(new VarInsnNode(ALOAD, callbackVarIndex));
        insnList.add(new VarInsnNode(ALOAD, rtIndex));
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL,
                p(BlackcatJavaAgentCallback.class), "doVoidFinish",
                sig(void.class, BlackcatMethodRt.class), false));

        return insnList;
    }

    private InsnList getThrowTraceInsts() {
        InsnList insnList = new InsnList();

        int exceptionVariablePosition = getFistAvailablePosition();
        insnList.add(new VarInsnNode(ASTORE, exceptionVariablePosition));

        methodOffset++;
        insnList.add(new VarInsnNode(ALOAD, callbackVarIndex));
        insnList.add(new VarInsnNode(ALOAD, rtIndex));
        insnList.add(new VarInsnNode(ALOAD, exceptionVariablePosition));
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL,
                p(BlackcatJavaAgentCallback.class), "doThrowableCaught",
                sig(void.class, BlackcatMethodRt.class, Throwable.class), false));

        insnList.add(new VarInsnNode(ALOAD, exceptionVariablePosition));

        return insnList;
    }

    private InsnList getReturnTraceInsts() {
        InsnList insnList = new InsnList();

        int retunedVariablePosition = getFistAvailablePosition();
        insnList.add(getStoreInst(methodReturnType, retunedVariablePosition));

        updateMethodOffset(methodReturnType);
        insnList.add(new VarInsnNode(ALOAD, callbackVarIndex));
        insnList.add(new VarInsnNode(ALOAD, rtIndex));
        insnList.add(getLoadInst(methodReturnType, retunedVariablePosition));
        MethodInsnNode mNode = getWrapperCtorInst(methodReturnType);
        if (mNode != null) insnList.add(mNode);
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL,
                p(BlackcatJavaAgentCallback.class), "doFinish",
                sig(void.class, BlackcatMethodRt.class, Object.class), false));

        insnList.add(getLoadInst(methodReturnType, retunedVariablePosition));

        return insnList;

    }

    private int getFistAvailablePosition() {
        return methodNode.maxLocals + methodOffset;
    }

    protected void updateMethodOffset(Type type) {
        char charType = type.getDescriptor().charAt(0);
        methodOffset += (charType == 'J' || charType == 'D') ? 2 : 1;
    }

    public int getArgumentPosition(int argNo) {
        return Helper.getArgPosition(methodOffset, methodArgs, argNo);
    }
}
