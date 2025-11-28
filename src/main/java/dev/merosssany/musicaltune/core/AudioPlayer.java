package dev.merosssany.musicaltune.core;

import static org.lwjgl.openal.AL10.*;

import java.nio.IntBuffer;
import dev.merosssany.musicaltune.event.AudioStopPlayingListener;
import dev.merosssany.musicaltune.event.Pulse;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.merosssany.musicaltune.init.OggLoader.AudioData;

import java.nio.ByteBuffer;
import java.util.ArrayList;

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
	private final Pulse pulse = new Pulse(250,this::checkForAudioStatus);
	private ArrayList<AudioStopPlayingListener> listeners = new ArrayList<>(); // List to hold listeners

	// Method to add listeners
	public void addAudioPlaybackListener(AudioStopPlayingListener listener) {
		if (listener != null && !listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	// Method to remove listeners
	public void removeAudioPlaybackListener(AudioStopPlayingListener listener) {
		listeners.remove(listener);
	}

	// Update handlePlaybackCompletion to notify listeners
	private void handlePlaybackCompletion() {
		// ... cleanup ...

		// Notify all registered listeners
		for (AudioStopPlayingListener listener : listeners) {
			try {
				listener.onAudioStopped(this); // Call the listener's method, pass 'this' or relevant data
			} catch (Exception e) {
				logger.error("Error notifying audio playback listener", e);
				// Handle exceptions from listeners if necessary
			}
		}
		// No need to clear the list here, listeners stay registered
	}


	public void play(AudioData audioData) {
		if (initialized) {
			logger.info("Playing music...");
			stop();
			bufferId = alGenBuffers();
			sourceId = alGenSources();

			int format;
			if (audioData.channels == 1) {
				format = AL_FORMAT_MONO16;
			} else if (audioData.channels == 2) {
				format = AL_FORMAT_STEREO16;
			} else {
				throw new IllegalStateException("Only mono or stereo is supported");
			}

			alBufferData(bufferId, format, audioData.samples, audioData.sampleRate);
			alSourcei(sourceId, AL_BUFFER, bufferId);
			alSourcePlay(sourceId);
			pulse.start();
		}
	}

	private void checkForAudioStatus() {
		if (isStoped()) {
			for (AudioStopPlayingListener listener : listeners) {
				try {
					listener.onAudioStopped(this); // Call the listener's method, pass 'this' or relevant data
				} catch (Exception e) {
					logger.error("Error notifying audio playback listener", e);
					// Handle exceptions from listeners if necessary
				}
			}
			pulse.stop();
		}
	}

	public void cleanup() {
		if (initialized) {
			stop();
			if (context != MemoryUtil.NULL) {
				ALC10.alcDestroyContext(context);
				context = 0;
			}
			if (device != MemoryUtil.NULL) {
				ALC10.alcCloseDevice(device);
				device = 0;
			}
			initialized = false;
		}
	}

	public void stop() {
		if (initialized) {
			if (sourceId != 0) {
				alDeleteSources(sourceId);
				sourceId = 0;
			}
			if (bufferId != 0) {
				alDeleteBuffers(bufferId);
				bufferId = 0;
			}
		}
	}

	public void init() {
		initialized = true;
		// 1. Open default device
		device = ALC10.alcOpenDevice((ByteBuffer) null);
		if (device == MemoryUtil.NULL)
			throw new IllegalStateException("Failed to open the default OpenAL device.");

		// 2. Create context
		context = ALC10.alcCreateContext(device, (IntBuffer) null);
		if (context == MemoryUtil.NULL)
			throw new IllegalStateException("Failed to create OpenAL context.");

		// 3. Make context current
		ALC10.alcMakeContextCurrent(context);

		// 4. Create capabilities
		AL.createCapabilities(ALC.createCapabilities(device));
	}

	// --- Start the Fade Out ---
	public void fadeOut(float durationSeconds) {
		if (initialized) {
			// The new Thread(() -> { ... }).start(); was commented out in this version
//				new Thread(() -> {
			logger.debug("Fading Out...");
			float initialGain = AL10.alGetSourcef(sourceId, AL10.AL_GAIN);
			int steps = 20;
			float sleepTime = durationSeconds / steps;

			for (int i = steps; i >= 0; i--) {
				float gain = initialGain * ((float) i / steps);
				AL10.alSourcef(sourceId, AL10.AL_GAIN, gain);
				try {
					Thread.sleep((long) (sleepTime * 1000));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			AL10.alSourceStop(sourceId);
//			}).start();
		}
	}

	public boolean isPlaying() {
		int state = AL10.alGetSourcei(sourceId, AL_SOURCE_STATE);
		throwIfErrored();
        return state == AL_PLAYING;
	}

	public boolean isStoped() throws IllegalStateException {
		throwIfErrored();
		return  getOpenALState() == AL_STOPPED;
	}

	public int getOpenALState() {
		throwIfErrored();
		return AL10.alGetSourcei(sourceId,AL_SOURCE_STATE);
	}

	public boolean isPaused() {
		throwIfErrored();
		return AL10.alGetSourcei(sourceId,AL_SOURCE_STATE) == AL_PAUSED;
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
            // Fade is complete
            logger.debug("Non-blocking fade out complete. Stopping source {}.", sourceId);
			alSourceStop(sourceId); // Stop the source (Use static import)

            // Clean up source and buffer after fade stop
            // Check bufferId != 0 before deleting buffer (safety check)
            if (bufferId != 0) alDeleteBuffers(bufferId); // Use static import
            // sourceId should be valid here if sourceId != 0 condition passed above
            if (sourceId != 0) alDeleteSources(sourceId); // Use static import


            // Reset IDs
			sourceId = 0;
			bufferId = 0;
			isFading = false; // End the fade state

            // Check for errors after stopping/deleting
            int error = AL10.alGetError();
			if (error != AL_NO_ERROR) {
                logger.error("OpenAL Error stopping/deleting after fade completion: {}", AL10.alGetString(error));
			}

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

	// Note: The updateFade method was introduced after this version
	// --- Update the Fade Progress (Called from a Tick/Time Update) ---
	// public void updateFade(float deltaTime) { ... }
}