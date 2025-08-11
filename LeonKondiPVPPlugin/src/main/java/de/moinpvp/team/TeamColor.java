package de.moinpvp.team;

import org.bukkit.ChatColor;

public enum TeamColor {
    RED("team_red", "teamRot", ChatColor.RED),
    YELLOW("team_yellow", "teamGelb", ChatColor.YELLOW),
    BLUE("team_blue", "teamBlau", ChatColor.BLUE),
    GREEN("team_green", "teamGrün", ChatColor.GREEN),
    NONE("none", "keinTeam", ChatColor.WHITE);

    private final String scoreboardName;
    private final String joinKeyword;
    private final ChatColor chatColor;

    TeamColor(String scoreboardName, String joinKeyword, ChatColor chatColor) {
        this.scoreboardName = scoreboardName;
        this.joinKeyword = joinKeyword;
        this.chatColor = chatColor;
    }

    public String scoreboardName() {
        return scoreboardName;
    }

    public String joinKeyword() {
        return joinKeyword;
    }

    public ChatColor chat() {
        return chatColor;
    }

    public static TeamColor fromJoinArg(String arg) {
        if (arg == null) return null;
        String s = arg.toLowerCase();
        for (TeamColor c : values()) {
            if (c == NONE) continue;
            if (s.equalsIgnoreCase(c.name()) || s.equalsIgnoreCase(c.joinKeyword())) {
                return c;
            }
            switch (s) {
                case "teamrot", "rot" -> { return RED; }
                case "teamgelb", "gelb" -> { return YELLOW; }
                case "teamblau", "blau" -> { return BLUE; }
                case "teamgrün", "teamgruen", "grün", "gruen" -> { return GREEN; }
                default -> {}
            }
        }
        return null;
    }
}
