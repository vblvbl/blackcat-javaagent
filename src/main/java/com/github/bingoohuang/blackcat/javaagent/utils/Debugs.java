package com.github.bingoohuang.blackcat.javaagent.utils;

import com.github.bingoohuang.blackcat.javaagent.BlackcatCreateTransformedClassFile;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static com.github.bingoohuang.blackcat.javaagent.utils.Asms.ci;

public class Debugs {
    public static void writeClassFile(
            ClassNode cn, String className, byte[] bytes) {
        if (!shouldCreateTransformedClassFile(cn)) return;

        try {
            String name = Asms.c(className) + ".class";
            FileOutputStream fos = new FileOutputStream(name, true);
            try {
                fos.write(bytes);
            } finally {
                fos.close();
            }
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
