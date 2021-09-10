package me.rancraftplayz.mappingsconverter;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

/**
 * Original code from fabric-loom (https://github.com/FabricMC/fabric-loom)
 * Licensed under the MIT License
 */

public class Checksum {

    public static boolean equals(File file, String checksum) {
        if (file == null || !file.exists()) {
            return false;
        }

        try {
            HashCode hash = Files.asByteSource(file).hash(Hashing.sha1());
            return hash.toString().equals(checksum);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static byte[] sha256(File file) {
        try {
            HashCode hash = Files.asByteSource(file).hash(Hashing.sha256());
            return hash.asBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get file hash");
        }
    }

    public static String truncatedSha256(File file) {
        try {
            HashCode hash = Files.asByteSource(file).hash(Hashing.sha256());
            return hash.toString().substring(0, 12);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to get file hash of " + file, e);
        }
    }
}
