package dev.merosssany.musicaltone.data;

import dev.merosssany.musicaltone.StartupMusicalTone;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = StartupMusicalTone.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec CONFIG_SPEC;
    
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> volume;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> probability;
    
    static {
        volume = BUILDER
                .comment("Volume of the tracks.\n\nFormat: track name.ext:volume")
                .defineList("volume", List.of(), Config::validate);
        
        probability = BUILDER
                .comment("The chance of the track playing.\n\nFormat: track name.ext:probability")
                .defineList("probability", List.of(), Config::validate);
        
        CONFIG_SPEC = BUILDER.build();
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
    
    @SubscribeEvent
    public static void onLoad(ModConfigEvent e) {
        Data.load();
    }
}
