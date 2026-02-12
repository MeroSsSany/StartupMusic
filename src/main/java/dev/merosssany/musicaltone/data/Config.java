package dev.merosssany.musicaltone.data;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class Config {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec CONFIG_SPEC = BUILDER.build();
    
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> volume;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> probability;
    
    static {
        volume = BUILDER
                .comment("Volume of the tracks. Format: track name.ext:volume")
                .defineList("volume", List.of(), Config::validate);
        
        probability = BUILDER
                .comment("The chance of the track playing. Format: track name.ext:probability")
                .defineList("probability", List.of(), Config::validate);
    }
    
    private static boolean validate(Object o) {
        if (o instanceof String entry) {
            if (entry.contains(":")) {
                String[] parts = entry.split(":");
                
                if (parts.length == 2) {
                    try {
                        Integer.parseInt(parts[1]);
                        return true;
                        
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }
        return false;
    }
}
