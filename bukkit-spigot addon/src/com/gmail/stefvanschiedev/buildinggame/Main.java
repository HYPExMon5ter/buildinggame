package com.gmail.stefvanschiedev.buildinggame;

import com.gmail.stefvanschiedev.buildinggame.utils.JoinSign;
import com.gmail.stefvanschiedev.buildinggame.utils.bungeecord.BungeeCordHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.gmail.stefvanschiedev.buildinggame.commands.BuildingGameCommand;
import com.gmail.stefvanschiedev.buildinggame.events.block.JoinSignBreak;
import com.gmail.stefvanschiedev.buildinggame.events.block.JoinSignCreate;
import com.gmail.stefvanschiedev.buildinggame.events.player.signs.ClickJoinSign;
import com.gmail.stefvanschiedev.buildinggame.files.SettingsManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Main class for this plugin
 *
 * @since 2.1.0
 */
public class Main extends JavaPlugin {

    /**
     * An instance of this class for the singleton pattern
     */
	private static Main instance;

    /**
     * Called whenever this plugin is being enabled
     *
     * @since 2.1.0
     */
	@Override
	public void onEnable() {
		instance = this;
		
		CommandExecutor cmd = new BuildingGameCommand();
		
		getCommand("bg").setExecutor(cmd);
		getCommand("buildinggame").setExecutor(cmd);
		
		Bukkit.getPluginManager().registerEvents(new JoinSignBreak(), this);
		Bukkit.getPluginManager().registerEvents(new JoinSignCreate(), this);
		
		Bukkit.getPluginManager().registerEvents(new ClickJoinSign(), this);

        Bukkit.getPluginManager().registerEvents(BungeeCordHandler.getInstance(), this);
	
		SettingsManager.getInstance().setup(this);
        JoinSign.load();
		
		getLogger().info("BuildingGame - BungeeCord Addon has been enabled");
	}

    /**
     * Called whenever this plugin is being disabled
     *
     * @since 2.1.0
     */
	@Override
	public void onDisable() {
		instance = null;
		
		getLogger().info("BuildingGame - BungeeCord Addon has been disabled");
	}

    /**
     * Returns the instance of this class
     *
     * @return this class
     * @since 2.1.0
     */
	@NotNull
	@Contract(pure = true)
    public static Plugin getInstance() {
		return instance;
	}
}