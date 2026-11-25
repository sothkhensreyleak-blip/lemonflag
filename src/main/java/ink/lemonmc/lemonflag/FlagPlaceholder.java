package ink.lemonmc.lemonflag;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class FlagPlaceholder extends PlaceholderExpansion {

    private final LemonFlag plugin;

    public FlagPlaceholder(LemonFlag plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "lemon";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Crownixed";
    }

    @Override
    public @NotNull String getVersion() {
        return "3.1.0";
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

        if (params.equalsIgnoreCase("country")) {
            return plugin.getPlayerCountryCode(player.getUniqueId());
        }

        return null;
    }
}
