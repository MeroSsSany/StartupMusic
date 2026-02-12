package dev.merosssany.musicaltone.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraftforge.fml.loading.FMLPaths;

public class FileManager {
	private static final Logger logger = LogUtils.getLogger();
	
	public static Path getGameDir() {
		return FMLPaths.CONFIGDIR.get();
	}
	
	public static void createFolder(String folderName, Path path) throws IOException {
        logger.debug("Creating folder {} at path: {}", folderName, path);
		Files.createDirectories(path.resolve(folderName));
	}
	
	public static void init() throws IOException {
		Path game = getGameDir();
		Path musicFolder = Path.of(game.toString(), "music");
		
		if (!musicFolder.toFile().exists()) createFolder("music",game);
	}
	
	public static File getFile(String path) throws IOException {
		Path game = getMusicFolder();
		File file = Path.of(game.toString(), path).toFile();
		if (file.exists()) return file;
		
		else throw new IOException("File does not exists at path: "+file.getAbsolutePath());
	}
	
	public static File getFile(Path path) throws IOException {
		File file = path.toFile();
		if (file.exists()) return file;
		else throw new IOException("File does not exists at path: "+file.getAbsolutePath());
	}
	
	public static Path getMusicFolder() throws IOException {
		Path game = getGameDir();
		Path music = Path.of(game.toString(), "music");
		
		if (music.toFile().exists()) return music;
		else {
			Path ret = game.resolve("music");
			Files.createDirectories(ret);
			return ret;
		}
	}
	
	public static File[] getAllFilesFrom(Path path) throws IOException {
		File file = path.toFile();
		
		if (file.isDirectory()) return file.listFiles();
		else throw new IOException("This is a file, not a directory at "+path.toAbsolutePath());
	}
	
	public static File getRandomFileFrom(Path path) throws IOException {
		File[] files = getAllFilesFrom(path);
		double rand = Math.floor(Math.random() * files.length);
		return files[(int) rand];
	}
}

