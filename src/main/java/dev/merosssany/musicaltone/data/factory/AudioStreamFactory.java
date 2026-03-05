package dev.merosssany.musicaltone.data.factory;

import dev.merosssany.musicaltone.data.stream.AudioStream;

public interface AudioStreamFactory {
    AudioStream create(String file) throws Exception;
}
