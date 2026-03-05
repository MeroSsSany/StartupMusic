package dev.merosssany.musicaltone.data;

import dev.merosssany.musicaltone.StartupMusicalTone;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = StartupMusicalTone.modId, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec CONFIG_SPEC;
    
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> volume;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> probability;
    public static final ForgeConfigSpec.IntValue floatsPerSample;
    
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
                        int value = Integer.parseInt(parts[1]);
                        return value >= 0 && value <= 100;
                        
                    } catch (NumberFormatException ignored) {
                    }
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
                        int value = Integer.parseInt(parts[1]);
                        return value >= 0;
                        
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return false;
    }
    
    @SubscribeEvent
    public static void onLoad(ModConfigEvent e) {
        Data.load();
        
        StartupMusicalTone.startPlaying();
    }
}
