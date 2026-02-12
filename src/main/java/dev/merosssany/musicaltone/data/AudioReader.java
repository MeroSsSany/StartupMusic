package dev.merosssany.musicaltone.data;

import dev.merosssany.musicaltone.data.mp3.Mp3Stream;
import dev.merosssany.musicaltone.data.ogg.OggStream;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class AudioReader {
    public static AudioStream getStreamFromFile(File file) throws IOException {
        if (file.exists() && file.isFile()) {
            String name = file.getName();
            
            if (name.contains(".ogg")) return new OggStream(file.getAbsolutePath());
            else if (name.contains(".mp3")) return new Mp3Stream(file.getAbsolutePath());
        }
        return null;
    }
}
