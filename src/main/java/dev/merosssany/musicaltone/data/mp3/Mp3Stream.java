package dev.merosssany.musicaltone.data.mp3;

import dev.merosssany.musicaltone.data.AudioStream;
import javazoom.jl.decoder.*;
import org.lwjgl.openal.AL10;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ShortBuffer;

public class Mp3Stream implements AudioStream, AutoCloseable {
    private final int channels;
    private Bitstream bitstream;
    private Decoder decoder;
    private boolean finished = false;
    private int sampleRate = 44100;
    
    public Mp3Stream(String path) throws IOException {
        int channels1 = 1;
        bitstream = new Bitstream(new FileInputStream(path));
        decoder = new Decoder();
        
        // Peek at the first frame to get the correct metadata immediately
        try {
            Header firstFrame = bitstream.readFrame();
            if (firstFrame != null) {
                this.sampleRate = firstFrame.sample_frequency();
                // Important: We don't "closeFrame" or "decode" yet
                // OR we must handle the fact that we've advanced the stream.
                bitstream.unreadFrame(); // Some versions of JLayer support this,
                channels1 = (firstFrame.mode() == Header.SINGLE_CHANNEL) ? 1 : 2;
            }
        } catch (BitstreamException e) {
            this.sampleRate = 44100; // Fallback
        }
        this.channels = channels1;
    }
    
    public int getChannels() {
        return channels; // JLayer decodes to stereo
    }
    
    public int getSampleRate() {
        return sampleRate; // standard for most MP3s
    }
    
    public boolean isFinished() {
        return finished;
    }
    
    public int readSamples(ShortBuffer buffer) {
        if (finished) return 0;
        
        try {
            Header frameHeader = bitstream.readFrame();
            if (frameHeader == null) {
                finished = true;
                return 0;
            }
            
            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(frameHeader, bitstream);
            this.sampleRate = output.getSampleFrequency(); // Get the REAL rate from the file
            
            short[] pcm = output.getBuffer();
            buffer.clear();
            buffer.put(pcm).flip();
            bitstream.closeFrame();
            return output.getBufferLength() / getChannels(); // number of samples per channel
        } catch (BitstreamException | DecoderException e) {
            finished = true;
            return 0;
        }
    }
    
    public int getFormat() {
        return getChannels() == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
    }
    
    @Override
    public void close() throws BitstreamException {
        bitstream.close();
    }
}

