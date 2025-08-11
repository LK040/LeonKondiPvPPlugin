package de.moinpvp.commands;

import de.moinpvp.game.GameManager;
import org.bukkit.command.*;

public class StartGameCommand implements CommandExecutor {
    private final GameManager gameManager;

    public StartGameCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        gameManager.startGame(sender);
        return true;
    }
}
