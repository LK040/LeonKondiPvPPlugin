package de.moinpvp.game;

import de.moinpvp.MoinPvPPlugin;
import de.moinpvp.team.TeamColor;
import de.moinpvp.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class RespawnManager {
    private final MoinPvPPlugin plugin;
    private final TeamManager teamManager;
    private GameManager gameManager;

    private static class WaitingInfo {
        UUID playerId;
        TeamColor team;
        long remainingMillis;
        Location anchor;
        BukkitTask task;

        WaitingInfo(UUID playerId, TeamColor team, long remainingMillis, Location anchor) {
            this.playerId = playerId;
            this.team = team;
            this.remainingMillis = remainingMillis;
            this.anchor = anchor.clone();
        }
    }

    private final Map<UUID, WaitingInfo> waitingPlayers = new HashMap<>();

    public RespawnManager(MoinPvPPlugin plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
    }

    public void setGameManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public boolean isWaiting(UUID playerId) {
        return waitingPlayers.containsKey(playerId);
    }

    public void cancelWaiting(UUID playerId) {
        WaitingInfo info = waitingPlayers.remove(playerId);
        if (info != null && info.task != null) info.task.cancel();
    }

    public void handleDeath(Player player, Location deathLoc) {
        if (gameManager == null || !gameManager.isRunning()) return;

        TeamColor color = teamManager.getPlayerTeam(player).orElse(TeamColor.NONE);
        if (color == TeamColor.NONE) {
            player.setGameMode(GameMode.SPECTATOR);
            return;
        }

        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(deathLoc);

        if (!gameManager.areNewRespawnsAllowed()) {
            player.sendMessage(ChatColor.RED + "Keine neuen Respawns mehr.");
            return;
        }

        extendAllWaitersOfTeam(color, 60_000);
        scheduleOrExtend(color, player.getUniqueId(), deathLoc, 60_000);
    }

    private void scheduleOrExtend(TeamColor team, UUID playerId, Location anchor, long baseMillis) {
        WaitingInfo existing = waitingPlayers.get(playerId);
        if (existing != null) {
            existing.remainingMillis += baseMillis;
            return;
        }
        WaitingInfo info = new WaitingInfo(playerId, team, baseMillis, anchor);
        waitingPlayers.put(playerId, info);
        Player p = Bukkit.getPlayer(playerId);
        if (p != null) p.sendMessage(ChatColor.YELLOW + "Du wirst in " + (baseMillis / 1000) + "s bei einem Teamkameraden respawnen.");

        info.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (gameManager == null || !gameManager.isRunning()) {
                cancelWaiting(playerId);
                return;
            }
            Player pl = Bukkit.getPlayer(playerId);
            if (pl == null) return;
            info.remainingMillis -= 1000;
            if (info.remainingMillis <= 0) {
                Optional<Location> teammateLoc = pickRandomAliveTeammateLocation(team);
                if (teammateLoc.isPresent()) {
                    cancelWaiting(playerId);
                    pl.setGameMode(GameMode.SURVIVAL);
                    pl.teleport(teammateLoc.get());
                    if (pl.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH) != null) {
                        pl.setHealth(pl.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
                    } else {
                        pl.setHealth(20.0);
                    }
                    pl.setFoodLevel(20);
                    pl.setSaturation(20f);
                    pl.sendMessage(ChatColor.GREEN + "Respawn bei einem Teamkameraden!");
                } else {
                    cancelWaiting(playerId);
                    pl.setGameMode(GameMode.SPECTATOR);
                }
            } else {
                if (pl.getLocation().distanceSquared(info.anchor) > 0.01) {
                    pl.teleport(info.anchor);
                }
                if (info.remainingMillis % 10_000 == 0) {
                    pl.sendMessage(ChatColor.GRAY + "Respawn in " + (info.remainingMillis / 1000) + "s...");
                }
            }
        }, 20L, 20L);
    }

    private Optional<Location> pickRandomAliveTeammateLocation(TeamColor team) {
        List<Player> alive = teamManager.getTeamPlayers(team).stream()
                .filter(p -> p.getGameMode() == GameMode.SURVIVAL)
                .filter(p -> !isWaiting(p.getUniqueId()))
                .toList();
        if (alive.isEmpty()) return Optional.empty();
        Player target = alive.get(new Random().nextInt(alive.size()));
        return Optional.of(target.getLocation());
    }

    private void extendAllWaitersOfTeam(TeamColor team, long millis) {
        for (WaitingInfo wi : waitingPlayers.values()) {
            if (wi.team == team) {
                wi.remainingMillis += millis;
                Player p = Bukkit.getPlayer(wi.playerId);
                if (p != null) {
                    p.sendMessage(ChatColor.RED + "+60s zusätzliche Wartezeit (Teamkamerad gestorben).");
                }
            }
        }
    }

    public void onQuit(Player p) {
        cancelWaiting(p.getUniqueId());
    }
}
