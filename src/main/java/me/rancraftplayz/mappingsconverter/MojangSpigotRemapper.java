package me.rancraftplayz.mappingsconverter;

import net.fabricmc.lorenztiny.TinyMappingsWriter;
import net.fabricmc.stitch.commands.tinyv2.CommandMergeTinyV2;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.io.proguard.ProGuardReader;
import org.cadixdev.lorenz.io.srg.csrg.CSrgReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MojangSpigotRemapper {
    public static void main(String[] args) throws IOException {
        try {
            File inputFile = new File(args[0]);
            File proguardFile = new File(args[1]);
            File csrgFile = new File(args[2]);
            if (inputFile.exists()) {
                List<Path> libs = new ArrayList<>();
                libs.add(new File(args[3]).toPath());
                remap(inputFile.toPath(), proguardFile.toPath(), csrgFile.toPath(), libs);
                return;
            }
        } catch (ArrayIndexOutOfBoundsException exception) {
            exception.printStackTrace();
        }
        System.out.println("File does not exist or not found! (Or it's some other error idk)");
    }

    public static void remap(Path input, Path proguardPath, Path csrgPath, List<Path> libraries) throws IOException {
        File proguard = proguardPath.toFile();
        File csrg = csrgPath.toFile();

        File mappings = proguardCsrgTiny(proguard, csrg, input.toFile());

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

    private static File proguardCsrgTiny(File proguard, File csrg, File outputPath) throws IOException {
        MappingSet proguardMappings = new ProGuardReader(Files.newBufferedReader(proguard.toPath())).read();
        MappingSet csrgMappings = new CSrgReader(Files.newBufferedReader(csrg.toPath())).read();

        try (TinyMappingsWriter pWriter = new TinyMappingsWriter(Files.newBufferedWriter(Path.of(proguard.toPath() + "-out.tiny")), "official", "mojang")) {
            pWriter.write(proguardMappings.reverse());
        }
        try (TinyMappingsWriter cWriter = new TinyMappingsWriter(Files.newBufferedWriter(Path.of(csrg.toPath() + "-out.tiny")), "official", "spigot")) {
            cWriter.write(csrgMappings);
        }

        File output = new File(outputPath.getParent() + "/proguard-csrg.tiny");

        return mergeTinyV2(new File(String.valueOf(Path.of(proguard.toPath() + "-out.tiny"))), new File(String.valueOf(Path.of(csrg.toPath() + "-out.tiny"))), output);
    }

    // Don't mind me just going to use stitch in a scuffed way
    private static File mergeTinyV2(File mergeA, File mergeB, File output) throws IOException {
        String[] args = new String[]{mergeA.getPath(), mergeB.getPath(), output.getPath()};
        new CommandMergeTinyV2().run(args);

        return output;
    }
}
