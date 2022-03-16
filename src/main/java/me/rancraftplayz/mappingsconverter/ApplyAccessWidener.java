package me.rancraftplayz.mappingsconverter;

import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerClassVisitor;
import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.zeroturnaround.zip.ZipUtil;
import org.zeroturnaround.zip.transform.ByteArrayZipEntryTransformer;
import org.zeroturnaround.zip.transform.ZipEntryTransformer;
import org.zeroturnaround.zip.transform.ZipEntryTransformerEntry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;

/**
 * Original code from fabric-loom (https://github.com/FabricMC/fabric-loom) (AccessWidenerJarProcessor)
 * Licensed under the MIT License
 */

public class ApplyAccessWidener {
    private AccessWidener accessWidener = new AccessWidener();
    private AccessWidenerReader accessWidenerReader = new AccessWidenerReader(accessWidener);
    private byte[] inputHash;

    public ApplyAccessWidener(File accessWidener) {
        inputHash = Checksum.sha256(accessWidener);

        try (BufferedReader reader = new BufferedReader(new FileReader(accessWidener))) {
            accessWidenerReader.read(reader);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read project access widener file");
        }
    }

    public ApplyAccessWidener(File accessWidener, File mappings, List<Path> libraries) throws IOException {
        inputHash = Checksum.sha256(accessWidener);
        File UwU = new File(accessWidener.getParent(), "tempUwU.accesswidener");
        if (UwU.exists()) {
            UwU.delete();
        }

        this.accessWidener = new RemapAccessWidener(accessWidener, UwU).remap(mappings, libraries).accessWidener;
        UwU.delete();
    }

    public void apply(File file) {
        ZipUtil.transformEntries(file, getTransformers(accessWidener.getTargets()));
        //ZipUtil.addEntry(file, "aw.sha256", inputHash);
    }

    private ZipEntryTransformerEntry[] getTransformers(Set<String> classes) {
        return classes.stream()
                .map(string -> new ZipEntryTransformerEntry(string.replaceAll("\\.", "/") + ".class", getTransformer(string)))
                .toArray(ZipEntryTransformerEntry[]::new);
    }

    private ZipEntryTransformer getTransformer(String className) {
        return new ByteArrayZipEntryTransformer() {
            @Override
            protected byte[] transform(ZipEntry zipEntry, byte[] input) {
                ClassReader reader = new ClassReader(input);
                ClassWriter writer = new ClassWriter(0);
                ClassVisitor classVisitor = AccessWidenerClassVisitor.createClassVisitor(Opcodes.ASM9, writer, accessWidener);

                reader.accept(classVisitor, 0);
                return writer.toByteArray();
            }
        };
    }

}
