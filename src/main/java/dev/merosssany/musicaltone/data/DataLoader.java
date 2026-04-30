package dev.merosssany.musicaltone.data;

import com.mojang.logging.LogUtils;
import dev.merosssany.musicaltone.core.FileManager;
import net.neoforged.fml.event.config.ModConfigEvent; // New Import
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class DataLoader {
    public static final ConcurrentHashMap<String, Integer> probability = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Integer> volume = new ConcurrentHashMap<>();
    private static final Logger logger = LogUtils.getLogger();
    private static Path musicFolder;
    
    static {
        try {
            musicFolder = FileManager.getMusicFolder();
        } catch (IOException e) {
            musicFolder = FileManager.geConfigDir().resolve("music");
        }
    }
    
    public static void onConfigLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == Config.CONFIG_SPEC) {
            load();
        }
    }
    
    public static void getMap(List<? extends String> list, Map<String, Integer> map) {
        for (String entry : list) {
            String[] parts = entry.split(":", 2);
            if (parts.length != 2) {
                logger.warn("Invalid entry (no colon found): {}", entry);
                continue;
            }
            
            try {
                String fileName = parts[0].trim();
                // 1.21 check: Ensure we handle the Path correctly
                if (Files.exists(musicFolder.resolve(fileName))) {
                    map.put(fileName, Integer.parseInt(parts[1].trim()));
                } else {
                    logger.debug("Skipping config entry, file not found: {}", fileName);
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid number format in entry: {}", entry);
            }
        }
    }
    
    public static void load() {
        volume.clear();
        probability.clear();
        
        getMap(Config.volume.get(), volume);
        getMap(Config.probability.get(), probability);
        
        logger.info("Loaded {} tracks with volume, {} tracks with probability", volume.size(), probability.size());
    }
    
    public static String getRandomTrack() {
        if (probability.isEmpty()) return null;
        
        int totalWeight = probability.values().stream().mapToInt(Integer::intValue).sum();
        if (totalWeight <= 0) return null;
        
        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        for (Map.Entry<String, Integer> entry : probability.entrySet()) {
            random -= entry.getValue();
            if (random < 0) return entry.getKey();
        }
        return null;
    }
}