package dev.merosssany.musicaltone.core;

import org.lwjgl.openal.ALC10;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioThread extends Thread {
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
                
                if (!player.isPlaying() && player.hasTrack()) {
                    if (endCallback != null) {
                        endCallback.run();
                    }
                }
                
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ignored) {}
            }
        } finally {
            player.cleanup();
            ALC10.alcMakeContextCurrent(0);
        }
    }
    
    public void stopAudio() {
        running.set(false);
        interrupt();
    }
    
    public void startStream(String path) {
        tasks.add(() -> player.startStream(path));
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
