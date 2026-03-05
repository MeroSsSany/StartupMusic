package dev.merosssany.musicaltone;

import java.io.File;
import java.io.IOException;

import dev.merosssany.musicaltone.core.AudioThread;
import dev.merosssany.musicaltone.data.AudioReader;
import dev.merosssany.musicaltone.data.Config;
import dev.merosssany.musicaltone.data.Data;
import dev.merosssany.musicaltone.data.factory.DefaultFactories;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.merosssany.musicaltone.core.AudioPlayer;
import dev.merosssany.musicaltone.core.FileManager;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.ModLoadingWarning;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import static dev.merosssany.musicaltone.data.Config.CONFIG_SPEC;

@Mod(StartupMusicalTone.modId)
public class StartupMusicalTone {
    public static final String modId = "startupmusicmod";
    private static final Logger logger = LogUtils.getLogger();
    
    private static boolean isPlaying = false;
    private static AudioThread thread;
    
    public StartupMusicalTone(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.CLIENT, CONFIG_SPEC);
        DefaultFactories.register();
        
        thread = new AudioThread(new AudioPlayer());
        
        try {
            FileManager.getRandomFileFrom(FileManager.getMusicFolder(), AudioReader.getSupportedFiles());
        } catch (IOException e) {
            showError(e);
            return;
        }
        
        context.getModEventBus().addListener(this::onLoadingComplete);
    }
    
    private static void playMusic() {
        String trackKey = Data.getRandomTrack(); // This is the filename from config (e.g. "theme.ogg")
        File file;
        
        try {
            if (trackKey == null || trackKey.isEmpty()) {
                file = FileManager.getRandomFileFrom(FileManager.getMusicFolder(), AudioReader.getSupportedFiles());
                // If it's a random file NOT in the config, use its actual name for volume lookup
                trackKey = file.getName();
            } else {
                file = FileManager.getFile(trackKey);
            }
            
            logger.info("Streaming \"{}\"...", file.getName());
            thread.startStream(file);
            
            // Final local variable for the lambda
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
    
    @SuppressWarnings("removal")
    public static void showError(Exception e) {
        logger.error("An error has happened", e);
        
        ModLoader.get().addWarning(new ModLoadingWarning(ModLoadingContext.get().getActiveContainer().getModInfo(),
                ModLoadingStage.CONSTRUCT, "Startup Musical Tone failed to load ogg file: " + e.getMessage(), e
        ));
    }
    
    @SuppressWarnings("removal")
    public static void showError(String msg) {
        logger.error("An error has happened: {}", msg);
        
        ModLoader.get().addWarning(new ModLoadingWarning(ModLoadingContext.get().getActiveContainer().getModInfo(),
                ModLoadingStage.CONSTRUCT, "Startup Musical Tone: " + msg
        ));
    }
    
    @SuppressWarnings("removal")
    public static void showError(String msg, Exception e) {
        logger.error("An error has happened: {}", msg, e);
        
        ModLoader.get().addWarning(new ModLoadingWarning(ModLoadingContext.get().getActiveContainer().getModInfo(),
                ModLoadingStage.CONSTRUCT, "Startup Musical Tone: " + msg, e
        ));
    }
    
    public void onLoadingComplete(FMLLoadCompleteEvent event) {
        thread.addTask(() -> {
            thread.getPlayer().startFadeOut(1.5f);
            thread.finalizeAudio();
        });
    }
    
    public static synchronized void startPlaying() {
        if (isPlaying) return; // Should only run once per session
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
            showError(e);
        }
    }
}
