package dev.merosssany.musicaltone.core;

import com.mojang.logging.LogUtils;
import dev.merosssany.musicaltone.data.AudioReader;
import dev.merosssany.musicaltone.data.stream.AudioStream;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import javax.sound.sampled.*;
import java.io.File;
import java.nio.ShortBuffer;

public class AudioPlayer {
    private static final Logger logger = LogUtils.getLogger();
    
    private SourceDataLine line;
    private AudioStream currentStream;
    
    private boolean streaming = false;
    private boolean isFading = false;
    
    private float fadeTimeElapsed;
    private float fadeDurationSeconds;
    private float initialFadeGain; // In linear 0.0 - 1.0
    private float currentVolume = 1.0f;
    
    private static int samplesToRead = 4096;
    private byte[] byteBuffer;
    private ShortBuffer shortBuf;
    
    public void startStream(File path) throws Exception {
        stop();
        currentStream = AudioReader.getStreamFromFile(path);
        if (currentStream == null) return;
        
        // Just prepare the buffers, don't open the line yet!
        int channels = currentStream.getChannels();
        shortBuf = MemoryUtil.memAllocShort(samplesToRead * channels);
        byteBuffer = new byte[samplesToRead * channels * 2];
        
        streaming = true;
        logger.info("Stream prepared for: {}", path.getName());
    }
    
    public void updateStreaming() {
        if (!streaming || currentStream == null) return;
        
        shortBuf.clear();
        int samplesPerChannel = currentStream.readSamples(shortBuf);
        
        if (samplesPerChannel > 0) {
            int actualRate = currentStream.getSampleRate();
            int actualChannels = currentStream.getChannels();
            
            // Check if we need to (re)open the line (Rate Correction)
            if (line == null || line.getFormat().getSampleRate() != (float)actualRate) {
                if (actualRate > 1) {
                    reopenLine(actualRate, actualChannels);
                }
            }
            
            if (line != null) {
                int totalSamples = samplesPerChannel * actualChannels;
                int bytesToWrite = totalSamples * 2;
                
                // Ensure byteBuffer is always exactly the right size for this chunk
                if (byteBuffer.length != bytesToWrite) {
                    byteBuffer = new byte[bytesToWrite];
                }
                
                // Convert Shorts to Bytes (Little Endian)
                for (int i = 0; i < totalSamples; i++) {
                    short sample = shortBuf.get(i);
                    byteBuffer[i * 2] = (byte) (sample & 0xFF);
                    byteBuffer[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
                }
                
                // The thread will wait here until the audio hardware is ready.
                line.write(byteBuffer, 0, bytesToWrite);
            }
        }
        
        if (currentStream.isFinished()) {
            stop();
        }
    }
    
    private void reopenLine(int rate, int channels) {
        try {
            if (line != null) { line.stop(); line.close(); }
            
            AudioFormat format = new AudioFormat((float)rate, 16, channels, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            setVolume(currentVolume);
            logger.info("Audio line established at {}Hz ({} channels)", rate, channels);
        } catch (Exception e) {
            logger.error("Failed to open Java Sound line", e);
        }
    }
    
    public void setVolume(float volume) {
        this.currentVolume = volume;
        if (line == null || !line.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
        
        FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        
        // Convert linear 0.0-1.0 to Decibels
        // dB = 20 * log10(volume)
        float dB = (float) (Math.log(Math.max(volume, 0.0001f)) / Math.log(10.0) * 20.0);
        gainControl.setValue(Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB)));
    }
    
    public void startFadeOut(float durationSeconds) {
        if (!streaming || isFading) return;
        isFading = true;
        fadeDurationSeconds = durationSeconds;
        fadeTimeElapsed = 0;
        initialFadeGain = currentVolume;
        logger.debug("Starting Java Sound fade out: {}s", durationSeconds);
    }
    
    public void updateFadeProgress(float deltaTime) {
        if (!isFading || !streaming) return;
        
        fadeTimeElapsed += deltaTime;
        if (fadeTimeElapsed >= fadeDurationSeconds) {
            setVolume(0.0f); // Ensure it's fully silent
            finishAndStop(); // New graceful stop
        } else {
            float progress = fadeTimeElapsed / fadeDurationSeconds;
            setVolume(initialFadeGain * (1.0f - progress));
        }
    }
    
    private void finishAndStop() {
        isFading = false;
        
        new Thread(() -> {
            if (line != null) {
                line.drain();
                stop();
            }
        }).start();
    }
    
    public synchronized void stop() {
        streaming = false;
        isFading = false;
        
        // Copy the reference to a local variable (Thread Safety 101)
        SourceDataLine localLine = this.line;
        
        if (localLine != null) {
            try {
                localLine.stop();
                localLine.flush();
                localLine.close();
            } catch (Exception ignored) {
            } finally {
                // Only set the class member to null if it's still pointing to our local copy
                if (this.line == localLine) {
                    this.line = null;
                }
            }
        }
        
        if (currentStream != null) {
            try { currentStream.close(); } catch (Exception ignored) {}
            currentStream = null;
        }
    }
    
    public void cleanup() {
        stop();
    }
    
    public void setSamples(int samples) {
        samplesToRead = samples;
    }
    
    public int getSamples() {
        return samplesToRead;
    }
    
    public boolean isStreaming() { return streaming; }
}