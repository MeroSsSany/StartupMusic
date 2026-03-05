package dev.merosssany.musicaltone.data.stream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class WavStream implements AudioStream, AutoCloseable {
    private final AudioInputStream stream;
    private final int channels;
    private final int sampleRate;
    private boolean finished = false;
    
    public WavStream(String path) throws Exception {
        File file = new File(path);
        this.stream = AudioSystem.getAudioInputStream(file);
        
        AudioFormat format = stream.getFormat();
        this.channels = format.getChannels();
        this.sampleRate = (int) format.getSampleRate();
        
        // Safety check: only support 16-bit for now to match AudioPlayer
        if (format.getSampleSizeInBits() != 16) {
            throw new UnsupportedAudioFileException("Only 16-bit WAV files are supported.");
        }
    }
    
    @Override
    public int readSamples(ShortBuffer buffer) {
        if (finished) return 0;
        
        try {
            // Calculate how many bytes we need to fill the ShortBuffer
            int bytesToRead = buffer.capacity() * 2;
            byte[] bytes = new byte[bytesToRead];
            int bytesRead = stream.read(bytes);
            
            if (bytesRead <= 0) {
                finished = true;
                return 0;
            }
            
            buffer.clear();

            ShortBuffer incomingShorts = ByteBuffer.wrap(bytes, 0, bytesRead)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer();

            buffer.put(incomingShorts);
            buffer.flip();
            
            // Return samples per channel
            return (bytesRead / 2) / channels;
            
        } catch (Exception e) {
            finished = true;
            return 0;
        }
    }
    
    @Override public int getChannels() { return channels; }
    @Override public int getSampleRate() { return sampleRate; }
    @Override public boolean isFinished() { return finished; }
    
    @Override
    public void close() {
        try { stream.close(); } catch (Exception ignored) {}
    }
}
