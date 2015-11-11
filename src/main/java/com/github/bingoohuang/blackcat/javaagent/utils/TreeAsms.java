package com.github.bingoohuang.blackcat.javaagent.utils;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import static com.github.bingoohuang.blackcat.javaagent.utils.Asms.ci;
import static com.github.bingoohuang.blackcat.javaagent.utils.Asms.sig;
import static org.objectweb.asm.Opcodes.*;

public class TreeAsms {
    public static VarInsnNode getLoadInst(Type type, int position) {
        int opCode;
        switch (type.getDescriptor().charAt(0)) {
            case 'B':
            case 'C':
            case 'I':
            case 'Z':
            case 'S':
                opCode = ILOAD;
                break;
            case 'D':
                opCode = DLOAD;
                break;
            case 'F':
                opCode = FLOAD;
                break;
            case 'J':
                opCode = LLOAD;
                break;
            case 'L':
            case '[':
                opCode = ALOAD;
                break;
            default:
                throw new ClassFormatError("Invalid method signature: "
                        + type.getDescriptor());
        }

        return new VarInsnNode(opCode, position);
    }

    public static InsnList getClassRefInst(Type type, int majorVersion) {
        InsnList list = new InsnList();
        char charType = type.getDescriptor().charAt(0);
        String wrapper;
        switch (charType) {
            case 'B':
                wrapper = "java/lang/Byte";
                break;
            case 'C':
                wrapper = "java/lang/Character";
                break;
            case 'D':
                wrapper = "java/lang/Double";
                break;
            case 'F':
                wrapper = "java/lang/Float";
                break;
            case 'I':
                wrapper = "java/lang/Integer";
                break;
            case 'J':
                wrapper = "java/lang/Long";
                break;
            case 'L':
            case '[':
                return getClassConstantRef(type, majorVersion);
            case 'Z':
                wrapper = "java/lang/Boolean";
                break;
            case 'S':
                wrapper = "java/lang/Short";
                break;
            default:
                throw new ClassFormatError("Invalid method signature: "
                        + type.getDescriptor());
        }

        list.add(new FieldInsnNode(GETSTATIC, wrapper, "TYPE", ci(Class.class)));
        return list;

    }

    public static MethodInsnNode getWrapperCtorInst(Type type) {

        char charType = type.getDescriptor().charAt(0);
        String wrapper;
        switch (charType) {
            case 'B':
                wrapper = "java/lang/Byte";
                break;
            case 'C':
                wrapper = "java/lang/Character";
                break;
            case 'D':
                wrapper = "java/lang/Double";
                break;
            case 'F':
                wrapper = "java/lang/Float";
                break;
            case 'I':
                wrapper = "java/lang/Integer";
                break;
            case 'J':
                wrapper = "java/lang/Long";
                break;
            case 'L':
                return null;
            case '[':
                return null;
            case 'Z':
                wrapper = "java/lang/Boolean";
                break;
            case 'S':
                wrapper = "java/lang/Short";
                break;
            default:
                throw new ClassFormatError("Invalid method signature: "
                        + type.getDescriptor());
        }

        return new MethodInsnNode(INVOKESTATIC, wrapper, "valueOf",
                "(" + charType + ")L" + wrapper + ";", false);

    }

    public static VarInsnNode getStoreInst(Type type, int position) {
        int opCode;
        switch (type.getDescriptor().charAt(0)) {
            case 'B':
            case 'C':
            case 'I':
            case 'Z':
            case 'S':
                opCode = ISTORE;
                break;
            case 'D':
                opCode = DSTORE;
                break;
            case 'F':
                opCode = FSTORE;
                break;
            case 'J':
                opCode = LSTORE;
                break;
            case 'L':
            case '[':
                opCode = ASTORE;
                break;
            default:
                throw new ClassFormatError("Invalid method signature: "
                        + type.getDescriptor());
        }
        return new VarInsnNode(opCode, position);
    }

    public static AbstractInsnNode getPushInst(int value) {
        if (value == -1) {
            return new InsnNode(ICONST_M1);
        } else if (value == 0) {
            return new InsnNode(ICONST_0);
        } else if (value == 1) {
            return new InsnNode(ICONST_1);
        } else if (value == 2) {
            return new InsnNode(ICONST_2);
        } else if (value == 3) {
            return new InsnNode(ICONST_3);
        } else if (value == 4) {
            return new InsnNode(ICONST_4);
        } else if (value == 5) {
            return new InsnNode(ICONST_5);
        } else if ((value >= -128) && (value <= 127)) {
            return new IntInsnNode(BIPUSH, value);
        } else if ((value >= -32768) && (value <= 32767)) {
            return new IntInsnNode(SIPUSH, value);
        } else {
            return new LdcInsnNode(value);
        }
    }

    public static InsnList getClassConstantRef(Type type, int majorVersion) {
        InsnList il = new InsnList();

        if (majorVersion >= V1_5) {
            il.add(new LdcInsnNode(type));
        } else {
            String internalName = type.getInternalName();
            String fullyQualifiedName = internalName.replaceAll("/", ".");
            il.add(new LdcInsnNode(fullyQualifiedName));
            il.add(new MethodInsnNode(INVOKESTATIC,
                    "java/lang/Class", "forName",
                    sig(Class.class, String.class), false));
        }
        return il;
    }

}
