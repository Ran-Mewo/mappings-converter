package me.rancraftplayz.mappingsconverter;

import me.ran.mappings_stuff_idk.MappingsGenerator;
import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerWriter;
import net.fabricmc.lorenztiny.TinyMappingsWriter;
import net.fabricmc.stitch.commands.tinyv2.CommandMergeTinyV2;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.io.proguard.ProGuardReader;
import org.cadixdev.lorenz.io.srg.csrg.CSrgReader;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MojangSpigotRemapper {
    public static void main(String[] args) throws IOException {
        try {
            File inputFile = new File(args[0]);
            String mcVersion = args[1];
            if (inputFile.exists()) {
                List<Path> libs = new ArrayList<>();
                libs.add(new File(args[2]).toPath());
                remap(inputFile.toPath(), new File("mappings/"), mcVersion, libs);
                return;
            }
        } catch (ArrayIndexOutOfBoundsException exception) {
            exception.printStackTrace();
        }
        System.out.println("File does not exist or not found! (Or it's some other error idk)");
    }

    public static void remapAll(Path input, File mappingsDir, String mcVersion, List<Path> libraries, @Nullable Path accessWidener,@Nullable List<Path> accessWidnerLibs) throws IOException {
        File mappings = proguardCsrgTiny(mcVersion, mappingsDir);
        if (accessWidener != null) {
            remapAccessWidener(input, accessWidener, mappings.toPath(), Objects.requireNonNullElseGet(accessWidnerLibs, ArrayList::new));
        }
        remap(input, mappings, libraries);
    }

    public static void remapAccessWidener(Path inputJar, Path input, Path mappings, List<Path> libs) throws IOException {
        AccessWidener accessWidener = RemapAccessWidener.remap(input.toFile(), mappings.toFile(), libs, true);

        AccessWidenerWriter writer = new AccessWidenerWriter(accessWidener);

        StringWriter wat = new StringWriter();
        writer.write(wat);

        File output = Paths.get(input.toFile().getParent() + "/" + org.apache.commons.io.FilenameUtils.removeExtension(input.toFile().getName()) + "-obf.accesswidener").toFile();
        if (output.exists()) {
            output.delete();
        }
        output.createNewFile();

        FileWriter ono = new FileWriter(output);
        ono.write(wat.toString());
        ono.close();

        ZipUtils.addFilesToZip(inputJar.toFile(), new File[]{output});
        output.delete();
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
