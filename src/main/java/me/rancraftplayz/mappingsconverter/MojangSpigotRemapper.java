package me.rancraftplayz.mappingsconverter;

import me.ran.mappings_stuff_idk.MappingsGenerator;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MojangSpigotRemapper {
    public static void remapAll(Path input, File mappingsDir, String mcVersion, List<Path> libraries, @Nullable Path accessWidener,@Nullable List<Path> accessWidnerLibs) throws IOException {
        File mappings = proguardCsrgTiny(mcVersion, mappingsDir);
        if (accessWidener != null) {
            if (accessWidnerLibs == null) accessWidnerLibs = new ArrayList<>();
            remapAccessWidener(input, accessWidener, mappings.toPath(), accessWidnerLibs);
        }
        remap(input, mappings, libraries);
    }

    public static void remapAccessWidener(Path inputJar, Path input, Path mappings, List<Path> libs) throws IOException {
        File output = Paths.get(input.toFile().getParent() + "/" + org.apache.commons.io.FilenameUtils.removeExtension(input.toFile().getName()) + "-obf.accesswidener").toFile();
        if (output.exists()) {
            output.delete();
        }

        File accessWidener = new RemapAccessWidener(input.toFile(), output).remap(mappings.toFile(), libs, true).outputFile;

        ZipUtils.addFilesToZip(inputJar.toFile(), new File[]{accessWidener});
        output.delete();
    }

    public static void remapAccessWidenerNoNewFile(Path input, Path mappings, List<Path> libs) throws IOException {
        new RemapAccessWidener(input.toFile(), input.toFile()).remap(mappings.toFile(), libs, true);
    }

    public static void remap(Path input, File mappingsDir, String mcVersion, List<Path> libraries) throws IOException {
        File mappings = proguardCsrgTiny(mcVersion, mappingsDir);

        File tempJar = Paths.get(input + ".temp.jar").toFile();

        TinyRemapper remapper = TinyRemapper.newRemapper().withMappings(TinyUtils.createTinyMappingProvider(mappings.toPath(), "mojang", "spigot")).extension(new MixinExtension()).ignoreConflicts(true).renameInvalidLocals(true).rebuildSourceFilenames(true).resolveMissing(true).build();

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

    public static void remap(Path input, File mappings, List<Path> libraries) throws IOException {
        File tempJar = Paths.get(input + ".temp.jar").toFile();

        TinyRemapper remapper = TinyRemapper.newRemapper().withMappings(TinyUtils.createTinyMappingProvider(mappings.toPath(), "mojang", "spigot")).extension(new MixinExtension()).ignoreConflicts(true).renameInvalidLocals(true).rebuildSourceFilenames(true).resolveMissing(true).build();

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

    public static File proguardCsrgTiny(String mcVersion, File outputPath) throws IOException {
        return MappingsGenerator.generateMojmapSpigotMappings(mcVersion, outputPath);
    }
}
