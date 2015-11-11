package com.github.bingoohuang.blackcat.javaagent.utils;

import com.github.bingoohuang.blackcat.javaagent.annotations.BlackcatCreateTransformedClassFile;
import com.google.common.io.Files;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;

public class Debugs {
    public static void writeClassFile(
            ClassNode cn, String className, byte[] bytes) {
        if (!Asms.isAnnotationPresent(cn,
                BlackcatCreateTransformedClassFile.class)) return;

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
}
