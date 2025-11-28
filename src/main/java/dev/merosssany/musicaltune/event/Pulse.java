package dev.merosssany.musicaltune.event;

import net.minecraftforge.eventbus.api.Event;

public class Pulse {
    private boolean Start;
    private final long breakTime;
    private final Thread integration;
    private final Runnable callback;

    public Pulse(long time, Runnable callback) {
        this.breakTime = time;
        this.integration = new Thread(this::pulse);
        this.callback = callback;
    }

    private void pulse() {
        while (this.Start) {
            try {
                this.callback.run();
                Thread.sleep(Math.abs(breakTime));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        this.Start = true;
        this.integration.start();
    }

    public void stop() {
        this.Start = false;
        integration.stop();
    }
}
