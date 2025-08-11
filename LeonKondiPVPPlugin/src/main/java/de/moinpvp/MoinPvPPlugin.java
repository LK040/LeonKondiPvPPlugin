package de.moinpvp;

import de.moinpvp.commands.JoinCommand;
import de.moinpvp.commands.StartGameCommand;
import de.moinpvp.commands.TeamResetCommand;
import de.moinpvp.game.GameManager;
import de.moinpvp.game.RespawnManager;
import de.moinpvp.listeners.ChatListener;
import de.moinpvp.listeners.CombatListener;
import de.moinpvp.listeners.PlayerLifecycleListener;
import de.moinpvp.listeners.PortalListener;
import de.moinpvp.team.TeamManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MoinPvPPlugin extends JavaPlugin {
    private TeamManager teamManager;
    private GameManager gameManager;
    private RespawnManager respawnManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        teamManager = new TeamManager(this);
        respawnManager = new RespawnManager(this, teamManager);
        gameManager = new GameManager(this, teamManager, respawnManager);
        respawnManager.setGameManager(gameManager);

        JoinCommand joinCmd = new JoinCommand(teamManager);
        getCommand("join").setExecutor(joinCmd);
        getCommand("join").setTabCompleter(joinCmd);
        getCommand("teamreset").setExecutor(new TeamResetCommand(teamManager));
        getCommand("startgame").setExecutor(new StartGameCommand(gameManager));

        getServer().getPluginManager().registerEvents(new CombatListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new ChatListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new PortalListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerLifecycleListener(gameManager, respawnManager), this);
    }

    @Override
    public void onDisable() {
    }
}
