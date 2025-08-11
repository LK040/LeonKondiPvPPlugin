package de.moinpvp.listeners;

import de.moinpvp.game.GameManager;
import de.moinpvp.game.RespawnManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerLifecycleListener implements Listener {
    private final GameManager gameManager;
    private final RespawnManager respawnManager;

    public PlayerLifecycleListener(GameManager gameManager, RespawnManager respawnManager) {
        this.gameManager = gameManager;
        this.respawnManager = respawnManager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        gameManager.handleKillCredit(event.getEntity(), event.getEntity().getKiller());
        gameManager.onPlayerDied(event.getEntity());
        respawnManager.handleDeath(event.getEntity(), event.getEntity().getLocation());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (gameManager.isRunning()) {
            Player p = event.getPlayer();
            p.setGameMode(GameMode.SPECTATOR);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        respawnManager.onQuit(p);
        gameManager.onPossibleElimination(p.getUniqueId());
    }

    @EventHandler
    public void onMoveWhileWaiting(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (respawnManager.isWaiting(p.getUniqueId())) {
            Location from = event.getFrom();
            if (event.getTo() != null && !from.toVector().equals(event.getTo().toVector())) {
                event.setTo(from);
            }
        }
    }
}
