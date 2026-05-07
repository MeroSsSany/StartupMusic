package dev.merosssany.musicaltone.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.merosssany.musicaltone.StartupMusicalTone;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("startupmusicmod.json").toFile();
    
    // The actual data object
    public static ConfigData instance = new ConfigData();
    
    public static class ConfigData {
        // "Basic" Section
        public List<String> volume = new ArrayList<>();
        public List<String> probability = new ArrayList<>();
        
        // "Advanced" Section
        public int floatsPerBatch = 4096;
    }
    
    // These maintain compatibility with your existing Data.java code
    public static Supplier<List<String>> volume = () -> instance.volume;
    public static Supplier<List<String>> probability = () -> instance.probability;
    public static Supplier<Integer> floatsPerSample = () -> instance.floatsPerBatch;
    
    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save(); // Create default file if it doesn't exist
        } else {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                instance = GSON.fromJson(reader, ConfigData.class);
                validate(); // Ensure users didn't put weird values in the JSON
            } catch (IOException e) {
                System.err.println("Failed to load config, using defaults.");
            }
        }
        
        Data.load();
    }
    
    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Manual validation since we don't have ForgeConfigSpec.Builder anymore.
     */
    private static void validate() {
        if (instance.floatsPerBatch < 1024 || instance.floatsPerBatch > 1048576) {
            instance.floatsPerBatch = 4096;
        }
        // You can add more logic here to scrub the lists if needed
    }
}