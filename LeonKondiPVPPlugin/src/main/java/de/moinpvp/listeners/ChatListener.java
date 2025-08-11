package de.moinpvp.listeners;

import de.moinpvp.team.TeamColor;
import de.moinpvp.team.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    private final TeamManager teamManager;

    public ChatListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player p = event.getPlayer();
        TeamColor color = teamManager.getPlayerTeam(p).orElse(TeamColor.NONE);
        ChatColor cc = color.chat();
        event.setFormat(cc + p.getName() + ChatColor.RESET + ": " + event.getMessage());
    }
}
