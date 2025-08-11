package de.moinpvp.game;

import de.moinpvp.MoinPvPPlugin;
import de.moinpvp.team.TeamColor;
import de.moinpvp.team.TeamManager;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;

public class GameManager {
    private final MoinPvPPlugin plugin;
    private final TeamManager teamManager;
    private final RespawnManager respawnManager;

    private boolean running = false;
    private boolean allowNewRespawns = true;
    private BukkitTask borderAnnouncerTask;
    private int lastAnnouncedHundreds = Integer.MAX_VALUE;
    private final Map<UUID, Integer> kills = new HashMap<>();

    public GameManager(MoinPvPPlugin plugin, TeamManager teamManager, RespawnManager respawnManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.respawnManager = respawnManager;
    }

    public boolean isRunning() {
        return running;
    }

    public void startGame(CommandSender initiator) {
        if (running) {
            initiator.sendMessage(ChatColor.RED + "Spiel läuft bereits.");
            return;
        }
        if (!teamManager.everyoneHasATeam()) {
            initiator.sendMessage(ChatColor.RED + "Nicht alle Spieler sind in einem Team.");
            return;
        }

        teamManager.disableEmptyTeamsForThisGame();

        World world = Bukkit.getWorld(plugin.getConfig().getString("worldName", "world"));
        if (world == null) {
            initiator.sendMessage(ChatColor.RED + "Welt nicht gefunden.");
            return;
        }

        double initial = plugin.getConfig().getDouble("initialBorderDiameter", 4000);
        double finalSize = plugin.getConfig().getDouble("finalBorderDiameter", 100);
        long shrinkSeconds = plugin.getConfig().getLong("shrinkDurationSeconds", 7200);
        int announceEvery = plugin.getConfig().getInt("announceEveryBlocks", 100);

        WorldBorder border = world.getWorldBorder();
        border.setCenter(0, 0);
        border.setDamageAmount(0.5);
        border.setWarningTime(10);
        border.setWarningDistance(10);
        border.setSize(initial, 1);

        teamManager.setTeamChangeLocked(true);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.getInventory().clear();
            p.setFoodLevel(20);
            p.setSaturation(20f);
            if (p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH) != null) {
                p.setHealth(p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
            } else {
                p.setHealth(20.0);
            }
            p.setGameMode(GameMode.SURVIVAL);
        }

        Map<TeamColor, String> spawns = readSpawns();
        for (TeamColor color : teamManager.getActiveTeams()) {
            Set<Player> members = teamManager.getTeamPlayers(color);
            if (members.isEmpty()) continue;
            String s = spawns.get(color);
            if (s == null) continue;
            String[] parts = s.split(",");
            int x = Integer.parseInt(parts[0].trim());
            int z = Integer.parseInt(parts[1].trim());
            int y = world.getHighestBlockYAt(x, z) + 1;
            Location loc = new Location(world, x + 0.5, y, z + 0.5);
            for (Player p : members) {
                p.teleport(loc);
            }
        }

        border.setSize(finalSize, shrinkSeconds);
        running = true;
        allowNewRespawns = true;
        lastAnnouncedHundreds = (int) Math.ceil(initial / announceEvery);

        if (borderAnnouncerTask != null) borderAnnouncerTask.cancel();
        borderAnnouncerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            double size = world.getWorldBorder().getSize();
            int hundreds = (int) Math.floor(size / announceEvery);
            if (hundreds < lastAnnouncedHundreds) {
                lastAnnouncedHundreds = hundreds;
                int now = hundreds * announceEvery;
                Bukkit.broadcastMessage(ChatColor.AQUA + "Weltgrenze: " + now + " x " + now);
                if (now == 1000) {
                    Bukkit.broadcastMessage(ChatColor.GOLD + "Warnung: Ab 500x500 gibt es keine neuen Respawns mehr!");
                }
                if (now <= 500 && allowNewRespawns) {
                    allowNewRespawns = false;
                    Bukkit.broadcastMessage(ChatColor.RED + "Ab jetzt gibt es keine neuen Respawns mehr.");
                }
            }
            checkWinCondition();
        }, 40L, 100L);
        Bukkit.broadcastMessage(ChatColor.GREEN + "Spiel gestartet!");
    }

    private Map<TeamColor, String> readSpawns() {
        Map<TeamColor, String> map = new EnumMap<>(TeamColor.class);
        if (plugin.getConfig().isConfigurationSection("teamSpawns")) {
            for (String key : plugin.getConfig().getConfigurationSection("teamSpawns").getKeys(false)) {
                try {
                    TeamColor color = TeamColor.valueOf(key);
                    map.put(color, plugin.getConfig().getString("teamSpawns." + key));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return map;
    }

    public boolean areNewRespawnsAllowed() {
        return allowNewRespawns;
    }

    public void handleKillCredit(Player dead, Player killer) {
        if (!isRunning()) return;
        if (killer == null) return;
        kills.merge(killer.getUniqueId(), 1, Integer::sum);
    }

    public void onPlayerDied(Player player) {
        checkTeamEliminationAfterDeath(player.getUniqueId());
    }

    public void onPossibleElimination(UUID playerId) {
        checkTeamEliminationAfterDeath(playerId);
    }

    private void checkTeamEliminationAfterDeath(UUID playerId) {
        Map<TeamColor, Long> aliveCounts = new EnumMap<>(TeamColor.class);
        for (TeamColor color : teamManager.getActiveTeams()) {
            long alive = teamManager.getTeamPlayerIds(color).stream()
                    .map(Bukkit::getPlayer)
                    .filter(Objects::nonNull)
                    .filter(p -> p.getGameMode() == GameMode.SURVIVAL)
                    .filter(p -> !respawnManager.isWaiting(p.getUniqueId()))
                    .count();
            aliveCounts.put(color, alive);
        }
        for (Map.Entry<TeamColor, Long> e : aliveCounts.entrySet()) {
            if (e.getValue() == 0 && !teamManager.getTeamPlayers(e.getKey()).isEmpty()) {
                eliminateTeam(e.getKey());
            }
        }
        checkWinCondition();
    }

    private void eliminateTeam(TeamColor color) {
        Set<UUID> ids = teamManager.getTeamPlayerIds(color);
        for (UUID id : ids) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                respawnManager.cancelWaiting(p.getUniqueId());
                p.setGameMode(GameMode.SPECTATOR);
            }
        }
        Bukkit.broadcastMessage(ChatColor.RED + "Team " + color.chat() + color.name() + ChatColor.RED + " wurde eliminiert!");
    }

    public void checkWinCondition() {
        if (!running) return;
        List<TeamColor> contenders = teamManager.getActiveTeams().stream()
                .filter(color -> teamManager.getTeamPlayerIds(color).stream()
                        .map(Bukkit::getPlayer)
                        .filter(Objects::nonNull)
                        .anyMatch(p -> p.getGameMode() == GameMode.SURVIVAL && !respawnManager.isWaiting(p.getUniqueId())))
                .collect(Collectors.toList());
        if (contenders.size() == 1) {
            TeamColor winner = contenders.get(0);
            UUID top = kills.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
            String topName = top != null && Bukkit.getPlayer(top) != null ? Bukkit.getPlayer(top).getName() : "Niemand";
            int topKills = top != null ? kills.getOrDefault(top, 0) : 0;
            Bukkit.broadcastMessage(ChatColor.GREEN + "Team " + winner.chat() + winner.name() + ChatColor.GREEN + " hat gewonnen!");
            Bukkit.broadcastMessage(ChatColor.AQUA + "Top-Killer: " + topName + " (" + topKills + " Kills)");

            endGame();
        }
    }

    public void endGame() {
        running = false;
        allowNewRespawns = false;
        if (borderAnnouncerTask != null) {
            borderAnnouncerTask.cancel();
            borderAnnouncerTask = null;
        }
        World world = Bukkit.getWorld(plugin.getConfig().getString("worldName", "world"));
        if (world != null) {
            WorldBorder border = world.getWorldBorder();
            border.setSize(6_000_000);
            border.setCenter(0, 0);
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            respawnManager.cancelWaiting(p.getUniqueId());
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
            p.teleport(world != null ? world.getSpawnLocation() : p.getWorld().getSpawnLocation());
        }
        kills.clear();
        teamManager.setTeamChangeLocked(false);
        Bukkit.broadcastMessage(ChatColor.GRAY + "Spiel beendet. Teams können neu gewählt werden.");
    }
}
