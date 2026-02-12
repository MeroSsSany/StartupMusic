package dev.merosssany.musicaltone.data.ogg;

import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class OggStream implements AutoCloseable {
    private final long decoder;
    private final int channels;
    private final int sampleRate;
    private boolean finished = false;
    
    public OggStream(String path) {
        File file = new File(path);
        if (!file.exists()) throw new RuntimeException("OGG file not found: " + path);
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer error = stack.mallocInt(1);
            
            decoder = STBVorbis.stb_vorbis_open_filename(
                    file.getAbsolutePath(),
                    error,
                    null
            );
            
            if (decoder == MemoryUtil.NULL) {
                throw new RuntimeException("Failed to open OGG. Error: " + error.get(0));
            }
            
            STBVorbisInfo info = STBVorbisInfo.malloc(stack);
            STBVorbis.stb_vorbis_get_info(decoder, info);
            
            channels = info.channels();
            sampleRate = info.sample_rate();
        }
    }
    
    public int getChannels() {
        return channels;
    }
    
    public int getSampleRate() {
        return sampleRate;
    }
    
    public int getFormat() {
        return channels == 1
                ? AL10.AL_FORMAT_MONO16
                : AL10.AL_FORMAT_STEREO16;
    }
    
    /**
     * Reads PCM samples into provided buffer.
     * Returns number of samples read.
     */
    public int readSamples(ShortBuffer targetBuffer) {
        if (finished) return 0;
        
        int samples = STBVorbis.stb_vorbis_get_samples_short_interleaved(
                decoder,
                channels,
                targetBuffer
        );
        
        if (samples == 0) {
            finished = true;
        }
        
        return samples;
    }
    
    public boolean isFinished() {
        return finished;
    }
    
    @Override
    public void close() {
        STBVorbis.stb_vorbis_close(decoder);
    }
}

