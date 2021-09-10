package me.rancraftplayz.mappingsconverter;

import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerRemapper;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class RemapAccessWidener {
    public static AccessWidener remap(File accessWidenerFile, File mappings, List<Path> libraries) {
        AccessWidener accessWidener = new AccessWidener();
        AccessWidenerReader accessWidenerReader = new AccessWidenerReader(accessWidener);

        try (BufferedReader reader = new BufferedReader(new FileReader(accessWidenerFile))) {
            accessWidenerReader.read(reader);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read project access widener file");
        }

        if (accessWidener.getNamespace().equals("spigot")) {
            TinyRemapper tinyRemapper = TinyRemapper.newRemapper().withMappings(TinyUtils.createTinyMappingProvider(mappings.toPath(), "spigot", "mojang")).ignoreConflicts(true).renameInvalidLocals(true).rebuildSourceFilenames(true).resolveMissing(true).build();
            for (Path path : libraries) {
                tinyRemapper.readClassPath(path);
            }

            AccessWidenerRemapper remapper = new AccessWidenerRemapper(accessWidener, tinyRemapper.getRemapper(), "mojang");
            accessWidener = remapper.remap();

            tinyRemapper.finish();
        }
        if (accessWidener.getNamespace().equals("official")) {
            TinyRemapper tinyRemapper = TinyRemapper.newRemapper().withMappings(TinyUtils.createTinyMappingProvider(mappings.toPath(), "official", "mojang")).ignoreConflicts(true).renameInvalidLocals(true).rebuildSourceFilenames(true).resolveMissing(true).build();
            for (Path path : libraries) {
                tinyRemapper.readClassPath(path);
            }

            AccessWidenerRemapper remapper = new AccessWidenerRemapper(accessWidener, tinyRemapper.getRemapper(), "mojang");
            accessWidener = remapper.remap();

            tinyRemapper.finish();
        }
        //System.out.println(accessWidener.getNamespace());
        return accessWidener;
    }

    public static AccessWidener remap(File accessWidenerFile, File mappings, List<Path> libraries, boolean forIgnite) {
        if (!forIgnite) {
            return remap(accessWidenerFile, mappings, libraries);
        }

        AccessWidener accessWidener = new AccessWidener();
        AccessWidenerReader accessWidenerReader = new AccessWidenerReader(accessWidener);

        try (BufferedReader reader = new BufferedReader(new FileReader(accessWidenerFile))) {
            accessWidenerReader.read(reader);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read project access widener file");
        }

        if (accessWidener.getNamespace().equals("mojang")) {
            TinyRemapper tinyRemapper = TinyRemapper.newRemapper().withMappings(TinyUtils.createTinyMappingProvider(mappings.toPath(), "mojang", "spigot")).ignoreConflicts(true).renameInvalidLocals(true).rebuildSourceFilenames(true).resolveMissing(true).build();
            for (Path path : libraries) {
                tinyRemapper.readClassPath(path);
            }

            AccessWidenerRemapper remapper = new AccessWidenerRemapper(accessWidener, tinyRemapper.getRemapper(), "spigot");
            accessWidener = remapper.remap();

            tinyRemapper.finish();
        }
        if (accessWidener.getNamespace().equals("official")) {
            TinyRemapper tinyRemapper = TinyRemapper.newRemapper().withMappings(TinyUtils.createTinyMappingProvider(mappings.toPath(), "official", "spigot")).ignoreConflicts(true).renameInvalidLocals(true).rebuildSourceFilenames(true).resolveMissing(true).build();
            for (Path path : libraries) {
                tinyRemapper.readClassPath(path);
            }

            AccessWidenerRemapper remapper = new AccessWidenerRemapper(accessWidener, tinyRemapper.getRemapper(), "spigot");
            accessWidener = remapper.remap();

            tinyRemapper.finish();
        }
        //System.out.println(accessWidener.getNamespace());
        return accessWidener;
    }
}
