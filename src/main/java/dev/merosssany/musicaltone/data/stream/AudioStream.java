package dev.merosssany.musicaltone.data.stream;

import java.nio.ShortBuffer;

public interface AudioStream {
    int getChannels();
    int getSampleRate();
    int readSamples(ShortBuffer targetBuffer);
    boolean isFinished();
    void close() throws Exception;
}
