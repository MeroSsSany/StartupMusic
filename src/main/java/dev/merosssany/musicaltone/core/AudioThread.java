package dev.merosssany.musicaltone.core;

import com.mojang.logging.LogUtils;
import org.lwjgl.openal.ALC10;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioThread extends Thread {
    private static final AtomicBoolean started = new AtomicBoolean();
    private static final Logger logger = LogUtils.getLogger();
    
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private final AudioPlayer player;
    private Runnable endCallback;
    
    public AudioThread(AudioPlayer player) {
        super("Music-Audio-Thread");
        this.player = player;
        
        setDaemon(true);
    }
    
    @Override
    public void run() {
        player.init();
        
        try {
            while (running.get()) {
                Runnable task;
                while ((task = tasks.poll()) != null) task.run();
                
                player.updateStreaming();
                player.updateFadeProgress(0.005f);
                
                if (!player.isStreaming() && player.hasTrack()) {
                    player.stop();
                    if (endCallback != null) {
                        endCallback.run();
                    }
                }
                
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ignored) {}
            }
        } finally {
            started.set(false);
            player.cleanup();
            ALC10.alcMakeContextCurrent(0);
        }
    }
    
    @Override
    public synchronized void start() {
        if (started.get()) return;
        
        started.set(true);
        super.start();
    }
    
    public void stopAudio() {
        running.set(false);
        interrupt();
    }
    
    public void startStream(File path) {
        tasks.add(() -> {
            try {
                player.startStream(path);
            } catch (IOException e) {
                logger.error("Failed to stream audio", e);
            }
        });
    }
    
    public void addTask(Runnable runnable) {
        tasks.add(runnable);
    }
    
    public void setEndCallback(Runnable endCallback) {
        this.endCallback = endCallback;
    }
    
    public AudioPlayer getPlayer() {
        return player;
    }
}
