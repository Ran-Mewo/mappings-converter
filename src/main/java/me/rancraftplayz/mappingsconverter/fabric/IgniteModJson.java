package me.rancraftplayz.mappingsconverter.fabric;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class IgniteModJson {
    private @SerializedName("id") String id;
    private @SerializedName("version") String version;
    private @SerializedName("original_entry") String entry;
    private @SerializedName("dependencies") List<String> requiredDependencies;
    private @SerializedName("optional_dependencies") List<String> optionalDependencies;
    private @SerializedName("mixins") List<String> mixins;
    private @SerializedName("access_wideners") List<String> accessWideners;

    public IgniteModJson(@NotNull String id,
                     @NotNull String version,
                     @Nullable String entry,
                     @Nullable List<String> requiredDependencies,
                     @Nullable List<String> optionalDependencies,
                     @Nullable List<String> mixins,
                     @Nullable List<String> accessWideners) {
        this.id = id;
        this.version = version;
        this.entry = entry;
        this.requiredDependencies = requiredDependencies;
        this.optionalDependencies = optionalDependencies;
        this.mixins = mixins;
        this.accessWideners = accessWideners;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public String getEntry() {
        return entry;
    }

    public List<String> getRequiredDependencies() {
        return requiredDependencies;
    }

    public List<String> getOptionalDependencies() {
        return optionalDependencies;
    }

    public List<String> getMixins() {
        return mixins;
    }

    public List<String> getAccessWideners() {
        return accessWideners;
    }
}
