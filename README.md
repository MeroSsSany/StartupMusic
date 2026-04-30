# 🎵 Startup Music Tone  

---
**Startup Music Tone** is a mod that plays any music at the startup of the game (while the game loading screen).

---
## 📥 Installation
1. Download the mod from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/startup-music)
2. Make sure you have **NeoForge** installed.
3. Drop the `.jar` file that you downloaded from CurseForge into your `/mods` folder.
4. Place your desired `.ogg`, `.oga`, `.mp3` or `.wav` file in the `config/music/` folder.
---
## ⚙️ How it Works
This mod hooks into the game's early bootstrap phase to initialize the OpenAL audio system before the vanilla music engine starts. 
Then it selects a random music from the folder `./config/music/` and plays it during the bootstrap.  

