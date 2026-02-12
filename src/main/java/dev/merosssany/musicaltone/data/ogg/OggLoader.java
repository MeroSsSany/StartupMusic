package dev.merosssany.musicaltone.data.ogg;

import java.io.File;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class OggLoader {
    public record AudioData(ShortBuffer samples, int channels, int sampleRate) {
        
        public float getDurationInSeconds() {
            return (float) samples.capacity() / (sampleRate * channels);
        }
        
        public ShortBuffer data() {
            return samples;
        }
        
        public int format() {
            if (channels == 1) return AL10.AL_FORMAT_MONO16;
            if (channels == 2) return AL10.AL_FORMAT_STEREO16;
            throw new IllegalStateException("Unsupported channel count: " + channels);
        }
        
    }
	
	public static AudioData loadOgg(String path) throws Exception {
	    File file = new File(path);
	    if (!file.exists() || !file.isFile()) {
	        throw new RuntimeException("OGG file does not exist: " + path);
	    }

	    try (MemoryStack stack = MemoryStack.stackPush()) {
	        IntBuffer error = stack.mallocInt(1);
	        long decoder = STBVorbis.stb_vorbis_open_filename(file.getAbsolutePath(), error, null);
	        if (decoder == MemoryUtil.NULL) {
	            throw new RuntimeException("Failed to open OGG file. Error: " + error.get(0));
	        }

	        STBVorbisInfo info = STBVorbisInfo.malloc(stack);
	        STBVorbis.stb_vorbis_get_info(decoder, info);

	        int channels = info.channels();
	        int sampleRate = info.sample_rate();

	        int samplesCount = STBVorbis.stb_vorbis_stream_length_in_samples(decoder) * channels;
	        ShortBuffer pcm = MemoryUtil.memAllocShort(samplesCount);

	        STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm);
	        STBVorbis.stb_vorbis_close(decoder);

	        return new AudioData(pcm, channels, sampleRate);
	    }
	}

}
