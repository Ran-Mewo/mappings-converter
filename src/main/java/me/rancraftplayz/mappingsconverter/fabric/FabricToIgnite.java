package me.rancraftplayz.mappingsconverter.fabric;

import com.google.gson.Gson;
import fr.stevecohen.jarmanager.JarPacker;
import fr.stevecohen.jarmanager.JarUnpacker;
import me.ran.mappings_stuff_idk.util.Spigot;
import me.rancraftplayz.mappingsconverter.MojangSpigotRemapper;
import me.rancraftplayz.mappingsconverter.fabric.metadata.LoaderModMetadata;
import me.rancraftplayz.mappingsconverter.fabric.metadata.ParseMetadataException;
import me.rancraftplayz.mappingsconverter.fabric.metadata.V0ModMetadataParser;
import me.rancraftplayz.mappingsconverter.fabric.metadata.V1ModMetadataParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.impl.lib.gson.JsonReader;
import net.fabricmc.loader.impl.lib.gson.JsonToken;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.fabricmc.modified.OutputConsumerPath;
import net.fabricmc.modified.TinyRemapper;
import net.fabricmc.modified.TinyUtils;
import net.fabricmc.modified.extension.mixin.MixinExtension;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FabricToIgnite {
//    public static void main(String[] args) throws ParseMetadataException, IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
//        convert(new File("lithium.jar"), new File("outputJar"), new File("temp"), false, "1.18.2", false);
//    }
    public LoaderModMetadata modMetadata;
    public AccessWidenerRemapper awRemapper;
    public EntryPointer entryPointer;

    public FabricToIgnite(AccessWidenerRemapper remapper, EntryPointer entryPointer) {
        this.awRemapper = remapper;
        this.entryPointer = entryPointer;
    }

    public File convert(File file, File outputDir, File tempDir, boolean metaverse, String mcVersion, boolean isDevelopment) throws IOException, ParseMetadataException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        File jarFile = new File(outputDir, file.getName());
        if (!exists(jarFile.exists(), metaverse)) {
            if (tempDir.exists()) tempDir.delete();
            tempDir.mkdirs();
            JarUnpacker unpacker = new JarUnpacker();
            unpacker.unpack(file.getPath(), tempDir.getPath());

            modMetadata = readModMetadata(new FileInputStream(new File(tempDir, "fabric.mod.json")));
            entryPointer.loadMetadata(modMetadata);
            if (!modMetadata.getJars().isEmpty()) {
                File metaInf = new File(tempDir, "META-INF/jars");
                File[] files = metaInf.listFiles();

                for (File f : files) {
                    if (FilenameUtils.getExtension(f.getName()).equals("jar")) {
                        File omg = new File(tempDir, "META-INF/jars/" + "temp/");
                        convert(f, outputDir, omg, true, mcVersion, isDevelopment);
                    }
                }
            }
            convertJson(new FileInputStream(new File(tempDir, "fabric.mod.json")), new File(tempDir, "ignite.mod.json"));

            if (isDevelopment) {
                return map(jarFile, tempDir, mcVersion, "mojang", metaverse, file, outputDir, isDevelopment, modMetadata.getAccessWidener());
            } else {
                return map(jarFile, tempDir, mcVersion, "spigot", metaverse, file, outputDir, isDevelopment, modMetadata.getAccessWidener());
            }
        }
        return jarFile;
    }

//    static List<File> classFiles = new ArrayList<>();
    private File map(File jarFile, File directory, String mcVersion, String to, boolean metaverse, File file, File outputDir, boolean isDevelopment, @Nullable String accessWidenerName) throws IOException, ParseMetadataException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
//        if (!classFiles.isEmpty()) classFiles = new ArrayList<>();
//        findClassFiles(directory);
//
//        for (File clazz : classFiles) {
//            System.out.println(clazz.getName());
//            TinyRemapper remapper = TinyRemapper.newRemapper().withMappings(TinyUtils.createTinyMappingProvider(MojangSpigotRemapper.proguardCsrgTiny(mcVersion, new File("mappings/")).toPath(), "intermediary", to)).extension(new MixinExtension()).resolveMissing(true).build();
//
//            File tempClass = Paths.get(clazz + ".temp.class").toFile();
//            OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(tempClass.toPath()).build();
//            remapper.readInputs(clazz.toPath());
//            remapper.apply(outputConsumer);
//            outputConsumer.close();
//            remapper.finish();
//
//            Files.deleteIfExists(clazz.toPath());
//            tempClass.renameTo(clazz);
//        }
        File accessWidener = null;
        if (accessWidenerName != null) {
            accessWidener = new File(directory, accessWidenerName);
        }

        File minecraftJarIntermediary = new File(new File("cache/"), "minecraft-" + mcVersion + "-intermediary.jar");
        if (!minecraftJarIntermediary.exists()) {
            File uwu = Spigot.downloadServerJar(mcVersion);
            remap(uwu.toPath(), MojangSpigotRemapper.proguardCsrgTiny(mcVersion, new File("cache/mappings/")), new ArrayList<>(), to, true, "intermediary");
            uwu.renameTo(minecraftJarIntermediary);
        }
        File minecraftJarYarn = new File(new File("cache/"), "minecraft-" + mcVersion + "-yarn.jar");
        if (!minecraftJarYarn.exists()) {
            File uwu = Spigot.downloadServerJar(mcVersion);
            remap(uwu.toPath(), MojangSpigotRemapper.proguardCsrgTiny(mcVersion, new File("cache/mappings/")), new ArrayList<>(), to, true, "named");
            uwu.renameTo(minecraftJarYarn);
        }
        File minecraftJarMojang = new File(new File("cache/"), "minecraft-" + mcVersion + "-mojang.jar");
        if (!minecraftJarMojang.exists()) {
            File uwu = Spigot.downloadServerJar(mcVersion);
            remap(uwu.toPath(), MojangSpigotRemapper.proguardCsrgTiny(mcVersion, new File("cache/mappings/")), new ArrayList<>(), to, true, "mojang");
            uwu.renameTo(minecraftJarMojang);
        }
        File minecraftJarSpigot = new File(new File("cache/"), "minecraft-" + mcVersion + "-spigot.jar");
        if (!minecraftJarSpigot.exists()) {
            File uwu = Spigot.downloadServerJar(mcVersion);
            uwu.renameTo(minecraftJarSpigot);
        }

        JarPacker packer = new JarPacker();
        List<Path> libs = new ArrayList<>();
        libs.add(minecraftJarIntermediary.toPath());
        libs.add(minecraftJarYarn.toPath());
        libs.add(minecraftJarMojang.toPath());
        libs.add(minecraftJarSpigot.toPath());

        if (accessWidener != null) {
//            MojangSpigotRemapper.remapAccessWidenerNoNewFile(accessWidener.toPath(), MojangSpigotRemapper.proguardCsrgTiny(mcVersion, new File("cache/mappings/")).toPath(), libs);
            awRemapper.remap(accessWidener);
        }

        jarFile.getParentFile().mkdirs();

        remap(packer.pack(directory.getAbsolutePath(), jarFile.getAbsolutePath()).toPath(), MojangSpigotRemapper.proguardCsrgTiny(mcVersion, new File("cache/mappings/")), libs, to, false, "intermediary");

//        classFiles = new ArrayList<>();
        deleteDirectory(directory);
        remap(jarFile.toPath(), MojangSpigotRemapper.proguardCsrgTiny(mcVersion, new File("cache/mappings/")), libs, to, false, "named");
        deleteDirectory(directory);
        remap(jarFile.toPath(), MojangSpigotRemapper.proguardCsrgTiny(mcVersion, new File("cache/mappings/")), libs, to, false, "mojang");
        deleteDirectory(directory);
        return jarFile;
    }

    private void remap(Path input, File mappings, List<Path> libraries, String to, boolean minecraftServer, String intermediary) throws IOException {
        File tempJar = Paths.get(input + ".temp.jar").toFile();

        TinyRemapper remapper;
        if (minecraftServer) {
            remapper = TinyRemapper.newRemapper().withMappings(TinyUtils.createTinyMappingProvider(mappings.toPath(), "spigot", intermediary)).ignoreConflicts(true).renameInvalidLocals(true).rebuildSourceFilenames(true).resolveMissing(true).build();
        } else {
            remapper = TinyRemapper.newRemapper().withMappings(TinyUtils.createTinyMappingProvider(mappings.toPath(), intermediary, to)).extension(new MixinExtension()).ignoreConflicts(true).renameInvalidLocals(true).rebuildSourceFilenames(true).resolveMissing(true).build();
        }

        OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(Paths.get(input + ".temp.jar")).build();
        outputConsumer.addNonClassFiles(input);
        remapper.readInputs(input);

        for (Path path : libraries) {
            remapper.readClassPath(path);
        }

        remapper.apply(outputConsumer);
        outputConsumer.close();
        remapper.finish();

        Files.deleteIfExists(input);
        tempJar.renameTo(input.toFile());
    }

//    private static void findClassFiles(File directory) {
//        File[] classes = directory.listFiles();
//        for (File file : classes) {
//            if (FilenameUtils.getExtension(file.getName()).equals("class")) {
//                classFiles.add(file);
//            }
//            if (file.isDirectory()) {
//                findClassFiles(file);
//            }
//        }
//    }

    static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    private static boolean exists(boolean exists, boolean metaverse) {
        if (metaverse) {
            return false;
        }
        return exists;
    }

    public void convertJson(InputStream is, File outputFile) throws ParseMetadataException, IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        if (modMetadata == null) {
            modMetadata = readModMetadata(is);
            entryPointer.loadMetadata(modMetadata);
        }

        List<String> dependencies = modMetadata.getDependencies().stream().filter(d -> d.getKind() == ModDependency.Kind.DEPENDS).map(ModDependency::getModId).collect(java.util.stream.Collectors.toList());
        List<String> accessWideners = new ArrayList<>();
        List<String> actualDependencies = new ArrayList<>();

        dependencies.forEach(s -> {
            if (Objects.equals(s, "fabricloader") || Objects.equals(s, "fabric-loader")) {
//                dependencies.remove(s);
                actualDependencies.add("ignited-fabricloader");
                return;
            }
            if (Objects.equals(s, "java") || Objects.equals(s, "minecraft")) {
//                dependencies.remove(s);
                return;
            }
            actualDependencies.add(s);
        });
        accessWideners.add(modMetadata.getAccessWidener());

        IgniteModJson igniteModJson = new IgniteModJson(modMetadata.getId(), modMetadata.getVersion().getFriendlyString(), null, actualDependencies, null, modMetadata.getMixinConfigs(EnvType.SERVER).stream().toList(), accessWideners);
        // Convert IgniteModJson to json
        Gson gson = new Gson();
        String json = gson.toJson(igniteModJson);
        if (outputFile.exists()) outputFile.delete();
        outputFile.createNewFile();
        FileWriter fw = new FileWriter(outputFile);
        fw.write(json);
        fw.flush();
        fw.close();
    }

    // All the code below and in the metadata folder are from the Fabric Loader
    // Which is licensed under Apache License 2.0

    public static LoaderModMetadata readModMetadata(InputStream is) throws IOException, ParseMetadataException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        // So some context:
        // Per the json specification, ordering of fields is not typically enforced.
        // Furthermore we cannot guarantee the `schemaVersion` is the first field in every `fabric.mod.json`
        //
        // To work around this, we do the following:
        // Try to read first field
        // If the first field is the schemaVersion, read the file normally.
        //
        // If the first field is not the schema version, fallback to a more exhaustive check.
        // Read the rest of the file, looking for the `schemaVersion` field.
        // If we find the field, cache the value
        // If there happens to be another `schemaVersion` that has a differing value, then fail.
        // At the end, if we find no `schemaVersion` then assume the `schemaVersion` is 0
        // Re-read the JSON file.
        int schemaVersion = 0;

        try (JsonReader reader = new JsonReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            reader.setRewindEnabled(true);

            if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                throw new ParseMetadataException("Root of \"fabric.mod.json\" must be an object", reader);
            }

            reader.beginObject();

            boolean firstField = true;

            while (reader.hasNext()) {
                // Try to read the schemaVersion
                String key = reader.nextName();

                if (key.equals("schemaVersion")) {
                    if (reader.peek() != JsonToken.NUMBER) {
                        throw new ParseMetadataException("\"schemaVersion\" must be a number.", reader);
                    }

                    schemaVersion = reader.nextInt();

                    if (firstField) {
                        reader.setRewindEnabled(false);
                        // Finish reading the metadata
                        LoaderModMetadata ret = readModMetadata(reader, schemaVersion);
                        reader.endObject();

                        return ret;
                    }

                    // schemaVersion found, but after some content -> start over to parse all data with the detected version
                    break;
                } else {
                    reader.skipValue();
                }

//                if (!IGNORED_KEYS.contains(key)) {
//                    firstField = false;
//                }
            }

            // Slow path, schema version wasn't specified early enough, re-read with detected/inferred version

            reader.rewind();
            reader.setRewindEnabled(false);

            reader.beginObject();
            LoaderModMetadata ret = readModMetadata(reader, schemaVersion);
            reader.endObject();

            if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
                Log.warn(LogCategory.METADATA, "\"fabric.mod.json\" from mod %s did not have \"schemaVersion\" as first field.", ret.getId());
            }

            return ret;
        }
    }

    private static LoaderModMetadata readModMetadata(JsonReader reader, int schemaVersion) throws IOException, ParseMetadataException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        switch (schemaVersion) {
            case 1:
                return V1ModMetadataParser.parse(reader);
            case 0:
                return V0ModMetadataParser.parse(reader);
            default:
                if (schemaVersion > 0) {
                    throw new ParseMetadataException(String.format("This version of fabric-loader doesn't support the newer schema version of \"%s\""
                            + "\nPlease update fabric-loader to be able to read this.", schemaVersion));
                }

                throw new ParseMetadataException(String.format("Invalid/Unsupported schema version \"%s\" was found", schemaVersion));
        }
    }
}
