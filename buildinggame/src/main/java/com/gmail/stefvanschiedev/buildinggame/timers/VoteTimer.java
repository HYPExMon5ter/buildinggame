package com.gmail.stefvanschiedev.buildinggame.timers;

import com.gmail.stefvanschiedev.buildinggame.managers.arenas.ArenaManager;
import com.gmail.stefvanschiedev.buildinggame.managers.files.SettingsManager;
import com.gmail.stefvanschiedev.buildinggame.managers.messages.MessageManager;
import com.gmail.stefvanschiedev.buildinggame.timers.utils.Timer;
import com.gmail.stefvanschiedev.buildinggame.utils.GameState;
import com.gmail.stefvanschiedev.buildinggame.utils.Vote;
import com.gmail.stefvanschiedev.buildinggame.utils.arena.Arena;
import com.gmail.stefvanschiedev.buildinggame.utils.gameplayer.GamePlayer;
import com.gmail.stefvanschiedev.buildinggame.utils.plot.Plot;
import com.gmail.stefvanschiedev.buildinggame.utils.region.Region;
import org.bukkit.Bukkit;
import org.bukkit.WeatherType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This handles the voting time for this arena
 *
 * @since 2.1.0
 */
public class VoteTimer extends Timer {

	/**
     * The original amount of seconds per plot
     */
	private final int originalSeconds;

	/**
     * The plot which is currently being voted on
     */
	private Plot plot;

    /**
     * The list of players plots we haven't seen
     */
    private ArrayList<Player> plotsVisited;

    /**
     * The player of the plot we're visiting
     */
    private Player visiting;

	/**
     * The config.yml YAML configuration
     */
	private final YamlConfiguration config = SettingsManager.getInstance().getConfig();

	/**
     * The messages.yml YAML configuration
     */
	private final YamlConfiguration messages = SettingsManager.getInstance().getMessages();

	/**
     * Constructs a new VoteTimer with the given amount of seconds
     *
     * @param seconds the amount of time per plot
     * @param arena the arena this timer belongs to
     */
	public VoteTimer(int seconds, Arena arena) {
	    super(arena);

		this.seconds = seconds;
		originalSeconds = seconds;
	}

    /**
     * Called whenever a second has passed. This will generate a new plot if needed or end the game if all plots have
     * been voted on.
     *
     * @since 2.1.0
     */
    @Override
    public void run() {
        running = true;

        //Grabs a plot to teleport to
        if (plotsVisited == null || plotsVisited.isEmpty()) {
            plotsVisited = new ArrayList<Player>();
            arena.getUsedPlots().forEach(plot -> plot.getAllGamePlayers().forEach(gamePlayer -> {
                var player = gamePlayer.getPlayer();
                plotsVisited.add(player);
            }));
        }

        if (seconds == originalSeconds) {
            visiting = plotsVisited.listIterator().next();
            //Bukkit.getLogger().warning("Visiting: " + visiting.getName());

            plot = arena.getPlot(visiting);

            if (Bukkit.getPluginManager().isPluginEnabled("WorldEdit")) {
                Region region = plot.getBoundary();

                var fileName = plot.getArena().getSubject() + "-" + visiting.getName() + ".schem";
                var file = new File(SettingsManager.getInstance().getWinnerSchematicsFolder(), fileName);

                region.saveSchematic(file);
            }

            arena.getUsedPlots().forEach(plot -> plot.getAllGamePlayers().forEach(gamePlayer -> {
                var player = gamePlayer.getPlayer();

                if (!config.getBoolean("names-after-voting") &&
                    config.getBoolean("scoreboards.vote.enable")) {
                    arena.getVoteScoreboard(plot).show(player);
                }

                player.setPlayerTime(this.plot.getTime(), false);
                player.setPlayerWeather(this.plot.isRaining() ? WeatherType.DOWNFALL : WeatherType.CLEAR);
            }));

            arena.setVotingPlot(plot);


        }

        if (seconds <= 0) {
            arena.getUsedPlots().stream().flatMap(plot -> plot.getGamePlayers().stream()).forEach(player -> {
                Player pl = player.getPlayer();

                if (config.getBoolean("names-after-voting")) {
                    messages.getStringList("voting.message").forEach(message ->
                        MessageManager.getInstance().send(pl, message
                            .replace("%playerplot%", visiting.getName())));

                    player.addTitleAndSubtitle(messages.getString("voting.title")
                            .replace("%playerplot%", visiting.getName()),
                        messages.getString("voting.subtitle")
                            .replace("%playerplot%", visiting.getName()));
                    player.sendActionbar(messages.getString("voting.actionbar")
                        .replace("%playerplot%", visiting.getName()));
                }
            });
            plotsVisited.remove(visiting);

            //called on last plot, resetting phase

            if (plotsVisited.isEmpty()) {
                //resetting
                arena.setState(GameState.RESETING);
                plotsVisited.clear();
                running = false;
                this.cancel();

                arena.getUsedPlots().stream().flatMap(plot -> plot.getGamePlayers().stream()).forEach(player -> {
                    Player pl = player.getPlayer();
                    plot.getArena().getLobby().teleport(pl);
                });

                return;
            }
            seconds = originalSeconds;
        }

        seconds--;
    }
}