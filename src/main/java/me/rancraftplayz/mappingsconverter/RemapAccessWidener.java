package me.rancraftplayz.mappingsconverter;

import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerRemapper;
import net.fabricmc.accesswidener.AccessWidenerWriter;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;

import java.io.*;
import java.nio.file.Path;
import java.util.List;

public class RemapAccessWidener {
    File accessWidenerFile;
    public File outputFile;
    public AccessWidener accessWidener;

    public RemapAccessWidener(File accessWidenerFile, File outputFile) {
        this.accessWidenerFile = accessWidenerFile;
        this.outputFile = outputFile;
    }

    public RemapAccessWidener remap(File mappings, List<Path> libraries) throws IOException {
        AccessWidener accessWidener = new AccessWidener();
        AccessWidenerReader accessWidenerReader = new AccessWidenerReader(accessWidener);
        int version = 1;

        try (BufferedReader reader = new BufferedReader(new FileReader(accessWidenerFile))) {
            version = AccessWidenerReader.readVersion(reader);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(accessWidenerFile))) {
            accessWidenerReader.read(reader);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read project access widener file");
        }

        TinyRemapper tinyRemapper = null;
        AccessWidenerWriter writer = new AccessWidenerWriter(version);
        AccessWidenerReader reader = null;

        if (accessWidener.getNamespace().equals("spigot")) {
            tinyRemapper = TinyRemapper.newRemapper().withMappings(TinyUtils.createTinyMappingProvider(mappings.toPath(), "spigot", "mojang")).ignoreConflicts(true).renameInvalidLocals(true).rebuildSourceFilenames(true).resolveMissing(true).build();
            for (Path path : libraries) {
                tinyRemapper.readClassPath(path);
            }

            AccessWidenerRemapper remapper = new AccessWidenerRemapper(writer, tinyRemapper.getRemapper(), "spigot", "mojang");
            reader = new AccessWidenerReader(remapper);
        }
        if (accessWidener.getNamespace().equals("official")) {
            tinyRemapper = TinyRemapper.newRemapper().withMappings(TinyUtils.createTinyMappingProvider(mappings.toPath(), "official", "mojang")).ignoreConflicts(true).renameInvalidLocals(true).rebuildSourceFilenames(true).resolveMissing(true).build();
            for (Path path : libraries) {
                tinyRemapper.readClassPath(path);
            }

            AccessWidenerRemapper remapper = new AccessWidenerRemapper(writer, tinyRemapper.getRemapper(), "official", "mojang");
            reader = new AccessWidenerReader(remapper);
        }
        if (accessWidener.getNamespace().equals("intermediary")) {
            tinyRemapper = TinyRemapper.newRemapper().withMappings(TinyUtils.createTinyMappingProvider(mappings.toPath(), "intermediary", "mojang")).ignoreConflicts(true).renameInvalidLocals(true).rebuildSourceFilenames(true).resolveMissing(true).build();
            for (Path path : libraries) {
                tinyRemapper.readClassPath(path);
            }

            AccessWidenerRemapper remapper = new AccessWidenerRemapper(writer, tinyRemapper.getRemapper(), "intermediary", "mojang");
            reader = new AccessWidenerReader(remapper);
        }
        if (accessWidener.getNamespace().equals("named")) {
            tinyRemapper = TinyRemapper.newRemapper().withMappings(TinyUtils.createTinyMappingProvider(mappings.toPath(), "named", "mojang")).ignoreConflicts(true).renameInvalidLocals(true).rebuildSourceFilenames(true).resolveMissing(true).build();
            for (Path path : libraries) {
                tinyRemapper.readClassPath(path);
            }

            AccessWidenerRemapper remapper = new AccessWidenerRemapper(writer, tinyRemapper.getRemapper(), "named", "mojang");
            reader = new AccessWidenerReader(remapper);
        }
        //System.out.println(accessWidener.getNamespace());
        try (BufferedReader actualReader = new BufferedReader(new FileReader(accessWidenerFile))) {
            reader.read(actualReader);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read project access widener file");
        }

        // Write bytes taken from writer.write() to file
        File temp = new File("access-widener-remapped.temp");
        temp.createNewFile();
        try (FileOutputStream fos = new FileOutputStream(temp)) {
            fos.write(writer.write());
            tinyRemapper.finish();
            fos.flush();
            fos.close();
        }
        AccessWidener accessWidenerReturnable = new AccessWidener();
        AccessWidenerReader accessWidenerReaderReturnable = new AccessWidenerReader(accessWidenerReturnable);

        try (BufferedReader readerReturnable = new BufferedReader(new FileReader(temp))) {
            accessWidenerReaderReturnable.read(readerReturnable);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read project access widener file");
        }
        if (outputFile.exists()) outputFile.delete();
        temp.renameTo(outputFile);
        this.accessWidener = accessWidenerReturnable;
        return this;
    }

    public RemapAccessWidener remap(File mappings, List<Path> libraries, boolean forIgnite) throws IOException {
        if (!forIgnite) {
            return remap(mappings, libraries);
        }

        AccessWidener accessWidener = new AccessWidener();
        AccessWidenerReader accessWidenerReader = new AccessWidenerReader(accessWidener);
        int version = 1;

        try (BufferedReader reader = new BufferedReader(new FileReader(accessWidenerFile))) {
            version = AccessWidenerReader.readVersion(reader);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(accessWidenerFile))) {
            accessWidenerReader.read(reader);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read project access widener file");
        }

        TinyRemapper tinyRemapper = null;
        AccessWidenerWriter writer = new AccessWidenerWriter(version);
        AccessWidenerReader reader = null;

        if (accessWidener.getNamespace().equals("mojang")) {
            tinyRemapper = TinyRemapper.newRemapper().withMappings(TinyUtils.createTinyMappingProvider(mappings.toPath(), "mojang", "spigot")).ignoreConflicts(true).renameInvalidLocals(true).rebuildSourceFilenames(true).resolveMissing(true).build();
            for (Path path : libraries) {
                tinyRemapper.readClassPath(path);
            }

            AccessWidenerRemapper remapper = new AccessWidenerRemapper(writer, tinyRemapper.getRemapper(), "mojang", "spigot");
            reader = new AccessWidenerReader(remapper);
        }
        if (accessWidener.getNamespace().equals("official")) {
            tinyRemapper = TinyRemapper.newRemapper().withMappings(TinyUtils.createTinyMappingProvider(mappings.toPath(), "official", "spigot")).ignoreConflicts(true).renameInvalidLocals(true).rebuildSourceFilenames(true).resolveMissing(true).build();
            for (Path path : libraries) {
                tinyRemapper.readClassPath(path);
            }

            AccessWidenerRemapper remapper = new AccessWidenerRemapper(writer, tinyRemapper.getRemapper(), "official", "spigot");
            reader = new AccessWidenerReader(remapper);
        }
        if (accessWidener.getNamespace().equals("intermediary")) {
            tinyRemapper = TinyRemapper.newRemapper().withMappings(TinyUtils.createTinyMappingProvider(mappings.toPath(), "intermediary", "spigot")).ignoreConflicts(true).renameInvalidLocals(true).rebuildSourceFilenames(true).resolveMissing(true).build();
            for (Path path : libraries) {
                tinyRemapper.readClassPath(path);
            }

            AccessWidenerRemapper remapper = new AccessWidenerRemapper(writer, tinyRemapper.getRemapper(), "intermediary", "spigot");
            reader = new AccessWidenerReader(remapper);
        }
        if (accessWidener.getNamespace().equals("named")) {
            tinyRemapper = TinyRemapper.newRemapper().withMappings(TinyUtils.createTinyMappingProvider(mappings.toPath(), "named", "spigot")).ignoreConflicts(true).renameInvalidLocals(true).rebuildSourceFilenames(true).resolveMissing(true).build();
            for (Path path : libraries) {
                tinyRemapper.readClassPath(path);
            }

            AccessWidenerRemapper remapper = new AccessWidenerRemapper(writer, tinyRemapper.getRemapper(), "named", "spigot");
            reader = new AccessWidenerReader(remapper);
        }
        //System.out.println(accessWidener.getNamespace());
        try (BufferedReader actualReader = new BufferedReader(new FileReader(accessWidenerFile))) {
            reader.read(actualReader);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read project access widener file");
        }

        // Write bytes taken from writer.write() to file
        File temp = new File("access-widener-remapped.temp");
        temp.createNewFile();
        try (FileOutputStream fos = new FileOutputStream(temp)) {
            fos.write(writer.write());
            tinyRemapper.finish();
            fos.flush();
            fos.close();
        }
        AccessWidener accessWidenerReturnable = new AccessWidener();
        AccessWidenerReader accessWidenerReaderReturnable = new AccessWidenerReader(accessWidenerReturnable);

        try (BufferedReader readerReturnable = new BufferedReader(new FileReader(temp))) {
            accessWidenerReaderReturnable.read(readerReturnable);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read project access widener file");
        }
        if (outputFile.exists()) outputFile.delete();
        temp.renameTo(outputFile);
        accessWidener = accessWidenerReturnable;
        return this;
    }
}
