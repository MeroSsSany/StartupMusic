package dev.merosssany.musicaltone.data;

import java.nio.ShortBuffer;

public interface AudioStream {
    int getChannels();
    int getSampleRate();
    int getFormat();
    int readSamples(ShortBuffer targetBuffer);
    boolean isFinished();
    void close() throws Exception;
}
