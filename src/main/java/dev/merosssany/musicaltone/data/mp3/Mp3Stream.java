package dev.merosssany.musicaltone.data.mp3;

import dev.merosssany.musicaltone.data.AudioStream;
import javazoom.jl.decoder.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ShortBuffer;

public class Mp3Stream implements AudioStream, AutoCloseable {
    private Bitstream bitstream;
    private Decoder decoder;
    private boolean finished = false;
    
    public Mp3Stream(String path) throws IOException {
        bitstream = new Bitstream(new FileInputStream(path));
        decoder = new Decoder();
    }
    
    public int getChannels() {
        return 2; // JLayer decodes to stereo
    }
    
    public int getSampleRate() {
        return 44100; // standard for most MP3s
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
        return getChannels() == 1 ? org.lwjgl.openal.AL10.AL_FORMAT_MONO16 : org.lwjgl.openal.AL10.AL_FORMAT_STEREO16;
    }
    
    @Override
    public void close() throws BitstreamException {
        bitstream.close();
    }
}

