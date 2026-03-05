package dev.merosssany.musicaltone.data;

import dev.merosssany.musicaltone.data.stream.AudioStream;
import dev.merosssany.musicaltone.data.factory.AudioStreamFactory;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AudioReader {
    private static final Map<String, AudioStreamFactory> decoders = new ConcurrentHashMap<>();
    
    public static AudioStream getStreamFromFile(File file) throws Exception {
        if (file.exists() && file.isFile()) {
            String name = file.getName();
            String[] dots = name.split("\\.");
            String ext = dots[dots.length-1];
            
            return decoders.get(ext).create(file.getAbsolutePath());
        }
        return null;
    }
    
    public static void registerFactory(String ext, AudioStreamFactory factory) {
        decoders.put(ext, factory);
    }
    
    public static Set<String> getSupportedFiles() {
        return decoders.keySet();
    }
}
