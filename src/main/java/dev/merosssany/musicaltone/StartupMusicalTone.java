package dev.merosssany.musicaltone;

import java.io.File;
import java.io.IOException;

import dev.merosssany.musicaltone.core.AudioThread;
import dev.merosssany.musicaltone.data.AudioReader;
import dev.merosssany.musicaltone.data.Config;
import dev.merosssany.musicaltone.data.Data;
import dev.merosssany.musicaltone.data.factory.DefaultFactories;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.merosssany.musicaltone.core.AudioPlayer;
import dev.merosssany.musicaltone.core.FileManager;

public class StartupMusicalTone implements ClientModInitializer {
    public static final String modId = "startupmusicmod";
    private static final Logger logger = LoggerFactory.getLogger(modId);
    
    private static boolean isPlaying = false;
    private static AudioThread thread;
    
    @Override
    public void onInitializeClient() {
        logger.info("Initializing Startup Music Tone...");
        DefaultFactories.register();
        Config.load();
        
        thread = new AudioThread(new AudioPlayer());
        
        try {
            // Check if folder is valid
            FileManager.getRandomFileFrom(FileManager.getMusicFolder(), AudioReader.getSupportedFiles());
        } catch (IOException e) {
            showError("Failed to access music folder", e);
        }
        
        StartupMusicalTone.startPlaying();
    }
    
    private static void playMusic() {
        String trackKey = Data.getRandomTrack();
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
                float vol = (float) Data.volume.getOrDefault(finalKey, 100) / 100f;
                thread.getPlayer().setVolume(vol);
            });
            
        } catch (IOException e) {
            logger.warn("Failed to play music file", e);
            showError(e);
        }
    }
    
    // Fabric doesn't have a ModLoadingWarning GUI, so we use standard logging.
    public static void showError(Exception e) {
        logger.error("An error has happened: {}", e.getMessage(), e);
    }
    
    public static void showError(String msg) {
        logger.error("An error has happened: {}", msg);
    }
    
    public static void showError(String msg, Exception e) {
        logger.error("An error has happened: {}", msg, e);
    }
    
    public static void stopMusic() {
        if (!isPlaying) return;
        if (thread != null) {
            thread.addTask(() -> {
                thread.getPlayer().startFadeOut(1.5f);
                thread.finalizeAudio();
            });
        }
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
                    Thread.currentThread().interrupt();
                }
            });
            
        } catch (Exception e) {
            showError(e);
        }
    }
}