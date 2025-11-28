package dev.merosssany.musicaltune.event;

import dev.merosssany.musicaltune.core.AudioPlayer;

public interface AudioStopPlayingListener {
    void onAudioStopped(AudioPlayer player); // Method called when audio stops, pass player instance or other data
    // Add other notification methods if needed (e.g., onAudioStarted, onError)
}

