package de.moinpvp.team;

import de.moinpvp.MoinPvPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.stream.Collectors;

public class TeamManager {
    private final MoinPvPPlugin plugin;
    private final Scoreboard scoreboard;
    private final Map<UUID, TeamColor> playerTeams = new HashMap<>();
    private final Set<TeamColor> disabledForThisGame = new HashSet<>();
    private boolean teamChangeLocked = false;

    public TeamManager(MoinPvPPlugin plugin) {
        this.plugin = plugin;
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        ensureScoreboardTeams();
    }

    private void ensureScoreboardTeams() {
        for (TeamColor color : List.of(TeamColor.RED, TeamColor.YELLOW, TeamColor.BLUE, TeamColor.GREEN)) {
            Team team = scoreboard.getTeam(color.scoreboardName());
            if (team == null) {
                team = scoreboard.registerNewTeam(color.scoreboardName());
            }
            team.setColor(color.chat());
            team.setAllowFriendlyFire(false);
            team.setCanSeeFriendlyInvisibles(true);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.HIDE_FOR_OTHER_TEAMS);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            team.setPrefix(color.chat().toString());
        }
    }

    public boolean isTeamChangeLocked() {
        return teamChangeLocked;
    }

    public void setTeamChangeLocked(boolean locked) {
        this.teamChangeLocked = locked;
    }

    public void resetAll() {
        playerTeams.clear();
        disabledForThisGame.clear();
        teamChangeLocked = false;
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(scoreboard);
        }
        for (TeamColor color : List.of(TeamColor.RED, TeamColor.YELLOW, TeamColor.BLUE, TeamColor.GREEN)) {
            Team t = scoreboard.getTeam(color.scoreboardName());
            if (t != null) {
                new HashSet<>(t.getEntries()).forEach(t::removeEntry);
            }
        }
    }

    public Optional<TeamColor> getPlayerTeam(Player player) {
        return Optional.ofNullable(playerTeams.get(player.getUniqueId()));
    }

    public boolean isInTeam(Player player) {
        return playerTeams.containsKey(player.getUniqueId());
    }

    public Set<Player> getTeamPlayers(TeamColor color) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> color.equals(playerTeams.get(p.getUniqueId())))
                .collect(Collectors.toSet());
    }

    public Set<UUID> getTeamPlayerIds(TeamColor color) {
        return playerTeams.entrySet().stream()
                .filter(e -> e.getValue() == color)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public Set<TeamColor> getActiveTeams() {
        EnumSet<TeamColor> active = EnumSet.noneOf(TeamColor.class);
        for (TeamColor c : TeamColor.values()) {
            if (c == TeamColor.NONE) continue;
            if (!disabledForThisGame.contains(c)) active.add(c);
        }
        return active;
    }

    public void disableEmptyTeamsForThisGame() {
        for (TeamColor color : List.of(TeamColor.RED, TeamColor.YELLOW, TeamColor.BLUE, TeamColor.GREEN)) {
            if (getTeamPlayers(color).isEmpty()) {
                disabledForThisGame.add(color);
            }
        }
    }

    public boolean joinTeam(Player player, TeamColor color) {
        if (color == null || color == TeamColor.NONE) return false;
        if (teamChangeLocked) {
            player.sendMessage(ChatColor.RED + "Teamwechsel ist nach Spielstart nicht mehr möglich.");
            return false;
        }
        if (disabledForThisGame.contains(color)) {
            player.sendMessage(ChatColor.RED + "Dieses Team ist deaktiviert.");
            return false;
        }
        leaveTeam(player, false);
        playerTeams.put(player.getUniqueId(), color);
        Team t = scoreboard.getTeam(color.scoreboardName());
        if (t != null) {
            t.addEntry(player.getName());
        }
        player.sendMessage(ChatColor.GRAY + "Du bist " + color.chat() + color.name() + ChatColor.GRAY + " beigetreten.");
        return true;
    }

    public void leaveTeam(Player player, boolean notify) {
        TeamColor previous = playerTeams.remove(player.getUniqueId());
        if (previous != null) {
            Team t = scoreboard.getTeam(previous.scoreboardName());
            if (t != null) {
                t.removeEntry(player.getName());
            }
        }
        if (notify) {
            player.sendMessage(ChatColor.GRAY + "Du hast dein Team verlassen.");
        }
    }

    public boolean areTeammates(Player a, Player b) {
        return getPlayerTeam(a).isPresent() && getPlayerTeam(a).equals(getPlayerTeam(b));
    }

    public boolean everyoneHasATeam() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!isInTeam(p)) return false;
        }
        return true;
    }
}
