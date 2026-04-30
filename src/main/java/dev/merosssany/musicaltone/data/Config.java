package dev.merosssany.musicaltone.data;

import dev.merosssany.musicaltone.StartupMusicalTone;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber; // Changed
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec; // Changed from ForgeConfigSpec

import java.util.List;

@EventBusSubscriber(modid = StartupMusicalTone.MOD_ID)
public class Config {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec CONFIG_SPEC;
    
    public static final ModConfigSpec.ConfigValue<List<? extends String>> volume;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> probability;
    public static final ModConfigSpec.IntValue floatsPerSample;
    
    static {
        BUILDER.push("Basic");
        
        volume = BUILDER
                .comment("Volume of the tracks.\n\nFormat: track name.ext:volume")
                .defineList("volume", List.of(), Config::validateVolume);
        
        probability = BUILDER
                .comment("The chance of the track playing.\n\nFormat: track name.ext:probability")
                .defineList("probability", List.of(), Config::validateProbability);
        
        BUILDER.pop();
        BUILDER.push("Advanced");
        
        floatsPerSample = BUILDER
                .comment("How many floats (the audio data) will be sent per batch.")
                .defineInRange("floatsPerBatch", 4096, 1024, 1048576);
        
        BUILDER.pop();
        CONFIG_SPEC = BUILDER.build();
    }
    
    private static boolean validateVolume(Object o) {
        if (o instanceof String entry) {
            if (entry.contains(":")) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    try {
                        int value = Integer.parseInt(parts[1].trim());
                        return value >= 0 && value <= 100;
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return false;
    }
    
    private static boolean validateProbability(Object o) {
        if (o instanceof String entry) {
            if (entry.contains(":")) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    try {
                        int value = Integer.parseInt(parts[1].trim());
                        return value >= 0;
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return false;
    }
    
    @SubscribeEvent
    public static void onLoad(ModConfigEvent e) {
        // Ensure we only react to OUR config being loaded
        if (e.getConfig().getSpec() == CONFIG_SPEC) {
            DataLoader.load();
            StartupMusicalTone.startPlaying();
        }
    }
}