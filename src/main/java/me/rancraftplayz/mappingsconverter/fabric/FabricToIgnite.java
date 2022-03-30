package me.rancraftplayz.mappingsconverter.fabric;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
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
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class FabricToIgnite {
    public LoaderModMetadata modMetadata;
    public AccessWidenerRemapper awRemapper;
    public EntryPointer entryPointer;
    public MixinReferenceRemapper mixinReferenceRemapper;

    public FabricToIgnite(@Nullable MixinReferenceRemapper mixinReferenceRemapper, @Nullable AccessWidenerRemapper remapper, @Nullable EntryPointer entryPointer) {
        this.awRemapper = remapper;
        this.entryPointer = entryPointer;
        this.mixinReferenceRemapper = mixinReferenceRemapper;
    }

    public File convert(File file, File outputDir, File tempDir, boolean metaverse, String mcVersion, boolean isDevelopment) throws IOException, ParseMetadataException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        File jarFile = new File(outputDir, file.getName());
        if (!exists(jarFile.exists(), metaverse)) {
            if (tempDir.exists()) tempDir.delete();
            tempDir.mkdirs();
            JarUnpacker unpacker = new JarUnpacker();
            unpacker.unpack(file.getPath(), tempDir.getPath());

            modMetadata = readModMetadata(new FileInputStream(new File(tempDir, "fabric.mod.json")));
            if (entryPointer != null) entryPointer.loadMetadata(modMetadata);
            if (modMetadata.getEnvironment().matches(EnvType.SERVER)) {
                if (!modMetadata.getJars().isEmpty()) {
                    File metaInf = new File(tempDir, "META-INF/jars");
                    File[] files = metaInf.listFiles();

                    for (File f : files) {
                        if (FilenameUtils.getExtension(f.getName()).equals("jar")) {
                            File omg = new File(tempDir, "META-INF/jars/" + "temp/");
                            convert(f, metaInf, omg, true, mcVersion, isDevelopment);
                        }
                    }
                }
                File metaInf = new File(tempDir, "META-INF/");
                File[] files = metaInf.listFiles();

                for (File f : files) {
                    if (FilenameUtils.getExtension(f.getName()).equals("SF") || FilenameUtils.getExtension(f.getName()).equals("RSA")) {
                        f.delete();
                    }
                }
                convertJson(new FileInputStream(new File(tempDir, "fabric.mod.json")), new File(tempDir, "ignite.mod.json"));
                convertMixinJsons(new FileInputStream(new File(tempDir, "fabric.mod.json")), tempDir);

                if (isDevelopment) {
                    return map(jarFile, tempDir, mcVersion, "mojang", metaverse, file, outputDir, isDevelopment, modMetadata.getAccessWidener());
                } else {
                    return map(jarFile, tempDir, mcVersion, "spigot", metaverse, file, outputDir, isDevelopment, modMetadata.getAccessWidener());
                }
            } else {
                FileUtils.copyFile(file, jarFile);
            }
        }
        return jarFile;
    }

    public void convertMixinJsons(InputStream is, File mixinLocation) throws ParseMetadataException, IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        if (modMetadata == null) {
            modMetadata = readModMetadata(is);
            if (entryPointer != null) entryPointer.loadMetadata(modMetadata);
        }

        modMetadata.getMixinConfigs(EnvType.SERVER).forEach(mixinConfig -> {
            File mixinJson = new File(mixinLocation, mixinConfig);
            if (mixinJson.exists()) {
                try {
                    com.google.gson.stream.JsonReader reader = new com.google.gson.stream.JsonReader(new FileReader(mixinJson));
                    JsonObject jsonObject = new JsonParser().parse(reader).getAsJsonObject();
                    reader.close();
                    if (jsonObject.has("client")) {
                        jsonObject.remove("client");
                    }
                    if (jsonObject.has("compatibilityLevel")) {
                        jsonObject.remove("compatibilityLevel");
                    }
                    if (!jsonObject.has("minVersion")) {
                        jsonObject.add("minVersion", new JsonPrimitive("0.8"));
                    }
                    FileWriter writer = new FileWriter(mixinJson, false);
                    writer.write(jsonObject.toString());
                    writer.flush();
                    writer.close();

                    if (jsonObject.has("refmap")) {
                        String refmap = jsonObject.get("refmap").getAsString();
                        if (mixinReferenceRemapper != null) mixinReferenceRemapper.remap(new File(mixinLocation, refmap));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private File map(File jarFile, File directory, String mcVersion, String to, boolean metaverse, File file, File outputDir, boolean isDevelopment, @Nullable String accessWidenerName) throws IOException, ParseMetadataException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        File accessWidener = null;
        if (accessWidenerName != null) {
            accessWidener = new File(directory, accessWidenerName);
        }

        File minecraftJarIntermediary = new File(new File("cache/"), "minecraft-" + mcVersion + "-intermediary.jar");
        if (!minecraftJarIntermediary.exists()) {
            File uwu = getServerJar(mcVersion, MojangSpigotRemapper.proguardCsrgTiny(mcVersion, new File("cache/mappings/")));
            remap(uwu.toPath(), MojangSpigotRemapper.proguardCsrgTiny(mcVersion, new File("cache/mappings/")), new ArrayList<>(), to, true, "intermediary");
            uwu.renameTo(minecraftJarIntermediary);
        }
        File minecraftJarYarn = new File(new File("cache/"), "minecraft-" + mcVersion + "-yarn.jar");
        if (!minecraftJarYarn.exists()) {
            File uwu = getServerJar(mcVersion, MojangSpigotRemapper.proguardCsrgTiny(mcVersion, new File("cache/mappings/")));
            remap(uwu.toPath(), MojangSpigotRemapper.proguardCsrgTiny(mcVersion, new File("cache/mappings/")), new ArrayList<>(), to, true, "named");
            uwu.renameTo(minecraftJarYarn);
        }
        File minecraftJarMojang = new File(new File("cache/"), "minecraft-" + mcVersion + "-mojang.jar");
        if (!minecraftJarMojang.exists()) {
            File uwu = getServerJar(mcVersion, MojangSpigotRemapper.proguardCsrgTiny(mcVersion, new File("cache/mappings/")));
            remap(uwu.toPath(), MojangSpigotRemapper.proguardCsrgTiny(mcVersion, new File("cache/mappings/")), new ArrayList<>(), to, true, "mojang");
            uwu.renameTo(minecraftJarMojang);
        }
        File minecraftJarSpigot = new File(new File("cache/"), "minecraft-" + mcVersion + "-spigot.jar");
        if (!minecraftJarSpigot.exists()) {
            File uwu = getServerJar(mcVersion, MojangSpigotRemapper.proguardCsrgTiny(mcVersion, new File("cache/mappings/")));
            uwu.renameTo(minecraftJarSpigot);
        }

        JarPacker packer = new JarPacker();
        List<Path> libs = new ArrayList<>();
        libs.add(minecraftJarIntermediary.toPath());
        libs.add(minecraftJarYarn.toPath());
        libs.add(minecraftJarMojang.toPath());
        libs.add(minecraftJarSpigot.toPath());

        if (accessWidener != null) {
            MojangSpigotRemapper.remapAccessWidenerNoNewFile(accessWidener.toPath(), MojangSpigotRemapper.proguardCsrgTiny(mcVersion, new File("cache/mappings/")).toPath(), libs);
//            awRemapper.remap(accessWidener);
        }

        jarFile.getParentFile().mkdirs();

        remap(packer.pack(directory.getAbsolutePath(), jarFile.getAbsolutePath()).toPath(), MojangSpigotRemapper.proguardCsrgTiny(mcVersion, new File("cache/mappings/")), libs, "intermediary", false, "named");
        deleteDirectory(directory);

        remap(jarFile.toPath(), MojangSpigotRemapper.proguardCsrgTiny(mcVersion, new File("cache/mappings/")), libs, "named", false, "intermediary");
        deleteDirectory(directory);

        remap(jarFile.toPath(), MojangSpigotRemapper.proguardCsrgTiny(mcVersion, new File("cache/mappings/")), libs, "mojang", false, "named");
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

    public static File getServerJar(String mcVersion, File mappingsFile) throws IOException {
        File serverJar = new File("cache/", "minecraft-" + mcVersion + "-spigot.jar");
        File cachedServerJar = new File("cache/", "minecraft-" + mcVersion + "-spigot-cached.jar");
        if (!cachedServerJar.exists()) {
            if (!serverJar.exists()) {
                serverJar = remap(getMojangServerJar(mcVersion).toPath(), mappingsFile, serverJar);
            }
            Files.copy(serverJar.toPath(), cachedServerJar.toPath());
        }
        return cachedServerJar;
    }

    private static File remap(Path input, File mappings, File outputFile) throws IOException {
        TinyRemapper remapper = TinyRemapper.newRemapper().withMappings(TinyUtils.createTinyMappingProvider(mappings.toPath(), "official", "spigot")).ignoreConflicts(true).renameInvalidLocals(true).rebuildSourceFilenames(true).resolveMissing(true).build();

        OutputConsumerPath outputConsumer = (new OutputConsumerPath.Builder(outputFile.toPath()).build());
        outputConsumer.addNonClassFiles(input);
        remapper.readInputs(input);

        remapper.apply(outputConsumer);
        outputConsumer.close();
        remapper.finish();
        return outputFile;
    }

    private static File getMojangServerJar(String mcVersion) throws IOException {
        File mojangJar = new File("cache/", "mojang_" + mcVersion + ".jar");
        File serverJar = new File("cache/", "server_" + mcVersion + ".jar");
        if (serverJar.exists()) {
            return serverJar;
        }

        if (mojangJar.exists()) {
            File tempDir = new File("cache/", UUID.randomUUID() + "/");
            if (!tempDir.exists()) tempDir.mkdirs();
            JarUnpacker unpacker = new JarUnpacker();
            unpacker.unpack(mojangJar.getPath(), tempDir.getPath());

            File serverJer = new File(tempDir, "/META-INF/versions/" + mcVersion + "/" + "server-" + mcVersion + ".jar");
            serverJer.renameTo(serverJar);
            tempDir.delete();
            if (tempDir.exists()) deleteDirectory(tempDir);
        } else {
            System.out.println("MINECRAFT JAR NOT FOUND! EXPECT THERE TO BE ISSUES");
            System.out.println("To add the minecraft jar manually get the minecraft jar *it needs to be obfuscated like the official mojang server* and put it in the cache folder and name it server_" + mcVersion + ".jar");
        }
        return serverJar;
    }

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
            if (entryPointer != null) entryPointer.loadMetadata(modMetadata);
        }

        List<String> dependencies = modMetadata.getDependencies().stream().filter(d -> d.getKind() == ModDependency.Kind.DEPENDS).map(ModDependency::getModId).collect(java.util.stream.Collectors.toList());
        List<String> accessWideners = new ArrayList<>();
        List<String> actualDependencies = new ArrayList<>();

        dependencies.forEach(s -> {
            if (Objects.equals(s, "fabricloader") || Objects.equals(s, "fabric-loader")) {
                actualDependencies.add("!ignited-fabricloader");
                return;
            }
            if (Objects.equals(s, "java") || Objects.equals(s, "minecraft")) {
                return;
            }
            actualDependencies.add(s);
        });
        if (modMetadata.getAccessWidener() != null) {
            accessWideners.add(modMetadata.getAccessWidener());
        }

        IgniteModJson igniteModJson = new IgniteModJson(modMetadata.getId(), modMetadata.getVersion().getFriendlyString(), null, null, actualDependencies, new ArrayList<>(modMetadata.getMixinConfigs(EnvType.SERVER)), accessWideners);
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
