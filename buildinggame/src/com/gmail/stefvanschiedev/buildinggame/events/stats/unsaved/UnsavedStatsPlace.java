package com.gmail.stefvanschiedev.buildinggame.events.stats.unsaved;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import com.gmail.stefvanschiedev.buildinggame.managers.arenas.ArenaManager;
import com.gmail.stefvanschiedev.buildinggame.utils.GameState;
import com.gmail.stefvanschiedev.buildinggame.utils.arena.Arena;

/**
 * Handles unsaved block placed
 *
 * @since 2.2.0
 */
public class UnsavedStatsPlace implements Listener {

    /**
     * Handles unsaved block placed
     *
     * @param e an event representing a block being placed
     * @see BlockPlaceEvent
     * @since 2.2.0
     */
	@EventHandler(ignoreCancelled = true)
	public static void onBlockPlace(BlockPlaceEvent e) {
		Player player = e.getPlayer();
		
		Arena arena = ArenaManager.getInstance().getArena(player);
		
		if (arena == null)
			return;
		
		if (arena.getState() != GameState.BUILDING)
			return;
		
		arena.getPlot(player).getGamePlayer(player).setBlocksPlaced(arena.getPlot(player).getGamePlayer(player).getBlocksPlaced() + 1);
	}
}