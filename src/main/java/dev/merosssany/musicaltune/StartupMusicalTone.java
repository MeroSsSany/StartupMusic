package dev.merosssany.musicaltune;

import java.io.File;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.merosssany.musicaltune.core.AudioPlayer;
import dev.merosssany.musicaltune.core.FileManager;
import dev.merosssany.musicaltune.init.OggLoader;
import dev.merosssany.musicaltune.init.OggLoader.AudioData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.ModLoadingWarning;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.fml.config.ModConfig;

@Mod("startupmusicmod")
@EventBusSubscriber(modid = "startupmusicmod", bus = Bus.MOD)
public class StartupMusicalTone {
	public static final String modid = "startupmusicmod";

	private static final AudioPlayer player = new AudioPlayer();
	private static final Logger logger = LogUtils.getLogger();
	private static boolean success = true;

	public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	public static final ForgeConfigSpec.BooleanValue PLAY_ON_LOAD = BUILDER
			.comment("Play a randomly selected music when the world start load").define("play_on_loading_world", false);

	public static final ForgeConfigSpec CONFIG_SPEC = BUILDER.build();
	private static boolean loadComplete = false;

	public StartupMusicalTone(FMLJavaModLoadingContext context) {
		context.registerConfig(ModConfig.Type.CLIENT, CONFIG_SPEC);

		try {
			FileManager.init();
			player.init();
			StartupMusicalTone.playMusic();
		} catch (Exception e) {
			showError(e);
		}
	}

	public static void playMusic() throws Exception {
		File file = FileManager.getRandomFileFrom(FileManager.getMusicFolder());
		AudioData data = OggLoader.loadOgg(file.getAbsolutePath());
		player.play(data);
	}

	@SuppressWarnings("removal")
	private static void showError(Exception e) {
		logger.error("An error has happened", e);
		success = false;
		ModLoader.get().addWarning(new ModLoadingWarning(ModLoadingContext.get().getActiveContainer().getModInfo(),
				ModLoadingStage.CONSTRUCT, "Startup Musical Tone failed to load ogg file: " + e.getMessage(), e

		));
	}

	@SubscribeEvent
	public static void onLoadingComplete(FMLLoadCompleteEvent event) {
		loadComplete = true;
		if (success)
			player.fadeOut(3f);
	}

	public static void onGameShuttingDown(GameShuttingDownEvent event) {
		player.cleanup();
	}

	public static Logger getLogger() {
		return logger;
	}
}
