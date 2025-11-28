package dev.merosssany.musicaltune.event;

import java.util.ArrayList;

import java.util.Iterator;

import dev.merosssany.musicaltune.StartupMusicalTone;
import dev.merosssany.musicaltune.core.AudioPlayer;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;

@EventBusSubscriber(modid = StartupMusicalTone.modid, bus = Bus.FORGE, value= {Dist.CLIENT})
public class WorldEventManager {
	private static ArrayList<String> queuedMessages = new ArrayList<String>();
	private static boolean playerLoggedIn = false;
	private static AudioPlayer player;

	public static Player localPlayer;

	public static void enqueueMessage(String message) {
		if (!playerLoggedIn)
			queuedMessages.add(message);
		else {
			Component messageComponent = Component.literal("<Startup Music Mod> " + message);
			localPlayer.sendSystemMessage(messageComponent);
		}

	}
	
	public static void init(AudioPlayer audioPlayer) {
		player = audioPlayer;
	}

//	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		localPlayer = event.getEntity();
		sendMessages(event.getEntity());
		playerLoggedIn = true;
		player.stop();
	}

	private static void sendMessages(Player player) {
	    Iterator<String> iterator = queuedMessages.iterator();
	    while (iterator.hasNext()) {
	        String message = iterator.next();
	        Component messageComponent = Component.literal("<Startup Music Mod> " + message);
	        player.sendSystemMessage(messageComponent);
	        iterator.remove(); // Correctly removes the current element using the iterator
	    }
	}
	
	@SubscribeEvent
	public static void onGameShuttingDown(GameShuttingDownEvent event) {
		StartupMusicalTone.onGameShuttingDown(event);
	}
}
