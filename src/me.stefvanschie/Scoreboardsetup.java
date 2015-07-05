package me.stefvanschie;

import org.bukkit.scoreboard.DisplaySlot;

public class Scoreboardsetup
{
	public static void setup()
	{
        BuildingGame.main.scoreboard = BuildingGame.main.manager.getNewScoreboard();
        BuildingGame.main.objective = BuildingGame.main.scoreboard.registerNewObjective("votes", "dummy");
        BuildingGame.main.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        BuildingGame.main.objective.setDisplayName(BuildingGame.main.messages.getString("global.scoreboardHeader")
        		.replaceAll("&", "§"));
	}
}