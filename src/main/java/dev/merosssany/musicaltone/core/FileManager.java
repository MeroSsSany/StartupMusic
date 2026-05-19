package dev.merosssany.musicaltone.core;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Random;

public class FileManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RANDOM = new Random();
    
    public static Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }
    
    public static Path getMusicFolder() throws IOException {
        Path musicPath = getConfigDir().resolve("music");
        if (Files.notExists(musicPath)) {
            LOGGER.info("Creating music folder at: {}", musicPath);
            Files.createDirectories(musicPath);
        }
        return musicPath;
    }
    
    public static void init() {
        try {
            getMusicFolder(); // Ensures folder exists on startup
        } catch (IOException e) {
            LOGGER.error("Failed to initialize music folder!", e);
        }
    }
    
    public static File getFile(String fileName) throws IOException {
        Path filePath = getMusicFolder().resolve(fileName);
        File file = filePath.toFile();
        
        if (file.exists()) return file;
        throw new IOException("Music file not found: " + file.getAbsolutePath());
    }
    
    public static File getRandomFile(Collection<String> allowedExtensions) throws IOException {
        Path path = getMusicFolder();
        File[] files = path.toFile().listFiles((dir, name) -> {
            String lowercaseName = name.toLowerCase();
            return allowedExtensions.stream().anyMatch(ext -> lowercaseName.endsWith("." + ext.toLowerCase()));
        });
        
        if (files == null || files.length == 0) {
            throw new IOException("No supported music files found in " + path);
        }
        
        return files[RANDOM.nextInt(files.length)];
    }
}