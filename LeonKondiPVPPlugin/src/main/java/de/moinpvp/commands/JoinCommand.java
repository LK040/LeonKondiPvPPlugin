package de.moinpvp.commands;

import de.moinpvp.team.TeamColor;
import de.moinpvp.team.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;

public class JoinCommand implements CommandExecutor, TabCompleter {
    private final TeamManager teamManager;

    public JoinCommand(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur als Spieler nutzbar.");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Benutzung: /join <teamRot|teamGelb|teamGrün|teamBlau>");
            return true;
        }
        TeamColor color = TeamColor.fromJoinArg(args[0]);
        if (color == null || color == TeamColor.NONE) {
            sender.sendMessage(ChatColor.RED + "Unbekanntes Team.");
            return true;
        }
        teamManager.joinTeam(player, color);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("teamRot", "teamGelb", "teamGrün", "teamBlau");
        }
        return List.of();
    }
}
