package de.moinpvp.commands;

import de.moinpvp.team.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class TeamResetCommand implements CommandExecutor {
    private final TeamManager teamManager;

    public TeamResetCommand(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur als Spieler nutzbar.");
            return true;
        }
        if (teamManager.isTeamChangeLocked()) {
            player.sendMessage(ChatColor.RED + "Teamwechsel ist nach Spielstart nicht mehr möglich.");
            return true;
        }
        teamManager.leaveTeam(player, true);
        return true;
    }
}
