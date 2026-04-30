package dev.merosssany.musicaltone;

import com.mojang.logging.LogUtils;
import dev.merosssany.musicaltone.core.AudioPlayer;
import dev.merosssany.musicaltone.core.AudioThread;
import dev.merosssany.musicaltone.core.FileManager;
import dev.merosssany.musicaltone.data.AudioReader;
import dev.merosssany.musicaltone.data.Config;
import dev.merosssany.musicaltone.data.DataLoader;
import dev.merosssany.musicaltone.data.factory.DefaultFactories;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;

import static dev.merosssany.musicaltone.data.Config.CONFIG_SPEC;

@Mod(StartupMusicalTone.MOD_ID)
public class StartupMusicalTone {
    public static final String MOD_ID = "startupmusicmod";
    private static final Logger logger = LogUtils.getLogger();
    
    private static boolean isPlaying = false;
    private static AudioThread thread;
    
    public StartupMusicalTone(IEventBus modEventBus, ModContainer container) {
        // Register Config
        container.registerConfig(ModConfig.Type.CLIENT, CONFIG_SPEC);
        
        modEventBus.addListener(DataLoader::onConfigLoad);
        
        DefaultFactories.register();
        
        thread = new AudioThread(new AudioPlayer());
        
        try {
            FileManager.getRandomFileFrom(FileManager.getMusicFolder(), AudioReader.getSupportedFiles());
        } catch (IOException e) {
            logError(e);
            return;
        }
        
        // 2. Register lifecycle event on the injected modEventBus
        modEventBus.addListener(this::onLoadingComplete);
    }
    
    private static void playMusic() {
        String trackKey = DataLoader.getRandomTrack();
        File file;
        
        try {
            if (trackKey == null || trackKey.isEmpty()) {
                file = FileManager.getRandomFileFrom(FileManager.getMusicFolder(), AudioReader.getSupportedFiles());
                trackKey = file.getName();
            } else {
                file = FileManager.getFile(trackKey);
            }
            
            logger.info("Streaming \"{}\"...", file.getName());
            thread.startStream(file);
            
            final String finalKey = trackKey;
            thread.addTask(() -> {
                float vol = (float) DataLoader.volume.getOrDefault(finalKey, 100) / 100f;
                thread.getPlayer().setVolume(vol);
            });
            
        } catch (IOException e) {
            logger.warn("Failed to play music file", e);
            logError(e);
        }
    }
    
    // 3. Updated Error Logging (Old Warning System is Gone)
    public static void logError(Exception e) {
        logger.error("Startup Musical Tone failed to load ogg file: {}", e.getMessage(), e);
    }
    
    public static void logError(String msg) {
        logger.error("Startup Musical Tone: {}", msg);
    }
    
    public void onLoadingComplete(FMLLoadCompleteEvent event) {
        thread.addTask(() -> {
            thread.getPlayer().startFadeOut(1.5f);
            thread.finalizeAudio();
        });
    }
    
    public static synchronized void startPlaying() {
        if (isPlaying) return;
        isPlaying = true;
        
        playMusic();
        thread.start();
        thread.getPlayer().setSamples(Config.floatsPerSample.get());
        
        try {
            FileManager.init();
            thread.setEndCallback(() -> {
                try {
                    Thread.sleep(1000);
                    playMusic();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            logError(e);
        }
    }
}