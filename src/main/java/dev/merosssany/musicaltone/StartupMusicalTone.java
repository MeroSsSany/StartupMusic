package dev.merosssany.musicaltone;

import java.io.IOException;

import dev.merosssany.musicaltone.core.AudioThread;
import dev.merosssany.musicaltone.data.Data;
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

@Mod(StartupMusicalTone.MODID)
public class StartupMusicalTone {
    public static final String MODID = "startupmusicmod";
    
    private static final Logger logger = LogUtils.getLogger();
    
    private final AudioThread thread;
    
    public StartupMusicalTone(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.CLIENT, CONFIG_SPEC);
        
        thread = new AudioThread(new AudioPlayer());
        thread.start();
        
        try {
            FileManager.init();
            playMusic();
            thread.setEndCallback(this::playMusic);
            
        } catch (Exception e) {
            showError(e);
        }
        
        context.getModEventBus().addListener(this::onLoadingComplete);
    }
    
    public void playMusic() {
        String track = Data.getRandomTrack();
        String file;
        
        try {
            if (track == null) {
                file = FileManager.getRandomFileFrom(FileManager.getMusicFolder()).getAbsolutePath();
            } else file = FileManager.getFile(track).getAbsolutePath();
            
            thread.startStream(file);
            String trackName = FileManager.getFile(track).getName();
            thread.addTask(() -> thread.getPlayer().setVolume((float) Data.volume.getOrDefault(trackName, 100) / 100));
            
        } catch (IOException e) {
            logger.warn("Failed to play music file", e);
            showError(e);
        }
    }
    
    @SuppressWarnings("removal")
    private static void showError(Exception e) {
        logger.error("An error has happened", e);
        
        ModLoader.get().addWarning(new ModLoadingWarning(ModLoadingContext.get().getActiveContainer().getModInfo(),
                ModLoadingStage.CONSTRUCT, "Startup Musical Tone failed to load ogg file: " + e.getMessage(), e
        
        ));
    }
    
    @SuppressWarnings("removal")
    private static void showError(String msg, Exception e) {
        logger.error("An error has happened: {}", msg);
        
        ModLoader.get().addWarning(new ModLoadingWarning(ModLoadingContext.get().getActiveContainer().getModInfo(),
                ModLoadingStage.CONSTRUCT, "Startup Musical Tone: " + msg, e
        
        ));
    }
    
    public void onLoadingComplete(FMLLoadCompleteEvent event) {
        thread.stopAudio();
    }
}
