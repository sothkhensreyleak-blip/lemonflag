package com.lemon.flag;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class FlagExpansion extends PlaceholderExpansion {

    private final LemonFlag plugin;

    public FlagExpansion(LemonFlag plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "lemon";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Lemon";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.2";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        if (params.equalsIgnoreCase("flag")) {
            return plugin.getPlayerFlag(player.getUniqueId());
        }

        return null;
    }
}
