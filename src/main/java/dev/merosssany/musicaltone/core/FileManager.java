package dev.merosssany.musicaltone.core;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

public class FileManager {
    private static final Logger logger = LogUtils.getLogger();
    
    public static Path geConfigDir() {
        // Still returns the /config folder path
        return FMLPaths.CONFIGDIR.get();
    }
    
    public static void createFolder(String folderName, Path path) throws IOException {
        logger.debug("Creating folder {} at path: {}", folderName, path);
        Files.createDirectories(path.resolve(folderName));
    }
    
    public static void init() throws IOException {
        // Simply calling getMusicFolder will ensure it exists
        getMusicFolder();
    }
    
    public static File getFile(String path) throws IOException {
        Path musicDir = getMusicFolder();
        File file = musicDir.resolve(path).toFile();
        if (file.exists()) return file;
        
        throw new IOException("File does not exist at path: " + file.getAbsolutePath());
    }
    
    public static Path getMusicFolder() throws IOException {
        // Using .resolve is cleaner than string concatenation
        Path musicPath = geConfigDir().resolve("music");
        if (Files.notExists(musicPath)) {
            Files.createDirectories(musicPath);
        }
        return musicPath;
    }
    
    public static File[] getAllFilesFrom(Path path, FilenameFilter consumer) throws IOException {
        File folder = path.toFile();
        if (!folder.isDirectory()) throw new IOException("Not a directory: " + path);
        
        return folder.listFiles(consumer);
    }
    
    public static File getRandomFileFrom(Path path, Collection<String> filter) throws IOException {
        File[] files = getAllFilesFrom(path, (dir, name) -> {
            int lastDot = name.lastIndexOf('.');
            if (lastDot == -1) return false;
            String ext = name.substring(lastDot + 1).toLowerCase();
            return filter.contains(ext);
        });
        
        if (files == null || files.length == 0) {
            throw new IOException("No supported music files found at: " + path.toAbsolutePath());
        }
        
        int index = (int) (Math.random() * files.length);
        return files[index];
    }
}