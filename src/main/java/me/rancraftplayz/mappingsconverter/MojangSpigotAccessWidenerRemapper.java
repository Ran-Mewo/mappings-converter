package me.rancraftplayz.mappingsconverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class MojangSpigotAccessWidenerRemapper {
    public static void remap(Path input, String mcVersion, List<Path> libraries) throws IOException {
        File mappings = MojangSpigotRemapper.proguardCsrgTiny(mcVersion, new File("mappings/"));

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
