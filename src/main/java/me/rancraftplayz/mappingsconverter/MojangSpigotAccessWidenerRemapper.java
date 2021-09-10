package me.rancraftplayz.mappingsconverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class MojangSpigotAccessWidenerRemapper {
    public static void remap(Path input, Path proguardPath, Path csrgPath, List<Path> libraries, Path output) throws IOException {
        File proguard = proguardPath.toFile();
        File csrg = csrgPath.toFile();

        File mappings = MojangSpigotRemapper.proguardCsrgTiny(proguard, csrg, output.toFile());

        ApplyAccessWidener applier = new ApplyAccessWidener(input.toFile(), mappings, libraries);

        for (Path libPath : libraries) {
            applier.apply(libPath.toFile());
        }
    }

    public static void remap(Path input, Path mappings, List<Path> libraries) {
        ApplyAccessWidener applier = new ApplyAccessWidener(input.toFile(), mappings.toFile(), libraries);

        for (Path libPath : libraries) {
            applier.apply(libPath.toFile());
        }
    }
}
