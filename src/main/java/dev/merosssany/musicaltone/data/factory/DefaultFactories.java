package dev.merosssany.musicaltone.data.factory;

import dev.merosssany.musicaltone.data.AudioReader;
import dev.merosssany.musicaltone.data.stream.Mp3Stream;
import dev.merosssany.musicaltone.data.stream.OggStream;
import dev.merosssany.musicaltone.data.stream.WavStream;

public class DefaultFactories {
    public static void register() {
        AudioReader.registerFactory("mp3", Mp3Stream::new);
        AudioReader.registerFactory("ogg", OggStream::new);
        AudioReader.registerFactory("oga", OggStream::new);
        AudioReader.registerFactory("wav", WavStream::new);
    }
}
