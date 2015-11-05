package com.github.bingoohuang.blackcat.javaagent.utils;

import com.github.bingoohuang.blackcat.javaagent.BlackcatCreateTransformedClassFile;
import com.google.common.io.Files;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.github.bingoohuang.blackcat.javaagent.utils.Asms.ci;

public class Debugs {
    public static void writeClassFile(
            ClassNode cn, String className, byte[] bytes) {
        if (!shouldCreateTransformedClassFile(cn)) return;

        writeClassFile(className, bytes);
    }

    private static void writeClassFile(String className, byte[] bytes) {
        try {
            String classFilename = Asms.c(className) + ".class";
            Files.write(bytes, new File(classFilename));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static boolean shouldCreateTransformedClassFile(ClassNode cn) {
        if (cn.visibleAnnotations == null) return false;

        String expectedDesc = ci(BlackcatCreateTransformedClassFile.class);
        List<AnnotationNode> visibleAnnotations = cn.visibleAnnotations;
        for (AnnotationNode visibleAnnotation : visibleAnnotations) {
            if (expectedDesc.equals(visibleAnnotation.desc)) return true;
        }

        return false;
    }
}
