package dev.merosssany.musicaltone.core;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.alcCloseDevice;
import static org.lwjgl.openal.ALC10.alcDestroyContext;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;

import dev.merosssany.musicaltone.data.AudioReader;
import dev.merosssany.musicaltone.data.AudioStream;
import dev.merosssany.musicaltone.data.ogg.OggStream;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.merosssany.musicaltone.data.ogg.OggLoader.AudioData;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class AudioPlayer {
	private int bufferId;
	private int sourceId;
	private long device;
	private long context;
	private boolean initialized = false;
	private boolean isFading;
	private float fadeTimeElapsed;
	private float fadeDurationSeconds;
	private float initialFadeGain;
	private static final Logger logger = LogUtils.getLogger();
    
    private static final int BUFFER_COUNT = 4;
    private static final int STREAM_BUFFER_SAMPLES = 4096 * 8;
    
    private int[] buffers = new int[BUFFER_COUNT];
    private ShortBuffer streamBuffer;
    private AudioStream currentStream;
    private boolean streaming;
    
    public void play(AudioData audioData) {
		if (initialized) {
			logger.info("Playing music...");
			stop();
   
			bufferId = alGenBuffers();

			int format;
			if (audioData.channels() == 1) {
				format = AL_FORMAT_MONO16;
			} else if (audioData.channels() == 2) {
				format = AL_FORMAT_STEREO16;
			} else {
				throw new IllegalStateException("Only mono or stereo is supported");
			}

			alBufferData(bufferId, format, audioData.samples(), audioData.sampleRate());
			alSourcei(sourceId, AL_BUFFER, bufferId);
			alSourcePlay(sourceId);
		}
	}
    
    public void startStream(File path) throws IOException {
        stop();
        
        currentStream = AudioReader.getStreamFromFile(path);
        
        sourceId = AL10.alGenSources();
        
        for (int i = 0; i < BUFFER_COUNT; i++) {
            buffers[i] = AL10.alGenBuffers();
        }
        
        streamBuffer = MemoryUtil.memAllocShort(STREAM_BUFFER_SAMPLES);
        
        // Pre-fill queue
        for (int i = 0; i < BUFFER_COUNT; i++) {
            streamBuffer.clear();
            
            int samples = currentStream.readSamples(streamBuffer);
            if (samples == 0) break;
            
            streamBuffer.limit(samples * currentStream.getChannels());
            
            AL10.alBufferData(
                    buffers[i],
                    currentStream.getFormat(),
                    streamBuffer,
                    currentStream.getSampleRate()
            );
            
            AL10.alSourceQueueBuffers(sourceId, buffers[i]);
        }
        
        AL10.alSourcePlay(sourceId);
        streaming = true;
    }
    
    public void updateStreaming() {
        if (!streaming) return;
        
        int processed = AL10.alGetSourcei(sourceId, AL10.AL_BUFFERS_PROCESSED);
        
        while (processed-- > 0) {
            int buffer = AL10.alSourceUnqueueBuffers(sourceId);
            
            streamBuffer.clear();
            
            int samples = currentStream.readSamples(streamBuffer);
            
            if (samples > 0) {
                streamBuffer.limit(samples * currentStream.getChannels());
                
                AL10.alBufferData(
                        buffer,
                        currentStream.getFormat(),
                        streamBuffer,
                        currentStream.getSampleRate()
                );
                
                AL10.alSourceQueueBuffers(sourceId, buffer);
            }
        }
        
        int state = AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE);
        
        if (state != AL10.AL_PLAYING && !currentStream.isFinished()) {
            AL10.alSourcePlay(sourceId);
        }
        
        if (currentStream.isFinished()) {
            stop();
        }
    }
    
    public void cleanup() {
        if (!initialized) return;
        
        stop();
        
        if (sourceId != 0) {
            alDeleteSources(sourceId);
            sourceId = 0;
        }
        
        if (context != MemoryUtil.NULL) {
            alcDestroyContext(context);
            context = 0;
        }
        
        if (device != MemoryUtil.NULL) {
            alcCloseDevice(device);
            device = 0;
        }
        
        initialized = false;
    }
    
    public void setVolume(float volume) {
        if (!initialized || sourceId == 0) return;
        
        volume = Math.max(0.0f, Math.min(1.0f, volume)); // clamp between 0 and 1
        AL10.alSourcef(sourceId, AL10.AL_GAIN, volume);
    }
    
    public void init() {
        device = ALC10.alcOpenDevice((ByteBuffer) null);
        if (device == MemoryUtil.NULL)
            throw new IllegalStateException("Failed to open device");
        
        context = ALC10.alcCreateContext(device, (IntBuffer) null);
        if (context == MemoryUtil.NULL)
            throw new IllegalStateException("Failed to create context");
        
        ALC10.alcMakeContextCurrent(context);
        AL.createCapabilities(ALC.createCapabilities(device));
        
        sourceId = alGenSources();
        initialized = true;
    }
    
	public boolean isPlaying() {
        if (!initialized || sourceId == 0) return false;
		int state = AL10.alGetSourcei(sourceId, AL_SOURCE_STATE);
		throwIfErrored();
        return state == AL_PLAYING;
	}

	public boolean isStopped() throws IllegalStateException {
        if (!initialized || sourceId == 0) return false;
		throwIfErrored();
		return  getOpenALState() == AL_STOPPED;
	}

	public int getOpenALState() {
        if (!initialized || sourceId == 0) return 0;
		throwIfErrored();
		return AL10.alGetSourcei(sourceId,AL_SOURCE_STATE);
	}

	public boolean isPaused() {
        if (!initialized || sourceId == 0) return false;
		throwIfErrored();
		return AL10.alGetSourcei(sourceId,AL_SOURCE_STATE) == AL_PAUSED;
	}
    
    public boolean hasTrack() {
        return sourceId != 0;
    }
    
    private void throwIfErrored() throws IllegalStateException {
		if (hasError()) {
			throw new IllegalStateException("There was an error in OpenAL, code: "+getOpenALError());
		}
	}

	public int getOpenALError(){
		return AL10.alGetError();
	}

	public boolean hasError() {
        return getOpenALError() != AL_NO_ERROR;
    }
	
	public void startFadeOut(float durationSeconds) {
        // Cannot start a fade if not initialized, no valid source, or already fading
		if (!initialized || sourceId == 0 || isFading) {
			if (isFading) logger.debug("Already fading out.");
            if (!initialized) logger.warn("Cannot start fade, AudioPlayer not initialized.");
            if (sourceId == 0) logger.warn("Cannot start fade, no active source.");
			return;
		}
        // Check for invalid duration
		if (durationSeconds <= 0) {
            logger.warn("Fade duration must be positive. Stopping sound immediately.");
			stop(); // Stop immediately if duration is 0 or less
			return;
		}


		logger.debug("Starting non-blocking fade out over {} seconds.", durationSeconds);
		isFading = true;
		fadeDurationSeconds = durationSeconds;
		fadeTimeElapsed = 0; // Reset timer for a new fade
		initialFadeGain = AL10.alGetSourcef(sourceId, AL10.AL_GAIN); // Get current gain
        int error = AL10.alGetError(); // Check error after getting initial gain
		if (error != AL_NO_ERROR) {
            logger.warn("OpenAL Error getting initial gain for fade: {}. Using 1.0f.", AL10.alGetString(error));
            // Continue with initialGain potentially being default (1.0f) if error occurs
			initialFadeGain = 1.0f; // Default gain fallback
		}
	}

    // --- Update Non-Blocking Fade Progress Function ---
    // This method should be called repeatedly by a time source (like a game tick)
    // to smoothly update the audio source's volume during a fade-out.
    // This method is non-blocking and calculates the current volume based on time elapsed.
    // 'deltaTime' is the time elapsed since the last call to this method, in seconds.
	public void updateFadeProgress(float deltaTime) {
        // Only update if initialized, currently fading, and have a valid source
		if (!initialized || !isFading || sourceId == 0) {
            // If fading was active but source became invalid unexpectedly, stop fading state
            if (isFading) {
                logger.debug("Fade stopped unexpectedly (source invalid or uninitialized).");
                isFading = false;
                fadeDurationSeconds = 0;
                fadeTimeElapsed = 0;
                initialFadeGain = 0;
            }
			return;
		}

		fadeTimeElapsed += deltaTime; // Increment float timer

		if (fadeTimeElapsed >= fadeDurationSeconds) {
           stop();

		} else {
            // Fade is in progress
			float progress = fadeTimeElapsed / fadeDurationSeconds; // Progress from 0.0 to 1.0
            // Ensure progress stays within [0, 1] in case of slight float inaccuracies
			progress = Math.min(1.0f, Math.max(0.0f, progress));

            // Calculate current gain (linear fade: decreases from initialGain to 0)
			float currentGain = initialFadeGain * (1.0f - progress);

			AL10.alSourcef(sourceId, AL10.AL_GAIN, currentGain); // Apply the calculated gain
            int error = AL10.alGetError(); // Check for errors after setting gain
			if (error != AL_NO_ERROR) {
                logger.error("OpenAL Error updating gain during fade: {}", AL10.alGetString(error));
			}
		}
	}
    
    public void stop() {
        streaming = false;
        
        if (sourceId != 0) {
            AL10.alSourceStop(sourceId);
            
            int queued = AL10.alGetSourcei(sourceId, AL10.AL_BUFFERS_QUEUED);
            while (queued-- > 0) {
                AL10.alSourceUnqueueBuffers(sourceId);
            }
            
            AL10.alDeleteSources(sourceId);
            sourceId = 0;
        }
        
        for (int i = 0; i < BUFFER_COUNT; i++) {
            if (buffers[i] != 0) {
                AL10.alDeleteBuffers(buffers[i]);
                buffers[i] = 0;
            }
        }
        
        if (streamBuffer != null) {
            MemoryUtil.memFree(streamBuffer);
            streamBuffer = null;
        }
        
        if (currentStream != null) {
            try {
                currentStream.close();
            } catch (Exception e) {
                logger.error("Failed to close stream.",e);
            }
            currentStream = null;
        }
    }
}