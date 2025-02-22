package com.gmail.stefvanschiedev.buildinggame.managers.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import com.gmail.stefvanschiedev.buildinggame.Main;
import com.gmail.stefvanschiedev.buildinggame.managers.arenas.ArenaManager;
import com.gmail.stefvanschiedev.buildinggame.managers.files.SettingsManager;
import com.gmail.stefvanschiedev.buildinggame.managers.mainspawn.MainSpawnManager;
import com.gmail.stefvanschiedev.buildinggame.managers.messages.MessageManager;
import com.gmail.stefvanschiedev.buildinggame.timers.BuildTimer;
import com.gmail.stefvanschiedev.buildinggame.timers.FileCheckerTimer;
import com.gmail.stefvanschiedev.buildinggame.timers.VoteTimer;
import com.gmail.stefvanschiedev.buildinggame.utils.Booster;
import com.gmail.stefvanschiedev.buildinggame.utils.GameState;
import com.gmail.stefvanschiedev.buildinggame.utils.Vote;
import com.gmail.stefvanschiedev.buildinggame.utils.arena.Arena;
import com.gmail.stefvanschiedev.buildinggame.utils.arena.ArenaMode;
import com.gmail.stefvanschiedev.buildinggame.utils.gameplayer.GamePlayer;
import com.gmail.stefvanschiedev.buildinggame.utils.gameplayer.GamePlayerType;
import com.gmail.stefvanschiedev.buildinggame.utils.guis.ArenaSelection;
import com.gmail.stefvanschiedev.buildinggame.utils.guis.ReportMenu;
import com.gmail.stefvanschiedev.buildinggame.utils.item.ClickEvent;
import com.gmail.stefvanschiedev.buildinggame.utils.item.ItemBuilder;
import com.gmail.stefvanschiedev.buildinggame.utils.item.datatype.PlotDataType;
import com.gmail.stefvanschiedev.buildinggame.utils.plot.Plot;
import com.gmail.stefvanschiedev.buildinggame.utils.potential.PotentialLocation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * This class handles all subcommands for the buildinggame command
 *
 * @since 2.1.0
 */
@CommandAlias("buildinggame|bg")
public class CommandManager extends BaseCommand {

    /**
     * Called whenever a command sender wants to start a booster
     *
     * @param sender the command sender
     * @param multiplier the booster multiplier
     * @param duration the duration of the booster
     * @param player the player for which the booster is intended, when null, the booster is global
     * @since 5.8.0
     */
    @Subcommand("booster")
    @Description("Activate a booster")
    @CommandPermission("bg.booster")
    @CommandCompletion("@nothing @nothing @players")
    public void onBooster(CommandSender sender, float multiplier, int duration, @Optional Player player) {
        new Booster(sender, multiplier, duration, player).start();

        MessageManager.getInstance().send(sender, ChatColor.GREEN + "Activated " +
            (player == null ? "global booster" : "booster for " + player.getName()) + " for " + duration + " seconds");
    }

    /**
     * Called whenever a command sender wants to create a new arena
     *
     * @param sender the command sender
     * @param name the arena name
     * @since 5.8.0
     */
    @Subcommand("createarena")
    @Description("Create an arena")
    @CommandPermission("bg.createarena")
    public void onCreateArena(CommandSender sender, @Conditions("arenanotexist") @Single String name) {
        YamlConfiguration arenas = SettingsManager.getInstance().getArenas();
        YamlConfiguration config = SettingsManager.getInstance().getConfig();
        YamlConfiguration messages = SettingsManager.getInstance().getMessages();

        int buildTime = config.getInt("timers.build");
        int lobbyTime = config.getInt("timers.lobby");
        int voteTime = config.getInt("timers.vote");
        int winTime = config.getInt("timers.win");

        arenas.set(name + ".mode", "SOLO");
        arenas.set(name + ".timer", buildTime);
        arenas.set(name + ".lobby-timer", lobbyTime);
        arenas.set(name + ".vote-timer", voteTime);
        arenas.set(name + ".win-timer", winTime);
        SettingsManager.getInstance().save();

        Arena arena = new Arena(name);
        arena.setMode(ArenaMode.SOLO);
        arena.setBuildTimer(new BuildTimer(buildTime, arena));
        arena.setVoteTimer(new VoteTimer(voteTime, arena));

        ArenaManager.getInstance().getArenas().add(arena);

        messages.getStringList("commands.createarena.success").forEach(message ->
            MessageManager.getInstance().send(sender, message.replace("%arena%", name)));
    }

    /**
     * Called whenever a command sender wants to delete an arena
     *
     * @param sender the command sender
     * @param arena the arena to delete
     * @since 5.8.0
     */
    @Subcommand("deletearena")
    @Description("Delete an arena")
    @CommandPermission("bg.deletearena")
    @CommandCompletion("@arenas")
    public void onDeleteArena(CommandSender sender, Arena arena) {
        YamlConfiguration arenas = SettingsManager.getInstance().getArenas();
        YamlConfiguration messages = SettingsManager.getInstance().getMessages();

        arenas.set(arena.getName(), null);
        SettingsManager.getInstance().save();

        ArenaManager.getInstance().getArenas().remove(arena);

        messages.getStringList("commands.deletearena.success").forEach(message ->
            MessageManager.getInstance().send(sender, message.replace("%arena%", arena.getName())));
    }

    /**
     * Called whenever a command sender wants to delete a spawn
     *
     * @param sender the command sender
     * @param arena the arena
     * @param spawn the spawn point to delete
     * @since 5.8.0
     */
    @Subcommand("deletespawn")
    @Description("Delete a spawn")
    @CommandPermission("bg.deletespawn")
    @CommandCompletion("@arenas @nothing")
    public void onDeleteSpawn(CommandSender sender, Arena arena, int spawn) {
        YamlConfiguration arenas = SettingsManager.getInstance().getArenas();
        YamlConfiguration messages = SettingsManager.getInstance().getMessages();

        var plot = arena.getPlot(spawn);

        if (plot == null) {
            MessageManager.getInstance().send(sender,
                ChatColor.RED + "That's not a valid plot. Try to create one first.");
            return;
        }

        var maxPlayers = arenas.getInt(arena.getName() + ".maxplayers");

        arena.removePlot(plot);

        for (var i = plot.getId(); i < maxPlayers; i++) {
            arenas.set(arena.getName() + '.' + i, arenas.getConfigurationSection(arena.getName() + '.' + i + 1));

            if (i != plot.getId()) {
                arena.getPlot(i).setId(i - 1);
            }
        }

        arenas.set(arena.getName() + '.' + maxPlayers, null);
        arenas.set(arena.getName() + ".maxplayers", maxPlayers - 1);

        SettingsManager.getInstance().save();

        arena.setMaxPlayers(maxPlayers - 1);

        messages.getStringList("commands.deletespawn.success").forEach(message ->
            MessageManager.getInstance().send(sender, message.replace("%place%", plot.getId() + "")));
    }

    /**
     * Called when debugging
     */
/*    @Subcommand("debug")
    @Description("Debugging")
    @CommandPermission("bg.debug")
    @CommandCompletion("@nothing")
    public void onDebug(Player player) {
        YamlConfiguration messages = SettingsManager.getInstance().getMessages();

            var arena = ArenaManager.getInstance().getArena(player);

            if (arena != null) {
                MessageManager.getInstance().send(player, ChatColor.RED + arena);
            } else {
                MessageManager.getInstance().send(player, ChatColor.RED + "Not in an arena");
            }

    }*/

    /**
     * Called whenever a command sender wants to start an arena
     *
     * @param sender the command sender
     * @param arena the arena, if the sender is a player and the player is in an arena, this may be null
     */
    @Subcommand("forcestart")
    @Description("Force an arena to start")
    @CommandPermission("bg.forcestart")
    @CommandCompletion("@arenas @nothing")
    public void onForceStart(CommandSender sender, @Optional Arena arena, @Optional String theme) {
        YamlConfiguration messages = SettingsManager.getInstance().getMessages();
        if (sender instanceof Player player) {
            var playerArena = ArenaManager.getInstance().getArena(player);

            if (playerArena != null && arena == null) {
                if (playerArena.getState() != GameState.WAITING && playerArena.getState() != GameState.STARTING &&
                    playerArena.getState() != GameState.FULL) {
                    MessageManager.getInstance().send(sender, ChatColor.RED + "The arena is already in game");
                    return;
                }
                if (theme != null) {
                    playerArena.getSubjectMenu().forceTheme(theme);
                    messages.getStringList("commands.forcetheme.success").forEach(message ->
                        MessageManager.getInstance().send(player, message.replace("%theme%", theme)));
                }
                playerArena.preStart();
                MessageManager.getInstance().send(sender, ChatColor.GREEN + "Started arena");
                return;
            }
        }

        if (arena == null) {
            MessageManager.getInstance().send(sender, ChatColor.RED + "Please specify an arena");
            return;
        }

        if (arena.getPlayers() < 1) {
            MessageManager.getInstance().send(sender,
                ChatColor.RED + "Arena could not start. There are no players!");
            return;
        }

        if (arena.getState() != GameState.WAITING && arena.getState() != GameState.STARTING &&
            arena.getState() != GameState.FULL) {
            MessageManager.getInstance().send(sender, ChatColor.RED + "The arena is already in game");
            return;
        }

        if (theme != null) {
            arena.getSubjectMenu().forceTheme(theme);
            messages.getStringList("commands.forcetheme.success").forEach(message ->
                MessageManager.getInstance().send(sender, message.replace("%theme%", theme)));
        }

        arena.preStart();
        MessageManager.getInstance().send(sender, ChatColor.GREEN + "Started arena");
    }

    /**
     * Called whenever a command sender wants to start an arena
     *
     * @param sender the command sender
     * @param arena the arena, if the sender is a player and the player is in an arena, this may be null
     */
    @Subcommand("forcestop")
    @Description("Force an arena to stop")
    @CommandPermission("bg.forcestop")
    @CommandCompletion("@arenas")
    public void onForceStop(CommandSender sender, @Optional Arena arena) {
        if (sender instanceof Player player) {
            var playerArena = ArenaManager.getInstance().getArena(player);

            if (playerArena != null && arena == null) {
                if (playerArena.getState() == GameState.WAITING) {
                    MessageManager.getInstance().send(sender, ChatColor.RED + "You can't stop a game that hasn't started");
                    return;
                }
                playerArena.forceStop();
                MessageManager.getInstance().send(sender, ChatColor.GREEN + "Stopped arena");
                return;
            }
        }

        if (arena == null) {
            MessageManager.getInstance().send(sender, ChatColor.RED + "Please specify an arena");
            return;
        }

        if (arena.getState() == GameState.WAITING) {
            MessageManager.getInstance().send(sender, ChatColor.RED + "You can't stop a game that hasn't started");
            return;
        }
        arena.forceStop();
        MessageManager.getInstance().send(sender, ChatColor.GREEN + "Stopped arena");
    }

    /**
     * Called whenever a players wants to specify a theme
     *
     * @param player the player
     * @param theme the theme
     * @since 5.8.0
     */
    @Subcommand("forcetheme")
    @Description("Force a theme to be chosen")
    @CommandPermission("bg.forcetheme")
    @CommandCompletion("@nothing")
    public void onForceTheme(Player player, String theme) {
        YamlConfiguration messages = SettingsManager.getInstance().getMessages();

        var arena = ArenaManager.getInstance().getArena(player);

        if (arena == null) {
            MessageManager.getInstance().send(player, ChatColor.RED + "You aren't in an arena");
            return;
        }

        arena.getSubjectMenu().forceTheme(theme);

        messages.getStringList("commands.forcetheme.success").forEach(message ->
            MessageManager.getInstance().send(player, message.replace("%theme%", theme)));
    }

    /**
     * Called whenever a players wants to specify a winner
     *
     * @param player the player sending the command
     * @param winner the winner
     * @since 12.0.1
     */
    @Subcommand("winner")
    @Description("Force a winner to be chosen")
    @CommandPermission("bg.winner")
    @CommandCompletion("@players")
    public void onSetWinner(Player player, String winner) {
        var playerArena = ArenaManager.getInstance().getArena("Game1");

        if (playerArena != null) {
            if (playerArena.getState() == GameState.BUILDING) {
                MessageManager.getInstance().send(player, ChatColor.RED + "You can't set a winner yet");
                return;
            }
            if (winner == null) {
                MessageManager.getInstance().send(player, ChatColor.RED + "Please specify a winner");
                return;
            }

            //MessageManager.getInstance().send(player, ChatColor.GREEN + "Player: " + player.getName() + " | Winner: " + winner);

            playerArena.getUsedPlots().stream().flatMap(plot -> plot.getGamePlayers().stream()).forEach(arenaPlayer -> {
                arenaPlayer.addTitleAndSubtitle(ChatColor.DARK_AQUA + "The winner is", ChatColor.AQUA + winner + "!");
            });

            playerArena.nextMatch();

        }
    }

    /**
     * Called whenever a player wants to join an arena
     *
     * @param player the player
     * @param arena the arena to join
     * @since 5.8.0
     */
    @Subcommand("join")
    @Description("Join an arena")
    @CommandPermission("bg.join")
    @CommandCompletion("@arenas")
    public void onJoin(Player player, @Optional Arena arena) {
        YamlConfiguration messages = SettingsManager.getInstance().getMessages();

        if (arena == null) {
            for (Arena a : ArenaManager.getInstance().getArenas()) {
                if (!a.canJoin()) {
                    continue;
                }

                new ArenaSelection().show(player);
                return;
            }

            MessageManager.getInstance().send(player, messages.getStringList("join.no-arena"));
            return;
        }

        arena.join(player);
    }

    /**
     * Called whenever a player wants to leave the currently joined arena
     *
     * @param player the player who wants to leave
     * @since 5.8.0
     */
    @Subcommand("leave")
    @Description("Leave the arena you're in")
    @CommandPermission("bg.leave")
    public void onLeave(Player player) {
        var arena = ArenaManager.getInstance().getArena(player);

        if (arena == null) {
            MessageManager.getInstance().send(player, ChatColor.RED + "You're not in an arena");
            return;
        }

        arena.leave(player, true);
    }

    /**
     * Called whenever a command sender wants to view a list of all arenas
     *
     * @param sender the command sender who wants to view all arenas
     * @since 5.8.0
     */
    @Subcommand("list")
    @Description("List all arenas")
    @CommandPermission("bg.list")
    public void onList(CommandSender sender) {
        ArenaManager.getInstance().getArenas().forEach(arena ->
            MessageManager.getInstance().send(sender, ChatColor.DARK_AQUA + " - " + arena.getName() +
                ChatColor.DARK_GREEN + " - " + arena.getState().toString().toLowerCase(Locale.getDefault())));
    }

    /**
     * Called whenever a command sender wants to reload the plugin
     *
     * @param sender the command sender
     * @since 5.8.0
     */
    @Subcommand("reload")
    @Description("Reload the plugin")
    @CommandPermission("bg.reload")
    public void onReload(CommandSender sender) {
        ArenaManager.getInstance().getArenas().stream().filter(arena -> arena.getPlayers() > 0).forEach(Arena::stop);

        FileCheckerTimer runnable = SettingsManager.getInstance().getRunnable();

        if (!runnable.isCancelled())
            runnable.cancel();

        Main.getInstance().loadPlugin(true);

        MessageManager.getInstance().send(sender, ChatColor.GREEN + "Reloaded the plugin!");
    }

    /**
     * Called when a player wants to view the reportsd
     *
     * @param player the player executing this command
     * @since 6.5.0
     */
    @Subcommand("reports")
    @Description("Show all reports made")
    @CommandPermission("bg.reports")
    public void onReports(Player player) {
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldEdit")) {
            player.sendMessage(ChatColor.RED + "WorldEdit needs to be enabled in order for this feature to work.");
            return;
        }

        new ReportMenu().show(player);
    }

    /**
     * Called whenever a player wants to set the boundary of a plot
     *
     * @param player the player
     * @param arena the arena
     * @param id the plot id to set the boundary of
     * @since 5.8.0
     */
    @Subcommand("setbounds")
    @Description("Set the boundary of a plot (inclusive)")
    @CommandPermission("bg.setbounds")
    @CommandCompletion("@arenas @nothing")
    public void onSetBounds(Player player, Arena arena, int id) {
        final var plot = arena.getPlot(id);

        if (plot == null) {
            MessageManager.getInstance().send(player, ChatColor.RED + "That's not a valid plot");
            return;
        }

        player.getInventory().setItemInMainHand(new ItemBuilder(player, Material.STICK)
            .setDisplayName(ChatColor.LIGHT_PURPLE + "Wand")
            .addContext("plot", PlotDataType.getInstance(), plot)
            .setClickEvent(ClickEvent.BOUNDS_CLICK)
            .build()
        );
    }

    /**
     * Called whenever a player wants to set the floor of a plot
     *
     * @param player the player
     * @param arena the arena
     * @param id the plot id to set the floor of
     * @since 5.8.0
     */
    @Subcommand("setfloor")
    @Description("Set the floor of a plot (inclusive)")
    @CommandPermission("bg.setfloor")
    @CommandCompletion("@arenas @nothing")
    public void onSetFloor(Player player, Arena arena, int id) {
        final var plot = arena.getPlot(id);

        if (plot == null) {
            MessageManager.getInstance().send(player, ChatColor.RED + "That's not a valid plot");
            return;
        }

        player.getInventory().setItemInMainHand(new ItemBuilder(player, Material.STICK)
            .setDisplayName(ChatColor.LIGHT_PURPLE + "Wand")
            .addContext("plot", PlotDataType.getInstance(), plot)
            .setClickEvent(ClickEvent.FLOOR_CLICK)
            .build()
        );
    }

    /**
     * Called whenever a command sender wants to change the game mode of an arena
     *
     * @param sender the command sender
     * @param arena the arena
     * @param arenaMode the new arena mode
     * @since 5.8.0
     */
    @Subcommand("setgamemode")
    @Description("Set the game mode of an arena")
    @CommandPermission("bg.setgamemode")
    @CommandCompletion("@arenas @arenamodes")
    public void onSetGameMode(CommandSender sender, Arena arena, ArenaMode arenaMode) {
        YamlConfiguration arenas = SettingsManager.getInstance().getArenas();

        arenas.set(arena.getName() + ".mode", arenaMode.toString());
        SettingsManager.getInstance().save();

        arena.setMode(arenaMode);

        MessageManager.getInstance().send(sender, ChatColor.GREEN +
            "Successfully changed game mode of arena " + arena.getName() + " to " +
            arenaMode.toString().toLowerCase(Locale.getDefault()));
    }

    /**
     * Called whenever a player wants to set the lobby of an arena
     *
     * @param player the player
     * @param arena the arena which lobby will be changed
     * @since 5.8.0
     */
    @Subcommand("setlobby")
    @Description("Set the lobby")
    @CommandPermission("bg.setlobby")
    @CommandCompletion("@arenas")
    public void onSetLobby(Player player, Arena arena) {
        YamlConfiguration arenas = SettingsManager.getInstance().getArenas();
        YamlConfiguration messages = SettingsManager.getInstance().getMessages();

        Location location = player.getLocation();
        String worldName = location.getWorld().getName();
        int blockX = location.getBlockX();
        int blockY = location.getBlockY();
        int blockZ = location.getBlockZ();
        float pitch = location.getPitch();
        float yaw = location.getYaw();

        arenas.set(arena.getName() + ".lobby.server", player.getServer().getName());
        arenas.set(arena.getName() + ".lobby.world", worldName);
        arenas.set(arena.getName() + ".lobby.x", blockX);
        arenas.set(arena.getName() + ".lobby.y", blockY);
        arenas.set(arena.getName() + ".lobby.z", blockZ);
        arenas.set(arena.getName() + ".lobby.pitch", pitch);
        arenas.set(arena.getName() + ".lobby.yaw", yaw);
        SettingsManager.getInstance().save();

        arena.setLobby(new PotentialLocation(() -> Bukkit.getWorld(worldName), blockX, blockY, blockZ, yaw, pitch));

        MessageManager.getInstance().send(player, messages.getStringList("commands.setlobby.success"));
    }

    /**
     * Called whenever a player wants to set the main spawn
     *
     * @param player the player
     * @since 5.8.0
     */
    @Subcommand("setmainspawn")
    @Description("Set the main spawn")
    @CommandPermission("bg.setmainspawn")
    public void onSetMainSpawn(Player player) {
        YamlConfiguration arenas = SettingsManager.getInstance().getArenas();
        YamlConfiguration config = SettingsManager.getInstance().getConfig();
        YamlConfiguration messages = SettingsManager.getInstance().getMessages();

        List<String> worlds = config.getStringList("scoreboards.main.worlds.enable");

        if (arenas.contains("main-spawn.world"))
            worlds.remove(arenas.getString("main-spawn.world"));

        Location location = player.getLocation();

        worlds.add(location.getWorld().getName());
        config.set("scoreboards.main.worlds.enable", worlds);

        arenas.set("main-spawn.server", player.getServer().getName());
        arenas.set("main-spawn.world", location.getWorld().getName());
        arenas.set("main-spawn.x", location.getBlockX());
        arenas.set("main-spawn.y", location.getBlockY());
        arenas.set("main-spawn.z", location.getBlockZ());
        arenas.set("main-spawn.pitch", location.getPitch());
        arenas.set("main-spawn.yaw", location.getYaw());
        SettingsManager.getInstance().save();

        MainSpawnManager.getInstance().setMainSpawn(new PotentialLocation(location));
        MessageManager.getInstance().send(player, messages.getStringList("commands.setmainspawn.success"));
    }

    /**
     * Called whenever a command sender wants to set the amount of matches
     *
     * @param sender the command sender
     * @param arena the arena
     * @param matches the new amount of matches
     * @since 5.8.0
     */
    @Subcommand("setmatches")
    @Description("Set the amount of matches to play")
    @CommandPermission("bg.setmatches")
    @CommandCompletion("@arenas @nothing")
    public void onSetMatches(CommandSender sender, Arena arena, int matches) {
        YamlConfiguration arenas = SettingsManager.getInstance().getArenas();

        if (!arena.isEmpty()) {
            MessageManager.getInstance().send(sender, ChatColor.RED +
                "The arena isn't empty, changing the matches now is likely to cause problems. Please wait until the arena is empty.");
            return;
        }

        arenas.set(arena.getName() + ".matches", matches);
        SettingsManager.getInstance().save();

        arena.setMaxMatches(matches);

        MessageManager.getInstance().send(sender, ChatColor.GREEN + "Amount of matches changed!");
    }

    /**
     * Called whenever a command sender wants to set the maximum amount of players
     *
     * @param sender the command sender
     * @param arena the arena
     * @param maxPlayers the maximum amount of players
     * @since 5.8.0
     */
    @Subcommand("setmaxplayers")
    @Description("Set the maximum amount of players in an arena")
    @CommandPermission("bg.setmaxplayers")
    @CommandCompletion("@arenas @nothing")
    public void onSetMaxPlayers(CommandSender sender, Arena arena, int maxPlayers) {
        YamlConfiguration arenas = SettingsManager.getInstance().getArenas();
        YamlConfiguration config = SettingsManager.getInstance().getConfig();

        if (arena.getMode() == ArenaMode.SOLO) {
            MessageManager.getInstance().send(sender,
                ChatColor.RED + "You can only modify the maximum amount of players from arenas which are in team mode");
            return;
        }

        if (arena.getPlots().isEmpty()) {
            MessageManager.getInstance().send(sender,
                ChatColor.RED + "You first need to create plots before setting the max players");
            return;
        }

        if (maxPlayers % arena.getPlots().size() != 0) {
            MessageManager.getInstance().send(sender, ChatColor.RED +
                "Your max players has to be a number divisible by " + arena.getPlots().size());
            return;
        }

        arenas.set(arena.getName() + ".maxplayers", maxPlayers);

        //add parts to config
        for (int i = 0; i < maxPlayers; i++) {
            if (config.contains("team-selection.team." + i))
                continue;

            config.set("team-selection.team." + i + ".id", "paper");
        }

        SettingsManager.getInstance().save();

        arena.setMaxPlayers(maxPlayers);

        MessageManager.getInstance().send(sender, ChatColor.GREEN + "Max players changed!");
    }

    /**
     * Called whenever a command sender wants to set the minimum amount of players
     *
     * @param sender the command sender
     * @param arena the arena
     * @param minPlayers the minimum amount of players
     * @since 5.8.0
     */
    @Subcommand("setminplayers")
    @Description("Set the minimum amount of players")
    @CommandPermission("bg.setminplayers")
    @CommandCompletion("@arenas @nothing")
    public void onSetMinPlayers(CommandSender sender, Arena arena, int minPlayers) {
        YamlConfiguration arenas = SettingsManager.getInstance().getArenas();
        YamlConfiguration messages = SettingsManager.getInstance().getMessages();

        arenas.set(arena.getName() + ".minplayers", minPlayers);
        SettingsManager.getInstance().save();

        arena.setMinPlayers(minPlayers);

        MessageManager.getInstance().send(sender, messages.getStringList("commands.setminplayers.success"));
    }

    /**
     * Called whenever a player wants to set a new spawn
     *
     * @param player the player
     * @param arena the arena
     * @since 5.8.0
     */
    @Subcommand("setspawn")
    @Description("Set a new spawn")
    @CommandPermission("bg.setspawn")
    @CommandCompletion("@arenas")
    public void onSetSpawn(Player player, Arena arena) {
        YamlConfiguration arenas = SettingsManager.getInstance().getArenas();
        YamlConfiguration messages = SettingsManager.getInstance().getMessages();

        int place = arena.getMaxPlayers() + 1;
        String name = arena.getName();

        Location location = player.getLocation();
        String worldName = location.getWorld().getName();
        int blockX = location.getBlockX();
        int blockY = location.getBlockY();
        int blockZ = location.getBlockZ();
        float pitch = location.getPitch();
        float yaw = location.getYaw();

        arenas.set(name + '.' + place + ".server", player.getServer().getName());
        arenas.set(name + '.' + place + ".world", worldName);
        arenas.set(name + '.' + place + ".x", blockX);
        arenas.set(name + '.' + place + ".y", blockY);
        arenas.set(name + '.' + place + ".z", blockZ);
        arenas.set(name + '.' + place + ".pitch", pitch);
        arenas.set(name + '.' + place + ".yaw", yaw);
        arenas.set(name + ".maxplayers", place);
        SettingsManager.getInstance().save();

        Plot plot = new Plot(arena, place);
        plot.setLocation(new PotentialLocation(() -> Bukkit.getWorld(worldName), blockX, blockY, blockZ, yaw, pitch));

        arena.addPlot(plot);
        arena.setMaxPlayers(place);

        messages.getStringList("commands.setspawn.success").forEach(message ->
            MessageManager.getInstance().send(player, message.replace("%place%", place + "")));
    }

    /**
     * Called whenever a command sender wants to change the timer
     *
     * @param sender the command sender
     * @param arena the arena
     * @param seconds the new amount of seconds
     * @since 5.8.0
     */
    @Subcommand("settimer")
    @Description("Change the timer")
    @CommandPermission("bg.settimer")
    @CommandCompletion("@arenas @nothing")
    public void onSetTimer(CommandSender sender, Arena arena, int seconds) {
        YamlConfiguration arenas = SettingsManager.getInstance().getArenas();

        arenas.set(arena.getName() + ".timer", seconds);
        SettingsManager.getInstance().save();

        arena.setBuildTimer(new BuildTimer(seconds, arena));

        MessageManager.getInstance().send(sender, ChatColor.GREEN + "Timer setting for arena '" +
            arena.getName() + "' changed to '" + seconds + '\'');
    }

    /**
     * Called whenever a command sender wants to change a setting
     *
     * @param sender the command sender
     * @param path the path in the config file
     * @param args the arguments
     * @since 5.8.0
     */
    @Subcommand("setting")
    @Description("Change a setting")
    @CommandPermission("bg.setting")
    public void onSetting(CommandSender sender, String path, @Split(" ") String[] args) {
        YamlConfiguration config = SettingsManager.getInstance().getConfig();

        if (!config.contains(path)) {
            MessageManager.getInstance().send(sender, ChatColor.RED + "That setting doesn't exist");
            return;
        }

        if (config.isList(path)) {
            if (args.length < 2) {
                MessageManager.getInstance().send(sender, ChatColor.RED +
                    "Please specify if you want to add or remove this value and the value");
                return;
            }

            if (args[0].equalsIgnoreCase("add")) {
                //add the value
                List<String> list = config.getStringList(path);
                list.add(args[1]);
                config.set(path, list);

                MessageManager.getInstance().send(sender, ChatColor.GREEN + "Value added to config");
            } else if (args[0].equalsIgnoreCase("remove")) {
                //remove the value
                List<String> list = config.getStringList(path);
                list.remove(args[1]);
                config.set(path, list);

                MessageManager.getInstance().send(sender, ChatColor.GREEN + "Value removed from config");
            }

            SettingsManager.getInstance().save();
            return;
        }

        //whole bunch of checking
        if (config.isBoolean(path))
            config.set(path, Boolean.parseBoolean(args[0]));
        else if (config.isDouble(path)) {
            try {
                config.set(path, Double.parseDouble(args[0]));
            } catch (NumberFormatException e) {
                MessageManager.getInstance().send(sender, ChatColor.RED +
                    "Value type isn't the same as in the config, it should be a double");
                return;
            }
        } else if (config.isInt(path)) {
            try {
                config.set(path, Integer.parseInt(args[0]));
            } catch (NumberFormatException e) {
                MessageManager.getInstance().send(sender, ChatColor.RED +
                    "Value type isn't the same as in the config, it should be an integer");
                return;
            }
        } else if (config.isLong(path)) {
            try {
                config.set(path, Long.parseLong(args[0]));
            } catch (NumberFormatException e) {
                MessageManager.getInstance().send(sender, ChatColor.RED +
                    "Value type isn't the same as in the config, it should be a long");
                return;
            }
        } else if (config.isString(path))
            config.set(path, args[0]);
        else {
            MessageManager.getInstance().send(sender, ChatColor.YELLOW +
                "Unable to change setting with commands, please change this setting by hand");
            return;
        }

        MessageManager.getInstance().send(sender, ChatColor.GREEN + "Value changed in config");
        SettingsManager.getInstance().save();
    }

    /**
     * Called whenever a command sender wants to change the vote timer
     *
     * @param sender the command sender
     * @param arena the arena
     * @param seconds the amount of seconds
     * @since 5.8.0
     */
    @Subcommand("setvotetimer")
    @Description("Change the vote timer")
    @CommandPermission("bg.setvotetimer")
    @CommandCompletion("@arenas @nothing")
    public void onSetVoteTimer(CommandSender sender, Arena arena, int seconds) {
        YamlConfiguration arenas = SettingsManager.getInstance().getArenas();

        arenas.set(arena.getName() + ".vote-timer", seconds);
        SettingsManager.getInstance().save();

        arena.setVoteTimer(new VoteTimer(seconds, arena));

        MessageManager.getInstance().send(sender, ChatColor.GREEN + "Vote timer setting for arena '" +
            arena.getName() + "' changed to '" + seconds + '\'');
    }

    /**
     * Called whenever a player wants to spectate
     *
     * @since 5.8.0
     */
    @Subcommand("spectate")
    @Description("Spectate a player")
    @CommandPermission("bg.spectate")
    public void onSpectate(Player player) {
        if (VoteTimer.spectators.contains(player)) {
            MessageManager.getInstance().send(player, ChatColor.RED + "You're not spectating the game anymore");
            VoteTimer.spectators.remove(player);
            return;
        }
        MessageManager.getInstance().send(player, ChatColor.GREEN + "You're now spectating the game");
        VoteTimer.spectators.add(player);
    }

    /**
     * Called whenever a player wants to vote on a plot
     *
     * @param player the player
     * @since 5.8.0
     */
    @Subcommand("vote")
    @Description("Vote on someone's plot")
    @CommandPermission("bg.vote")
    @CommandCompletion("@range:1-10")
    public void onVote(Player player) {
        YamlConfiguration messages = SettingsManager.getInstance().getMessages();

        var arena = ArenaManager.getInstance().getArena(player);

        if (arena == null) {
            MessageManager.getInstance().send(player, ChatColor.RED + "You're not in an arena");
            return;
        }

        if (arena.getState() != GameState.VOTING) {
            MessageManager.getInstance().send(player, ChatColor.RED + "We aren't in the voting stage yet");
            return;
        }

        var plot = arena.getVotingPlot();
        plot.addVote(new Vote(player));

        MessageManager.getInstance().send(player, messages.getString("vote.message")
            .replace("%playerplot%", plot.getPlayerFormat()));
    }



    /**
     * Shows an automatically generated help display to the sender
     *
     * @param sender the sender which should see the help menu
     * @param help the command supplied for the help system
     * @since 5.9.0
     */
    @HelpCommand
    public void doHelp(CommandSender sender, CommandHelp help) {
        help.showHelp();
    }
}