package de.moinpvp.listeners;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

public class PortalListener implements Listener {
    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        World.Environment to = event.getTo() != null ? event.getTo().getEnvironment() : null;
        if (to == World.Environment.NETHER || to == World.Environment.THE_END) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("Nether und End sind in diesem Spielmodus deaktiviert.");
        }
    }
}
