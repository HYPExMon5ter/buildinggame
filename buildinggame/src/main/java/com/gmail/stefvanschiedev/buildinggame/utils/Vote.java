package com.gmail.stefvanschiedev.buildinggame.utils;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a vote on a plot
 *
 * @since 2.1.0
 */
public class Vote {

    /**
     * The player who gave the vote
     */
	private final Player sender;

    /**
     * Constructs a new vote
     *
     * @param sender the player who gave the vote
     */
	public Vote(Player sender) {
		this.sender = sender;
	}

    /**
     * Returns the player who gave this vote
     *
     * @return the player
     * @since 2.1.0
     */
    @NotNull
    @Contract(pure = true)
	public Player getSender() {
		return sender;
	}
}