package dev.merosssany.musicaltone.data;

import com.mojang.logging.LogUtils;
import dev.merosssany.musicaltone.core.FileManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class Data {
    public static final ConcurrentHashMap<String, Integer> probability = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Integer> volume = new ConcurrentHashMap<>();
    private static final Logger logger = LogUtils.getLogger();
    
    public static void getMap(List<String> list, Map<String, Integer> map) {
        for (String entry : list) {
            String[] parts = entry.split(":", 2);
            
            if (parts.length != 2) {
                logger.warn("Invalid entry (no colon found): {}", entry);
                continue;
            }
            
            try {
                String file = parts[0].trim();
                if (Files.exists(FileManager.getMusicFolder().resolve(file)))
                    map.put(file, Integer.parseInt(parts[1].trim()));
                
            } catch (NumberFormatException e) {
                logger.warn("Invalid volume entry: {}", entry);
            } catch (IOException ignored) {}
        }
    }
    
    public static void load() {
        getMap((List<String>) Config.volume.get(), volume);
        getMap((List<String>) Config.probability.get(), probability);
        
        logger.info("Loaded {} tracks with volume, {} tracks with probability", volume.size(), probability.size());
    }
    
    public static String getRandomTrack() {
        if (probability.isEmpty()) {
            logger.warn("No tracks available to select from");
            return null;
        }
        
        int totalWeight = probability.values().stream().mapToInt(Integer::intValue).sum();
        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        
        for (Map.Entry<String, Integer> entry : probability.entrySet()) {
            random -= entry.getValue();
            if (random < 0) {
                return entry.getKey();
            }
        }
        
        return null;
    }
}
