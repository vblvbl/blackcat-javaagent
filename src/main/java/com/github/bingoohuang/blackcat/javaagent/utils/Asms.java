package com.github.bingoohuang.blackcat.javaagent.utils;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.annotation.Annotation;
import java.util.List;

public class Asms {
    public static boolean isAnyMethodAnnotationPresent(
            List<MethodNode> methods, Class<? extends Annotation> annotationClass) {
        for (MethodNode mn : methods) {
            if (isAnnotationPresent(mn, annotationClass)) return true;
        }

        return false;
    }

    public static boolean isAnnotationPresent(
            ClassNode cn,
            Class<? extends Annotation> annotationClass) {
        List<AnnotationNode> visibleAnnotations = cn.visibleAnnotations;
        return isAnnotationPresent(annotationClass, visibleAnnotations);
    }

    public static boolean isAnnotationPresent(
            MethodNode mn,
            Class<? extends Annotation> annotationClass) {
        List<AnnotationNode> visibleAnnotations = mn.visibleAnnotations;
        return isAnnotationPresent(annotationClass, visibleAnnotations);
    }


    private static boolean isAnnotationPresent(
            Class<? extends Annotation> annotationClass,
            List<AnnotationNode> visibleAnnotations) {
        if (visibleAnnotations == null) return false;

        String expectedDesc = ci(annotationClass);
        for (AnnotationNode visibleAnnotation : visibleAnnotations) {
            if (expectedDesc.equals(visibleAnnotation.desc)) return true;
        }

        return false;
    }


    // Creates a dotted class name from a path/package name
    public static String c(String p) {
        return p.replace('/', '.');
    }

    // Creates a class path name, from a Class.
    public static String p(Class n) {
        return n.getName().replace('.', '/');
    }

    public static String p(String className) {
        return className.replace('.', '/');
    }


    // Creates a class identifier of form Labc/abc;, from a Class.
    public static String ci(Class n) {
        if (n.isArray()) {
            n = n.getComponentType();
            if (n.isPrimitive()) {
                if (n == Byte.TYPE) {
                    return "[B";
                } else if (n == Boolean.TYPE) {
                    return "[Z";
                } else if (n == Short.TYPE) {
                    return "[S";
                } else if (n == Character.TYPE) {
                    return "[C";
                } else if (n == Integer.TYPE) {
                    return "[I";
                } else if (n == Float.TYPE) {
                    return "[F";
                } else if (n == Double.TYPE) {
                    return "[D";
                } else if (n == Long.TYPE) {
                    return "[J";
                } else {
                    throw new RuntimeException("Unrecognized type in compiler: " + n.getName());
                }
            } else {
                return "[" + ci(n);
            }
        } else {
            if (n.isPrimitive()) {
                if (n == Byte.TYPE) {
                    return "B";
                } else if (n == Boolean.TYPE) {
                    return "Z";
                } else if (n == Short.TYPE) {
                    return "S";
                } else if (n == Character.TYPE) {
                    return "C";
                } else if (n == Integer.TYPE) {
                    return "I";
                } else if (n == Float.TYPE) {
                    return "F";
                } else if (n == Double.TYPE) {
                    return "D";
                } else if (n == Long.TYPE) {
                    return "J";
                } else if (n == Void.TYPE) {
                    return "V";
                } else {
                    throw new RuntimeException("Unrecognized type in compiler: " + n.getName());
                }
            } else {
                return "L" + p(n) + ";";
            }
        }
    }

    // Create a method signature from the given param types and return values
    public static String sig(Class retval, Class... params) {
        return sigParams(params) + ci(retval);
    }

    public static String sig(Class[] retvalParams) {
        Class[] justParams = new Class[retvalParams.length - 1];
        System.arraycopy(retvalParams, 1, justParams, 0, justParams.length);
        return sigParams(justParams) + ci(retvalParams[0]);
    }

    public static String sig(Class retval, String descriptor, Class... params) {
        return sigParams(descriptor, params) + ci(retval);
    }

    public static String sigParams(Class... params) {
        StringBuilder signature = new StringBuilder("(");

        for (int i = 0; i < params.length; i++) {
            signature.append(ci(params[i]));
        }

        signature.append(")");

        return signature.toString();
    }

    public static String sigParams(String descriptor, Class... params) {
        StringBuilder signature = new StringBuilder("(");

        signature.append(descriptor);

        for (int i = 0; i < params.length; i++) {
            signature.append(ci(params[i]));
        }

        signature.append(")");

        return signature.toString();
    }

}