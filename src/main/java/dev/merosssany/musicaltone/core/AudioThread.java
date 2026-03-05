package dev.merosssany.musicaltone.core;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static dev.merosssany.musicaltone.StartupMusicalTone.showError;

public class AudioThread extends Thread {
    private static final AtomicBoolean started = new AtomicBoolean();
    private static final AtomicBoolean end = new AtomicBoolean();
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
        try {
            long lastTime = System.nanoTime();
            
            while (running.get()) {
                long currentTime = System.nanoTime();
                float deltaTime = (currentTime - lastTime) / 1_000_000_000f;
                lastTime = currentTime;
                Runnable task;
                while ((task = tasks.poll()) != null) task.run();
                
                player.updateStreaming();
                player.updateFadeProgress(deltaTime);
                
                if (!player.isStreaming() && end.get()) break;
                
                if (!player.isStreaming() && running.get()) {
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
            end.set(false);
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
    }
    
    public void startStream(File path) {
        tasks.add(() -> {
            try {
                player.startStream(path);
            } catch (Exception e) {
                logger.error("Failed to stream audio", e);
                showError("Failed to stream audio", e);
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
    
    public void finalizeAudio() {
        end.set(true);
    }
}
